package com.vickezi.globals.events;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Service for producing messages to a Kafka queue.
 *
 * @param <T> the type of the message
 */
@Service
public class MessageProducerService<T> {
    private static final Logger logger = LoggerFactory.getLogger(MessageProducerService.class);

    private final KafkaTemplate<String, T> template;
    /**
     * Constructs a new MessageProducerService with the given KafkaTemplate.
     *
     * @param template the KafkaTemplate to use for sending messages
     */
    public MessageProducerService(KafkaTemplate<String, T> template) {
        this.template = template;
    }
    /**
     * Asynchronously sends a message to the specified Kafka topic and handles both success and failure cases.
     * This method leverages the {@link KafkaTemplate#send(String, Object)} method to send a message to a Kafka topic.
     * It returns a {@link CompletableFuture} that allows the caller to track the message sending result asynchronously.
     * Steps:
     * 1. The message is sent to the Kafka topic using {@link KafkaTemplate#send(String, Object)}.
     * 2. Once the message is sent, a success handler is invoked using {@link CompletableFuture#thenApply(Function)} to log
     *    details of the successful message sending, including the partition and offset where the message was written.
     *    The {@link RecordMetadata} is retrieved from the {@link SendResult} to access this information.
     * 3. If the message sending fails, the failure handler is triggered using {@link CompletableFuture#exceptionallyCompose(Function)}.
     *    The exception is logged with detailed error information, and a new {@link CompletableFuture} representing the failure
     *    is returned using {@link CompletableFuture#failedFuture(Throwable)}. This ensures the caller can handle failures in a consistent manner.
     *
     * @param topic The Kafka topic to which the message is sent.
     * @param message The message payload to be sent to the Kafka topic.
     * @return A {@link CompletableFuture} that will eventually contain the {@link SendResult} if the message was sent successfully,
     *         or a failed future if there was an error in sending the message.
     * @throws RuntimeException If an error occurs while sending the message (handled in the exceptionallyCompose block).
     * Usage Example:
     * <pre>
     * addMessageToQueue("my-topic", "Test Message")
     *     .thenAccept(result -> {
     *         // Handle success
     *     })
     *     .exceptionally(ex -> {
     *         // Handle failure
     *         return null;
     *     });
     * </pre>
     */
    public CompletableFuture<SendResult<String, T>> addMessageToQueue(String topic, T message) {
        return template.send(topic, message)
                .thenApply(result -> {
                    RecordMetadata metadata = result.getRecordMetadata();
                    logger.info("✅ Message sent successfully to topic: {} | partition: {} | offset: {}",
                            metadata.topic(), metadata.partition(), metadata.offset());
                    return result;
                })
                .exceptionallyCompose(ex -> {
                    logger.error("❌ Failed to send message to topic: {} | Error: {}", topic, ex.getMessage(), ex);
                    return CompletableFuture.failedFuture(new RuntimeException("Gateway message sending failed", ex));
                });
    }
}
