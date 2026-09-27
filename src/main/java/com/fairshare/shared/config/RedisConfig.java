package com.fairshare.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.net.URI;

@Configuration
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    public LettuceConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.url:${REDIS_URL:}}") String redisUrl,
            @Value("${spring.data.redis.host:${REDIS_HOST:localhost}}") String host,
            @Value("${spring.data.redis.port:${REDIS_PORT:6379}}") int port,
            @Value("${spring.data.redis.password:${REDIS_PASSWORD:}}") String password
    ) {
        if (redisUrl != null && !redisUrl.isBlank()) {
            try {
                URI uri = URI.create(redisUrl);
                String uriHost = uri.getHost();
                int uriPort = uri.getPort() > 0 ? uri.getPort() : 6379;
                RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(uriHost, uriPort);

                String userInfo = uri.getUserInfo();
                if (userInfo != null && !userInfo.isBlank()) {
                    String[] parts = userInfo.split(":", 2);
                    if (parts.length > 1) {
                        config.setUsername(parts[0]);
                        config.setPassword(RedisPassword.of(parts[1]));
                    } else {
                        config.setPassword(RedisPassword.of(parts[0]));
                    }
                }

                LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder();
                if ("rediss".equalsIgnoreCase(uri.getScheme())) {
                    builder.useSsl();
                }

                log.info("Configured Redis via REDIS_URL to {}:{}", uriHost, uriPort);
                return new LettuceConnectionFactory(config, builder.build());
            } catch (Exception e) {
                log.warn("Failed to parse REDIS_URL ({}), falling back to host/port: {}", redisUrl, e.getMessage());
            }
        }

        log.info("Configured Redis via host/port to {}:{}", host, port);
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        if (password != null && !password.isBlank()) {
            config.setPassword(RedisPassword.of(password));
        }
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }
}
