package com.fairshare.user.controller;

import com.fairshare.shared.dto.ApiResponse;
import com.fairshare.shared.security.UserPrincipal;
import com.fairshare.user.dto.OnboardingStepRequest;
import com.fairshare.user.dto.UpdateProfileRequest;
import com.fairshare.user.dto.UserProfileResponse;
import com.fairshare.user.service.OnboardingService;
import com.fairshare.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User profile and onboarding operations")
public class UserController {

    private final UserService userService;
    private final OnboardingService onboardingService;

    public UserController(UserService userService, OnboardingService onboardingService) {
        this.userService = userService;
        this.onboardingService = onboardingService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserProfileResponse profile = userService.getProfile(principal.id());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateProfileRequest request
    ) {
        UserProfileResponse updated = userService.updateProfile(principal.id(), request);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Profile updated successfully"));
    }

    @PutMapping("/me/onboarding")
    @Operation(summary = "Advance onboarding journey step")
    public ResponseEntity<ApiResponse<UserProfileResponse>> advanceOnboarding(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody OnboardingStepRequest request
    ) {
        UserProfileResponse updated = onboardingService.advanceStep(principal.id(), request.step());
        return ResponseEntity.ok(ApiResponse.ok(updated, "Onboarding step updated"));
    }
}
