package com.fairshare.group.repository;

import com.fairshare.group.model.GroupMember;
import com.fairshare.group.model.GroupMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {
    List<GroupMember> findByGroupId(UUID groupId);
    List<GroupMember> findByGroupIdIn(List<UUID> groupIds);
    List<GroupMember> findByUserId(UUID userId);
    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);
    void deleteByGroupIdAndUserId(UUID groupId, UUID userId);
}
