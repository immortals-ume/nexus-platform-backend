package com.immortals.notificationservice.domain.port;

import com.immortals.notificationservice.domain.model.Notification;

/**
 * Port interface for notification delivery providers (Hexagonal Architecture)
 */
public interface NotificationProvider {
    
    /**
     * Get unique provider identifier
     */
    String getProviderId();
    
    /**
     * Send notification through the provider
     * 
     * @param notification the notification to send
     * @return true if sent successfully, false otherwise
     */
    boolean send(Notification notification);
    
    /**
     * Check if this provider supports the notification type
     */
    boolean supports(Notification.NotificationType type);
    
    /**
     * Get provider priority (lower is higher priority)
     */
    default int getPriority() {
        return 100;
    }
}
