package com.fairshare.auth.service;

import com.fairshare.auth.dto.AuthResponse;
import com.fairshare.auth.dto.LoginRequest;
import com.fairshare.auth.dto.RegisterRequest;
import com.fairshare.auth.oauth.AppleOAuth2Handler;
import com.fairshare.auth.oauth.GitHubOAuth2Handler;
import com.fairshare.auth.oauth.GoogleOAuth2Handler;
import com.fairshare.shared.exception.BadRequestException;
import com.fairshare.shared.exception.UnauthorizedException;
import com.fairshare.shared.security.JwtTokenProvider;
import com.fairshare.user.dto.UserProfileResponse;
import com.fairshare.user.model.User;
import com.fairshare.user.model.UserIdentity;
import com.fairshare.user.repository.UserIdentityRepository;
import com.fairshare.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String REFRESH_PREFIX = "fairshare:refresh:";

    private final UserRepository userRepository;
    private final UserIdentityRepository identityRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final GoogleOAuth2Handler googleOAuth2Handler;
    private final GitHubOAuth2Handler gitHubOAuth2Handler;
    private final AppleOAuth2Handler appleOAuth2Handler;
    private final OtpService otpService;
    private final EmailVerificationService emailVerificationService;

    @Autowired(required = false)
    @Qualifier("redisTemplate")
    private RedisTemplate<String, String> redisTemplate;
    private final Map<String, String> inMemoryRefreshStore = new ConcurrentHashMap<>();

    public AuthService(
            UserRepository userRepository,
            UserIdentityRepository identityRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            GoogleOAuth2Handler googleOAuth2Handler,
            GitHubOAuth2Handler gitHubOAuth2Handler,
            AppleOAuth2Handler appleOAuth2Handler,
            OtpService otpService,
            EmailVerificationService emailVerificationService
    ) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.googleOAuth2Handler = googleOAuth2Handler;
        this.gitHubOAuth2Handler = gitHubOAuth2Handler;
        this.appleOAuth2Handler = appleOAuth2Handler;
        this.otpService = otpService;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String phone = OtpService.normalizePhone(request.phone());
        otpService.consumeVerificationToken(request.phoneVerificationToken(), phone, OtpService.OtpPurpose.SIGN_UP);
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) throw new BadRequestException("That email address is already linked to an account.");
        if (userRepository.existsByPhone(phone)) throw new BadRequestException("That mobile number is already linked to an account. Sign in instead.");

        User user = new User(UUID.randomUUID(), email, passwordEncoder.encode(request.password()),
                (request.firstName().trim() + " " + request.lastName().trim()).trim());
        user.setNameParts(request.firstName(), request.lastName());
        user.setDateOfBirth(request.dateOfBirth());
        user.setPhone(phone);
        user.setPhoneVerifiedAt(Instant.now());
        user.setOnboardingStep("review_profile");
        if (request.defaultCurrency() != null && !request.defaultCurrency().isBlank()) {
            user.setDefaultCurrency(request.defaultCurrency().trim().toUpperCase());
        }

        User saved = userRepository.save(user);
        emailVerificationService.issue(saved);
        return generateAuthResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        return generateAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public Optional<AuthResponse> loginWithVerifiedPhone(String verifiedPhone) {
        return userRepository.findByPhone(verifiedPhone).map(this::generateAuthResponse);
    }

    @Transactional
    public AuthResponse handleGoogleCallback(String code) {
        GoogleOAuth2Handler.GoogleUserInfo info = googleOAuth2Handler.exchangeCodeForUserInfo(code);
        if (!info.emailVerified()) throw new UnauthorizedException("Google email verification is required.");
        return completeSocialLogin("google", info.sub(), info.email(), info.name(), info.picture());
    }

    @Transactional
    public AuthResponse handleGithubCallback(String code) {
        GitHubOAuth2Handler.GitHubUserInfo info = gitHubOAuth2Handler.exchangeCodeForUserInfo(code);
        return completeSocialLogin("github", info.id(), info.email(), info.name(), info.avatarUrl());
    }

    @Transactional
    public AuthResponse handleAppleCallback(String code, String userJson) {
        AppleOAuth2Handler.AppleUserInfo info = appleOAuth2Handler.exchangeCodeForUserInfo(code, userJson);
        if (!info.emailVerified()) throw new UnauthorizedException("Apple email verification is required.");
        return completeSocialLogin("apple", info.id(), info.email(), info.name(), null);
    }

    private AuthResponse completeSocialLogin(String provider, String providerId, String rawEmail, String providedName, String avatarUrl) {
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase();
        User user = identityRepository.findByProviderAndProviderUserId(provider, providerId)
                .flatMap(identity -> userRepository.findById(identity.getUserId()))
                .orElseGet(() -> linkOrCreateSocialUser(provider, providerId, email, providedName, avatarUrl));
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(Instant.now());
            user = userRepository.save(user);
        }
        return generateAuthResponse(user);
    }

    private User linkOrCreateSocialUser(String provider, String providerId, String email, String providedName, String avatarUrl) {
        User user = email.isBlank() ? null : userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            String safeEmail = email.isBlank() ? provider + "+" + providerId + "@identity.fairshare.app" : email;
            String name = providedName == null || providedName.isBlank() ? "New Fairshare member" : providedName.trim();
            user = new User(UUID.randomUUID(), safeEmail, null, name);
            user.setOnboardingStep("review_profile");
            user.setEmailVerifiedAt(Instant.now());
            if (avatarUrl != null && !avatarUrl.isBlank()) user.setAvatarUrl(avatarUrl);
            user = userRepository.save(user);
        }
        identityRepository.save(new UserIdentity(user.getId(), provider, providerId, email));
        return user;
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        String userIdStr = getStoredRefreshToken(refreshToken);
        if (userIdStr == null) throw new UnauthorizedException("Invalid or expired refresh token");
        User user = userRepository.findById(UUID.fromString(userIdStr))
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        deleteStoredRefreshToken(refreshToken);
        return generateAuthResponse(user);
    }

    public void logout(String refreshToken) { deleteStoredRefreshToken(refreshToken); }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = UUID.randomUUID().toString();
        saveRefreshToken(refreshToken, user.getId().toString());
        return new AuthResponse(accessToken, refreshToken, UserProfileResponse.from(user));
    }

    private void saveRefreshToken(String token, String userId) {
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(REFRESH_PREFIX + token, userId, Duration.ofDays(30));
                return;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable, falling back to memory for refresh token: {}", e.getMessage());
        }
        inMemoryRefreshStore.put(REFRESH_PREFIX + token, userId);
    }

    private String getStoredRefreshToken(String token) {
        try {
            if (redisTemplate != null) {
                String value = redisTemplate.opsForValue().get(REFRESH_PREFIX + token);
                if (value != null) return value;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable while reading refresh token: {}", e.getMessage());
        }
        return inMemoryRefreshStore.get(REFRESH_PREFIX + token);
    }

    private void deleteStoredRefreshToken(String token) {
        try { if (redisTemplate != null) redisTemplate.delete(REFRESH_PREFIX + token); }
        catch (Exception e) { log.warn("Redis unavailable while deleting refresh token: {}", e.getMessage()); }
        inMemoryRefreshStore.remove(REFRESH_PREFIX + token);
    }
}
