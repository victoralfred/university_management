package com.vickezi.gateway.routes;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class ServiceRoutes {
    @Bean
    public RouterFunction<ServerResponse> registrationRoutes(RegistrationHandlerImpl handler){
        return RouterFunctions.route().path("/api/v1/registration", path->path
                .POST("",handler::register)
                .GET("", handler::verifyEmail)
        ).build();
    }
}
