package com.fairshare.request.repository;

import com.fairshare.request.model.MoneyRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MoneyRequestRepository extends JpaRepository<MoneyRequest, UUID> {
    List<MoneyRequest> findByToUserIdOrFromUserIdOrderByCreatedAtDesc(UUID toUserId, UUID fromUserId);

    @Modifying
    @Query("UPDATE MoneyRequest r SET r.status = 'expired', r.updatedAt = :now WHERE r.expiresAt < :now AND r.status = 'open'")
    int expireOldRequests(@Param("now") Instant now);
}
