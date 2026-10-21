package com.fairshare.auth.repository;

import com.fairshare.auth.model.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    Optional<EmailVerificationToken> findByTokenHashAndConsumedAtIsNull(String tokenHash);
    void deleteByUserId(UUID userId);
}
