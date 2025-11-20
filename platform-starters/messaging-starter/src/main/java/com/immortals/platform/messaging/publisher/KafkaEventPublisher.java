package com.immortals.platform.messaging.publisher;

import com.immortals.platform.messaging.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka implementation of EventPublisher.
 * Handles publishing domain events to Kafka topics with automatic
 * correlation ID propagation and transactional support.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    @Override
    public <T> CompletableFuture<Void> publish(String topic, DomainEvent<T> event) {
        return publish(topic, event.getAggregateId(), event);
    }

    @Override
    public <T> CompletableFuture<Void> publish(String topic, String key, DomainEvent<T> event) {
        enrichEvent(event);
        
        log.debug("Publishing event {} to topic {} with key {}", 
            event.getEventType(), topic, key);
        
        CompletableFuture<SendResult<String, Object>> future = 
            kafkaTemplate.send(topic, key, event);
        
        return future.handle((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event {} to topic {}: {}", 
                    event.getEventType(), topic, ex.getMessage(), ex);
                throw new EventPublishException(
                    "Failed to publish event to topic: " + topic, ex);
            }
            
            log.info("Successfully published event {} to topic {} with offset {}", 
                event.getEventType(), topic, 
                result.getRecordMetadata().offset());
            return null;
        });
    }

    @Override
    @Transactional
    public <T> void publishSync(String topic, DomainEvent<T> event) {
        publishSync(topic, event.getAggregateId(), event);
    }

    @Override
    @Transactional
    public <T> void publishSync(String topic, String key, DomainEvent<T> event) {
        enrichEvent(event);
        
        log.debug("Publishing event {} synchronously to topic {} with key {}", 
            event.getEventType(), topic, key);
        
        try {
            SendResult<String, Object> result = 
                kafkaTemplate.send(topic, key, event).get();
            
            log.info("Successfully published event {} to topic {} with offset {}", 
                event.getEventType(), topic, 
                result.getRecordMetadata().offset());
        } catch (Exception ex) {
            log.error("Failed to publish event {} to topic {}: {}", 
                event.getEventType(), topic, ex.getMessage(), ex);
            throw new EventPublishException(
                "Failed to publish event to topic: " + topic, ex);
        }
    }

    /**
     * Enrich event with correlation ID and source information
     */
    private <T> void enrichEvent(DomainEvent<T> event) {
        // Set source service if not already set
        if (event.getSource() == null) {
            event.setSource(serviceName);
        }
        
        // Propagate correlation ID from current context if available
        if (event.getCorrelationId() == null) {
            String correlationId = getCorrelationIdFromContext();
            if (correlationId != null) {
                event.setCorrelationId(correlationId);
            }
        }
    }

    /**
     * Get correlation ID from current thread context
     * This should integrate with the observability-starter's correlation ID mechanism
     */
    private String getCorrelationIdFromContext() {
        // Try to get from MDC (set by observability-starter)
        try {
            return org.slf4j.MDC.get("correlationId");
        } catch (Exception e) {
            log.debug("Could not retrieve correlation ID from MDC", e);
            return null;
        }
    }

    /**
     * Exception thrown when event publishing fails
     */
    public static class EventPublishException extends RuntimeException {
        public EventPublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
