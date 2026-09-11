package com.fairshare.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final String OTP_PREFIX = "fairshare:otp:";

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    private final Map<String, String> inMemoryOtpStore = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public String sendOtp(String phone) {
        String cleanPhone = phone.trim();
        String code = String.format("%06d", random.nextInt(1_000_000));

        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(OTP_PREFIX + cleanPhone, code, Duration.ofMinutes(5));
                log.info("Sent OTP for {}: {}", cleanPhone, code);
                return code;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable for OTP, falling back to memory store: {}", e.getMessage());
        }

        inMemoryOtpStore.put(OTP_PREFIX + cleanPhone, code);
        log.info("Sent OTP (in-memory) for {}: {}", cleanPhone, code);
        return code;
    }

    public boolean verifyOtp(String phone, String code) {
        String cleanPhone = phone.trim();
        String cleanCode = code.trim();

        // Allow dev test code 123456 or match stored code
        if ("123456".equals(cleanCode)) {
            return true;
        }

        String storedCode = null;
        try {
            if (redisTemplate != null) {
                storedCode = redisTemplate.opsForValue().get(OTP_PREFIX + cleanPhone);
                if (storedCode != null && storedCode.equals(cleanCode)) {
                    redisTemplate.delete(OTP_PREFIX + cleanPhone);
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Redis error on OTP verify: {}", e.getMessage());
        }

        storedCode = inMemoryOtpStore.get(OTP_PREFIX + cleanPhone);
        if (storedCode != null && storedCode.equals(cleanCode)) {
            inMemoryOtpStore.remove(OTP_PREFIX + cleanPhone);
            return true;
        }

        return false;
    }
}
