package com.immortals.notificationservice.service;

import com.immortals.notificationservice.domain.model.Notification;

/**
 * Service for scheduling notification retries
 */
public interface RetrySchedulerService {
    
    /**
     * Schedule a retry for failed notification
     */
    void scheduleRetry(Notification notification);
    
    /**
     * Calculate next retry time with exponential backoff
     */
    long calculateBackoffDelay(int retryCount);
    
    /**
     * Check if notification should be retried
     */
    boolean shouldRetry(Notification notification);
}
