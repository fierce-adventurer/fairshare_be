package com.fairshare.activity.repository;

import com.fairshare.activity.model.ActivityEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityEventRepository extends JpaRepository<ActivityEvent, UUID> {
    List<ActivityEvent> findByGroupIdOrderByCreatedAtDesc(UUID groupId, Pageable pageable);
    List<ActivityEvent> findByActorIdOrderByCreatedAtDesc(UUID actorId, Pageable pageable);

    @Query("SELECT a FROM ActivityEvent a WHERE a.groupId IN " +
           "(SELECT gm.groupId FROM GroupMember gm WHERE gm.userId = :userId) ORDER BY a.createdAt DESC")
    List<ActivityEvent> findAllForUser(@Param("userId") UUID userId, Pageable pageable);
}
