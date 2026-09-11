package com.fairshare.billing.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class BillAssigneeId implements Serializable {
    private UUID billId;
    private UUID userId;

    public BillAssigneeId() {}

    public BillAssigneeId(UUID billId, UUID userId) {
        this.billId = billId;
        this.userId = userId;
    }

    public UUID getBillId() { return billId; }
    public void setBillId(UUID billId) { this.billId = billId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BillAssigneeId that)) return false;
        return Objects.equals(billId, that.billId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(billId, userId);
    }
}
