package com.immortals.notificationservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for notification request events
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequestPayload {
    
    private String notificationType; // EMAIL, SMS, WHATSAPP
    private String recipient;
    private String subject;
    private String message;
    private String htmlContent;
}
