package com.vickezi.registration.queue;

import com.vickezi.globals.model.EmailRegistrationEvent;
import com.vickezi.globals.events.MessageProducerService;
import com.vickezi.globals.model.EmailVerificationEvent;
import com.vickezi.globals.model.RegistrationMessage;
import com.vickezi.registration.exception.RegistrationException;
import com.vickezi.registration.services.RegistrationServiceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import static com.vickezi.globals.util.Constants.*;
/**
 * Service responsible for receiving and processing email registration and verification events.
 * Listens to Kafka topics and processes events.
 * Does not use auto commit so we can manage the failures retries and writing back to the same cluster
 */
@Service
public class ReceiverNewEmailRegistrationMessage {
    private final RegistrationServiceHandler registrationServiceHandler;
    private final MessageProducerService<RegistrationMessage> messageProducerService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RetryTemplate retryTemplate;
    private static final Logger logger = LoggerFactory.getLogger(ReceiverNewEmailRegistrationMessage.class);
    /**
     * Constructs a new ReceiverNewEmailRegistrationMessage with the given dependencies.
     *
     * @param registrationServiceHandler the service handler for registration
     * @param messageProducerService the service for producing messages to Kafka
     * @param kafkaTemplate the Kafka template for sending messages
     * @param retryTemplate the template for retrying operations
     */
    public ReceiverNewEmailRegistrationMessage(RegistrationServiceHandler registrationServiceHandler,
                                               MessageProducerService<RegistrationMessage> messageProducerService,
                                               KafkaTemplate<String, Object> kafkaTemplate,
                                               RetryTemplate retryTemplate) {
        this.registrationServiceHandler = registrationServiceHandler;
        this.messageProducerService = messageProducerService;
        this.kafkaTemplate = kafkaTemplate;
        this.retryTemplate = retryTemplate;
    }
    /**
     * Listens to the Kafka topic for email registration events and processes them.
     *
     * @param email the received email registration event
     * @param ack Kafka acknowledgment object
     */
    @KafkaListener(topics = USER_EMAIL_REGISTERED_EVENT_TOPIC, groupId = "user-registration-group")
    public void handleEmailRegistration(final EmailRegistrationEvent email, final Acknowledgment ack) {
        if (email == null) {
            logger.warn("❌ Received null email event, skipping.");
            return;
        }
        logger.info("📨 Processing email registration");
        processMessage(email, () -> {
            RegistrationMessage message = registrationServiceHandler.registerUserByEmail(email.email());
            sendMessageToKafka(message,SEND_USER_EMAIL_REGISTRATION_MESSAGE);
            logger.info("✅ Email registration successful, ID: {}", message.messageId());
        }, ack);
    }
    /**
     * Listens to the Kafka topic for email verification events and processes them.
     *
     * @param emailVerificationEvent the received email verification event
     * @param ack Kafka acknowledgment object
     */
    @KafkaListener(topics = USER_EMAIL_REGISTRATION_CONFIRMATION_NOTICE_TOPIC, groupId = "email-verification-message-group")
    public void handleEmailVerification(final EmailVerificationEvent emailVerificationEvent,final Acknowledgment ack) {
        if (emailVerificationEvent == null) {
            logger.warn("❌ Received null email verification event, skipping.");
            return;
        }
        logger.info("📩 Processing email verification for ID");
        processMessage(emailVerificationEvent, () -> {
            registrationServiceHandler.confirmEmailLinkIsValid(emailVerificationEvent.token());
            logger.info("✅ Email verification successful for ID: {}", emailVerificationEvent.messageId());
        }, ack);
    }
    /**
     * Generic method to process messages.
     *
     * @param <T> the type of the message
     * @param message the message to process
     * @param messageProcessor the processing function
     * @param ack Kafka acknowledgment object
     */
    private <T> void processMessage(final T message,final  Runnable messageProcessor,final  Acknowledgment ack) {
        kafkaTemplate.executeInTransaction(trx -> {
            try {
                retryTemplate.execute(context -> {
                    messageProcessor.run();
                    return null;
                });

                ack.acknowledge();
                logger.info("✅ Message successfully processed");
            } catch (RegistrationException ex) {
                logger.error("❌ Registration error: {}", ex.getMessage(), ex);
                sendToDeadLetterTopic(message);
                ack.acknowledge();
            } catch (Exception ex) {
                logger.error("❌ Unexpected error: {}", ex.getMessage(), ex);
                sendToDeadLetterTopic(message);
                ack.acknowledge();
            }
            return null;
        });
    }
    /**
     * Sends a message to the Kafka topic.
     *
     * @param message the message to send
     */
    private void sendMessageToKafka(final RegistrationMessage message, final String topic) {
        messageProducerService.addMessageToQueue(topic, message)
                .exceptionally(ex -> {
                    logger.error("❌ Kafka send failure to topic {}: {}", topic, ex.getMessage(), ex);
                    return null;
                });
    }
    /**
     * Sends the message to the dead letter topic.
     *
     * @param <T> the type of the message
     * @param message the message to send
     */
    private <T> void sendToDeadLetterTopic(final T message) {
        if (message instanceof RegistrationMessage registrationMessage) {
            messageProducerService.sendToDeadLetterTopic(registrationMessage);
        } else {
            logger.error("❌ Message type {} not supported for DLQ", message.getClass().getSimpleName());
        }
    }
}
