package com.fairshare.auth.dto;

import com.fairshare.user.dto.UserProfileResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserProfileResponse user
) {}
