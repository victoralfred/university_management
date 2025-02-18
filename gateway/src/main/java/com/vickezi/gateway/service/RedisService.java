package com.vickezi.gateway.service;

import com.vickezi.gateway.routes.RegistrationHandlerImpl;
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

    public Mono<Void> setValue(String key, Object value) {
        logger.info("Adding new key/value to Redis store");
        return Mono.fromRunnable(()->redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(30)));
    }
    public Mono<Boolean> isIdempotent(String key, Object value) {
        logger.info("Checking Redis store for idempotency");
        return Mono.fromCallable(()->redisTemplate.opsForValue().setIfAbsent(key, value, Duration.ofMinutes(30)));
    }
}
