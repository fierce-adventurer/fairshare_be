package com.fairshare.expense.engine;

import com.fairshare.expense.model.AllocationType;
import com.fairshare.expense.model.Expense;
import com.fairshare.expense.model.ExpenseAllocation;
import com.fairshare.expense.repository.ExpenseAllocationRepository;
import com.fairshare.expense.repository.ExpenseRepository;
import com.fairshare.group.dto.DebtResponse;
import com.fairshare.group.dto.GroupBalanceResponse;
import com.fairshare.group.model.GroupMember;
import com.fairshare.group.repository.GroupMemberRepository;
import com.fairshare.payment.model.Payment;
import com.fairshare.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class BalanceService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseAllocationRepository allocationRepository;
    private final PaymentRepository paymentRepository;
    private final GroupMemberRepository memberRepository;
    private final DebtSimplifier debtSimplifier;

    public BalanceService(
            ExpenseRepository expenseRepository,
            ExpenseAllocationRepository allocationRepository,
            PaymentRepository paymentRepository,
            GroupMemberRepository memberRepository,
            DebtSimplifier debtSimplifier
    ) {
        this.expenseRepository = expenseRepository;
        this.allocationRepository = allocationRepository;
        this.paymentRepository = paymentRepository;
        this.memberRepository = memberRepository;
        this.debtSimplifier = debtSimplifier;
    }

    @Transactional(readOnly = true)
    public GroupBalanceResponse calculateGroupBalances(UUID groupId, String currency) {
        List<UUID> memberIds = memberRepository.findByGroupId(groupId).stream()
                .map(GroupMember::getUserId)
                .toList();

        Map<UUID, Long> balances = new LinkedHashMap<>();
        for (UUID id : memberIds) {
            balances.put(id, 0L);
        }

        // Expenses
        List<Expense> expenses = expenseRepository.findByGroupIdAndDeletedAtIsNullOrderByOccurredAtDesc(groupId)
                .stream()
                .filter(e -> currency == null || e.getCurrency().equalsIgnoreCase(currency))
                .toList();

        List<UUID> expenseIds = expenses.stream().map(Expense::getId).toList();
        if (!expenseIds.isEmpty()) {
            List<ExpenseAllocation> allocations = allocationRepository.findByExpenseIdIn(expenseIds);
            for (ExpenseAllocation alloc : allocations) {
                long current = balances.getOrDefault(alloc.getUserId(), 0L);
                if (alloc.getType() == AllocationType.PAYER) {
                    balances.put(alloc.getUserId(), current + alloc.getAmountMinor());
                } else if (alloc.getType() == AllocationType.SHARER) {
                    balances.put(alloc.getUserId(), current - alloc.getAmountMinor());
                }
            }
        }

        // Payments (settlements)
        List<Payment> payments = paymentRepository.findByGroupIdOrderByOccurredAtDesc(groupId)
                .stream()
                .filter(p -> currency == null || p.getCurrency().equalsIgnoreCase(currency))
                .toList();

        for (Payment payment : payments) {
            balances.merge(payment.getFromUserId(), payment.getAmountMinor(), Long::sum);
            balances.merge(payment.getToUserId(), -payment.getAmountMinor(), Long::sum);
        }

        List<DebtResponse> debts = debtSimplifier.simplify(balances);

        return new GroupBalanceResponse(
                groupId,
                currency != null ? currency : "INR",
                balances,
                debts
        );
    }
}
