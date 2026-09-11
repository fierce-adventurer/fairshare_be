package com.fairshare.billing.repository;

import com.fairshare.billing.model.UpcomingBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface UpcomingBillRepository extends JpaRepository<UpcomingBill, UUID> {
    @Query("SELECT b FROM UpcomingBill b JOIN BillAssignee ba ON b.id = ba.billId WHERE ba.userId = :userId ORDER BY b.dueDate ASC")
    List<UpcomingBill> findByAssignedUserId(@Param("userId") UUID userId);

    List<UpcomingBill> findByGroupIdOrderByDueDateAsc(UUID groupId);

    @Modifying
    @Query("UPDATE UpcomingBill b SET b.status = 'overdue', b.updatedAt = :now WHERE b.dueDate < :today AND b.status = 'pending'")
    int markOverdueBills(@Param("today") LocalDate today, @Param("now") Instant now);
}
