package com.fairshare.group.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record GroupBalanceResponse(
        UUID groupId,
        String currency,
        Map<UUID, Long> rawBalances,
        List<DebtResponse> simplifiedDebts
) {}
