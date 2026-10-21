package com.fairshare.auth.service;

import com.fairshare.shared.exception.BadRequestException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OAuthStateService {
    private static final String PREFIX = "fairshare:oauth-state:";
    private final RedisTemplate<String, String> redisTemplate;
    private final Map<String, StateEntry> inMemory = new ConcurrentHashMap<>();

    public OAuthStateService(@Qualifier("redisTemplate") ObjectProvider<RedisTemplate<String, String>> redisProvider) {
        this.redisTemplate = redisProvider.getIfAvailable();
    }

    public String issue(String requestedClient) {
        OAuthClient client = OAuthClient.from(requestedClient);
        String state = client.prefix + UUID.randomUUID();
        String key = PREFIX + state;
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(key, client.name(), Duration.ofMinutes(10));
                return state;
            }
        } catch (RuntimeException ignored) {}
        inMemory.put(key, new StateEntry(client, Instant.now().plus(Duration.ofMinutes(10))));
        return state;
    }

    public OAuthClient consume(String state) {
        if (state == null || state.isBlank()) throw new BadRequestException("The sign-in session is missing. Please try again.");
        String key = PREFIX + state;
        try {
            if (redisTemplate != null) {
                String value = redisTemplate.opsForValue().get(key);
                redisTemplate.delete(key);
                if (value != null) return OAuthClient.valueOf(value);
            }
        } catch (RuntimeException ignored) {}
        StateEntry entry = inMemory.remove(key);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("The sign-in session expired. Please start again.");
        }
        return entry.client();
    }

    public OAuthClient infer(String state) {
        return state != null && (state.startsWith("native.") || state.startsWith("mobile:")) ? OAuthClient.MOBILE : OAuthClient.WEB;
    }

    public enum OAuthClient {
        WEB("web:"), MOBILE("mobile:");
        private final String prefix;
        OAuthClient(String prefix) { this.prefix = prefix; }
        static OAuthClient from(String value) {
            return "native".equalsIgnoreCase(value) || "mobile".equalsIgnoreCase(value) ? MOBILE : WEB;
        }
    }

    private record StateEntry(OAuthClient client, Instant expiresAt) {}
}
