package com.fairshare.request.service;

import com.fairshare.request.dto.CreateMoneyRequestRequest;
import com.fairshare.request.dto.MoneyRequestResponse;
import com.fairshare.request.model.MoneyRequest;
import com.fairshare.request.repository.MoneyRequestRepository;
import com.fairshare.shared.exception.BadRequestException;
import com.fairshare.shared.exception.ResourceNotFoundException;
import com.fairshare.shared.exception.UnauthorizedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MoneyRequestService {

    private final MoneyRequestRepository requestRepository;

    public MoneyRequestService(MoneyRequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    @Transactional
    public MoneyRequestResponse createRequest(UUID fromUserId, CreateMoneyRequestRequest request) {
        if (fromUserId.equals(request.toUserId())) {
            throw new BadRequestException("You cannot request money from yourself.");
        }

        if (request.expiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Expiration date must be in the future.");
        }

        MoneyRequest moneyRequest = new MoneyRequest(
                UUID.randomUUID(),
                request.groupId(),
                fromUserId,
                request.toUserId(),
                request.currency() != null ? request.currency().trim().toUpperCase() : "INR",
                request.amountMinor(),
                request.description().trim(),
                request.expiresAt()
        );

        MoneyRequest saved = requestRepository.save(moneyRequest);
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
        if (!request.getToUserId().equals(userId) && !request.getFromUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to settle this request.");
        }

        request.setStatus("settled");
        MoneyRequest saved = requestRepository.save(request);
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
        return MoneyRequestResponse.from(saved);
    }

    private MoneyRequest findRequestById(UUID id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Money request not found with id: " + id));
    }
}
