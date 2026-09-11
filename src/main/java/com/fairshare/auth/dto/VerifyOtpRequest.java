package com.fairshare.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyOtpRequest(
        @NotBlank(message = "Phone number is required")
        String phone,
        @NotBlank(message = "OTP code is required")
        String code
) {}
