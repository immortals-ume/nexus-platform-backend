package com.immortals.notification.service.service;

import com.immortals.platform.domain.notifications.domain.model.Notification;

/**
 * Service for rate limiting notification delivery
 * Implements token bucket algorithm using Redis cache
 */
public interface RateLimitService {
    
    /**
     * Check if notification is within rate limit
     * 
     * @param userId the user ID
     * @param channel the notification channel
     * @return true if within rate limit, false if exceeded
     */
    boolean isWithinRateLimit(String userId, Notification.NotificationType channel);
    
    /**
     * Consume a token from the rate limit bucket
     * 
     * @param userId the user ID
     * @param channel the notification channel
     * @return true if token consumed successfully, false if rate limit exceeded
     */
    boolean consumeToken(String userId, Notification.NotificationType channel);
    
    /**
     * Get remaining tokens for user and channel
     * 
     * @param userId the user ID
     * @param channel the notification channel
     * @return number of remaining tokens
     */
    long getRemainingTokens(String userId, Notification.NotificationType channel);
    
    /**
     * Reset rate limit for user and channel
     * 
     * @param userId the user ID
     * @param channel the notification channel
     */
    void resetRateLimit(String userId, Notification.NotificationType channel);
}
