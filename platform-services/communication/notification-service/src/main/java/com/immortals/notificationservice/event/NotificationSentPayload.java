package com.immortals.notificationservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for notification sent events
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSentPayload {
    
    private String notificationId;
    private String notificationType;
    private String recipient;
    private boolean success;
    private String errorMessage;
}
