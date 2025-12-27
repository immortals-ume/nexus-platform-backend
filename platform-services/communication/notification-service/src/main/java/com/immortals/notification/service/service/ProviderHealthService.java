package com.immortals.notification.service.service;

import com.immortals.notification.service.domain.model.Notification;
import com.immortals.notification.service.domain.model.ProviderHealth;

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
    
    /**
     * Check if provider is healthy (without channel type)
     * Returns true if provider is healthy for any channel
     */
    default boolean isHealthy(String providerId) {
        // Default implementation - check if provider is generally healthy
        // Implementations can override for more sophisticated logic
        return true;
    }
}
