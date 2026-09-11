package com.fairshare.group.dto;

import java.util.UUID;

public record DebtResponse(
        UUID fromUserId,
        UUID toUserId,
        long amountMinor
) {}
