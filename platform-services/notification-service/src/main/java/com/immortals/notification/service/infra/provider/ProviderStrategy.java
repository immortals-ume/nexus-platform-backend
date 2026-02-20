package com.immortals.notification.service.infra.provider;

import com.immortals.platform.domain.notifications.domain.Notification;
import com.immortals.notification.service.application.usecase.port.NotificationProvider;

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
