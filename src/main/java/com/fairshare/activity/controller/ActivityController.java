package com.fairshare.activity.controller;

import com.fairshare.activity.dto.ActivityEventResponse;
import com.fairshare.activity.service.ActivityService;
import com.fairshare.shared.dto.ApiResponse;
import com.fairshare.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Activity", description = "Activity feeds and audit events")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping("/api/v1/groups/{groupId}/activity")
    @Operation(summary = "Get paginated activity feed for a specific group")
    public ResponseEntity<ApiResponse<List<ActivityEventResponse>>> getGroupActivity(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<ActivityEventResponse> events = activityService.listGroupActivity(principal.id(), groupId, page, size);
        return ResponseEntity.ok(ApiResponse.ok(events));
    }

    @GetMapping("/api/v1/activity")
    @Operation(summary = "Get cross-group chronological activity feed for current user")
    public ResponseEntity<ApiResponse<List<ActivityEventResponse>>> getUserActivity(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<ActivityEventResponse> events = activityService.listAllUserActivity(principal.id(), page, size);
        return ResponseEntity.ok(ApiResponse.ok(events));
    }
}
