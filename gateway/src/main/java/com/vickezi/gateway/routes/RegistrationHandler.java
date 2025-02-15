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
     * Handles user registration. When a user email is received, perform validation, checks if the email already exists.
     * Only adds a new record if it's a new email.
     *
     * @param serverRequest the server request containing the user email
     * @return a Mono emitting the server response
     */
    Mono<ServerResponse> register(ServerRequest serverRequest);
}
