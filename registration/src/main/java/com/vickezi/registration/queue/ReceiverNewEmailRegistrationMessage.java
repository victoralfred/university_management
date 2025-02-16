package com.vickezi.registration.queue;
import com.vickezi.globals.model.EmailRegistrationEvent;
import com.vickezi.globals.events.MessageProducerService;
import com.vickezi.globals.model.EmailVerificationEvent;
import com.vickezi.globals.model.RegistrationMessage;
import com.vickezi.registration.exception.RegistrationException;
import com.vickezi.registration.services.RegistrationServiceHandler;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;

import static com.vickezi.globals.util.Constants.*;

/**
 * Service responsible for receiving and processing email registration events.
 * Listens to Kafka topics and processes events asynchronously.
 */
@Service
public class ReceiverNewEmailRegistrationMessage {
    private final RegistrationServiceHandler registrationServiceHandler;
    private final MessageProducerService<RegistrationMessage> messageProducerService;
    private final ThreadPoolTaskExecutor asyncExecutor;
    private final RetryTemplate retryTemplate;
    private final Logger logger = LoggerFactory.getLogger(ReceiverNewEmailRegistrationMessage.class);
    /**
     * Constructs a new ReceiverNewEmailRegistrationMessage with the given dependencies.
     *
     * @param registrationServiceHandler the service handler for registration
     * @param messageProducerService the service for producing messages to Kafka
     * @param asyncExecutor the executor for asynchronous tasks
     * @param retryTemplate the template for retrying operations
     */
    public ReceiverNewEmailRegistrationMessage(RegistrationServiceHandler registrationServiceHandler,
                                               MessageProducerService<RegistrationMessage> messageProducerService,
                                               @Qualifier("kafkaConsumerTaskExecutor") ThreadPoolTaskExecutor asyncExecutor, RetryTemplate retryTemplate) {
        this.registrationServiceHandler = registrationServiceHandler;
        this.messageProducerService = messageProducerService;
        this.asyncExecutor = asyncExecutor;
        this.retryTemplate = retryTemplate;
    }
    /**
     * Listens to the Kafka topic for email registration events and processes them asynchronously.
     *
     * @param email The received email registration event.
     * @param ack Kafka acknowledgment object
     */
    @KafkaListener(topics = {USER_EMAIL_REGISTERED_EVENT_TOPIC},
    groupId = "user-registration-group")
    public void handleEmailRegistration(
            @Payload EmailRegistrationEvent email,
            Acknowledgment ack
    ) {
        if (email == null) {
            logger.warn("❌ Received null email event, skipping..");
            return;
        }
        logger.info("Processing email registration");

        processMessage(email, () -> {
            // Business logic: register user and send Kafka message
            RegistrationMessage message = registrationServiceHandler.registerUserByEmail(email.email());
            sendMessageToKafka(message);
            logger.info("✅ Successfully processed email registration ID: {}", message.messageId());
        }, ack);
    }
    /**
     * Listens to the Kafka topic for email verification links events and processes them asynchronously.
     *
     * @param emailVerificationEvent The received email verification event.
     * @param ack Kafka acknowledgment object
     */
    @KafkaListener(topics = {EMAIL_VERIFICATION_MESSAGE_TOPIC},
            groupId = "email-verification-message-group")
    public void emailVerificationMessage(@Payload EmailVerificationEvent emailVerificationEvent,
                                         Acknowledgment ack){
        if (emailVerificationEvent == null) {
            logger.warn("❌ Received null email event, skipping.");
            return;
        }
        logger.info("Processing email registration for ID: {}", emailVerificationEvent.messageId());

        processMessage(emailVerificationEvent, () -> {
            // Business logic: register user and send Kafka message
            try{
                registrationServiceHandler.confirmEmailLinkIsValid(emailVerificationEvent.token());
                logger.info("✅ Successfully processed email registration for ID: {}", emailVerificationEvent.messageId());
            }catch (RuntimeException ex){
                logger.error("❌ Failed to process message {}: {}", emailVerificationEvent.messageId(), ex.getMessage(),
                        ex);
                throw new RuntimeException(ex);
            }
        }, ack);
    }
    /**
     * Generic method to process messages.
     *
     * @param <T> The type of the message.
     * @param message The message to process.
     * @param messageProcessor The processing function.
     * @param ack Kafka acknowledgment object
     */
    private <T> void processMessage(T message, Runnable messageProcessor, Acknowledgment ack) {
        CompletableFuture
                .runAsync(() ->
                                retryTemplate.execute(context -> {
                                    messageProcessor.run(); // Executes registration + Kafka send
                                    return null;
                                }),
                        asyncExecutor
                )
                .thenRun(() -> {
                    ack.acknowledge(); // Acknowledge on success
                    logger.info("✅ Message processed successfully");
                })
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause();
                    logger.error("❌ Failed to process message{}",  cause.getMessage(), cause);

                    // Send to DLQ for non-retryable errors or retry exhaustion
                    sendToDeadLetterTopic(message);
                    ack.acknowledge(); // Prevent redelivery after DLQ
                    return null;
                });
    }
    /**
     * Sends a message to the Kafka topic, ensuring exceptions are handled.
     *
     * @param registrationMessage The message to send.
     */
    private void sendMessageToKafka(RegistrationMessage registrationMessage) {
        messageProducerService.addMessageToQueue(USER_REGISTRATION_CONFIRMATION_NOTICE_TOPIC,
                        registrationMessage)
                    .exceptionally(ex -> {
                        logger.error("❌ Failed to send message to Kafka topic {}: {}",
                                USER_REGISTRATION_CONFIRMATION_NOTICE_TOPIC,
                                ex.getMessage(), ex);
                        return null;
                    });
    }

    /**
     * Sends the message to the dead letter topic.
     *
     * @param <T> The type of the message.
     * @param message The message to send.
     */
    private <T>void sendToDeadLetterTopic(T message) {
        messageProducerService.sendToDeadLetterTopic((RegistrationMessage) message);
    }
}



