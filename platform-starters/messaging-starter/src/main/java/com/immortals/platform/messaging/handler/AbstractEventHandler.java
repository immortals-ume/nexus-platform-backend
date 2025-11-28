package com.immortals.platform.messaging.handler;

import com.immortals.platform.messaging.config.MessagingProperties;
import com.immortals.platform.messaging.event.DomainEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Abstract base class for event handlers.
 * Provides idempotency checking, automatic retry with exponential backoff,
 * dead letter queue handling, and metrics collection.
 *
 * @param <T> The type of the event payload
 */
@Slf4j
public abstract class AbstractEventHandler<T> {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MessagingProperties messagingProperties;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    private Counter successCounter;
    private Counter failureCounter;
    private Counter duplicateCounter;
    private Timer processingTimer;

    /**
     * Initialize metrics for this handler
     */
    protected void initMetrics(String handlerName) {
        if (meterRegistry != null) {
            successCounter = Counter.builder("event.handler.success")
                .tag("handler", handlerName)
                .description("Number of successfully processed events")
                .register(meterRegistry);

            failureCounter = Counter.builder("event.handler.failure")
                .tag("handler", handlerName)
                .description("Number of failed event processing attempts")
                .register(meterRegistry);

            duplicateCounter = Counter.builder("event.handler.duplicate")
                .tag("handler", handlerName)
                .description("Number of duplicate events detected")
                .register(meterRegistry);

            processingTimer = Timer.builder("event.handler.processing.time")
                .tag("handler", handlerName)
                .description("Time taken to process events")
                .register(meterRegistry);
        }
    }

    /**
     * Handle the event with idempotency checking and retry logic
     */
    protected void handleEvent(
            @Payload DomainEvent<T> event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        long startTime = System.nanoTime();
        String eventId = event.getEventId();

        try {
            log.info("Received event {} from topic {}: {}",
                eventId, topic, event.getEventType());

            if (messagingProperties.getIdempotency().isEnabled() && isDuplicate(eventId)) {
                log.warn("Duplicate event detected: {}. Skipping processing.", eventId);
                if (duplicateCounter != null) {
                    duplicateCounter.increment();
                }
                acknowledgment.acknowledge();
                return;
            }

            processWithRetry(event);

            if (messagingProperties.getIdempotency().isEnabled()) {
                markAsProcessed(eventId);
            }

            acknowledgment.acknowledge();

            if (successCounter != null) {
                successCounter.increment();
            }

            log.info("Successfully processed event {}", eventId);

        } catch (Exception ex) {
            log.error("Failed to process event {} after all retries: {}",
                eventId, ex.getMessage(), ex);

            if (failureCounter != null) {
                failureCounter.increment();
            }

            if (messagingProperties.getDlq().isEnabled()) {
                sendToDeadLetterQueue(event, topic, ex);
            }

            acknowledgment.acknowledge();

        } finally {
            if (processingTimer != null) {
                processingTimer.record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
            }
        }
    }

    /**
     * Process the event with exponential backoff retry
     */
    private void processWithRetry(DomainEvent<T> event) throws Exception {
        MessagingProperties.Retry retryConfig = messagingProperties.getRetry();

        if (!retryConfig.isEnabled()) {
            processEvent(event);
            return;
        }

        int maxAttempts = retryConfig.getMaxAttempts();
        Duration initialInterval = retryConfig.getInitialInterval();
        Duration maxInterval = retryConfig.getMaxInterval();
        double multiplier = retryConfig.getMultiplier();

        Exception lastException = null;

        for (int attempt = 0; attempt <= maxAttempts; attempt++) {
            try {
                processEvent(event);
                return;
            } catch (Exception ex) {
                lastException = ex;

                if (attempt < maxAttempts) {
                    long backoffMillis = calculateBackoff(
                        attempt, initialInterval, maxInterval, multiplier);

                    log.warn("Event processing failed (attempt {}/{}). Retrying in {}ms: {}",
                        attempt + 1, maxAttempts + 1, backoffMillis, ex.getMessage());

                    try {
                        Thread.sleep(backoffMillis);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                } else {
                    log.error("Event processing failed after {} attempts", maxAttempts + 1);
                }
            }
        }

        throw lastException;
    }

    /**
     * Calculate exponential backoff delay
     */
    private long calculateBackoff(int attempt, Duration initialInterval,
                                  Duration maxInterval, double multiplier) {
        long backoff = (long) (initialInterval.toMillis() * Math.pow(multiplier, attempt));
        return Math.min(backoff, maxInterval.toMillis());
    }

    /**
     * Check if event has already been processed (idempotency check)
     */
    private boolean isDuplicate(String eventId) {
        String key = messagingProperties.getIdempotency().getKeyPrefix() + eventId;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Mark event as processed in Redis
     */
    private void markAsProcessed(String eventId) {
        String key = messagingProperties.getIdempotency().getKeyPrefix() + eventId;
        Duration ttl = messagingProperties.getIdempotency().getTtl();
        redisTemplate.opsForValue().set(key, "processed", ttl);
    }

    /**
     * Send failed event to dead letter queue
     */
    private void sendToDeadLetterQueue(DomainEvent<T> event, String originalTopic, Exception ex) {
        try {
            String dlqTopic = originalTopic + messagingProperties.getDlq().getTopicSuffix();

            event.addMetadata("error.message", ex.getMessage());
            event.addMetadata("error.class", ex.getClass().getName());
            event.addMetadata("original.topic", originalTopic);
            event.addMetadata("failed.timestamp", String.valueOf(System.currentTimeMillis()));

            log.error("Sending event {} to DLQ topic {}", event.getEventId(), dlqTopic);

            com.immortals.platform.messaging.publisher.EventPublisher publisher = getEventPublisher();
            if (publisher != null) {
                try {
                    publisher.publish(dlqTopic, event).get();
                    log.info("Successfully sent event {} to DLQ topic {}", event.getEventId(), dlqTopic);
                } catch (Exception publishEx) {
                    log.error("Failed to publish to DLQ via EventPublisher: {}", publishEx.getMessage(), publishEx);
                    onSendToDeadLetterQueue(event, dlqTopic, ex);
                }
            } else {
                onSendToDeadLetterQueue(event, dlqTopic, ex);
            }

        } catch (Exception dlqEx) {
            log.error("Failed to send event to DLQ: {}", dlqEx.getMessage(), dlqEx);
        }
    }

    /**
     * Process the event. Must be implemented by subclasses.
     *
     * @param event The domain event to process
     * @throws Exception if processing fails
     */
    protected abstract void processEvent(DomainEvent<T> event) throws Exception;

    /**
     * Get the event type this handler processes
     */
    protected abstract String getEventType();

    /**
     * Hook for customizing DLQ behavior. Override if needed.
     */
    protected void onSendToDeadLetterQueue(DomainEvent<T> event, String dlqTopic, Exception ex) {
    }

    /**
     * Get the EventPublisher for sending to DLQ
     * Subclasses should override this if they want automatic DLQ publishing
     */
    protected com.immortals.platform.messaging.publisher.EventPublisher getEventPublisher() {
        return null;
    }
}
