package com.fairshare.admin.service;

import com.fairshare.admin.dto.DashboardStatsResponse;
import com.fairshare.admin.dto.RumMetricRequest;
import com.fairshare.billing.repository.UpcomingBillRepository;
import com.fairshare.expense.repository.ExpenseRepository;
import com.fairshare.group.repository.GroupRepository;
import com.fairshare.payment.repository.PaymentRepository;
import com.fairshare.request.repository.MoneyRequestRepository;
import com.fairshare.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class RumAnalyticsService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final ExpenseRepository expenseRepository;
    private final PaymentRepository paymentRepository;
    private final UpcomingBillRepository billRepository;
    private final MoneyRequestRepository requestRepository;

    private final Map<String, AtomicLong> rumEventCounters = new ConcurrentHashMap<>();

    public RumAnalyticsService(
            UserRepository userRepository,
            GroupRepository groupRepository,
            ExpenseRepository expenseRepository,
            PaymentRepository paymentRepository,
            UpcomingBillRepository billRepository,
            MoneyRequestRepository requestRepository
    ) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.expenseRepository = expenseRepository;
        this.paymentRepository = paymentRepository;
        this.billRepository = billRepository;
        this.requestRepository = requestRepository;
    }

    public void recordRumEvent(RumMetricRequest metric) {
        String key = metric.eventName() != null ? metric.eventName() : "general_event";
        rumEventCounters.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalGroups = groupRepository.count();
        long totalExpenses = expenseRepository.count();
        long totalPayments = paymentRepository.count();

        long totalAmountMinor = expenseRepository.sumActiveAmountMinor();

        Map<String, Long> onboardingDistribution = new HashMap<>();
        for (Object[] row : userRepository.countUsersByOnboardingStep()) {
            if (row != null && row.length >= 2) {
                String step = row[0] != null ? row[0].toString() : "welcome";
                long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
                onboardingDistribution.put(step, count);
            }
        }

        Map<String, Long> rumStats = rumEventCounters.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));

        return new DashboardStatsResponse(
                totalUsers,
                totalGroups,
                totalExpenses,
                totalPayments,
                totalAmountMinor,
                0, // overdue bills
                0, // active requests
                onboardingDistribution,
                rumStats
        );
    }
}
