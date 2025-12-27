package com.immortals.platform.domain.notifications.event;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Payload for notification sent events
 * Enhanced with new fields for tracking and monitoring
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSentPayload {
    
    private String notificationId;
    private String notificationType;
    private String recipient;
    private String providerId;  // NEW: Which provider was used
    private String providerMessageId;  // NEW: Provider's tracking ID
    private Boolean success;
    private String status;  // NEW: Delivery status (PENDING, SENT, DELIVERED, FAILED, READ)
    private String errorMessage;
    private LocalDateTime sentAt;  // NEW: Timestamp when notification was sent
}
