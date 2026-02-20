package com.immortals.notification.service.service;

import com.immortals.platform.domain.notifications.domain.Notification;

/**
 * Service for smart notification deduplication
 * Prevents sending duplicate notifications within time windows
 */
public interface DeduplicationService {
    
    /**
     * Check if notification is duplicate within time window
     */
    boolean isDuplicate(Notification notification);
    
    /**
     * Mark notification as sent for deduplication
     */
    void markAsSent(Notification notification);
    
    /**
     * Generate deduplication key
     */
    String generateDeduplicationKey(Notification notification);
    
    /**
     * Get deduplication window in seconds
     */
    long getDeduplicationWindow(Notification.NotificationType type);
}
