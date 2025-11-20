package com.immortals.notificationservice.kafka;

import com.immortals.notificationservice.application.usecase.SendNotificationUseCase;
import com.immortals.notificationservice.domain.model.Notification;
import com.immortals.notificationservice.event.NotificationRequestPayload;
import com.immortals.notificationservice.event.NotificationSentPayload;
import com.immortals.platform.messaging.event.DomainEvent;
import com.immortals.platform.messaging.handler.AbstractEventHandler;
import com.immortals.platform.messaging.publisher.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Event handler for notification requests using messaging-starter's AbstractEventHandler
 * Provides automatic idempotency, retry, DLQ, and metrics
 */
@Component
@Slf4j
public class NotificationEventListener extends AbstractEventHandler<NotificationRequestPayload> {

    private final SendNotificationUseCase sendNotificationUseCase;
    private final EventPublisher eventPublisher;

    public NotificationEventListener(
            SendNotificationUseCase sendNotificationUseCase,
            EventPublisher eventPublisher) {
        super();
        this.sendNotificationUseCase = sendNotificationUseCase;
        this.eventPublisher = eventPublisher;
        initMetrics("NotificationEventListener");
    }

    @Override
    @KafkaListener(
        topics = "${platform.messaging.topics.notification-requests:notification-requests}",
        groupId = "${spring.application.name:notification-service}"
    )
    public void handleEvent(
            @Payload DomainEvent<NotificationRequestPayload> event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        // AbstractEventHandler provides idempotency, retry, metrics, and DLQ handling
        super.handleEvent(event, topic, acknowledgment);
    }

    @Override
    protected void processEvent(DomainEvent<NotificationRequestPayload> event) throws Exception {
        NotificationRequestPayload payload = event.getPayload();
        
        log.info("Processing notification request: type={}, recipient={}, correlationId={}", 
                 payload.getNotificationType(), payload.getRecipient(), event.getCorrelationId());
        
        // Build domain notification
        Notification notification = Notification.builder()
                .eventId(event.getEventId())
                .type(Notification.NotificationType.valueOf(payload.getNotificationType().toUpperCase()))
                .recipient(payload.getRecipient())
                .subject(payload.getSubject())
                .message(payload.getMessage())
                .htmlContent(payload.getHtmlContent())
                .correlationId(event.getCorrelationId())
                .build();
        
        // Execute use case (handles idempotency internally)
        Notification result = sendNotificationUseCase.execute(notification);
        
        // Publish NotificationSent event
        publishNotificationSentEvent(result);
        
        log.info("Notification processed successfully: eventId={}, status={}", 
                 event.getEventId(), result.getStatus());
    }

    @Override
    protected String getEventType() {
        return "NotificationRequest";
    }
    
    private void publishNotificationSentEvent(Notification notification) {
        try {
            NotificationSentPayload payload = NotificationSentPayload.builder()
                    .notificationId(notification.getId().toString())
                    .notificationType(notification.getType().name())
                    .recipient(notification.getRecipient())
                    .success(notification.isSent())
                    .errorMessage(notification.getErrorMessage())
                    .build();
            
            DomainEvent<NotificationSentPayload> event = DomainEvent.<NotificationSentPayload>builder()
                    .eventType("NotificationSent")
                    .aggregateId(notification.getId().toString())
                    .aggregateType("Notification")
                    .payload(payload)
                    .correlationId(notification.getCorrelationId())
                    .build();
            
            eventPublisher.publish("notification-sent", event);
            
            log.info("Published NotificationSent event: notificationId={}", notification.getId());
            
        } catch (Exception e) {
            log.error("Failed to publish NotificationSent event: {}", e.getMessage(), e);
            // Don't fail the main processing if event publishing fails
        }
    }
}
