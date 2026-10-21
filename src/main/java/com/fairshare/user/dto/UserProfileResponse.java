package com.fairshare.user.dto;

import com.fairshare.user.model.User;
import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String name,
        String initials,
        String avatarUrl,
        String color,
        String phone,
        String instagramHandle,
        String upiId,
        String bankAccountName,
        String bankAccountNumber,
        String bankIfsc,
        String paymentQrData,
        String defaultCurrency,
        String language,
        String onboardingStep,
        String role,
        Instant createdAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getInitials(),
                user.getAvatarUrl(),
                user.getColor(),
                user.getPhone(),
                user.getInstagramHandle(),
                user.getUpiId(),
                user.getBankAccountName(),
                user.getBankAccountNumber(),
                user.getBankIfsc(),
                user.getPaymentQrData(),
                user.getDefaultCurrency(),
                user.getLanguage(),
                user.getOnboardingStep(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
