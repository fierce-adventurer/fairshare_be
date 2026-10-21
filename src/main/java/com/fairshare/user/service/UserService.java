package com.fairshare.user.service;

import com.fairshare.shared.exception.BadRequestException;
import com.fairshare.shared.exception.ResourceNotFoundException;
import com.fairshare.user.dto.UpdateProfileRequest;
import com.fairshare.user.dto.UserProfileResponse;
import com.fairshare.user.model.User;
import com.fairshare.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = findUserById(userId);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
            user.setInitials(User.computeInitials(request.name().trim()));
        }
        if (request.email() != null && !request.email().isBlank()) {
            String email = request.email().trim().toLowerCase();
            userRepository.findByEmail(email).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId())) {
                    throw new BadRequestException("That email address is already linked to another account.");
                }
            });
            user.setEmail(email);
        }
        if (request.phone() != null) {
            String trimmedPhone = request.phone().trim();
            if (!trimmedPhone.isBlank() && !trimmedPhone.equals(user.getPhone())) {
                userRepository.findByPhone(trimmedPhone).ifPresent(existing -> {
                    if (!existing.getId().equals(user.getId())) {
                        throw new BadRequestException("Phone number " + trimmedPhone + " is already linked to another account.");
                    }
                });
            }
            user.setPhone(trimmedPhone.isBlank() ? null : trimmedPhone);
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl().trim());
        }
        if (request.instagramHandle() != null) user.setInstagramHandle(normalizeOptional(request.instagramHandle().replaceFirst("^@", "")));
        if (request.upiId() != null) user.setUpiId(normalizeOptional(request.upiId()));
        if (request.bankAccountName() != null) user.setBankAccountName(normalizeOptional(request.bankAccountName()));
        if (request.bankAccountNumber() != null) user.setBankAccountNumber(normalizeOptional(request.bankAccountNumber()));
        if (request.bankIfsc() != null) {
            String ifsc = normalizeOptional(request.bankIfsc());
            user.setBankIfsc(ifsc == null ? null : ifsc.toUpperCase());
        }
        if (request.paymentQrData() != null) {
            String qr = normalizeOptional(request.paymentQrData());
            if (qr != null && !qr.matches("^data:image/(png|jpeg|webp);base64,.+$")) {
                throw new BadRequestException("Payment QR must be a PNG, JPEG, or WebP image.");
            }
            if (qr != null && qr.length() > 1_500_000) throw new BadRequestException("Payment QR must be smaller than 1 MB.");
            user.setPaymentQrData(qr);
        }
        if (request.color() != null) {
            user.setColor(request.color().trim());
        }
        if (request.defaultCurrency() != null) {
            user.setDefaultCurrency(request.defaultCurrency().trim().toUpperCase());
        }
        if (request.language() != null) {
            user.setLanguage(request.language().trim());
        }

        User saved = userRepository.save(user);
        return UserProfileResponse.from(saved);
    }

    private String normalizeOptional(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    @Transactional(readOnly = true)
    public User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }
}
