package com.fairshare.user.dto;

public record UpdateProfileRequest(
        String name,
        String phone,
        String avatarUrl,
        String color,
        String defaultCurrency,
        String language
) {}
