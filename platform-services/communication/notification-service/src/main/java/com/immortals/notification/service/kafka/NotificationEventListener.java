package com.immortals.notification.service.kafka;

import com.immortals.notification.service.application.usecase.SendNotificationUseCase;
import com.immortals.notification.service.domain.model.Notification;
import com.immortals.notification.service.domain.model.NotificationPriority;
import com.immortals.notification.service.event.NotificationRequestPayload;
import com.immortals.notification.service.event.NotificationSentPayload;
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
        
        // Build domain notification with all new fields
        Notification notification = Notification.builder()
                .eventId(event.getEventId())
                .type(Notification.NotificationType.valueOf(payload.getNotificationType().toUpperCase()))
                .recipient(payload.getRecipient())
                .countryCode(payload.getCountryCode())  // NEW: Country code from payload
                .locale(payload.getLocale())  // NEW: Locale for template localization
                .subject(payload.getSubject())
                .message(payload.getMessage())
                .htmlContent(payload.getHtmlContent())
                .templateCode(payload.getTemplateCode())  // NEW: Template code
                .templateVariables(payload.getTemplateVariables())  // NEW: Template variables
                .priority(payload.getPriority() != null ? 
                        payload.getPriority() : 
                        NotificationPriority.NORMAL)  // NEW: Priority
                .correlationId(event.getCorrelationId())  // Correlation ID propagation
                .scheduledAt(payload.getScheduledAt())  // NEW: Scheduled delivery time
                .metadata(payload.getMetadata())  // NEW: Additional metadata
                .build();
        
        // Execute use case (handles idempotency internally)
        Notification result = sendNotificationUseCase.execute(notification);
        
        // Publish NotificationSent event with correlation ID propagation
        publishNotificationSentEvent(result, event.getCorrelationId());
        
        log.info("Notification processed successfully: eventId={}, status={}, correlationId={}", 
                 event.getEventId(), result.getStatus(), event.getCorrelationId());
    }

    @Override
    protected String getEventType() {
        return "NotificationRequest";
    }
    
    private void publishNotificationSentEvent(Notification notification, String correlationId) {
        try {
            NotificationSentPayload payload = NotificationSentPayload.builder()
                    .notificationId(notification.getId().toString())
                    .notificationType(notification.getType().name())
                    .recipient(notification.getRecipient())
                    .providerId(notification.getProviderId())  // NEW: Provider ID
                    .providerMessageId(notification.getProviderMessageId())  // NEW: Provider message ID
                    .success(notification.isSent())
                    .status(notification.getDeliveryStatus() != null ? 
                            notification.getDeliveryStatus().name() : null)  // NEW: Delivery status
                    .errorMessage(notification.getErrorMessage())
                    .sentAt(notification.getProcessedAt())  // NEW: Sent timestamp
                    .build();
            
            DomainEvent<NotificationSentPayload> event = DomainEvent.<NotificationSentPayload>builder()
                    .eventType("NotificationSent")
                    .aggregateId(notification.getId().toString())
                    .aggregateType("Notification")
                    .payload(payload)
                    .correlationId(correlationId)  // Propagate correlation ID
                    .build();
            
            eventPublisher.publish("notification-sent", event);
            
            log.info("Published NotificationSent event: notificationId={}, correlationId={}", 
                    notification.getId(), correlationId);
            
        } catch (Exception e) {
            log.error("Failed to publish NotificationSent event: {}", e.getMessage(), e);
            // Don't fail the main processing if event publishing fails
        }
    }
}
