package com.fairshare.expense.service;

import com.fairshare.expense.dto.AllocationDto;
import com.fairshare.expense.dto.CreateExpenseRequest;
import com.fairshare.expense.dto.ExpenseResponse;
import com.fairshare.expense.dto.UpdateExpenseRequest;
import com.fairshare.expense.engine.SplitCalculator;
import com.fairshare.expense.model.AllocationType;
import com.fairshare.expense.model.Expense;
import com.fairshare.expense.model.ExpenseAllocation;
import com.fairshare.expense.repository.ExpenseAllocationRepository;
import com.fairshare.expense.repository.ExpenseRepository;
import com.fairshare.group.service.GroupService;
import com.fairshare.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseAllocationRepository allocationRepository;
    private final SplitCalculator splitCalculator;
    private final GroupService groupService;

    public ExpenseService(
            ExpenseRepository expenseRepository,
            ExpenseAllocationRepository allocationRepository,
            SplitCalculator splitCalculator,
            GroupService groupService
    ) {
        this.expenseRepository = expenseRepository;
        this.allocationRepository = allocationRepository;
        this.splitCalculator = splitCalculator;
        this.groupService = groupService;
    }

    @Transactional
    public ExpenseResponse createExpense(UUID userId, UUID groupId, CreateExpenseRequest request) {
        groupService.assertMembership(userId, groupId);

        // Check idempotency key if provided
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            Optional<Expense> existing = expenseRepository.findByGroupIdAndIdempotencyKey(groupId, request.idempotencyKey().trim());
            if (existing.isPresent()) {
                return getExpenseResponse(existing.get());
            }
        }

        // Validate financial invariant: sum(payers) == sum(shares) == total
        validateAllocations(request.amountMinor(), request.payers(), request.shares());

        Expense expense = new Expense(
                UUID.randomUUID(),
                groupId,
                request.description().trim(),
                request.notes() != null ? request.notes().trim() : null,
                request.category() != null ? request.category().trim() : "General",
                request.currency() != null ? request.currency().trim().toUpperCase() : "INR",
                request.amountMinor(),
                request.occurredAt() != null ? request.occurredAt() : Instant.now(),
                userId,
                request.idempotencyKey() != null ? request.idempotencyKey().trim() : null
        );

        Expense savedExpense = expenseRepository.save(expense);
        saveAllocations(savedExpense.getId(), request.payers(), request.shares());

        return getExpenseResponse(savedExpense);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> listGroupExpenses(UUID userId, UUID groupId) {
        groupService.assertMembership(userId, groupId);
        List<Expense> expenses = expenseRepository.findByGroupIdAndDeletedAtIsNullOrderByOccurredAtDesc(groupId);
        return toExpenseResponses(expenses);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> listAllUserExpenses(UUID userId) {
        List<Expense> expenses = expenseRepository.findAllForUser(userId);
        return toExpenseResponses(expenses);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpense(UUID userId, UUID expenseId) {
        Expense expense = findExpenseById(expenseId);
        groupService.assertMembership(userId, expense.getGroupId());
        return getExpenseResponse(expense);
    }

    @Transactional
    public ExpenseResponse updateExpense(UUID userId, UUID expenseId, UpdateExpenseRequest request) {
        Expense expense = findExpenseById(expenseId);
        groupService.assertMembership(userId, expense.getGroupId());

        validateAllocations(request.amountMinor(), request.payers(), request.shares());

        expense.setDescription(request.description().trim());
        expense.setNotes(request.notes() != null ? request.notes().trim() : null);
        if (request.category() != null) expense.setCategory(request.category().trim());
        if (request.currency() != null) expense.setCurrency(request.currency().trim().toUpperCase());
        expense.setAmountMinor(request.amountMinor());
        if (request.occurredAt() != null) expense.setOccurredAt(request.occurredAt());

        Expense saved = expenseRepository.save(expense);

        // Replace allocations
        allocationRepository.deleteByExpenseId(expenseId);
        saveAllocations(expenseId, request.payers(), request.shares());

        return getExpenseResponse(saved);
    }

    @Transactional
    public void deleteExpense(UUID userId, UUID expenseId) {
        Expense expense = findExpenseById(expenseId);
        groupService.assertMembership(userId, expense.getGroupId());
        expense.setDeletedAt(Instant.now());
        expenseRepository.save(expense);
    }

    private void validateAllocations(long totalMinor, List<AllocationDto> payers, List<AllocationDto> shares) {
        List<SplitCalculator.AllocationResult> payerResults = payers.stream()
                .map(p -> new SplitCalculator.AllocationResult(p.userId(), p.amountMinor()))
                .toList();
        List<SplitCalculator.AllocationResult> shareResults = shares.stream()
                .map(s -> new SplitCalculator.AllocationResult(s.userId(), s.amountMinor()))
                .toList();

        splitCalculator.validateExpenseInvariant(totalMinor, payerResults, shareResults);
    }

    private void saveAllocations(UUID expenseId, List<AllocationDto> payers, List<AllocationDto> shares) {
        List<ExpenseAllocation> allocations = new ArrayList<>();
        for (AllocationDto payer : payers) {
            allocations.add(new ExpenseAllocation(expenseId, payer.userId(), AllocationType.PAYER, payer.amountMinor()));
        }
        for (AllocationDto share : shares) {
            allocations.add(new ExpenseAllocation(expenseId, share.userId(), AllocationType.SHARER, share.amountMinor()));
        }
        allocationRepository.saveAll(allocations);
    }

    private List<ExpenseResponse> toExpenseResponses(List<Expense> expenses) {
        if (expenses.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> expenseIds = expenses.stream().map(Expense::getId).toList();
        List<ExpenseAllocation> allAllocations = allocationRepository.findByExpenseIdIn(expenseIds);
        Map<UUID, List<ExpenseAllocation>> allocationsByExpenseId = allAllocations.stream()
                .collect(Collectors.groupingBy(ExpenseAllocation::getExpenseId));

        return expenses.stream().map(expense -> {
            List<ExpenseAllocation> allocations = allocationsByExpenseId.getOrDefault(expense.getId(), Collections.emptyList());
            List<AllocationDto> payers = allocations.stream()
                    .filter(a -> a.getType() == AllocationType.PAYER)
                    .map(a -> new AllocationDto(a.getUserId(), a.getAmountMinor()))
                    .toList();
            List<AllocationDto> shares = allocations.stream()
                    .filter(a -> a.getType() == AllocationType.SHARER)
                    .map(a -> new AllocationDto(a.getUserId(), a.getAmountMinor()))
                    .toList();
            return ExpenseResponse.from(expense, payers, shares);
        }).toList();
    }

    private ExpenseResponse getExpenseResponse(Expense expense) {
        List<ExpenseAllocation> allocations = allocationRepository.findByExpenseId(expense.getId());
        List<AllocationDto> payers = allocations.stream()
                .filter(a -> a.getType() == AllocationType.PAYER)
                .map(a -> new AllocationDto(a.getUserId(), a.getAmountMinor()))
                .toList();
        List<AllocationDto> shares = allocations.stream()
                .filter(a -> a.getType() == AllocationType.SHARER)
                .map(a -> new AllocationDto(a.getUserId(), a.getAmountMinor()))
                .toList();

        return ExpenseResponse.from(expense, payers, shares);
    }

    private Expense findExpenseById(UUID expenseId) {
        return expenseRepository.findById(expenseId)
                .filter(e -> e.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found: " + expenseId));
    }
}
