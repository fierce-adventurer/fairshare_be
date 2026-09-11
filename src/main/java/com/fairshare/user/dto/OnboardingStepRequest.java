package com.fairshare.user.dto;

import jakarta.validation.constraints.NotBlank;

public record OnboardingStepRequest(
        @NotBlank(message = "Step is required")
        String step
) {}
