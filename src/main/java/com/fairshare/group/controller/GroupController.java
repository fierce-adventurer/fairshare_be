package com.fairshare.group.controller;

import com.fairshare.group.dto.AddMemberRequest;
import com.fairshare.group.dto.CreateGroupRequest;
import com.fairshare.group.dto.GroupBalanceResponse;
import com.fairshare.group.dto.GroupResponse;
import com.fairshare.group.service.GroupService;
import com.fairshare.shared.dto.ApiResponse;
import com.fairshare.shared.security.UserPrincipal;
import com.fairshare.user.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups")
@Tag(name = "Groups", description = "Group management, membership, and balance calculation")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    @Operation(summary = "List current user's groups")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> listGroups(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<GroupResponse> groups = groupService.listUserGroups(principal.id());
        return ResponseEntity.ok(ApiResponse.ok(groups));
    }

    @PostMapping
    @Operation(summary = "Create a new group")
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateGroupRequest request
    ) {
        GroupResponse group = groupService.createGroup(principal.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(group, "Group created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get group details and members")
    public ResponseEntity<ApiResponse<GroupResponse>> getGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        GroupResponse group = groupService.getGroup(principal.id(), id);
        return ResponseEntity.ok(ApiResponse.ok(group));
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Add a member to a group")
    public ResponseEntity<ApiResponse<GroupResponse>> addMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody AddMemberRequest request
    ) {
        GroupResponse group = groupService.addMember(principal.id(), id, request);
        return ResponseEntity.ok(ApiResponse.ok(group, "Member added successfully"));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Operation(summary = "Remove a member from a group")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @PathVariable UUID userId
    ) {
        groupService.removeMember(principal.id(), id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Member removed successfully"));
    }

    @GetMapping("/{id}/balances")
    @Operation(summary = "Get group raw net balances and simplified debt graph")
    public ResponseEntity<ApiResponse<GroupBalanceResponse>> getGroupBalances(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam(required = false) String currency
    ) {
        GroupBalanceResponse balances = groupService.getBalances(principal.id(), id, currency);
        return ResponseEntity.ok(ApiResponse.ok(balances));
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "List all member profiles of a group")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getGroupMembers(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        List<UserProfileResponse> members = groupService.getGroupMembers(principal.id(), id);
        return ResponseEntity.ok(ApiResponse.ok(members));
    }
}
