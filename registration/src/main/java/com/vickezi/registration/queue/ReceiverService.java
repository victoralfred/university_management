package com.vickezi.registration.queue;
import com.vickezi.globals.events.EmailRegistrationEvent;
import com.vickezi.registration.model.RegistrationMessage;
import com.vickezi.registration.model.Users;
import com.vickezi.registration.services.RegistrationServiceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static com.vickezi.globals.util.Constants.USER_EMAIL_REGISTERED_EVENT_TOPIC;

@Service
public class ReceiverService {
    private final RegistrationServiceHandler registrationServiceHandler;
    private final Logger logger = LoggerFactory.getLogger(ReceiverService.class);

    public ReceiverService(RegistrationServiceHandler registrationServiceHandler) {
        this.registrationServiceHandler = registrationServiceHandler;
    }

    @KafkaListener(topics = {USER_EMAIL_REGISTERED_EVENT_TOPIC},
    groupId = "user-registration-group")
    public void newEmailRegistration(EmailRegistrationEvent registrationEvent) {
        logger.info("Email topic {}", registrationEvent);
        processMessage(registrationEvent.email(),  ()-> {
            handleEmailRegistration(registrationEvent);
        });
    }
    /**
     * Processes a message received from a RabbitMQ queue.
     *
     * @param <T> The type of the message.
     * @param message The message to process.
     * @param messageProcessor The Runnable that processes the message.
     */
    private <T> void processMessage(T message, Runnable messageProcessor) {
            messageProcessor.run();
    }
    /**
     * Handles an email message by registering the user and sending a registration email.
     *
     * @param email The email object containing the user's email address.
     * @throws IllegalArgumentException If the email is null.
     * @throws RuntimeException         If an error occurs during email processing.
     */
    public void handleEmailRegistration(final EmailRegistrationEvent email) {
        if (email == null) {
            throw new IllegalArgumentException("Email cannot be null");
        }
        logger.info("Processing email");
        try {
            // Register the user using the provided email
            RegistrationMessage message = registrationServiceHandler.registerUserByEmail(email.email());
            logger.info("Successfully processed adding message to queue: {}", message);

            // Send a registration email
//            sendEmail(registration);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to process email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process email", e);
        }
    }

}



