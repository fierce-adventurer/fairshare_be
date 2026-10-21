package com.fairshare.request.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record CreateMoneyRequestRequest(
        UUID groupId,
        UUID toUserId,
        UUID contactId,
        String payerName,
        String payerMsisdn,
        UUID expenseId,
        Integer reminderIntervalDays,
        Set<String> reminderChannels,
        String payerInstagramId,
        String paymentMode,
        String currency,
        @Min(value = 1, message = "Amount must be at least 1 minor unit")
        long amountMinor,
        @NotBlank(message = "Description is required")
        String description,
        @NotNull(message = "Expiration timestamp is required")
        Instant expiresAt
) {}
