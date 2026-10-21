package com.fairshare.request.service;

import com.fairshare.request.dto.CreateMoneyRequestRequest;
import com.fairshare.request.dto.MoneyRequestResponse;
import com.fairshare.request.model.MoneyRequest;
import com.fairshare.request.repository.MoneyRequestRepository;
import com.fairshare.reminder.service.ReminderService;
import com.fairshare.shared.exception.BadRequestException;
import com.fairshare.shared.exception.ResourceNotFoundException;
import com.fairshare.shared.exception.UnauthorizedException;
import com.fairshare.user.model.User;
import com.fairshare.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class MoneyRequestService {

    private final MoneyRequestRepository requestRepository;
    private final ReminderService reminderService;
    private final UserRepository userRepository;

    public MoneyRequestService(MoneyRequestRepository requestRepository, ReminderService reminderService, UserRepository userRepository) {
        this.requestRepository = requestRepository;
        this.reminderService = reminderService;
        this.userRepository = userRepository;
    }

    @Transactional
    public MoneyRequestResponse createRequest(UUID fromUserId, CreateMoneyRequestRequest request) {
        if (request.toUserId() == null
                && (request.payerMsisdn() == null || request.payerMsisdn().isBlank())
                && (request.payerInstagramId() == null || request.payerInstagramId().isBlank())) {
            throw new BadRequestException("Choose a Fairshare payer or provide a WhatsApp number or Instagram recipient.");
        }
        if (fromUserId.equals(request.toUserId())) {
            throw new BadRequestException("You cannot request money from yourself.");
        }

        if (request.reminderIntervalDays() != null && !Set.of(7, 10, 15, 30).contains(request.reminderIntervalDays())) {
            throw new BadRequestException("Reminder interval must be 7, 10, 15, or 30 days.");
        }

        if (request.expiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Expiration date must be in the future.");
        }

        User sender = userRepository.findById(fromUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Requester account not found."));
        PaymentDestination payment = resolvePaymentDestination(sender, request.paymentMode());

        MoneyRequest moneyRequest = new MoneyRequest(
                UUID.randomUUID(),
                request.groupId(),
                fromUserId,
                request.toUserId(),
                request.contactId(),
                request.payerName(),
                request.payerMsisdn(),
                request.expenseId(),
                request.reminderIntervalDays(),
                request.reminderChannels(),
                request.payerInstagramId(),
                payment.mode(),
                payment.instructions(),
                payment.qrData(),
                request.currency() != null ? request.currency().trim().toUpperCase() : "INR",
                request.amountMinor(),
                request.description().trim(),
                request.expiresAt()
        );

        MoneyRequest saved = requestRepository.save(moneyRequest);
        if (request.reminderIntervalDays() != null) {
            reminderService.createForRequest(fromUserId, saved.getId(), saved.getDescription(), saved.getCurrency(), saved.getAmountMinor(),
                    saved.getToUserId(), saved.getPayerName(), saved.getPayerMsisdn(), saved.getPayerInstagramId(),
                    new java.util.LinkedHashSet<>(saved.getReminderChannels()), saved.getReminderIntervalDays(),
                    saved.getPaymentMode(), saved.getPaymentInstructions());
        }
        return MoneyRequestResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<MoneyRequestResponse> listUserRequests(UUID userId) {
        return requestRepository.findByToUserIdOrFromUserIdOrderByCreatedAtDesc(userId, userId)
                .stream()
                .map(MoneyRequestResponse::from)
                .toList();
    }

    @Transactional
    public MoneyRequestResponse settleRequest(UUID userId, UUID requestId) {
        MoneyRequest request = findRequestById(requestId);
        if (request.getToUserId() != null) {
            throw new BadRequestException("Registered payers must submit payment proof, which the requester verifies before settlement.");
        }
        if (!request.getFromUserId().equals(userId)) {
            throw new UnauthorizedException("Only the requester can settle an external payer request.");
        }

        request.setStatus("settled");
        request.setSettlementVerifiedAt(Instant.now());
        MoneyRequest saved = requestRepository.save(request);
        reminderService.changeStatusForSource(request.getFromUserId(), "request", request.getId(), "settled");
        return MoneyRequestResponse.from(saved);
    }

    @Transactional
    public MoneyRequestResponse submitSettlementProof(UUID payerId, UUID requestId, String transactionId, MultipartFile proof) {
        MoneyRequest request = findRequestById(requestId);
        if (request.getToUserId() == null || !request.getToUserId().equals(payerId)) {
            throw new UnauthorizedException("Only the registered payer can submit proof for this request.");
        }
        if (!"open".equals(request.getStatus())) throw new BadRequestException("This request is not open for payment proof.");
        String reference = transactionId == null ? "" : transactionId.trim();
        if (reference.length() < 4 || reference.length() > 128) throw new BadRequestException("Enter a valid transaction ID between 4 and 128 characters.");
        if (proof == null || proof.isEmpty()) throw new BadRequestException("Attach a payment screenshot.");
        if (proof.getSize() > 5L * 1024L * 1024L) throw new BadRequestException("Payment screenshots must be smaller than 5 MB.");
        String mimeType = proof.getContentType() == null ? "" : proof.getContentType().toLowerCase(Locale.ROOT);
        if (!Set.of("image/png", "image/jpeg", "image/webp").contains(mimeType)) {
            throw new BadRequestException("Payment proof must be a PNG, JPEG, or WebP image.");
        }
        try {
            request.setSettlementTransactionId(reference);
            request.setSettlementProofData(proof.getBytes());
            request.setSettlementProofMimeType(mimeType);
            request.setSettlementSubmittedAt(Instant.now());
            request.setStatus("proof_submitted");
            MoneyRequest saved = requestRepository.save(request);
            reminderService.changeStatusForSource(request.getFromUserId(), "request", request.getId(), "paused");
            return MoneyRequestResponse.from(saved);
        } catch (IOException exception) {
            throw new BadRequestException("The payment screenshot could not be read.");
        }
    }

    @Transactional(readOnly = true)
    public SettlementProof settlementProof(UUID userId, UUID requestId) {
        MoneyRequest request = findRequestById(requestId);
        if (!request.getFromUserId().equals(userId) && (request.getToUserId() == null || !request.getToUserId().equals(userId))) {
            throw new UnauthorizedException("You do not have access to this payment proof.");
        }
        if (!request.hasSettlementProof()) throw new ResourceNotFoundException("Payment proof is unavailable.");
        return new SettlementProof(request.getSettlementProofData(), request.getSettlementProofMimeType());
    }

    @Transactional
    public MoneyRequestResponse verifySettlement(UUID requesterId, UUID requestId) {
        MoneyRequest request = findRequestById(requestId);
        if (!request.getFromUserId().equals(requesterId)) throw new UnauthorizedException("Only the requester can verify this payment.");
        if (!"proof_submitted".equals(request.getStatus()) || !request.hasSettlementProof()) {
            throw new BadRequestException("The payer has not submitted payment proof.");
        }
        request.setStatus("settled");
        request.setSettlementVerifiedAt(Instant.now());
        MoneyRequest saved = requestRepository.save(request);
        reminderService.changeStatusForSource(request.getFromUserId(), "request", request.getId(), "settled");
        return MoneyRequestResponse.from(saved);
    }

    @Transactional
    public MoneyRequestResponse cancelRequest(UUID userId, UUID requestId) {
        MoneyRequest request = findRequestById(requestId);
        if (!request.getFromUserId().equals(userId)) {
            throw new UnauthorizedException("Only the requester can cancel this money request.");
        }

        request.setStatus("cancelled");
        MoneyRequest saved = requestRepository.save(request);
        reminderService.changeStatusForSource(request.getFromUserId(), "request", request.getId(), "settled");
        return MoneyRequestResponse.from(saved);
    }

    private PaymentDestination resolvePaymentDestination(User sender, String requestedMode) {
        String mode = requestedMode == null ? "" : requestedMode.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("UPI", "BANK", "QR").contains(mode)) {
            throw new BadRequestException("Choose UPI, bank transfer, or payment QR for this request.");
        }
        return switch (mode) {
            case "UPI" -> {
                if (sender.getUpiId() == null || sender.getUpiId().isBlank()) throw new BadRequestException("Add a UPI ID to your profile first.");
                yield new PaymentDestination(mode, "UPI ID: " + sender.getUpiId(), null);
            }
            case "BANK" -> {
                if (sender.getBankAccountName() == null || sender.getBankAccountNumber() == null || sender.getBankIfsc() == null) {
                    throw new BadRequestException("Add complete bank transfer details to your profile first.");
                }
                yield new PaymentDestination(mode, "Account name: " + sender.getBankAccountName()
                        + " · Account number: " + sender.getBankAccountNumber() + " · IFSC: " + sender.getBankIfsc(), null);
            }
            case "QR" -> {
                if (sender.getPaymentQrData() == null || sender.getPaymentQrData().isBlank()) throw new BadRequestException("Add a payment QR to your profile first.");
                yield new PaymentDestination(mode, "Use the Fairshare payment QR attached to this request", sender.getPaymentQrData());
            }
            default -> throw new BadRequestException("Unsupported payment method.");
        };
    }

    private MoneyRequest findRequestById(UUID id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Money request not found with id: " + id));
    }

    private record PaymentDestination(String mode, String instructions, String qrData) {}
    public record SettlementProof(byte[] data, String mimeType) {}
}
