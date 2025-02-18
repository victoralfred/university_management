package com.vickezi.registration.queue;

import com.vickezi.globals.events.MessageProducerService;
import com.vickezi.globals.model.EmailRegistrationEvent;
import com.vickezi.globals.model.EmailVerificationEvent;
import com.vickezi.globals.model.RegistrationMessage;
import com.vickezi.registration.exception.RegistrationException;
import com.vickezi.registration.services.RegistrationServiceHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;

import static com.vickezi.globals.util.Constants.USER_REGISTRATION_CONFIRMATION_NOTICE_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


class ReceiverNewEmailRegistrationMessageTest {

    @Mock
    private RegistrationServiceHandler registrationServiceHandler;

    @Mock
    private MessageProducerService<RegistrationMessage> messageProducerService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private RetryTemplate retryTemplate;

    @Mock
    private Acknowledgment acknowledgment;

    @Captor
    private ArgumentCaptor<RegistrationMessage> messageCaptor;

    @InjectMocks
    private ReceiverNewEmailRegistrationMessage receiver;

    private final String testEmail = "test@example.com";
    private final String testToken = "test-token-123";

    void setUp() {
        // Proper retry template configuration
        doAnswer(invocation -> {
            RetryCallback<?, ?> callback = invocation.getArgument(0);
            return callback.doWithRetry(null);
        }).when(retryTemplate).execute(any(RetryCallback.class));

        // Kafka transaction execution
        when(kafkaTemplate.executeInTransaction(any())).thenAnswer(invocation -> {
            KafkaOperations.OperationsCallback<String, Object, ?> callback = invocation.getArgument(0);
            return callback.doInOperations(kafkaTemplate);
        });
    }
    void handleEmailRegistration_Success() throws Exception {
        EmailRegistrationEvent event = new EmailRegistrationEvent(testEmail);
        RegistrationMessage mockMessage = new RegistrationMessage("test-id","teter","PENDING","test@email.com");

        when(registrationServiceHandler.registerUserByEmail(testEmail)).thenReturn(mockMessage);

        receiver.handleEmailRegistration(event, acknowledgment);

        verify(registrationServiceHandler).registerUserByEmail(testEmail);
        verify(messageProducerService).addMessageToQueue(
                eq(USER_REGISTRATION_CONFIRMATION_NOTICE_TOPIC),
                messageCaptor.capture()
        );
        verify(acknowledgment).acknowledge();
        assertThat(messageCaptor.getValue()).isEqualTo(mockMessage);
    }

    void handleEmailRegistration_NullEvent() {
        receiver.handleEmailRegistration(null, acknowledgment);

        verifyNoInteractions(registrationServiceHandler);
        verify(acknowledgment, never()).acknowledge();
    }

    void handleEmailVerification_Success() {
        EmailVerificationEvent event = new EmailVerificationEvent(testToken, "test-id");

        receiver.handleEmailVerification(event, acknowledgment);

        verify(registrationServiceHandler).confirmEmailLinkIsValid(testToken);
        verify(acknowledgment).acknowledge();
    }



    void handleEmailRegistration_RegistrationException() {
        EmailRegistrationEvent event = new EmailRegistrationEvent(testEmail);

        // Mock the transaction execution
        when(kafkaTemplate.executeInTransaction(any())).thenAnswer(invocation -> {
            KafkaOperations.OperationsCallback<String, Object, ?> callback = invocation.getArgument(0);
            return callback.doInOperations(kafkaTemplate);
        });

        // Throw exception during registration
        doThrow(new RegistrationException("Test error"))
                .when(registrationServiceHandler).registerUserByEmail(testEmail);

        receiver.handleEmailRegistration(event, acknowledgment);

        // Verify DLQ handling with ArgumentCaptor
        ArgumentCaptor<RegistrationMessage> dlqCaptor = ArgumentCaptor.forClass(RegistrationMessage.class);
        verify(messageProducerService).sendToDeadLetterTopic(dlqCaptor.capture());

        // Verify the captured message contains expected data
        assertThat(dlqCaptor.getValue().email()).isEqualTo(testEmail);
        verify(acknowledgment).acknowledge();
    }

    void handleEmailVerification_GeneralException() {
        EmailVerificationEvent event = new EmailVerificationEvent(testToken, "test-id");

        doThrow(new RuntimeException("Test error"))
                .when(registrationServiceHandler).confirmEmailLinkIsValid(testToken);

        receiver.handleEmailVerification(event, acknowledgment);

        verify(messageProducerService).sendToDeadLetterTopic(any());
        verify(acknowledgment).acknowledge();
    }

    void sendToDeadLetterTopic_UnsupportedType() {
        EmailVerificationEvent invalidMessage = new EmailVerificationEvent(testToken, "test-id");

        sendToDeadLetterTopic(invalidMessage);

        verify(messageProducerService, never()).sendToDeadLetterTopic(any());
    }
    void sendToDeadLetterTopic_ConvertsEmailEventToRegistrationMessage() {
        EmailRegistrationEvent event = new EmailRegistrationEvent(testEmail);

        sendToDeadLetterTopic(event);

        ArgumentCaptor<RegistrationMessage> captor = ArgumentCaptor.forClass(RegistrationMessage.class);
        verify(messageProducerService).sendToDeadLetterTopic(captor.capture());
        assertThat(captor.getValue().email()).isEqualTo(testEmail);
    }

    void sendToDeadLetterTopic_HandlesInvalidMessageTypes() {
        sendToDeadLetterTopic("invalid-message-type");
        verify(messageProducerService, never()).sendToDeadLetterTopic(any());
    }
    private <T> void sendToDeadLetterTopic(T message) {
        try {
            RegistrationMessage dlqMessage = new RegistrationMessage(
                    "error-id","","",
                    (message instanceof EmailRegistrationEvent)
                            ? ((EmailRegistrationEvent) message).email()
                            : "unknown-error"
            );
            messageProducerService.sendToDeadLetterTopic(dlqMessage);
        } catch (Exception ex) {
            System.out.printf("❌ Failed to create DLQ message %s", ex);
        }
    }
}