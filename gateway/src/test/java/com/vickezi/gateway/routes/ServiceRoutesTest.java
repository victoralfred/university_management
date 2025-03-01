package com.vickezi.gateway.routes;

import com.vickezi.globals.model.Response;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;

import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest
@Import(ServiceRoutes.class)
public class ServiceRoutesTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private RegistrationHandlerImpl registrationHandler;

    @Test
    void registerEndpoint_ShouldCallHandlerRegisterMethod() {
        // Arrange
        when(registrationHandler.register(any()))
                .thenReturn(ServerResponse.ok().bodyValue(new Response("Email Registration successful", true, 202)));

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"test@example.com\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Email Registration successful")
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.statusCode").isEqualTo(202);
    }

    @Test
    void verifyEmailEndpoint_ShouldCallHandlerVerifyEmailMethod() {
        // Arrange
        when(registrationHandler.verifyEmail(any()))
                .thenReturn(ServerResponse.ok().bodyValue(new Response("Email verification successful", true, 202)));

        // Act & Assert
        webTestClient.get()
                .uri("/api/v1/registration?token=abc123&messageId=msg456")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Email verification successful")
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.statusCode").isEqualTo(202);
    }
}