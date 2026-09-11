package com.fairshare.billing.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "bill_assignees")
@IdClass(BillAssigneeId.class)
public class BillAssignee {

    @Id
    @Column(name = "bill_id", nullable = false)
    private UUID billId;

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    public BillAssignee() {}

    public BillAssignee(UUID billId, UUID userId) {
        this.billId = billId;
        this.userId = userId;
    }

    public UUID getBillId() { return billId; }
    public void setBillId(UUID billId) { this.billId = billId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
}
