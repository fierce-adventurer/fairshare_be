package com.fairshare.payment.service;

import com.fairshare.group.service.GroupService;
import com.fairshare.payment.dto.PaymentResponse;
import com.fairshare.payment.dto.RecordPaymentRequest;
import com.fairshare.payment.model.Payment;
import com.fairshare.payment.repository.PaymentRepository;
import com.fairshare.shared.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final GroupService groupService;

    public PaymentService(PaymentRepository paymentRepository, GroupService groupService) {
        this.paymentRepository = paymentRepository;
        this.groupService = groupService;
    }

    @Transactional
    public PaymentResponse recordPayment(UUID currentUserId, UUID groupId, RecordPaymentRequest request) {
        groupService.assertMembership(currentUserId, groupId);

        if (request.fromUserId().equals(request.toUserId())) {
            throw new BadRequestException("Payer and recipient cannot be the same user.");
        }

        groupService.assertMembership(request.fromUserId(), groupId);
        groupService.assertMembership(request.toUserId(), groupId);

        Payment payment = new Payment(
                UUID.randomUUID(),
                groupId,
                request.fromUserId(),
                request.toUserId(),
                request.currency() != null ? request.currency().trim().toUpperCase() : "INR",
                request.amountMinor(),
                request.note() != null ? request.note().trim() : null,
                request.occurredAt() != null ? request.occurredAt() : Instant.now()
        );

        Payment saved = paymentRepository.save(payment);
        return PaymentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listGroupPayments(UUID currentUserId, UUID groupId) {
        groupService.assertMembership(currentUserId, groupId);
        return paymentRepository.findByGroupIdOrderByOccurredAtDesc(groupId)
                .stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Transactional
    public void deletePayment(UUID currentUserId, UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BadRequestException("Payment not found with id: " + paymentId));
        groupService.assertMembership(currentUserId, payment.getGroupId());
        paymentRepository.delete(payment);
    }
}
