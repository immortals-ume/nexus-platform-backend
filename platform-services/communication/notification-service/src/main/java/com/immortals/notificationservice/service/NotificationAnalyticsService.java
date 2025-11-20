package com.immortals.notificationservice.service;

import com.immortals.notificationservice.domain.model.Notification;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service for notification analytics and metrics
 */
public interface NotificationAnalyticsService {
    
    /**
     * Record notification sent
     */
    void recordSent(Notification notification);
    
    /**
     * Record notification delivered
     */
    void recordDelivered(String notificationId);
    
    /**
     * Record notification opened
     */
    void recordOpened(String notificationId);
    
    /**
     * Record notification clicked
     */
    void recordClicked(String notificationId);
    
    /**
     * Record notification bounced
     */
    void recordBounced(String notificationId, String reason);
    
    /**
     * Get delivery rate for time period
     */
    double getDeliveryRate(LocalDateTime start, LocalDateTime end);
    
    /**
     * Get open rate for time period
     */
    double getOpenRate(LocalDateTime start, LocalDateTime end);
    
    /**
     * Get click rate for time period
     */
    double getClickRate(LocalDateTime start, LocalDateTime end);
    
    /**
     * Get bounce rate for time period
     */
    double getBounceRate(LocalDateTime start, LocalDateTime end);
    
    /**
     * Get metrics by provider
     */
    Map<String, Object> getProviderMetrics(String providerId);
    
    /**
     * Get metrics by notification type
     */
    Map<String, Object> getTypeMetrics(Notification.NotificationType type);
}
