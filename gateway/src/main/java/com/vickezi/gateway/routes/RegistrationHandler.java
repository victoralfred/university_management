package com.vickezi.gateway.routes;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
/**
 * Interface for handling user registration.
 */
public interface RegistrationHandler {
    /**
     *  Handles user registration. When a user email is received
     *  first validate the email, if validation is successful, checks if the email already exists in cache,
     *  if it doesn't, add the email to cache for idempotent future checks, and finally add the email to the
     *  new registration message queue
     *  {@link com.vickezi.globals.util.Constants#USER_EMAIL_REGISTRATION_CONFIRMATION_NOTICE_TOPIC)
     * <p>
     *
     * @param serverRequest the server request containing the user email
     * @return a Mono emitting the server response
     */
    Mono<ServerResponse> register(ServerRequest serverRequest);
    /**
     * Handles email verification requests by validating request parameters,
     * checking idempotency, and queuing a message for further processing.
     * @param serverRequest The incoming HTTP request containing token and messageId parameters
     * @return A Mono that emits the server response with appropriate status code and body
     */
    Mono<ServerResponse> verifyEmail(ServerRequest serverRequest);
}
