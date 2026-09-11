package com.fairshare.request.dto;

import com.fairshare.request.model.MoneyRequest;
import java.time.Instant;
import java.util.UUID;

public record MoneyRequestResponse(
        UUID id,
        UUID groupId,
        UUID fromUserId,
        UUID toUserId,
        String currency,
        long amountMinor,
        String description,
        Instant expiresAt,
        String status,
        boolean isExpired,
        Instant createdAt,
        Instant updatedAt
) {
    public static MoneyRequestResponse from(MoneyRequest request) {
        boolean expired = "expired".equals(request.getStatus()) ||
                ("open".equals(request.getStatus()) && request.getExpiresAt().isBefore(Instant.now()));

        return new MoneyRequestResponse(
                request.getId(),
                request.getGroupId(),
                request.getFromUserId(),
                request.getToUserId(),
                request.getCurrency(),
                request.getAmountMinor(),
                request.getDescription(),
                request.getExpiresAt(),
                expired ? "expired" : request.getStatus(),
                expired,
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
