package com.fairshare.expense.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AllocationDto(
        @NotNull(message = "User ID is required in allocation")
        UUID userId,
        long amountMinor
) {}
