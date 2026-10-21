package com.fairshare.auth.dto;

public record VerifyPhoneOtpResponse(
        boolean verified,
        boolean registrationRequired,
        String phone,
        String verificationToken,
        AuthResponse auth
) {}
