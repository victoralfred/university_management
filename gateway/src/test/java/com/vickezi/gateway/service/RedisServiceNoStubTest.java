package com.vickezi.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)  // Disable strictness for the whole class
class RedisServiceNoStubTest {
    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisService redisService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        redisService = new RedisService(redisTemplate);
    }
    @Test
    void isIdempotent_KeyExists_ReturnsFalse() {
        // Arrange
        String key = "existing-key";
        Object value = new Object();

        when(redisTemplate.hasKey(key)).thenReturn(true);

        // Act
        Mono<Boolean> result = redisService.isIdempotent(key, value);

        // Assert
        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();

        verify(redisTemplate).hasKey(key);
        verify(valueOperations, never()).setIfAbsent(anyString(), any(), any(Duration.class));
    }
    @Test
    void deleteKey_ShouldDeleteFromRedis() {
        // Arrange
        String key = "key-to-delete";
        lenient().when(redisTemplate.delete(key)).thenReturn(true);  // Make the stub lenient

        // Act
        Mono<?> result = redisService.deleteKey(key);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(redisTemplate).delete(key);  // Verifying interaction
    }
}
