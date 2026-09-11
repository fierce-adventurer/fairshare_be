package com.fairshare.request.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreateMoneyRequestRequest(
        UUID groupId,
        @NotNull(message = "Recipient (toUserId) is required")
        UUID toUserId,
        String currency,
        @Min(value = 1, message = "Amount must be at least 1 minor unit")
        long amountMinor,
        @NotBlank(message = "Description is required")
        String description,
        @NotNull(message = "Expiration timestamp is required")
        Instant expiresAt
) {}
