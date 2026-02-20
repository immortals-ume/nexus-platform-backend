package com.immortals.platform.messaging.handler;

import com.immortals.platform.domain.shared.config.MessagingProperties;
import com.immortals.platform.domain.shared.event.DomainEvent;
import com.immortals.platform.messaging.publisher.EventPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Extended event handler that automatically publishes failed events to DLQ.
 * Services should extend this class instead of AbstractEventHandler to get
 * automatic DLQ publishing functionality.
 *
 * @param <T> The type of the event payload
 */
@Slf4j
public abstract class DlqEnabledEventHandler<T> extends AbstractEventHandler<T> {

    private final EventPublisher eventPublisher;

    public DlqEnabledEventHandler(StringRedisTemplate redisTemplate, 
                                  MessagingProperties messagingProperties, 
                                  MeterRegistry meterRegistry,
                                  EventPublisher eventPublisher) {
        super(redisTemplate, messagingProperties, meterRegistry);
        this.eventPublisher = eventPublisher;
    }

    @Override
    protected EventPublisher getEventPublisher() {
        return eventPublisher;
    }

    @Override
    protected void onSendToDeadLetterQueue(DomainEvent<T> event, String dlqTopic, Exception ex) {
        log.warn("EventPublisher not available or failed. Event {} will not be sent to DLQ topic {}. " +
                "Consider injecting EventPublisher bean or implementing custom DLQ logic.",
                event.getEventId(), dlqTopic);
    }
}
