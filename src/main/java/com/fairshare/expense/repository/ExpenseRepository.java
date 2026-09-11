package com.fairshare.expense.repository;

import com.fairshare.expense.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    List<Expense> findByGroupIdAndDeletedAtIsNullOrderByOccurredAtDesc(UUID groupId);

    @Query("SELECT e FROM Expense e WHERE e.deletedAt IS NULL AND e.groupId IN " +
           "(SELECT gm.groupId FROM GroupMember gm WHERE gm.userId = :userId) ORDER BY e.occurredAt DESC")
    List<Expense> findAllForUser(@Param("userId") UUID userId);

    Optional<Expense> findByGroupIdAndIdempotencyKey(UUID groupId, String idempotencyKey);
}
