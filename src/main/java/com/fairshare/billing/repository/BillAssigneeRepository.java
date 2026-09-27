package com.fairshare.billing.repository;

import com.fairshare.billing.model.BillAssignee;
import com.fairshare.billing.model.BillAssigneeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BillAssigneeRepository extends JpaRepository<BillAssignee, BillAssigneeId> {
    List<BillAssignee> findByBillId(UUID billId);
    List<BillAssignee> findByBillIdIn(List<UUID> billIds);
    void deleteByBillId(UUID billId);
}
