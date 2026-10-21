package com.fairshare.auth.service;

import com.fairshare.auth.model.EmailVerificationToken;
import com.fairshare.auth.repository.EmailVerificationTokenRepository;
import com.fairshare.shared.exception.BadRequestException;
import com.fairshare.user.model.User;
import com.fairshare.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class EmailVerificationService {
    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final boolean deliveryEnabled;
    private final boolean exposeDevLink;
    private final String fromAddress;
    private final String publicApiUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.email.delivery-enabled:false}") boolean deliveryEnabled,
            @Value("${app.email.expose-dev-link:false}") boolean exposeDevLink,
            @Value("${app.email.from:no-reply@fairshare.app}") String fromAddress,
            @Value("${app.public-api-url:http://localhost:8080}") String publicApiUrl
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.deliveryEnabled = deliveryEnabled;
        this.exposeDevLink = exposeDevLink;
        this.fromAddress = fromAddress;
        this.publicApiUrl = publicApiUrl.replaceAll("/+$", "");
    }

    @Transactional
    public String issue(User user) {
        tokenRepository.deleteByUserId(user.getId());
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokenRepository.save(new EmailVerificationToken(user.getId(), hash(rawToken), Instant.now().plus(Duration.ofHours(24))));
        String link = publicApiUrl + "/api/v1/auth/email/verify?token=" + rawToken;

        if (deliveryEnabled) {
            if (mailSender == null) {
                log.error("Email verification delivery is enabled but no mail sender is configured for user {}", user.getId());
            } else {
                try {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom(fromAddress);
                    message.setTo(user.getEmail());
                    message.setSubject("Verify your Fairshare email");
                    message.setText("Welcome to Fairshare. Verify your email within 24 hours:\n\n" + link + "\n\nIf you did not create this account, ignore this message.");
                    mailSender.send(message);
                } catch (RuntimeException deliveryFailure) {
                    log.error("Email verification delivery failed for user {}; the account remains usable and the message can be resent", user.getId(), deliveryFailure);
                }
            }
        } else {
            log.info("Development email verification prepared for user {}", user.getId());
        }
        return exposeDevLink ? link : null;
    }

    @Transactional
    public User verify(String rawToken) {
        EmailVerificationToken token = tokenRepository.findByTokenHashAndConsumedAtIsNull(hash(rawToken))
                .orElseThrow(() -> new BadRequestException("That email verification link is invalid or has already been used."));
        if (token.getExpiresAt().isBefore(Instant.now())) throw new BadRequestException("That email verification link has expired.");
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BadRequestException("The account linked to this verification request no longer exists."));
        token.setConsumedAt(Instant.now());
        user.setEmailVerifiedAt(Instant.now());
        tokenRepository.save(token);
        return userRepository.save(user);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
