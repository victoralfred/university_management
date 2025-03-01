package com.vickezi.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class RedisService {
    private final Logger logger = LoggerFactory.getLogger(RedisService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Stores a value in Redis with the specified key after validation has passed.
     *
     * @param key The key to store in Redis
     * @param value The value to store
     * @return A Mono that completes when the value has been stored
     */
    public Mono<Void> setValue(final String key, final Object value) {
        return Mono.fromRunnable(() ->
                redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(30))
        );
    }
    /**
     * Checks if a request is idempotent by verifying if the key already exists in Redis.
     * This method is designed to only check without modifying Redis for invalid inputs.
     *
     * @param key The key to check in Redis
     * @param value The value to store if the key doesn't exist
     * @return A Mono that emits true if the request is idempotent (key doesn't exist), false otherwise
     */
    public Mono<Boolean> isIdempotent(final String key, final Object value) {
        logger.info("Checking Redis store for idempotency");
        // First check if the key exists without modifying Redis
        return Mono.fromCallable(() -> !Boolean.TRUE.equals(redisTemplate.hasKey(key)))
                .flatMap(keyNotExists -> {
                    if (Boolean.parseBoolean(String.valueOf(keyNotExists))) {
                        // Only if the key doesn't exist, proceed with the temporary registration
                        // Use a different temporary key or format to avoid conflicts
                        return Mono.fromCallable(() ->
                                        redisTemplate.opsForValue().setIfAbsent("temp:" + key, value, Duration.ofMinutes(5)))
                                .defaultIfEmpty(false);
                    } else {
                        // Key already exists, not idempotent
                        return Mono.just(false);
                    }
                });
    }

    public <T>Mono<T> deleteKey(final T email) {
        return Mono.fromRunnable(()->redisTemplate.delete((String) email));
    }
}
