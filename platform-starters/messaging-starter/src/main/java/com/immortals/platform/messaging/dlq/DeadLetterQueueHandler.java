package com.immortals.platform.messaging.dlq;

import com.immortals.platform.messaging.config.MessagingProperties;
import com.immortals.platform.messaging.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.Serial;
import java.time.Instant;

/**
 * Handler for processing messages from dead letter queues.
 * Provides retry logic for DLQ messages and manual intervention support.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class DeadLetterQueueHandler {
    private final MessagingProperties messagingProperties;
    private final com.immortals.platform.messaging.publisher.EventPublisher eventPublisher;

    /**
     * Process messages from dead letter queue
     * This is a generic handler that logs DLQ messages
     * Specific services can extend this or create their own DLQ handlers
     */
    public void handleDeadLetterMessage(
            @Payload DomainEvent<?> event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        String eventId = event.getEventId();
        String originalTopic = event.getMetadata("original.topic");
        String errorMessage = event.getMetadata("error.message");
        String errorClass = event.getMetadata("error.class");
        String failedTimestamp = event.getMetadata("failed.timestamp");

        log.error("Processing DLQ message - Event ID: {}, Original Topic: {}, Event Type: {}",
                eventId, originalTopic, event.getEventType());
        log.error("Error Details - Class: {}, Message: {}, Failed At: {}",
                errorClass, errorMessage, failedTimestamp);

        try {
            logFailedMessageContext(event, originalTopic, errorMessage);

            if (messagingProperties.getDlq()
                    .isEnableManualRetry()) {
                storeForManualRetry(event, originalTopic, errorMessage);
            }

            acknowledgment.acknowledge();

            log.info("DLQ message {} logged and acknowledged", eventId);

        } catch (Exception ex) {
            log.error("Failed to process DLQ message {}: {}", eventId, ex.getMessage(), ex);
            acknowledgment.acknowledge();
        }
    }

    /**
     * Retry a message from DLQ
     * This can be called manually or triggered by an admin interface
     */
    public void retryFromDeadLetterQueue(DomainEvent<?> event, String originalTopic) {
        String eventId = event.getEventId();

        log.info("Attempting manual retry of DLQ message {} to topic {}",
                eventId, originalTopic);

        try {
            String errorClass = event.getMetadata("error.class");
            if (!shouldRetry(event, errorClass)) {
                log.warn("Message {} should not be retried", eventId);
                return;
            }

            String retryCountStr = event.getMetadata("retry.count");
            int retryCount = retryCountStr != null ? Integer.parseInt(retryCountStr) : 0;
            event.addMetadata("retry.count", String.valueOf(retryCount + 1));

            event.getMetadata().remove("error.message");
            event.getMetadata().remove("error.class");
            event.getMetadata().remove("original.topic");
            event.getMetadata().remove("failed.timestamp");

            event.addMetadata("retry.from.dlq", "true");
            event.addMetadata("retry.timestamp", Instant.now().toString());

            eventPublisher.publishSync(originalTopic, event);

            log.info("Successfully retried message {} to topic {}", eventId, originalTopic);

        } catch (Exception ex) {
            log.error("Failed to retry DLQ message {}: {}", eventId, ex.getMessage(), ex);
            throw new DlqRetryException("Failed to retry message from DLQ", ex);
        }
    }

    /**
     * Log full context of failed message for debugging
     */
    private void logFailedMessageContext(DomainEvent<?> event, String originalTopic,
                                         String errorMessage) {
        log.error("=== Failed Message Context ===");
        log.error("Event ID: {}", event.getEventId());
        log.error("Event Type: {}", event.getEventType());
        log.error("Aggregate ID: {}", event.getAggregateId());
        log.error("Aggregate Type: {}", event.getAggregateType());
        log.error("Original Topic: {}", originalTopic);
        log.error("Correlation ID: {}", event.getCorrelationId());
        log.error("Source: {}", event.getSource());
        log.error("Timestamp: {}", event.getTimestamp());
        log.error("Error: {}", errorMessage);
        log.error("Payload: {}", event.getPayload());
        log.error("Metadata: {}", event.getMetadata());
        log.error("=============================");
    }

    /**
     * Store failed message for manual intervention
     * In a production system, this would store to a database or monitoring system
     */
    private void storeForManualRetry(DomainEvent<?> event, String originalTopic,
                                     String errorMessage) {

        log.info("Message {} stored for manual retry consideration", event.getEventId());
    }

    /**
     * Check if a message should be retried based on error type and retry count
     */
    private boolean shouldRetry(DomainEvent<?> event, String errorClass) {
        String retryCountStr = event.getMetadata("retry.count");
        int retryCount = retryCountStr != null ? Integer.parseInt(retryCountStr) : 0;

        if (retryCount >= 3) {
            log.warn("Message {} has been retried {} times, not retrying again",
                    event.getEventId(), retryCount);
            return false;
        }

        if (errorClass != null && isNonRetryableError(errorClass)) {
            log.warn("Error class {} is non-retryable, not retrying message {}",
                    errorClass, event.getEventId());
            return false;
        }

        return true;
    }

    /**
     * Check if error is non-retryable (e.g., validation errors)
     */
    private boolean isNonRetryableError(String errorClass) {
        return errorClass.contains("ValidationException") ||
                errorClass.contains("IllegalArgumentException") ||
                errorClass.contains("BadRequestException");
    }

    /**
     * Exception thrown when DLQ retry fails
     */
    public static class DlqRetryException extends RuntimeException {
        @Serial
        private static final long serialVersionUID = 1L;
        public DlqRetryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
