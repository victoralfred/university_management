package com.vickezi.registration.queue;
import com.vickezi.globals.model.EmailRegistrationEvent;
import com.vickezi.globals.events.MessageProducerService;
import com.vickezi.globals.model.EmailVerificationEvent;
import com.vickezi.globals.model.RegistrationMessage;
import com.vickezi.registration.services.RegistrationServiceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.vickezi.globals.util.Constants.*;

/**
 * Service responsible for receiving and processing email registration events.
 * Listens to Kafka topics and processes events asynchronously.
 */
@Service
public class ReceiverNewEmailRegistrationMessage {
    private final RegistrationServiceHandler registrationServiceHandler;
    private final MessageProducerService<RegistrationMessage> messageProducerService;
    private final Executor asyncExecutor;

    private final Logger logger = LoggerFactory.getLogger(ReceiverNewEmailRegistrationMessage.class);

    public ReceiverNewEmailRegistrationMessage(RegistrationServiceHandler registrationServiceHandler, MessageProducerService<RegistrationMessage> messageProducerService, Executor asyncExecutor) {
        this.registrationServiceHandler = registrationServiceHandler;
        this.messageProducerService = messageProducerService;
        this.asyncExecutor = asyncExecutor;
    }
    /**
     * Listens to the Kafka topic for email registration events and processes them asynchronously.
     *
     * @param emailRegistrationEvent The received email registration event.
     */
    @KafkaListener(topics = {USER_EMAIL_REGISTERED_EVENT_TOPIC},
    groupId = "user-registration-group")
    public void newEmailRegistration(EmailRegistrationEvent emailRegistrationEvent) {
        logger.info("Received email registration event");
        processMessage(emailRegistrationEvent.email(),  ()-> {
            handleEmailRegistration(emailRegistrationEvent);
        });
    }
    /**
     * Listens to the Kafka topic for email verification links events and processes them asynchronously.
     *
     * @param emailVerificationEvent The received email verification event.
     */
    @KafkaListener(topics = {EMAIL_VERIFICATION_MESSAGE_TOPIC},
            groupId = "email-verification-message-group")
    public void emailVerificationMessage(EmailVerificationEvent emailVerificationEvent) {
        logger.info("Received email verification event");
        processMessage(emailVerificationEvent,  ()-> {
            verifyEmail(emailVerificationEvent);
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

    /**
     * Handles email registration asynchronously, registers the user, and sends a confirmation event.
     *
     * @param email The email registration event.
     */
    @Async
    public void handleEmailRegistration(final EmailRegistrationEvent email) {
        if (email == null) {
            logger.warn("Received a null email event, skipping processing.");
            return;
        }
        CompletableFuture.runAsync(() -> {
            logger.info("Processing email registration for: {}", email.email());
            try {
                RegistrationMessage message = registrationServiceHandler.registerUserByEmail(email.email());
                logger.info("Successfully registered user");
                sendMessageToKafka(message);
            } catch (Exception e) {
                logger.error("Unexpected error during email registration: {}", e.getMessage(), e);
            }
        }, asyncExecutor);
    }
    /**
     * Sends a message to the Kafka topic, ensuring exceptions are handled.
     *
     * @param registrationMessage {@link RegistrationMessage}The message to send.
     */
    private void sendMessageToKafka(RegistrationMessage registrationMessage) {
        messageProducerService.addMessageToQueue(USER_REGISTRATION_CONFIRMATION_NOTICE_TOPIC, registrationMessage)
                .exceptionally(ex -> {
                    logger.error("Failed to send message to Kafka topic {}: {}",
                            USER_REGISTRATION_CONFIRMATION_NOTICE_TOPIC,
                            ex.getMessage(), ex);
                    return null;
        });
    }
    private void verifyEmail(EmailVerificationEvent emailVerificationEvent) {
        try{
            registrationServiceHandler.confirmEmailLinkIsValid(emailVerificationEvent.token());
        }catch (RuntimeException e){
            logger.error("Email ID {} failed verification {}",emailVerificationEvent.messageId(), e.getMessage(), e.getCause());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}



