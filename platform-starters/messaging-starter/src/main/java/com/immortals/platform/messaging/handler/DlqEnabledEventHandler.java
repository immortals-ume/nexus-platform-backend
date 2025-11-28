package com.immortals.platform.messaging.handler;

import com.immortals.platform.messaging.event.DomainEvent;
import com.immortals.platform.messaging.publisher.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Extended event handler that automatically publishes failed events to DLQ.
 * Services should extend this class instead of AbstractEventHandler to get
 * automatic DLQ publishing functionality.
 *
 * @param <T> The type of the event payload
 */
@Slf4j
public abstract class DlqEnabledEventHandler<T> extends AbstractEventHandler<T> {

    @Autowired(required = false)
    private EventPublisher eventPublisher;

    @Override
    protected EventPublisher getEventPublisher() {
        return eventPublisher;
    }

    @Override
    protected void onSendToDeadLetterQueue(DomainEvent<T> event, String dlqTopic, Exception ex) {
        // This will only be called if EventPublisher is not available or publishing fails
        log.warn("EventPublisher not available or failed. Event {} will not be sent to DLQ topic {}. " +
                "Consider injecting EventPublisher bean or implementing custom DLQ logic.",
                event.getEventId(), dlqTopic);
    }
}
