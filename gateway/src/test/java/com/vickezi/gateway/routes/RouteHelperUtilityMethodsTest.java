package com.vickezi.gateway.routes;

import com.sun.jdi.request.DuplicateRequestException;
import com.vickezi.gateway.service.RedisService;
import com.vickezi.globals.events.MessageProducerService;
import com.vickezi.globals.model.EmailRegistrationEvent;
import com.vickezi.globals.model.EmailVerificationEvent;
import com.vickezi.globals.model.RegistrationEmail;
import com.vickezi.globals.model.Response;
import com.vickezi.globals.util.CustomValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.security.InvalidParameterException;
import java.util.Optional;

import static com.vickezi.globals.util.Constants.USER_EMAIL_REGISTERED_EVENT_TOPIC;
import static com.vickezi.globals.util.Constants.USER_EMAIL_REGISTRATION_CONFIRMATION_NOTICE_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class RouteHelperUtilityMethodsTest {

    // Concrete implementation of the abstract class for testing
    private static class TestRouteHelper extends RouteHelperUtilityMethods {
        // Exposing protected methods for testing
        public <T> Mono<T> testValidateIdempotency(T input, RedisService redisService) {
            return validateIdempotency(input, redisService);
        }

        public Mono<ServerResponse> testCreateSuccessResponse(String validatedEmail) {
            return createSuccessResponse(validatedEmail);
        }

        public Mono<ServerResponse> testHandleErrors(Throwable error) {
            return handleErrors(error);
        }

        public Response testResponseBuilder(String message, boolean status, int statusCode) {
            return responseBuilder(message, status, statusCode);
        }

        public Mono<Tuple2<String, String>> testExtractAndValidateParams(ServerRequest request) {
            return extractAndValidateParams(request);
        }

        public EmailVerificationEvent testSetValidationEmailEvent(String token, String messageId) {
            return setValidationEmailEvent(token, messageId);
        }

        public Mono<String> testGetValidatedParam(ServerRequest request, String paramName) {
            return getValidatedParam(request, paramName);
        }

        public <T> Mono<Void> testAddMessageToQueue(String topic, T message, MessageProducerService<T> messageQueue) {
            return addMessageToQueue(topic, message, messageQueue);
        }

        public Mono<String> testValidateEmailRegistrationAndPutInMessageQueue(
                RegistrationEmail email,
                MessageProducerService<EmailRegistrationEvent> messageProducerService,
                RedisService redisService) {
            return validateEmailRegistrationAndPutInMessageQueue(email, messageProducerService, redisService);
        }

        public Mono<EmailVerificationEvent> testProcessVerificationLinkAndAddToQueue(
                Tuple2<String, String> params,
                RedisService redisService,
                MessageProducerService<EmailVerificationEvent> verificationEventMessageProducerService) {
            return processVerificationLinkAndAddToQueue(params, redisService, verificationEventMessageProducerService);
        }
    }

    private TestRouteHelper routeHelper;

    @Mock
    private RedisService redisService;

    @Mock
    private MessageProducerService<EmailRegistrationEvent> emailRegistrationEventProducer;

    @Mock
    private MessageProducerService<EmailVerificationEvent> emailVerificationEventProducer;

    @Mock
    private ServerRequest serverRequest;

    @BeforeEach
    void setUp() {
        routeHelper = new TestRouteHelper();
    }

    @Test
    void validateIdempotency_ValidInput_ReturnsInput() {
        // Arrange
        String testInput = "valid-input";

        // Mock CustomValidator.genericValidation to not throw an exception
        // (This might require adjusting your test structure based on how validation is implemented)

        when(redisService.isIdempotent(anyString(), any())).thenReturn(Mono.just(true));

        // Act
        Mono<String> result = routeHelper.testValidateIdempotency(testInput, redisService);

        // Assert
        StepVerifier.create(result)
                .expectNext(testInput)  // Expect the original input back
                .verifyComplete();

        verify(redisService).isIdempotent(testInput,testInput);
    }

    @Test
    void validateIdempotency_DuplicateInput_ReturnsError() {
        // Arrange
        String testInput = "duplicate-input";
        when(redisService.isIdempotent(anyString(), any())).thenReturn(Mono.just(false));

        // Act
        Mono<String> result = routeHelper.testValidateIdempotency(testInput, redisService);

        // Assert - Expecting DuplicateRequestException, not InvalidInputException
        StepVerifier.create(result)
                .expectError(DuplicateRequestException.class)  // Changed to correct error type
                .verify();

        verify(redisService).isIdempotent(testInput,testInput);
    }

    @Test
    void validateIdempotency_InvalidInput_ReturnsError() {
        // Arrange
        String testInput = null; // Invalid input that would fail validation

        // Act
        Mono<String> result = routeHelper.testValidateIdempotency(testInput, redisService);

        // Assert
        StepVerifier.create(result)
                .expectError(CustomValidator.InvalidInputException.class)
                .verify();

        // No Redis call should be made for invalid input
        verify(redisService, never()).isIdempotent(anyString(), any());
    }

    @Test
    void createSuccessResponse_ValidEmail_ReturnsOkResponse() {
        // Arrange
        String email = "test@example.com";

        // Act
        Mono<ServerResponse> result = routeHelper.testCreateSuccessResponse(email);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    // Check status code is 200 (OK)
                    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void handleErrors_DuplicateRequestException_ReturnsConflictResponse() {
        // Arrange
        DuplicateRequestException exception = new DuplicateRequestException("Duplicate request");

        // Act
        Mono<ServerResponse> result = routeHelper.testHandleErrors(exception);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.statusCode().equals(HttpStatus.CONFLICT)
                )
                .verifyComplete();
    }

    @Test
    void handleErrors_InvalidInputException_ReturnsBadRequestResponse() {
        // Arrange
        CustomValidator.InvalidInputException exception =
                new CustomValidator.InvalidInputException("Invalid input");

        // Act
        Mono<ServerResponse> result = routeHelper.testHandleErrors(exception);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.statusCode().equals(HttpStatus.BAD_REQUEST)
                )
                .verifyComplete();
    }

    @Test
    void handleErrors_RuntimeExceptionWithInvalidEmailMessage_ReturnsBadRequestResponse() {
        // Arrange
        RuntimeException exception = new RuntimeException("Invalid email format");

        // Act
        Mono<ServerResponse> result = routeHelper.testHandleErrors(exception);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.statusCode().equals(HttpStatus.BAD_REQUEST)
                )
                .verifyComplete();
    }

    @Test
    void handleErrors_GenericException_ReturnsInternalServerErrorResponse() {
        // Arrange
        Exception exception = new Exception("Generic error");

        // Act
        Mono<ServerResponse> result = routeHelper.testHandleErrors(exception);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.statusCode().equals(HttpStatus.INTERNAL_SERVER_ERROR)
                )
                .verifyComplete();
    }

    @Test
    void responseBuilder_ValidInputs_ReturnsCorrectResponse() {
        // Arrange
        String message = "Test message";
        boolean status = true;
        int statusCode = 200;

        // Act
        Response response = routeHelper.testResponseBuilder(message, status, statusCode);

        // Assert
        assertThat(response.message()).isEqualTo(message);
        assertThat(response.success()).isEqualTo(status);
        assertThat(response.statusCode()).isEqualTo(statusCode);
    }

    @Test
    void extractAndValidateParams_ValidRequest_ReturnsTuple() {
        // Arrange
        String token = "valid-token";
        String messageId = "valid-message-id";

        when(serverRequest.queryParam("token")).thenReturn(Optional.of(token));
        when(serverRequest.queryParam("messageId")).thenReturn(Optional.of(messageId));

        // Act
        Mono<Tuple2<String, String>> result = routeHelper.testExtractAndValidateParams(serverRequest);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(tuple ->
                        tuple.getT1().equals(token) && tuple.getT2().equals(messageId)
                )
                .verifyComplete();
    }

    @Test
    void extractAndValidateParams_MissingToken_ReturnsError() {
        // Arrange
        when(serverRequest.queryParam("token")).thenReturn(Optional.empty());
        when(serverRequest.queryParam("messageId")).thenReturn(Optional.of("valid-message-id"));

        // Act
        Mono<Tuple2<String, String>> result = routeHelper.testExtractAndValidateParams(serverRequest);

        // Assert
        StepVerifier.create(result)
                .expectError(InvalidParameterException.class)
                .verify();
    }

    @Test
    void setValidationEmailEvent_ValidInputs_ReturnsCorrectEvent() {
        // Arrange
        String token = "test-token";
        String messageId = "test-message-id";

        // Act
        EmailVerificationEvent event = routeHelper.testSetValidationEmailEvent(token, messageId);

        // Assert
        assertThat(event.token()).isEqualTo(token);
        assertThat(event.messageId()).isEqualTo(messageId);
    }

    @Test
    void getValidatedParam_ValidParam_ReturnsValue() {
        // Arrange
        String paramName = "testParam";
        String paramValue = "testValue";

        when(serverRequest.queryParam(paramName)).thenReturn(Optional.of(paramValue));

        // Act
        Mono<String> result = routeHelper.testGetValidatedParam(serverRequest, paramName);

        // Assert
        StepVerifier.create(result)
                .expectNext(paramValue)
                .verifyComplete();
    }

    @Test
    void getValidatedParam_MissingParam_ReturnsError() {
        // Arrange
        String paramName = "missingParam";

        when(serverRequest.queryParam(paramName)).thenReturn(Optional.empty());

        // Act
        Mono<String> result = routeHelper.testGetValidatedParam(serverRequest, paramName);

        // Assert
        StepVerifier.create(result)
                .expectError(InvalidParameterException.class)
                .verify();
    }

    @Test
    void addMessageToQueue_ValidMessage_CompletesSuccessfully() {
        // Arrange
        String topic = "test-topic";
        EmailRegistrationEvent message = new EmailRegistrationEvent("test@example.com");


        // Act
        Mono<Void> result = routeHelper.testAddMessageToQueue(topic, message, emailRegistrationEventProducer);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(emailRegistrationEventProducer).addMessageToQueue(topic,message);
    }

    @Test
    void addMessageToQueue_ExceptionThrown_ReturnsError() {
        // Arrange
        String topic = "error-topic";
        EmailRegistrationEvent message = new EmailRegistrationEvent("test@example.com");

        doThrow(new RuntimeException("Queue error")).when(emailRegistrationEventProducer)
                .addMessageToQueue(anyString(), any());

        // Act
        Mono<Void> result = routeHelper.testAddMessageToQueue(topic, message, emailRegistrationEventProducer);

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void validateEmailRegistrationAndPutInMessageQueue_ValidEmail_ReturnsValidatedEmail() {
        // Arrange
        String email = "test@example.com";
        RegistrationEmail registrationEmail = new RegistrationEmail(email);

        when(redisService.setValue(anyString(), any())).thenReturn(Mono.empty());

        // Act
        Mono<String> result = routeHelper.testValidateEmailRegistrationAndPutInMessageQueue(
                registrationEmail, emailRegistrationEventProducer, redisService);

        // Assert
        StepVerifier.create(result)
                .expectNext(email)
                .verifyComplete();

        verify(redisService).setValue(email, registrationEmail);
        verify(emailRegistrationEventProducer).addMessageToQueue(
                eq(USER_EMAIL_REGISTERED_EVENT_TOPIC),
                any(EmailRegistrationEvent.class));
    }

    @Test
    void processVerificationLinkAndAddToQueue_ValidParams_ReturnsEvent() {
        // Arrange
        String token = "valid-token";
        String messageId = "valid-message-id";
        Tuple2<String, String> params = Tuples.of(token, messageId);

        when(redisService.isIdempotent(anyString(), any())).thenReturn(Mono.just(true));


        // Act
        Mono<EmailVerificationEvent> result = routeHelper.testProcessVerificationLinkAndAddToQueue(
                params, redisService, emailVerificationEventProducer);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(event ->
                        event.token().equals(token) && event.messageId().equals(messageId)
                )
                .verifyComplete();

        verify(redisService).isIdempotent(eq(messageId), any(EmailVerificationEvent.class));
        verify(emailVerificationEventProducer).addMessageToQueue(
                eq(USER_EMAIL_REGISTRATION_CONFIRMATION_NOTICE_TOPIC),
                any(EmailVerificationEvent.class));
    }

    @Test
    void processVerificationLinkAndAddToQueue_DuplicateVerification_ReturnsError() {
        // Arrange
        Tuple2<String, String> params = Tuples.of("token", "duplicate-id");

        when(redisService.isIdempotent(anyString(), any())).thenReturn(Mono.just(false));

        // Act
        Mono<EmailVerificationEvent> result = routeHelper.testProcessVerificationLinkAndAddToQueue(
                params, redisService, emailVerificationEventProducer);

        // Assert
        StepVerifier.create(result)
                .expectError(DuplicateRequestException.class)
                .verify();

        verify(emailVerificationEventProducer, never()).addMessageToQueue(anyString(), any());
    }


}