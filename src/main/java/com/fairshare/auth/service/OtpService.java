package com.fairshare.auth.service;

import com.fairshare.shared.exception.BadRequestException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {
    private static final String OTP_PREFIX = "fairshare:otp:";
    private static final String COOLDOWN_PREFIX = "fairshare:otp-cooldown:";
    private static final String PROOF_PREFIX = "fairshare:phone-proof:";
    private static final int MAX_ATTEMPTS = 5;

    private final RedisTemplate<String, String> redisTemplate;
    private final SmsDeliveryService smsDeliveryService;
    private final boolean exposeDevCode;
    private final Duration otpTtl;
    private final Duration proofTtl;
    private final Duration cooldown;
    private final String pepper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Challenge> inMemoryChallenges = new ConcurrentHashMap<>();
    private final Map<String, Instant> inMemoryCooldowns = new ConcurrentHashMap<>();
    private final Map<String, Proof> inMemoryProofs = new ConcurrentHashMap<>();

    public OtpService(
            @Qualifier("redisTemplate") ObjectProvider<RedisTemplate<String, String>> redisProvider,
            SmsDeliveryService smsDeliveryService,
            @Value("${app.auth.otp.expose-dev-code:false}") boolean exposeDevCode,
            @Value("${app.auth.otp.ttl-seconds:300}") long ttlSeconds,
            @Value("${app.auth.otp.proof-ttl-seconds:1800}") long proofTtlSeconds,
            @Value("${app.auth.otp.resend-cooldown-seconds:30}") long cooldownSeconds,
            @Value("${app.auth.otp.pepper:${app.jwt.secret}}") String pepper
    ) {
        this.redisTemplate = redisProvider.getIfAvailable();
        this.smsDeliveryService = smsDeliveryService;
        this.exposeDevCode = exposeDevCode;
        this.otpTtl = Duration.ofSeconds(Math.max(300, ttlSeconds));
        this.proofTtl = Duration.ofSeconds(Math.max(300, proofTtlSeconds));
        this.cooldown = Duration.ofSeconds(cooldownSeconds);
        this.pepper = pepper;
    }

    public OtpDispatch sendOtp(String rawPhone, OtpPurpose purpose) {
        String phone = normalizePhone(rawPhone);
        String challengeKey = challengeKey(phone, purpose);
        String cooldownKey = COOLDOWN_PREFIX + purpose.name() + ":" + phone;
        if (isCoolingDown(cooldownKey)) {
            throw new BadRequestException("Please wait before requesting another verification code.");
        }

        String code = String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
        Challenge challenge = new Challenge(hash(phone, purpose, code), 0, Instant.now().plus(otpTtl));
        storeChallenge(challengeKey, challenge);
        setCooldown(cooldownKey);

        try {
            smsDeliveryService.sendOtp(phone, code);
        } catch (RuntimeException deliveryFailure) {
            deleteChallenge(challengeKey);
            throw deliveryFailure;
        }
        return new OtpDispatch(phone, otpTtl.toSeconds(), exposeDevCode ? code : null);
    }

    public OtpVerification verifyOtp(String rawPhone, String rawCode, OtpPurpose purpose) {
        String phone = normalizePhone(rawPhone);
        String code = rawCode == null ? "" : rawCode.trim();
        if (!code.matches("\\d{6}")) throw new BadRequestException("Enter a valid 6-digit verification code.");

        String key = challengeKey(phone, purpose);
        Challenge challenge = readChallenge(key);
        if (challenge == null || challenge.expiresAt().isBefore(Instant.now())) {
            deleteChallenge(key);
            throw new BadRequestException("The verification code has expired. Request a new one.");
        }
        if (challenge.attempts() >= MAX_ATTEMPTS) {
            deleteChallenge(key);
            throw new BadRequestException("Too many incorrect attempts. Request a new code.");
        }
        if (!MessageDigest.isEqual(
                challenge.codeHash().getBytes(StandardCharsets.UTF_8),
                hash(phone, purpose, code).getBytes(StandardCharsets.UTF_8)
        )) {
            storeChallenge(key, new Challenge(challenge.codeHash(), challenge.attempts() + 1, challenge.expiresAt()));
            throw new BadRequestException("That verification code is incorrect.");
        }

        deleteChallenge(key);
        String proofToken = UUID.randomUUID().toString();
        storeProof(proofToken, new Proof(phone, purpose, Instant.now().plus(proofTtl)));
        return new OtpVerification(phone, proofToken);
    }

    public void consumeVerificationToken(String rawToken, String rawPhone, OtpPurpose expectedPurpose) {
        if (rawToken == null || rawToken.isBlank()) throw new BadRequestException("Phone verification is required.");
        String phone = normalizePhone(rawPhone);
        Proof proof = readAndDeleteProof(rawToken.trim());
        if (proof == null || proof.expiresAt().isBefore(Instant.now()) || proof.purpose() != expectedPurpose || !proof.phone().equals(phone)) {
            throw new BadRequestException("Phone verification has expired. Verify the number again.");
        }
    }

    public static String normalizePhone(String rawPhone) {
        String phone = rawPhone == null ? "" : rawPhone.replaceAll("[\\s()-]", "");
        if (!phone.matches("^\\+[1-9]\\d{7,14}$")) throw new BadRequestException("Enter a valid mobile number with country code.");
        if (phone.startsWith("+91") && !phone.matches("^\\+91[6-9]\\d{9}$")) {
            throw new BadRequestException("Indian mobile numbers must contain exactly 10 digits and start with 6, 7, 8, or 9.");
        }
        return phone;
    }

    private String challengeKey(String phone, OtpPurpose purpose) { return OTP_PREFIX + purpose.name() + ":" + phone; }

    private String hash(String phone, OtpPurpose purpose, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest((purpose.name() + "|" + phone + "|" + code + "|" + pepper).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private boolean isCoolingDown(String key) {
        try {
            if (redisTemplate != null && Boolean.TRUE.equals(redisTemplate.hasKey(key))) return true;
        } catch (RuntimeException ignored) {}
        Instant until = inMemoryCooldowns.get(key);
        if (until == null) return false;
        if (until.isBefore(Instant.now())) { inMemoryCooldowns.remove(key); return false; }
        return true;
    }

    private void setCooldown(String key) {
        try {
            if (redisTemplate != null) { redisTemplate.opsForValue().set(key, "1", cooldown); return; }
        } catch (RuntimeException ignored) {}
        inMemoryCooldowns.put(key, Instant.now().plus(cooldown));
    }

    private void storeChallenge(String key, Challenge challenge) {
        long seconds = Math.max(1, Duration.between(Instant.now(), challenge.expiresAt()).toSeconds());
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(key, challenge.codeHash() + "|" + challenge.attempts() + "|" + challenge.expiresAt().toEpochMilli(), Duration.ofSeconds(seconds));
                return;
            }
        } catch (RuntimeException ignored) {}
        inMemoryChallenges.put(key, challenge);
    }

    private Challenge readChallenge(String key) {
        try {
            if (redisTemplate != null) {
                String stored = redisTemplate.opsForValue().get(key);
                if (stored != null) {
                    String[] parts = stored.split("\\|");
                    return new Challenge(parts[0], Integer.parseInt(parts[1]), Instant.ofEpochMilli(Long.parseLong(parts[2])));
                }
            }
        } catch (RuntimeException ignored) {}
        return inMemoryChallenges.get(key);
    }

    private void deleteChallenge(String key) {
        try { if (redisTemplate != null) redisTemplate.delete(key); } catch (RuntimeException ignored) {}
        inMemoryChallenges.remove(key);
    }

    private void storeProof(String token, Proof proof) {
        String key = PROOF_PREFIX + token;
        long seconds = Math.max(1, Duration.between(Instant.now(), proof.expiresAt()).toSeconds());
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(
                        key,
                        proof.purpose().name() + "|" + proof.phone() + "|" + proof.expiresAt().toEpochMilli(),
                        Duration.ofSeconds(seconds)
                );
                return;
            }
        } catch (RuntimeException ignored) {}
        inMemoryProofs.put(key, proof);
    }

    private Proof readAndDeleteProof(String token) {
        String key = PROOF_PREFIX + token;
        try {
            if (redisTemplate != null) {
                String stored = redisTemplate.opsForValue().get(key);
                redisTemplate.delete(key);
                if (stored != null) {
                    String[] parts = stored.split("\\|", 3);
                    Instant expiresAt = parts.length == 3
                            ? Instant.ofEpochMilli(Long.parseLong(parts[2]))
                            : Instant.now().plusSeconds(1);
                    return new Proof(parts[1], OtpPurpose.valueOf(parts[0]), expiresAt);
                }
            }
        } catch (RuntimeException ignored) {}
        return inMemoryProofs.remove(key);
    }

    public enum OtpPurpose {
        SIGN_IN, SIGN_UP;

        public static OtpPurpose from(String value) {
            try { return value == null ? SIGN_IN : valueOf(value.trim().toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException invalid) { throw new BadRequestException("OTP purpose must be SIGN_IN or SIGN_UP."); }
        }
    }

    public record OtpDispatch(String phone, long expiresInSeconds, String devCode) {}
    public record OtpVerification(String phone, String verificationToken) {}
    private record Challenge(String codeHash, int attempts, Instant expiresAt) {}
    private record Proof(String phone, OtpPurpose purpose, Instant expiresAt) {}
}
