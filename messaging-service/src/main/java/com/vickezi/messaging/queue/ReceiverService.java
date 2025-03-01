package com.vickezi.messaging.queue;


import com.vickezi.globals.model.EmailVerificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import com.vickezi.globals.model.RegistrationMessage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.vickezi.globals.util.Constants.SEND_USER_EMAIL_REGISTRATION_MESSAGE;
import static com.vickezi.globals.util.Constants.USER_EMAIL_REGISTRATION_CONFIRMATION_NOTICE_TOPIC;

/**
 * Service responsible for receiving and processing email registration events.
 * Listens to Kafka topics and processes events asynchronously.
 */
@Service
public class ReceiverService {
    private final Executor asyncExecutor;
    private final EmailServiceImpl emailService;
    private final Logger logger = LoggerFactory.getLogger(ReceiverService.class);

    public ReceiverService(@Qualifier("applicationTaskExecutor") Executor asyncExecutor, EmailServiceImpl emailService) {
        this.asyncExecutor = asyncExecutor;
        this.emailService = emailService;
    }
    /**
     * Listens to the Kafka topic for email sending events and processes them asynchronously.
     *
     * @param registrationEvent The received email sending event.
     */
    @KafkaListener(topics = {SEND_USER_EMAIL_REGISTRATION_MESSAGE},
    groupId = "email-verification-message-group")
    public void listenOnNewEmailRegistration(RegistrationMessage registrationEvent) {
        logger.info("Received email registration event");
        processMessage(registrationEvent,  ()-> {
            sendEmail(registrationEvent);
        });
    }

    /**
     * Generic method to process messages.
     *
     * @param <T>              The type of the message.
     * @param message          The message to process.
     * @param messageProcessor The processing function.
     */
    private <T> void processMessage(T message, Runnable messageProcessor) {
            try{
                messageProcessor.run();
            }catch (Exception e){
                logger.error("Error processing message {}: {}", message, e.getMessage(), e);
            }
    }
    private <T>void sendEmail(RegistrationMessage registrationEvent) {
        if (registrationEvent == null) {
            logger.warn("Received a null email sending event, skipping processing.");
            return;
        }
        CompletableFuture.runAsync(() -> {
            logger.info("Processing email registration");
            try {
                emailService.sendEmail(registrationEvent);
            } catch (Exception e) {
                logger.error("Unexpected error during email registration: {}", e.getMessage(), e);
            }
        }, asyncExecutor);
    }
    
}



