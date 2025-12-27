package com.immortals.platform.domain.notifications.dto;

import com.immortals.platform.domain.notifications.domain.Notification;
import com.immortals.platform.domain.notifications.domain.NotificationPriority;
import lombok.*;

import java.util.Map;

/**
 * Request DTO for provider notification delivery
 * Contains all information needed by a provider to send a notification
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderRequest {
    
    private String notificationId;
    private String eventId;
    private Notification.NotificationType channel;
    private String recipient;
    private String countryCode;
    private String locale;
    private String subject;
    private String message;
    private String htmlContent;
    private NotificationPriority priority;
    private String correlationId;
    private Map<String, String> metadata;

    public static ProviderRequest fromNotification(Notification notification) {
        return ProviderRequest.builder()
                .notificationId(notification.getId() != null ? notification.getId().toString() : null)
                .eventId(notification.getEventId())
                .channel(notification.getType())
                .recipient(notification.getRecipient())
                .countryCode(notification.getCountryCode())
                .locale(notification.getLocale())
                .subject(notification.getSubject())
                .message(notification.getMessage())
                .htmlContent(notification.getHtmlContent())
                .priority(notification.getPriority())
                .correlationId(notification.getCorrelationId())
                .metadata(notification.getMetadata())
                .build();
    }
}
