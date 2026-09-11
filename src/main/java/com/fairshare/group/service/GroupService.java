package com.fairshare.group.service;

import com.fairshare.expense.engine.BalanceService;
import com.fairshare.group.dto.AddMemberRequest;
import com.fairshare.group.dto.CreateGroupRequest;
import com.fairshare.group.dto.GroupBalanceResponse;
import com.fairshare.group.dto.GroupResponse;
import com.fairshare.group.model.Group;
import com.fairshare.group.model.GroupMember;
import com.fairshare.group.repository.GroupMemberRepository;
import com.fairshare.group.repository.GroupRepository;
import com.fairshare.shared.exception.BadRequestException;
import com.fairshare.shared.exception.ResourceNotFoundException;
import com.fairshare.shared.exception.UnauthorizedException;
import com.fairshare.user.dto.UserProfileResponse;
import com.fairshare.user.model.User;
import com.fairshare.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final BalanceService balanceService;

    public GroupService(
            GroupRepository groupRepository,
            GroupMemberRepository memberRepository,
            UserRepository userRepository,
            BalanceService balanceService
    ) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.balanceService = balanceService;
    }

    @Transactional
    public GroupResponse createGroup(UUID creatorId, CreateGroupRequest request) {
        Group group = new Group(
                UUID.randomUUID(),
                request.name().trim(),
                request.kind() != null ? request.kind().trim() : "other",
                request.emoji() != null ? request.emoji().trim() : "💰",
                request.simplifyDebts() != null ? request.simplifyDebts() : true,
                creatorId
        );
        Group savedGroup = groupRepository.save(group);

        // Add creator as admin
        memberRepository.save(new GroupMember(savedGroup.getId(), creatorId, "admin"));

        Set<UUID> allMemberIds = new LinkedHashSet<>();
        allMemberIds.add(creatorId);

        if (request.memberIds() != null) {
            for (UUID memberId : request.memberIds()) {
                if (!memberId.equals(creatorId) && userRepository.existsById(memberId)) {
                    memberRepository.save(new GroupMember(savedGroup.getId(), memberId, "member"));
                    allMemberIds.add(memberId);
                }
            }
        }

        return GroupResponse.from(savedGroup, new ArrayList<>(allMemberIds));
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> listUserGroups(UUID userId) {
        List<Group> groups = groupRepository.findByMemberUserId(userId);
        List<GroupResponse> responses = new ArrayList<>();
        for (Group group : groups) {
            List<UUID> memberIds = memberRepository.findByGroupId(group.getId()).stream()
                    .map(GroupMember::getUserId)
                    .toList();
            responses.add(GroupResponse.from(group, memberIds));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroup(UUID userId, UUID groupId) {
        assertMembership(userId, groupId);
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId));

        List<UUID> memberIds = memberRepository.findByGroupId(groupId).stream()
                .map(GroupMember::getUserId)
                .toList();

        return GroupResponse.from(group, memberIds);
    }

    @Transactional
    public GroupResponse addMember(UUID currentUserId, UUID groupId, AddMemberRequest request) {
        assertMembership(currentUserId, groupId);

        UUID targetUserId = request.userId();
        if (targetUserId == null && request.msisdn() != null && !request.msisdn().isBlank()) {
            targetUserId = userRepository.findByPhone(request.msisdn().trim())
                    .map(User::getId)
                    .orElseThrow(() -> new BadRequestException("No registered user found with phone: " + request.msisdn()));
        }

        if (targetUserId == null) {
            throw new BadRequestException("Either userId or valid msisdn must be provided");
        }

        if (memberRepository.existsByGroupIdAndUserId(groupId, targetUserId)) {
            throw new BadRequestException("User is already a member of this group");
        }

        memberRepository.save(new GroupMember(groupId, targetUserId, "member"));
        return getGroup(currentUserId, groupId);
    }

    @Transactional
    public void removeMember(UUID currentUserId, UUID groupId, UUID targetUserId) {
        assertMembership(currentUserId, groupId);
        memberRepository.deleteByGroupIdAndUserId(groupId, targetUserId);
    }

    @Transactional(readOnly = true)
    public GroupBalanceResponse getBalances(UUID currentUserId, UUID groupId, String currency) {
        assertMembership(currentUserId, groupId);
        return balanceService.calculateGroupBalances(groupId, currency);
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getGroupMembers(UUID userId, UUID groupId) {
        assertMembership(userId, groupId);
        List<UUID> memberUserIds = memberRepository.findByGroupId(groupId).stream()
                .map(GroupMember::getUserId)
                .toList();
        return userRepository.findAllById(memberUserIds).stream()
                .map(UserProfileResponse::from)
                .toList();
    }

    public void assertMembership(UUID userId, UUID groupId) {
        if (!memberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new UnauthorizedException("You are not a member of this group");
        }
    }
}
