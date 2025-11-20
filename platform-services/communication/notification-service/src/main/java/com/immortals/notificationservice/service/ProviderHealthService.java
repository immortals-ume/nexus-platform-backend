package com.immortals.notificationservice.service;

import com.immortals.notificationservice.domain.model.Notification;
import com.immortals.notificationservice.domain.model.ProviderHealth;

/**
 * Service for tracking and managing provider health
 */
public interface ProviderHealthService {
    
    /**
     * Get health status for a provider
     */
    ProviderHealth getHealth(String providerId, Notification.NotificationType type);
    
    /**
     * Record successful delivery
     */
    void recordSuccess(String providerId, Notification.NotificationType type);
    
    /**
     * Record failed delivery
     */
    void recordFailure(String providerId, Notification.NotificationType type);
    
    /**
     * Check if provider is healthy
     */
    boolean isHealthy(String providerId, Notification.NotificationType type);
}
