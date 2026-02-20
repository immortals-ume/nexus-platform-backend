package com.immortals.notification.service.service;

import com.immortals.platform.domain.notifications.domain.Notification;

/**
 * Service for scheduling notification retries with exponential backoff
 * Implements Requirements 9.1, 9.2, 9.3, 9.4, 9.5
 */
public interface RetrySchedulerService {
    
    /**
     * Schedule a retry for failed notification with exponential backoff
     * Requirement 9.1: Retry with exponential backoff
     * Requirement 9.5: Retry with failover to alternative provider
     * 
     * @param notification the failed notification
     * @param attemptFailover whether to attempt failover to alternative provider
     */
    void scheduleRetry(Notification notification, boolean attemptFailover);
    
    /**
     * Calculate next retry time with exponential backoff
     * Requirement 9.2: Increment retry count and record timestamp
     * 
     * @param retryCount current retry count
     * @return delay in seconds
     */
    long calculateBackoffDelay(int retryCount);
    
    /**
     * Check if notification should be retried
     * Requirement 9.3: Mark as permanently failed when max retries exceeded
     * 
     * @param notification the notification to check
     * @return true if should retry, false if max retries reached
     */
    boolean shouldRetry(Notification notification);
    
    /**
     * Check if error is retryable
     * Requirement 9.4: Fail immediately for non-retryable errors
     * 
     * @param errorMessage the error message from provider
     * @return true if error is retryable, false otherwise
     */
    boolean isRetryableError(String errorMessage);
    
    /**
     * Mark notification as permanently failed
     * Requirement 9.3: Mark as permanently failed when max retries exceeded
     * 
     * @param notification the notification to mark as failed
     * @param reason the failure reason
     */
    void markAsPermanentlyFailed(Notification notification, String reason);
}
