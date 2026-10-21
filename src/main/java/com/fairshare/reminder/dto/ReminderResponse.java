package com.fairshare.reminder.dto;

import com.fairshare.reminder.model.PaymentReminder;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReminderResponse(UUID id, String sourceType, UUID sourceId, String expenseDescription, String currency,
        long amountMinor, UUID payerUserId, String payerName, String payerMsisdn, String payerInstagramId,
        String paymentMode, String paymentInstructions,
        List<String> channels, int intervalDays, String status,
        Instant nextSendAt, Instant lastSentAt, String lastDeliveryStatus, Instant createdAt, Instant updatedAt) {
    public static ReminderResponse from(PaymentReminder r){return new ReminderResponse(r.getId(),r.getSourceType(),r.getSourceId(),r.getExpenseDescription(),r.getCurrency(),r.getAmountMinor(),r.getPayerUserId(),r.getPayerName(),r.getPayerMsisdn(),r.getPayerInstagramId(),r.getPaymentMode(),r.getPaymentInstructions(),r.getChannels(),r.getIntervalDays(),r.getStatus(),r.getNextSendAt(),r.getLastSentAt(),r.getLastDeliveryStatus(),r.getCreatedAt(),r.getUpdatedAt());}
}
