package com.fairshare.billing.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "upcoming_bills")
public class UpcomingBill {

    @Id
    private UUID id;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, length = 20)
    private String recurrence = "once"; // once, weekly, monthly, yearly

    @Column(nullable = false, length = 20)
    private String status = "pending"; // pending, paid, overdue

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UpcomingBill() {
    }

    public UpcomingBill(
            UUID id,
            UUID groupId,
            String description,
            String currency,
            long amountMinor,
            LocalDate dueDate,
            String recurrence,
            String status,
            UUID createdBy
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.groupId = groupId;
        this.description = description;
        this.currency = currency != null ? currency : "INR";
        this.amountMinor = amountMinor;
        this.dueDate = dueDate;
        this.recurrence = recurrence != null ? recurrence : "once";
        this.status = status != null ? status : "pending";
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getGroupId() { return groupId; }
    public void setGroupId(UUID groupId) { this.groupId = groupId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public long getAmountMinor() { return amountMinor; }
    public void setAmountMinor(long amountMinor) { this.amountMinor = amountMinor; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getRecurrence() { return recurrence; }
    public void setRecurrence(String recurrence) { this.recurrence = recurrence; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
