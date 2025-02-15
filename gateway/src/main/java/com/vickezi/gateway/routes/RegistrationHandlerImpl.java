package com.vickezi.gateway.routes;

import com.vickezi.globals.model.EmailRegistrationEvent;
import com.vickezi.globals.events.MessageProducerService;
import com.vickezi.globals.model.EmailVerificationEvent;
import com.vickezi.globals.model.RegistrationEmail;
import com.vickezi.globals.model.Response;
import com.vickezi.globals.util.CustomValidator;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static com.vickezi.globals.util.Constants.*;

@Component
@Import(MessageProducerService.class)
public class RegistrationHandlerImpl implements RegistrationHandler{
    private final MessageProducerService<EmailRegistrationEvent> messageProducerService;
    private final MessageProducerService<EmailVerificationEvent> eventMessageProducerService;

    public RegistrationHandlerImpl(MessageProducerService<EmailRegistrationEvent> messageProducerService, MessageProducerService<EmailVerificationEvent> eventMessageProducerService) {
        this.messageProducerService = messageProducerService;
        this.eventMessageProducerService = eventMessageProducerService;
    }

    @Override
    public Mono<ServerResponse> register(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(RegistrationEmail.class)
                .flatMap(email -> {
                    try {
                        // Validate and sanitize the email
                        final String validatedEmail = String.valueOf(CustomValidator.validateInput(email.email())).trim();
                        // Check if the email is valid or not after validation
                        if (validatedEmail.isEmpty()) {
                            return ServerResponse.badRequest()
                                    .bodyValue("Invalid email provided");
                        }
                        // Add the email to the message Queue
                        addMessageToQueue(USER_EMAIL_REGISTERED_EVENT_TOPIC,
                                new EmailRegistrationEvent(validatedEmail),messageProducerService);
                        // Return a successful response with the validated email
                        return ServerResponse.ok()
                                .bodyValue(responseBuilder(OPERATION_SUCCESSFUL, true,
                                        HttpStatus.ACCEPTED.value()));
                    } catch (CustomValidator.InvalidInputException ex) {
                        // If validation fails, return a bad request with a specific message
                        return ServerResponse.badRequest()
                                .bodyValue(responseBuilder(OPERATION_FAILED, false,
                                        HttpStatus.BAD_REQUEST.value()));
                    } catch (Exception ex) {
                        // Catch unexpected errors and return a general server error
                        return ServerResponse.status(500)
                                .bodyValue(responseBuilder(ex.getMessage(), false,
                                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
                    }
                });
    }



    public Mono<ServerResponse> verifyEmail(ServerRequest serverRequest) {
        // Extract query parameters
        String token = serverRequest.queryParam("token").orElse(null);
        String messageId = serverRequest.queryParam("messageId").orElse(null);
        // Validate required parameters
        if (token == null || messageId == null) {
            return ServerResponse.badRequest().bodyValue(
                    responseBuilder("Email Verification Failed, try again", false,
                            HttpStatus.NOT_ACCEPTABLE.value()));
        }
        // Validate and sanitize the email
        final String validatedToken = String.valueOf(CustomValidator.validateStringLiterals(token)).trim();
        final String validatedMessageId = String.valueOf(CustomValidator.validateStringLiterals(token)).trim();
        // Add the email to the message Queue
        addMessageToQueue(EMAIL_VERIFICATION_MESSAGE_TOPIC,
                new EmailVerificationEvent(validatedToken, validatedMessageId),eventMessageProducerService);
        // Return a response
        // Return a successful response with the validated email
        return ServerResponse.ok()
                .bodyValue(responseBuilder(OPERATION_SUCCESSFUL, true,
                        HttpStatus.ACCEPTED.value()));
    }
    private <T>void addMessageToQueue(String topic, T message, MessageProducerService<T> messageQueue) {
        messageQueue.addMessageToQueue(topic,message);
    }
    private Response responseBuilder(String message, boolean status, int statusCode){
        return new Response(message, status,statusCode);
    }
}
