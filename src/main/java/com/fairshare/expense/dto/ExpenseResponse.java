package com.fairshare.expense.dto;

import com.fairshare.expense.model.Expense;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        UUID groupId,
        String description,
        String notes,
        String category,
        String currency,
        long amountMinor,
        Instant occurredAt,
        UUID createdBy,
        List<AllocationDto> payers,
        List<AllocationDto> shares,
        Instant createdAt,
        Instant updatedAt
) {
    public static ExpenseResponse from(Expense expense, List<AllocationDto> payers, List<AllocationDto> shares) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getGroupId(),
                expense.getDescription(),
                expense.getNotes(),
                expense.getCategory(),
                expense.getCurrency(),
                expense.getAmountMinor(),
                expense.getOccurredAt(),
                expense.getCreatedBy(),
                payers,
                shares,
                expense.getCreatedAt(),
                expense.getUpdatedAt()
        );
    }
}
