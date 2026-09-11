package com.fairshare.admin.dto;

import java.util.Map;

public record DashboardStatsResponse(
        long totalUsers,
        long totalGroups,
        long totalExpenses,
        long totalPayments,
        long totalAmountMinor,
        long totalOverdueBills,
        long totalActiveRequests,
        Map<String, Long> onboardingJourneyDistribution,
        Map<String, Long> recentRumEvents
) {}
