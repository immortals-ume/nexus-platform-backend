package com.immortals.notification.service.kafka;

import com.immortals.notification.service.application.usecase.impl.SendNotificationUseCase;
import com.immortals.notification.service.service.impl.SensitiveDataMasker;
import com.immortals.platform.domain.notifications.domain.Notification;
import com.immortals.platform.domain.notifications.domain.NotificationPriority;
import com.immortals.platform.domain.notifications.event.NotificationRequestPayload;
import com.immortals.platform.domain.notifications.event.NotificationSentPayload;
import com.immortals.platform.messaging.event.DomainEvent;
import com.immortals.platform.messaging.handler.AbstractEventHandler;
import com.immortals.platform.messaging.publisher.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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
    private final com.immortals.notification.service.configuration.NotificationServiceProperties properties;

    public NotificationEventListener(
            SendNotificationUseCase sendNotificationUseCase,
            EventPublisher eventPublisher,
            com.immortals.notification.service.configuration.NotificationServiceProperties properties) {
        super();
        this.sendNotificationUseCase = sendNotificationUseCase;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        initMetrics(properties.getEvents().getMetricsListenerName());
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
        super.handleEvent(event, topic, acknowledgment);
    }

    @Override
    protected void processEvent(DomainEvent<NotificationRequestPayload> event) throws Exception {
        NotificationRequestPayload payload = event.getPayload();
        
        // Add correlation ID to MDC for logging
        if (event.getCorrelationId() != null) {
            MDC.put("correlationId", event.getCorrelationId());
        }
        
        try {
            // Mask sensitive data in logs
            String maskedRecipient = SensitiveDataMasker.maskRecipient(payload.getRecipient());
            
            log.info("Processing notification request: type={}, recipient={}, correlationId={}", 
                     payload.getNotificationType(), maskedRecipient, event.getCorrelationId());
        
        // Build domain notification with all new fields
        Notification notification = Notification.builder()
                .eventId(event.getEventId())
                .type(Notification.NotificationType.valueOf(payload.getNotificationType().toUpperCase()))
                .recipient(payload.getRecipient())
                .countryCode(payload.getCountryCode())
                .locale(payload.getLocale())
                .subject(payload.getSubject())
                .message(payload.getMessage())
                .htmlContent(payload.getHtmlContent())
                .templateCode(payload.getTemplateCode())
                .templateVariables(payload.getTemplateVariables())
                .priority(payload.getPriority() != null ? 
                        payload.getPriority() : 
                        NotificationPriority.NORMAL)
                .correlationId(event.getCorrelationId())
                .scheduledAt(payload.getScheduledAt())
                .metadata(payload.getMetadata())
                .build();
        
            // Execute use case (handles idempotency internally)
            Notification result = sendNotificationUseCase.execute(notification);
            
            // Publish NotificationSent event with correlation ID propagation
            publishNotificationSentEvent(result, event.getCorrelationId());
            
            log.info("Notification processed successfully: eventId={}, status={}, correlationId={}", 
                     event.getEventId(), result.getStatus(), event.getCorrelationId());
        } finally {
            // Clean up MDC
            MDC.remove("correlationId");
        }
    }

    @Override
    protected String getEventType() {
        return properties.getEvents().getNotificationRequestType();
    }
    
    private void publishNotificationSentEvent(Notification notification, String correlationId) {
        try {
            NotificationSentPayload payload = NotificationSentPayload.builder()
                    .notificationId(notification.getId().toString())
                    .notificationType(notification.getType().name())
                    .recipient(notification.getRecipient())
                    .providerId(notification.getProviderId())
                    .providerMessageId(notification.getProviderMessageId())
                    .success(notification.isSent())
                    .status(notification.getDeliveryStatus() != null ? 
                            notification.getDeliveryStatus().name() : null)
                    .errorMessage(notification.getErrorMessage())
                    .sentAt(notification.getProcessedAt())
                    .build();
            
            DomainEvent<NotificationSentPayload> event = DomainEvent.<NotificationSentPayload>builder()
                    .eventType(properties.getEvents().getNotificationSentType())
                    .aggregateId(notification.getId().toString())
                    .aggregateType("Notification")
                    .payload(payload)
                    .correlationId(correlationId)
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
