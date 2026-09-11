package com.fairshare.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SendOtpRequest(
        @NotBlank(message = "Phone number is required")
        String phone
) {}
