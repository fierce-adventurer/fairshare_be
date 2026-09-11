package com.fairshare.billing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateBillRequest(
        UUID groupId,
        @NotBlank(message = "Description is required")
        String description,
        String currency,
        @Min(value = 1, message = "Amount must be at least 1 minor unit")
        long amountMinor,
        @NotNull(message = "Due date is required")
        LocalDate dueDate,
        String recurrence,
        List<UUID> assignedUserIds
) {}
