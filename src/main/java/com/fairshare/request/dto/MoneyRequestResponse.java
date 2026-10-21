package com.fairshare.request.dto;

import com.fairshare.request.model.MoneyRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MoneyRequestResponse(
        UUID id,
        UUID groupId,
        UUID fromUserId,
        UUID toUserId,
        UUID contactId,
        String payerName,
        String payerMsisdn,
        UUID expenseId,
        Integer reminderIntervalDays,
        List<String> reminderChannels,
        String payerInstagramId,
        String paymentMode,
        String paymentInstructions,
        String paymentQrData,
        String currency,
        long amountMinor,
        String description,
        Instant expiresAt,
        String status,
        boolean isExpired,
        String settlementTransactionId,
        boolean hasSettlementProof,
        Instant settlementSubmittedAt,
        Instant settlementVerifiedAt,
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
                request.getContactId(),
                request.getPayerName(),
                request.getPayerMsisdn(),
                request.getExpenseId(),
                request.getReminderIntervalDays(),
                request.getReminderChannels(),
                request.getPayerInstagramId(),
                request.getPaymentMode(),
                request.getPaymentInstructions(),
                request.getPaymentQrData(),
                request.getCurrency(),
                request.getAmountMinor(),
                request.getDescription(),
                request.getExpiresAt(),
                expired ? "expired" : request.getStatus(),
                expired,
                request.getSettlementTransactionId(),
                request.hasSettlementProof(),
                request.getSettlementSubmittedAt(),
                request.getSettlementVerifiedAt(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
