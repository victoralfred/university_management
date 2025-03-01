package com.vickezi.gateway.routes;

import com.vickezi.gateway.service.RedisService;
import com.vickezi.globals.model.EmailRegistrationEvent;
import com.vickezi.globals.events.MessageProducerService;
import com.vickezi.globals.model.EmailVerificationEvent;
import com.vickezi.globals.model.RegistrationEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@Import(MessageProducerService.class)
public class RegistrationHandlerImpl extends RouteHelperUtilityMethods implements RegistrationHandler{
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
                .flatMap(email->super.validateIdempotency(email.email(),redisService))
                .flatMap(email->super.validateEmailRegistrationAndPutInMessageQueue(new RegistrationEmail(email), messageProducerService, redisService))
                .flatMap(super::createSuccessResponse)
                .onErrorResume(this::handleErrors);
    }
    @Override
    public Mono<ServerResponse> verifyEmail(ServerRequest serverRequest) {
        return extractAndValidateParams(serverRequest)
                .flatMap(params -> super.processVerificationLinkAndAddToQueue(params, redisService, verificationEventMessageProducerService))
                .flatMap(email-> super.createSuccessResponse(email.messageId()))
                .onErrorResume(this::handleErrors);
    }

}
