package com.fairshare.expense.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.Instant;
import java.util.List;

public record CreateExpenseRequest(
        @NotBlank(message = "Description is required")
        String description,
        String notes,
        String category,
        String currency,
        @Min(value = 1, message = "Amount must be at least 1 minor unit")
        long amountMinor,
        Instant occurredAt,
        @NotEmpty(message = "At least one payer is required")
        List<AllocationDto> payers,
        @NotEmpty(message = "At least one sharer is required")
        List<AllocationDto> shares,
        String idempotencyKey
) {}
