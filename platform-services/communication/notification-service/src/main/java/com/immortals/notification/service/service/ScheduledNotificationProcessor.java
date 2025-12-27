package com.immortals.notification.service.service;

/**
 * Service for processing scheduled notifications
 * Requirement 13.3: Process notifications when scheduled time arrives
 */
public interface ScheduledNotificationProcessor {
    
    /**
     * Process all due scheduled notifications
     * This method is called periodically by a scheduled job
     */
    void processDueNotifications();
}
