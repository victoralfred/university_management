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
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.security.InvalidParameterException;

import static com.vickezi.globals.util.Constants.*;

@Component
@Import(MessageProducerService.class)
public class RegistrationHandlerImpl implements RegistrationHandler{
    private final MessageProducerService<EmailRegistrationEvent> messageProducerService;
    private final MessageProducerService<EmailVerificationEvent> verificationEventMessageProducerService;
    private final RedisService redisService;
    private final Logger logger = LoggerFactory.getLogger(RegistrationHandlerImpl.class);

    public RegistrationHandlerImpl(MessageProducerService<EmailRegistrationEvent> messageProducerService, MessageProducerService<EmailVerificationEvent> verificationEventMessageProducerService, RedisService redisService) {
        this.messageProducerService = messageProducerService;
        this.verificationEventMessageProducerService = verificationEventMessageProducerService;
        this.redisService = redisService;
    }

    @Override
    public Mono<ServerResponse> register(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(RegistrationEmail.class)
                .flatMap(email ->
                        redisService.isIdempotent(email.email(), email)
                                .flatMap(isIdempotent -> {
                                    if (!isIdempotent) {
                                        logger.error("❌ Duplicate Email registration attempt");
                                        return Mono.error(new DuplicateRequestException("Duplicate registration attempt"));
                                    }
                                    return Mono.just(email);
                                })
                )
                .flatMap(email -> {
                    final String validatedEmail;
                    try {
                        validatedEmail = String.valueOf(CustomValidator.validateInput(email.email())).trim();
                    } catch (CustomValidator.InvalidInputException ex) {
                        return Mono.error(ex);
                    }
                    return redisService.setValue(email.email(), email)
                            .then(addMessageToQueue(USER_EMAIL_REGISTERED_EVENT_TOPIC,
                                    new EmailRegistrationEvent(validatedEmail),messageProducerService))
                            .thenReturn(validatedEmail);
                })
                .flatMap(validatedEmail -> ServerResponse.ok()
                        .bodyValue(responseBuilder(OPERATION_SUCCESSFUL, true, HttpStatus.ACCEPTED.value()))
                )
                .onErrorResume(DuplicateRequestException.class, ex ->
                        ServerResponse.status(HttpStatus.CONFLICT)
                                .bodyValue(responseBuilder(ex.getMessage(), false, HttpStatus.CONFLICT.value()))
                )
                .onErrorResume(CustomValidator.InvalidInputException.class, ex ->
                        ServerResponse.badRequest()
                                .bodyValue(responseBuilder(OPERATION_FAILED, false, HttpStatus.BAD_REQUEST.value()))
                )
                .onErrorResume(ex ->
                        ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .bodyValue(responseBuilder("Server processing error", false,
                                        HttpStatus.INTERNAL_SERVER_ERROR.value()))
                );
    }

    public Mono<ServerResponse> verifyEmail(ServerRequest serverRequest) {
        return Mono.zip(
                        getValidatedParam(serverRequest, "token"),
                        getValidatedParam(serverRequest, "messageId")
                )
                .flatMap(tuple -> {
                    final String token = tuple.getT1();
                    final String messageId = tuple.getT2();
                    final EmailVerificationEvent event = setValidationEmailEvent(token, messageId);

                    return redisService.isIdempotent(messageId, event)
                            .flatMap(isIdempotent -> {
                                if (!isIdempotent) {
                                    logger.warn("Duplicate verification attempt for messageId: {}", messageId);
                                    return Mono.error(new DuplicateRequestException("Duplicate verification attempt"));
                                }
                                return addMessageToQueue(USER_REGISTRATION_CONFIRMATION_NOTICE_TOPIC,
                                        event, verificationEventMessageProducerService)
                                        .thenReturn(event);
                            });
                })
                .flatMap(event -> ServerResponse.ok()
                        .bodyValue(responseBuilder(OPERATION_SUCCESSFUL, true, HttpStatus.ACCEPTED.value()))
                )
                .onErrorResume(DuplicateRequestException.class, ex ->
                        ServerResponse.status(HttpStatus.CONFLICT)
                                .bodyValue(responseBuilder(ex.getMessage(), false, HttpStatus.CONFLICT.value()))
                )
                .onErrorResume(InvalidParameterException.class, ex ->
                        ServerResponse.badRequest()
                                .bodyValue(responseBuilder(ex.getMessage(), false, HttpStatus.BAD_REQUEST.value()))
                )
                .onErrorResume(ex -> {
                    logger.error("Email verification failed unexpectedly", ex);
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .bodyValue(responseBuilder("Verification processing error", false,
                                    HttpStatus.INTERNAL_SERVER_ERROR.value()));
                });
    }


    private <T>Mono<Void> addMessageToQueue(String topic, T message, MessageProducerService<T> messageQueue) {
       return Mono.fromRunnable(()-> messageQueue.addMessageToQueue(topic,message));
    }
    private Response responseBuilder(String message, boolean status, int statusCode){
        return new Response(message, status,statusCode);
    }
    private EmailVerificationEvent setValidationEmailEvent(String validatedToken, String validatedMessageId){
        return new EmailVerificationEvent(validatedToken, validatedMessageId);
    }
    private Mono<String> getValidatedParam(ServerRequest request, String paramName) {
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
}
