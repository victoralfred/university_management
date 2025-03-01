package com.vickezi.gateway.routes;

import com.vickezi.gateway.service.RedisService;
import com.vickezi.globals.events.MessageProducerService;
import com.vickezi.globals.model.EmailRegistrationEvent;
import com.vickezi.globals.model.EmailVerificationEvent;
import com.vickezi.globals.model.RegistrationEmail;
import com.vickezi.globals.util.CustomValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationHandlerImplTest {

    @Mock
    private MessageProducerService<EmailRegistrationEvent> messageProducerService;

    @Mock
    private MessageProducerService<EmailVerificationEvent> verificationEventMessageProducerService;

    @Mock
    private RedisService redisService;

    @Mock
    private ServerRequest serverRequest;

    @InjectMocks
    private RegistrationHandlerImpl registrationHandler;

    @BeforeEach
    void setUp() {
        // The @InjectMocks annotation doesn't work well with constructor injection in JUnit 5
        // So we manually create the handler here
        registrationHandler = new RegistrationHandlerImpl(
                messageProducerService,
                verificationEventMessageProducerService,
                redisService
        );
    }

    @Test
    void register_ValidEmail_ReturnsSuccessResponse() {
        // Arrange
        String testEmail = "test@example.com";
        RegistrationEmail registrationEmail = new RegistrationEmail(testEmail);

        when(serverRequest.bodyToMono(RegistrationEmail.class))
                .thenReturn(Mono.just(registrationEmail));

        when(redisService.isIdempotent(anyString(), any()))
                .thenReturn(Mono.just(true));

        when(redisService.setValue(anyString(), any()))
                .thenReturn(Mono.empty());

        doNothing().when(messageProducerService)
                .addMessageToQueue(anyString(), any(EmailRegistrationEvent.class));

        // Act
        Mono<ServerResponse> result = registrationHandler.register(serverRequest);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    // Here we're verifying the response status is OK (200)
                    return response.statusCode().is2xxSuccessful();
                })
                .verifyComplete();

        verify(redisService).isIdempotent(eq(testEmail), any());
        verify(redisService).setValue(eq(testEmail), any());
        verify(messageProducerService).addMessageToQueue(
                eq("user.email.registered.event"),
                any(EmailRegistrationEvent.class)
        );
    }

    @Test
    void register_DuplicateEmail_ReturnsConflictResponse() {
        // Arrange
        String testEmail = "existing@example.com";
        RegistrationEmail registrationEmail = new RegistrationEmail(testEmail);

        when(serverRequest.bodyToMono(RegistrationEmail.class))
                .thenReturn(Mono.just(registrationEmail));

        when(redisService.isIdempotent(anyString(), any()))
                .thenReturn(Mono.just(false));

        // Act
        Mono<ServerResponse> result = registrationHandler.register(serverRequest);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.statusCode().value() == 409 // Conflict status code
                )
                .verifyComplete();

        verify(redisService).isIdempotent(eq(testEmail), any());
        verify(redisService, never()).setValue(anyString(), any());
        verify(messageProducerService, never()).addMessageToQueue(anyString(), any());
    }

    @Test
    void register_InvalidEmail_ReturnsBadRequestResponse() {
        // Arrange
        when(serverRequest.bodyToMono(RegistrationEmail.class))
                .thenReturn(Mono.just(new RegistrationEmail("invalid-email")));

        when(redisService.isIdempotent(anyString(), any()))
                .thenReturn(Mono.error(new CustomValidator.InvalidInputException("Invalid email format")));

        // Act
        Mono<ServerResponse> result = registrationHandler.register(serverRequest);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.statusCode().value() == 400 // Bad Request status code
                )
                .verifyComplete();

        verify(redisService).isIdempotent(eq("invalid-email"), any());
        verify(redisService, never()).setValue(anyString(), any());
        verify(messageProducerService, never()).addMessageToQueue(anyString(), any());
    }

    @Test
    void verifyEmail_ValidParameters_ReturnsSuccessResponse() {
        // Arrange
        String token = "valid-token";
        String messageId = "valid-message-id";

        when(serverRequest.queryParam("token")).thenReturn(Optional.of(token));
        when(serverRequest.queryParam("messageId")).thenReturn(Optional.of(messageId));

        when(redisService.isIdempotent(eq(messageId), any()))
                .thenReturn(Mono.just(true));

        doNothing().when(verificationEventMessageProducerService)
                .addMessageToQueue(anyString(), any(EmailVerificationEvent.class));

        // Act
        Mono<ServerResponse> result = registrationHandler.verifyEmail(serverRequest);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.statusCode().is2xxSuccessful()
                )
                .verifyComplete();

        verify(verificationEventMessageProducerService).addMessageToQueue(
                eq("user.email.registration.confirmation.notice"),
                any(EmailVerificationEvent.class)
        );
    }

    @Test
    void verifyEmail_MissingParameters_ReturnsBadRequestResponse() {
        // Arrange
        when(serverRequest.queryParam("token")).thenReturn(Optional.of("valid-token"));
        when(serverRequest.queryParam("messageId")).thenReturn(Optional.empty());

        // Act
        Mono<ServerResponse> result = registrationHandler.verifyEmail(serverRequest);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.statusCode().value() == 400 // Bad Request status code
                )
                .verifyComplete();

        verify(verificationEventMessageProducerService, never()).addMessageToQueue(anyString(), any());
    }

    @Test
    void verifyEmail_DuplicateVerification_ReturnsConflictResponse() {
        // Arrange
        String token = "valid-token";
        String messageId = "already-verified-id";

        when(serverRequest.queryParam("token")).thenReturn(Optional.of(token));
        when(serverRequest.queryParam("messageId")).thenReturn(Optional.of(messageId));

        when(redisService.isIdempotent(eq(messageId), any()))
                .thenReturn(Mono.just(false));

        // Act
        Mono<ServerResponse> result = registrationHandler.verifyEmail(serverRequest);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.statusCode().value() == 409 // Conflict status code
                )
                .verifyComplete();

        verify(verificationEventMessageProducerService, never()).addMessageToQueue(anyString(), any());
    }
}