package com.fairshare.payment.dto;

import com.fairshare.payment.model.Payment;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID groupId,
        UUID fromUserId,
        UUID toUserId,
        String currency,
        long amountMinor,
        String note,
        Instant occurredAt,
        Instant createdAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getGroupId(),
                payment.getFromUserId(),
                payment.getToUserId(),
                payment.getCurrency(),
                payment.getAmountMinor(),
                payment.getNote(),
                payment.getOccurredAt(),
                payment.getCreatedAt()
        );
    }
}
