package com.fairshare.expense.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "expense_allocations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_allocation", columnNames = {"expense_id", "user_id", "type"})
})
public class ExpenseAllocation {

    @Id
    private UUID id;

    @Column(name = "expense_id", nullable = false)
    private UUID expenseId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AllocationType type;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    public ExpenseAllocation() {
    }

    public ExpenseAllocation(UUID expenseId, UUID userId, AllocationType type, long amountMinor) {
        this.id = UUID.randomUUID();
        this.expenseId = expenseId;
        this.userId = userId;
        this.type = type;
        this.amountMinor = amountMinor;
    }

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getExpenseId() { return expenseId; }
    public void setExpenseId(UUID expenseId) { this.expenseId = expenseId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public AllocationType getType() { return type; }
    public void setType(AllocationType type) { this.type = type; }
    public long getAmountMinor() { return amountMinor; }
    public void setAmountMinor(long amountMinor) { this.amountMinor = amountMinor; }
}
