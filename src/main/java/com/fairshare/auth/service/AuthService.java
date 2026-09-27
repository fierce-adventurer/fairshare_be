package com.fairshare.auth.service;

import com.fairshare.auth.dto.AuthResponse;
import com.fairshare.auth.dto.LoginRequest;
import com.fairshare.auth.dto.RegisterRequest;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    // Fallback store when Redis is unavailable (e.g. unit testing)
    private final Map<String, String> inMemoryRefreshStore = new ConcurrentHashMap<>();

    public AuthService(
            UserRepository userRepository,
            UserIdentityRepository identityRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            GoogleOAuth2Handler googleOAuth2Handler,
            GitHubOAuth2Handler gitHubOAuth2Handler
    ) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.googleOAuth2Handler = googleOAuth2Handler;
        this.gitHubOAuth2Handler = gitHubOAuth2Handler;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("User with email " + email + " already exists.");
        }

        User user = new User(
                UUID.randomUUID(),
                email,
                passwordEncoder.encode(request.password()),
                request.name().trim()
        );

        if (request.phone() != null && !request.phone().isBlank()) {
            user.setPhone(request.phone().trim());
        }
        if (request.defaultCurrency() != null && !request.defaultCurrency().isBlank()) {
            user.setDefaultCurrency(request.defaultCurrency().trim().toUpperCase());
        }

        User savedUser = userRepository.save(user);
        return generateAuthResponse(savedUser);
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

    @Transactional
    public AuthResponse handleGoogleCallback(String code) {
        GoogleOAuth2Handler.GoogleUserInfo userInfo = googleOAuth2Handler.exchangeCodeForUserInfo(code);
        String googleSub = userInfo.sub();
        String email = userInfo.email() != null ? userInfo.email().trim().toLowerCase() : "";

        // 1. Check if identity already exists
        Optional<UserIdentity> identityOpt = identityRepository.findByProviderAndProviderUserId("google", googleSub);
        User user;

        if (identityOpt.isPresent()) {
            user = userRepository.findById(identityOpt.get().getUserId())
                    .orElseThrow(() -> new UnauthorizedException("Linked user account not found"));
        } else {
            // 2. Link by existing email or create new user
            Optional<User> existingUser = userRepository.findByEmail(email);
            if (existingUser.isPresent()) {
                user = existingUser.get();
            } else {
                user = new User(
                        UUID.randomUUID(),
                        email,
                        null,
                        userInfo.name() != null ? userInfo.name() : "Google User"
                );
                if (userInfo.picture() != null) {
                    user.setAvatarUrl(userInfo.picture());
                }
                user = userRepository.save(user);
            }

            UserIdentity identity = new UserIdentity(user.getId(), "google", googleSub, email);
            identityRepository.save(identity);
        }

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse handleGithubCallback(String code) {
        GitHubOAuth2Handler.GitHubUserInfo userInfo = gitHubOAuth2Handler.exchangeCodeForUserInfo(code);
        String githubId = userInfo.id();
        String email = userInfo.email() != null ? userInfo.email().trim().toLowerCase() : "";

        // 1. Check if identity already exists
        Optional<UserIdentity> identityOpt = identityRepository.findByProviderAndProviderUserId("github", githubId);
        User user;

        if (identityOpt.isPresent()) {
            user = userRepository.findById(identityOpt.get().getUserId())
                    .orElseThrow(() -> new UnauthorizedException("Linked user account not found"));
        } else {
            // 2. Link by existing email or create new user
            Optional<User> existingUser = email.isBlank() ? Optional.empty() : userRepository.findByEmail(email);
            if (existingUser.isPresent()) {
                user = existingUser.get();
            } else {
                user = new User(
                        UUID.randomUUID(),
                        email.isBlank() ? githubId + "@github.user" : email,
                        null,
                        userInfo.name() != null && !userInfo.name().isBlank() ? userInfo.name() : "GitHub User"
                );
                if (userInfo.avatarUrl() != null && !userInfo.avatarUrl().isBlank()) {
                    user.setAvatarUrl(userInfo.avatarUrl());
                }
                user = userRepository.save(user);
            }

            UserIdentity identity = new UserIdentity(user.getId(), "github", githubId, email);
            identityRepository.save(identity);
        }

        return generateAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        String userIdStr = getStoredRefreshToken(refreshToken);
        if (userIdStr == null) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        UUID userId = UUID.fromString(userIdStr);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        // Rotate token
        deleteStoredRefreshToken(refreshToken);
        return generateAuthResponse(user);
    }

    public void logout(String refreshToken) {
        deleteStoredRefreshToken(refreshToken);
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = UUID.randomUUID().toString();

        saveRefreshToken(refreshToken, user.getId().toString());

        return new AuthResponse(
                accessToken,
                refreshToken,
                UserProfileResponse.from(user)
        );
    }

    private void saveRefreshToken(String token, String userId) {
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(REFRESH_PREFIX + token, userId, Duration.ofDays(30));
                return;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable, falling back to memory store for refresh token: {}", e.getMessage());
        }
        inMemoryRefreshStore.put(REFRESH_PREFIX + token, userId);
    }

    private String getStoredRefreshToken(String token) {
        try {
            if (redisTemplate != null) {
                String val = redisTemplate.opsForValue().get(REFRESH_PREFIX + token);
                if (val != null) return val;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable, checking memory store for refresh token: {}", e.getMessage());
        }
        return inMemoryRefreshStore.get(REFRESH_PREFIX + token);
    }

    private void deleteStoredRefreshToken(String token) {
        try {
            if (redisTemplate != null) {
                redisTemplate.delete(REFRESH_PREFIX + token);
            }
        } catch (Exception e) {
            log.warn("Redis unavailable during token delete: {}", e.getMessage());
        }
        inMemoryRefreshStore.remove(REFRESH_PREFIX + token);
    }
}
