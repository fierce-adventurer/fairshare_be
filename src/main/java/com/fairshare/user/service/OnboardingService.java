package com.fairshare.user.service;

import com.fairshare.auth.service.EmailVerificationService;
import com.fairshare.shared.exception.BadRequestException;
import com.fairshare.shared.exception.ResourceNotFoundException;
import com.fairshare.user.dto.CompleteOnboardingRequest;
import com.fairshare.user.dto.UserProfileResponse;
import com.fairshare.user.model.User;
import com.fairshare.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OnboardingService {

    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;

    public OnboardingService(UserRepository userRepository, EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    public UserProfileResponse advanceStep(UUID userId, String step) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        user.setOnboardingStep(step.trim());
        User saved = userRepository.save(user);
        return UserProfileResponse.from(saved);
    }

    @Transactional
    public UserProfileResponse complete(UUID userId, CompleteOnboardingRequest request) {
        if (!Boolean.TRUE.equals(request.acceptedTerms())) {
            throw new BadRequestException("Accept the Terms of Service and Privacy Policy to continue.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        String email = request.email().trim().toLowerCase();
        userRepository.findByEmail(email).ifPresent(existing -> {
            if (!existing.getId().equals(userId)) throw new BadRequestException("That email address is already linked to another account.");
        });

        boolean emailChanged = !email.equalsIgnoreCase(user.getEmail());
        user.setNameParts(request.firstName(), request.lastName());
        user.setDateOfBirth(request.dateOfBirth());
        user.setEmail(email);
        if (emailChanged) user.setEmailVerifiedAt(null);

        String requestedPhone = request.phone() == null ? "" : request.phone().trim();
        if (!requestedPhone.isBlank() && !requestedPhone.equals(user.getPhone())) {
            throw new BadRequestException("Verify a new mobile number before adding it to your profile.");
        }

        Instant now = Instant.now();
        user.setTermsAcceptedAt(now);
        user.setProfileReviewedAt(now);
        user.setOnboardingStep("done");
        User saved = userRepository.save(user);
        if (emailChanged || saved.getEmailVerifiedAt() == null) emailVerificationService.issue(saved);
        return UserProfileResponse.from(saved);
    }
}
