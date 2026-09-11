package com.fairshare.expense.repository;

import com.fairshare.expense.model.ExpenseAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseAllocationRepository extends JpaRepository<ExpenseAllocation, UUID> {
    List<ExpenseAllocation> findByExpenseId(UUID expenseId);
    List<ExpenseAllocation> findByExpenseIdIn(List<UUID> expenseIds);
    void deleteByExpenseId(UUID expenseId);
}
