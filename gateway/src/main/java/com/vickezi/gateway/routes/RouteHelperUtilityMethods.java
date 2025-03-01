package com.vickezi.gateway.routes;

import com.sun.jdi.request.DuplicateRequestException;
import com.vickezi.gateway.service.RedisService;
import com.vickezi.globals.model.EmailRegistrationEvent;
import com.vickezi.globals.events.MessageProducerService;
import com.vickezi.globals.model.EmailVerificationEvent;
import com.vickezi.globals.model.RegistrationEmail;
import com.vickezi.globals.model.Response;
import com.vickezi.globals.util.CustomValidator;
import jakarta.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.security.InvalidParameterException;
import static com.vickezi.globals.util.Constants.*;
public abstract class RouteHelperUtilityMethods {
    private final Logger logger = LoggerFactory.getLogger(RouteHelperUtilityMethods.class);

    /**
     * Validates whether a request is idempotent by first validating the input data
     * and then checking if it has already been processed in Redis.
     *
     * @param <T> The type of the input object
     * @param input The input object to validate and check for idempotency
     * @param redisService The Redis service to check idempotency
     * @return A Mono that emits the original input object if idempotent and valid
     * @throws CustomValidator.InvalidInputException If the input fails validation
     * @throws DuplicateRequestException If the request is not idempotent
     */
    protected <T> Mono<T> validateIdempotency(final T input, final RedisService redisService) {
        return Mono.fromCallable(() -> {
                    // Validate the input object, but ignore the returned value
                    // We only care that validation doesn't throw an exception
                    CustomValidator.genericValidation(input);
                    // Return the original input if validation passes
                    return input;
                })
                // Map any non-InvalidInputException to InvalidInputException
                .onErrorMap(ex -> !(ex instanceof CustomValidator.InvalidInputException),
                        ex -> new CustomValidator.InvalidInputException(ex.getMessage()))
                // Only check idempotency if validation passes
                .flatMap(validInput -> {
                    // Get a string representation for the Redis key
                    String key = String.valueOf(validInput);

                    // Check idempotency
                    return redisService.isIdempotent(key, validInput)
                            .flatMap(isIdempotent -> {
                                if (!isIdempotent) {
                                    logger.error("❌ Duplicate request attempt for: {}", key);
                                    return Mono.error(new DuplicateRequestException("Duplicate request attempt"));
                                }
                                // Return the original input object
                                return Mono.just(validInput);
                            });
                });
    }
    /**
     * Creates a success response for the client with appropriate HTTP status.
     *
     * @param validatedEmail The validated email that was processed
     * @return A Mono that emits a successful server response
     */
    protected Mono<ServerResponse> createSuccessResponse(final String validatedEmail) {
        return ServerResponse.ok()
                .bodyValue(responseBuilder(OPERATION_SUCCESSFUL, true, HttpStatus.ACCEPTED.value()));
    }
    /**
     * Handles errors that occur during request processing by mapping them to appropriate HTTP responses.
     *
     * @param error The error that occurred during processing
     * @return A Mono that emits an error response with appropriate status and message
     */
    protected Mono<ServerResponse> handleErrors(final Throwable error) {
        // Log the error first to ensure visibility
        logger.error("Error during request processing: {}", error.getMessage(), error);

        return switch (error) {
            case DuplicateRequestException duplicateRequestException -> ServerResponse.status(HttpStatus.CONFLICT)
                    .bodyValue(responseBuilder(error.getMessage(), false, HttpStatus.CONFLICT.value()));
            case CustomValidator.InvalidInputException invalidInputException -> ServerResponse.badRequest()
                    .bodyValue(responseBuilder(OPERATION_FAILED, false, HttpStatus.BAD_REQUEST.value()));
            case RuntimeException runtimeException when error.getMessage() != null && error.getMessage().contains("Invalid email format") ->
                // Handle the specific RuntimeException from email validation
                    ServerResponse.badRequest()
                            .bodyValue(responseBuilder("Invalid email format", false, HttpStatus.BAD_REQUEST.value()));
            default ->
                // Generic error handler for all other exceptions
                    ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .bodyValue(responseBuilder("Server processing error", false,
                                    HttpStatus.INTERNAL_SERVER_ERROR.value()));
        };
    }
    /**
     * Builds a standardized response object for client communication.
     *
     * @param message The message to include in the response
     * @param status The status flag indicating success or failure
     * @param statusCode The HTTP status code
     * @return A Response object containing the specified parameters
     */
    protected Response responseBuilder(final String message,
                             final boolean status,
                             final int statusCode){
        return new Response(message, status,statusCode);
    }
    /**
     * Extracts and validates required parameters from the server request.
     *
     * @param serverRequest The incoming HTTP request
     * @return A Mono that emits a tuple containing the validated token and messageId
     * @throws InvalidParameterException If parameters are missing or invalid
     */
    protected Mono<Tuple2<String, String>> extractAndValidateParams(final ServerRequest serverRequest) {
        return Mono.zip(
                getValidatedParam(serverRequest, "token"),
                getValidatedParam(serverRequest, "messageId")
        );
    }
    /**
     * Creates an EmailVerificationEvent from validated token and messageId.
     *
     * @param validatedToken The validated token from the request
     * @param validatedMessageId The validated messageId from the request
     * @return An EmailVerificationEvent object
     */
    protected EmailVerificationEvent setValidationEmailEvent(final String validatedToken,
                                                             final String validatedMessageId){
        return new EmailVerificationEvent(validatedToken, validatedMessageId);
    }
    /**
     * Extracts and validates a specific parameter from the server request.
     *
     * @param request The incoming HTTP request
     * @param paramName The name of the parameter to extract
     * @return A Mono that emits the validated parameter value
     * @throws InvalidParameterException If the parameter is missing or invalid
     */
    protected Mono<String> getValidatedParam(final ServerRequest request, final String paramName) {
        return Mono.fromCallable(() -> request.queryParam(paramName)
                        .orElseThrow(() -> new InvalidParameterException("Missing " + paramName)))
                .flatMap(value -> {
                    try {
                        return Mono.just(CustomValidator.validateStringLiterals(value).trim());
                    } catch (ValidationException ex) {
                        return Mono.error(new InvalidParameterException("Invalid " + paramName));
                    }
                });
    }
    /**
     * Adds a message to the specified queue asynchronously.
     *
     * @param <T> The type of message to be queued
     * @param topic The topic to which the message will be published
     * @param message The message to be queued
     * @param messageQueue The message producer service to use for queuing
     * @return A Mono that completes when the message has been queued
     */
    protected <T>Mono<Void> addMessageToQueue(final String topic, final T message,
                                              final MessageProducerService<T> messageQueue) {
        return Mono.defer(() -> {
            try {
                messageQueue.addMessageToQueue(topic, message);
                return Mono.empty();
            } catch (Exception e) {
                return Mono.error(e); // Properly propagate any exceptions
            }
        });
    }

    /**
     * Processes a validated email by storing it in Redis and queuing an email registration event.
     *
     * @param email The validated registration email
     * @param messageProducerService The message producer service for queuing events
     * @param redisService The Redis service for storing email data
     * @return A Mono that emits the validated email string if successful
     * @throws CustomValidator.InvalidInputException If the email format is invalid
     */
    protected Mono<String> validateEmailRegistrationAndPutInMessageQueue(final RegistrationEmail email,
                                                                         final MessageProducerService<EmailRegistrationEvent> messageProducerService,
                                                                         final RedisService redisService) {
        // We already validated the email in validateIdempotency, so we can use it directly
        String validatedEmail = email.email().trim();

        return redisService.setValue(email.email(), email)
                .then(addMessageToQueue(
                        USER_EMAIL_REGISTERED_EVENT_TOPIC,
                        new EmailRegistrationEvent(validatedEmail),
                        messageProducerService))
                .thenReturn(validatedEmail);
    }
    /**
     * Processes email verification by checking idempotency and queuing a confirmation event.
     *
     * @param params A tuple containing the token and messageId
     * @param redisService The Redis service for checking idempotency
     * @param verificationEventMessageProducerService The message producer service for queuing events
     * @return A Mono that emits the EmailVerificationEvent if successful
     * @throws DuplicateRequestException If this verification has already been processed
     */
    protected Mono<EmailVerificationEvent> processVerificationLinkAndAddToQueue(final Tuple2<String, String> params,
                                                                                final RedisService redisService,
                                                                                final MessageProducerService<EmailVerificationEvent> verificationEventMessageProducerService) {
        final String token = params.getT1();
        final String messageId = params.getT2();
        final EmailVerificationEvent event = setValidationEmailEvent(token, messageId);

        return redisService.isIdempotent(messageId, event)
                .flatMap(isIdempotent -> {
                    if (!isIdempotent) {
                        logger.warn("Duplicate verification attempt for messageId: {}", messageId);
                        return Mono.error(new DuplicateRequestException("Duplicate verification attempt"));
                    }
                    return addMessageToQueue(
                            USER_EMAIL_REGISTRATION_CONFIRMATION_NOTICE_TOPIC,
                            event,
                            verificationEventMessageProducerService
                    ).thenReturn(event);
                });
    }


}
