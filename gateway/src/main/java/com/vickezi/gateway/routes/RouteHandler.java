package com.vickezi.gateway.routes;

import com.vickezi.globals.util.CustomValidator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class RouteHandler implements RegistrationHandler{
    private final CustomValidator customValidator;

    public RouteHandler(CustomValidator customValidator) {
        this.customValidator = customValidator;
    }

    @Override
    public Mono<ServerResponse> register(ServerRequest serverRequest) {

        return ServerResponse.ok().bodyValue("Done");
    }
}
