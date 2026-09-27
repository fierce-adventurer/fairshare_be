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

    @Transactional(readOnly = true)
    public User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }
}
