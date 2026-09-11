package com.fairshare.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record RecordPaymentRequest(
        @NotNull(message = "Payer (fromUserId) is required")
        UUID fromUserId,

        @NotNull(message = "Recipient (toUserId) is required")
        UUID toUserId,

        String currency,

        @Min(value = 1, message = "Payment amount must be at least 1 minor unit")
        long amountMinor,

        String note,
        Instant occurredAt
) {}
