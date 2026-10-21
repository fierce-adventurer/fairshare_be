package com.fairshare.reminder.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

public record CreateReminderRequest(
        @NotBlank String sourceType,
        @NotNull UUID sourceId,
        @NotBlank String expenseDescription,
        String currency,
        @Min(1) long amountMinor,
        UUID payerUserId,
        String payerName,
        String payerMsisdn,
        String payerInstagramId,
        Set<String> channels,
        @NotNull Integer intervalDays
) {}
