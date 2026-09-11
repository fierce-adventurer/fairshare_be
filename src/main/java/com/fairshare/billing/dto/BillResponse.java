package com.fairshare.billing.dto;

import com.fairshare.billing.model.UpcomingBill;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BillResponse(
        UUID id,
        UUID groupId,
        String description,
        String currency,
        long amountMinor,
        LocalDate dueDate,
        String recurrence,
        String status,
        UUID createdBy,
        List<UUID> assignedUserIds,
        Instant createdAt,
        Instant updatedAt
) {
    public static BillResponse from(UpcomingBill bill, List<UUID> assignedUserIds) {
        return new BillResponse(
                bill.getId(),
                bill.getGroupId(),
                bill.getDescription(),
                bill.getCurrency(),
                bill.getAmountMinor(),
                bill.getDueDate(),
                bill.getRecurrence(),
                bill.getStatus(),
                bill.getCreatedBy(),
                assignedUserIds,
                bill.getCreatedAt(),
                bill.getUpdatedAt()
        );
    }
}
