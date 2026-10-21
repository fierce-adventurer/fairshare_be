package com.fairshare.user.dto;

public record UpdateProfileRequest(
        String name,
        String email,
        String phone,
        String instagramHandle,
        String avatarUrl,
        String color,
        String upiId,
        String bankAccountName,
        String bankAccountNumber,
        String bankIfsc,
        String paymentQrData,
        String defaultCurrency,
        String language
) {}
