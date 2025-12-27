package com.immortals.platform.domain.notifications.event;

import com.immortals.platform.domain.notifications.domain.NotificationPriority;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Payload for notification request events
 * Enhanced with new fields for multi-channel notification system
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequestPayload {
    
    private String notificationType; // EMAIL, SMS, WHATSAPP, PUSH_NOTIFICATION
    private String recipient;
    private String countryCode;  // NEW: For provider routing
    private String locale;  // NEW: For template localization
    private String subject;
    private String message;
    private String htmlContent;
    private String templateCode;  // NEW: Template code for rendering
    private Map<String, Object> templateVariables;  // NEW: Variables for template rendering
    private NotificationPriority priority;  // NEW: Notification priority
    private LocalDateTime scheduledAt;  // NEW: Scheduled delivery time
    private Map<String, String> metadata;  // NEW: Additional context
}
