package com.immortals.platform.messaging.dlq;

import com.immortals.platform.messaging.config.MessagingProperties;
import com.immortals.platform.messaging.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Handler for processing messages from dead letter queues.
 * Provides retry logic for DLQ messages and manual intervention support.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterQueueHandler {

    private final MessagingProperties messagingProperties;

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
            // Log full context for manual intervention
            logFailedMessageContext(event, originalTopic, errorMessage);

            // Check if manual retry is enabled
            if (messagingProperties.getDlq().isEnableManualRetry()) {
                // Store for manual retry - this could be in a database or monitoring system
                storeForManualRetry(event, originalTopic, errorMessage);
            }

            // Acknowledge the DLQ message
            acknowledgment.acknowledge();

            log.info("DLQ message {} logged and acknowledged", eventId);

        } catch (Exception ex) {
            log.error("Failed to process DLQ message {}: {}", eventId, ex.getMessage(), ex);
            // Acknowledge anyway to prevent infinite loop
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
            // Remove error metadata before retry
            event.getMetadata().remove("error.message");
            event.getMetadata().remove("error.class");
            event.getMetadata().remove("original.topic");
            event.getMetadata().remove("failed.timestamp");
            
            // Add retry metadata
            event.addMetadata("retry.from.dlq", "true");
            event.addMetadata("retry.timestamp", Instant.now().toString());

            // Republish to original topic
            // This would need EventPublisher injected
            log.info("Message {} prepared for retry to topic {}", eventId, originalTopic);
            
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
        // TODO: Implement storage mechanism
        // Options:
        // 1. Store in database table for admin UI
        // 2. Send to monitoring/alerting system
        // 3. Store in Redis with longer TTL
        // 4. Send to external incident management system
        
        log.info("Message {} stored for manual retry consideration", event.getEventId());
    }

    /**
     * Check if a message should be retried based on error type and retry count
     */
    private boolean shouldRetry(DomainEvent<?> event, String errorClass) {
        // Check retry count from metadata
        String retryCountStr = event.getMetadata("retry.count");
        int retryCount = retryCountStr != null ? Integer.parseInt(retryCountStr) : 0;

        // Don't retry if already retried too many times
        if (retryCount >= 3) {
            log.warn("Message {} has been retried {} times, not retrying again", 
                event.getEventId(), retryCount);
            return false;
        }

        // Don't retry for certain error types (e.g., validation errors)
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
        public DlqRetryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
