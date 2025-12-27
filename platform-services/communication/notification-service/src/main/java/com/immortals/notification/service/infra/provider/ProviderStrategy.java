package com.immortals.notification.service.infra.provider;

import com.immortals.notification.service.domain.model.Notification;
import com.immortals.notification.service.domain.port.NotificationProvider;

import java.util.List;

/**
 * Strategy interface for selecting notification providers
 * Supports failover, load balancing, and health-based routing
 */
public interface ProviderStrategy {
    
    /**
     * Select the best provider for the notification
     */
    NotificationProvider selectProvider(
            Notification notification,
            List<NotificationProvider> availableProviders
    );
    
    /**
     * Get strategy name
     */
    String getStrategyName();
}
