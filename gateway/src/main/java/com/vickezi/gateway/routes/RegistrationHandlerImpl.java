package com.vickezi.gateway.routes;

import com.vickezi.gateway.queue.MessageProducerService;
import com.vickezi.globals.events.EmailRegistrationEvent;
import com.vickezi.globals.model.RegistrationEmail;
import com.vickezi.globals.model.Response;
import com.vickezi.globals.util.CustomValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static com.vickezi.globals.util.Constants.*;

@Component
public class RegistrationHandlerImpl implements RegistrationHandler{
    private final MessageProducerService<EmailRegistrationEvent> messageProducerService;

    public RegistrationHandlerImpl(MessageProducerService<EmailRegistrationEvent> messageProducerService) {
        this.messageProducerService = messageProducerService;
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
                        // Do something with the email
                        messageProducerService.addMessageToQueue(USER_EMAIL_REGISTERED_EVENT_TOPIC,
                                new EmailRegistrationEvent(validatedEmail));
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

    private Response responseBuilder(String message, boolean status, int statusCode){
       return new Response(message, status,statusCode);
    }

}
