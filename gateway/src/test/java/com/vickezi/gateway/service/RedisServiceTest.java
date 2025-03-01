package com.vickezi.gateway.service;

import com.vickezi.globals.model.RegistrationEmail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

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
    void setValue_ShouldStoreValueInRedis() {
        // Arrange
        String key = "test-key";
        Object value = new RegistrationEmail("test@example.com");

        doNothing().when(valueOperations).set(anyString(), any(), any(Duration.class));


        // Act
        Mono<Void> result = redisService.setValue(key, value);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(valueOperations).set(key, eq(value), eq(Duration.ofMinutes(30)));
    }

    @Test
    void isIdempotent_KeyDoesNotExist_ReturnsTrue() {
        // Arrange
        String key = "new-key";
        Object value = new Object();

        when(redisTemplate.hasKey(key)).thenReturn(false);
        when(valueOperations.setIfAbsent(eq("temp:" + key), eq(value), any(Duration.class)))
                .thenReturn(true);

        // Act
        Mono<Boolean> result = redisService.isIdempotent(key, value);

        // Assert
        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();

        verify(redisTemplate).hasKey(key);
        verify(valueOperations).setIfAbsent("temp:" + key, eq(value), eq(Duration.ofMinutes(5)));
    }



    @Test
    void isIdempotent_TempKeyCreationFails_ReturnsFalse() {
        // Arrange
        String key = "contended-key";
        Object value = new Object();

        when(redisTemplate.hasKey(key)).thenReturn(false);
        when(valueOperations.setIfAbsent("temp:" + key, eq(value), any(Duration.class)))
                .thenReturn(false);

        // Act
        Mono<Boolean> result = redisService.isIdempotent(key, value);

        // Assert
        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();

        verify(redisTemplate).hasKey(key);
        verify(valueOperations).setIfAbsent("temp:" + key, eq(value), eq(Duration.ofMinutes(5)));
    }


}