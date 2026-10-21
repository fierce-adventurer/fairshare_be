package com.fairshare.user.dto;

import java.time.LocalDate;

public record UpdateProfileRequest(
        String name,
        String email,
        String phone,
        LocalDate dateOfBirth,
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
