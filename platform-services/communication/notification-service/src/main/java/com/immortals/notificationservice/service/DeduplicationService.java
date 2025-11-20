package com.immortals.notificationservice.service;

import com.immortals.notificationservice.domain.model.Notification;

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
