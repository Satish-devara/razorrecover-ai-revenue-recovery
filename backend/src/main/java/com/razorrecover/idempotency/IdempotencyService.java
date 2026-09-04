package com.razorrecover.idempotency;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {

    private static final Duration TTL = Duration.ofMinutes(30);
    private static final String PREFIX = "razorrecover:idempotency:";

    private final StringRedisTemplate redisTemplate;

    public IdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Optional<String> get(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                redisTemplate.opsForValue().get(key(idempotencyKey))
        );
    }

    public boolean claim(String idempotencyKey, String value) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return true;
        }

        Boolean created = redisTemplate.opsForValue().setIfAbsent(
                key(idempotencyKey),
                value,
                TTL
        );

        return Boolean.TRUE.equals(created);
    }

    private String key(String idempotencyKey) {
        return PREFIX + idempotencyKey;
    }
}
