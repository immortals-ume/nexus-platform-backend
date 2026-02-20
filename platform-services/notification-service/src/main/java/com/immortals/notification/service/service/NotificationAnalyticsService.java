package com.immortals.notification.service.service;

import com.immortals.platform.domain.notifications.dto.AnalyticsFilter;
import com.immortals.platform.domain.notifications.dto.AnalyticsMetrics;
import com.immortals.platform.domain.notifications.domain.Notification;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service for notification analytics and metrics
 * Enhanced to support aggregation queries with filtering by channel, provider, country, and time period
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
    
    /**
     * Get comprehensive analytics metrics with filtering support
     * Supports filtering by channel, provider, country, and time period
     * 
     * @param filter Analytics filter criteria
     * @return Comprehensive analytics metrics including breakdowns and failure categorization
     */
    AnalyticsMetrics getAnalytics(AnalyticsFilter filter);
    
    /**
     * Get aggregated metrics by channel
     * 
     * @param filter Analytics filter criteria
     * @return Map of channel to metrics
     */
    Map<String, AnalyticsMetrics.ChannelMetrics> getMetricsByChannel(AnalyticsFilter filter);
    
    /**
     * Get aggregated metrics by provider
     * 
     * @param filter Analytics filter criteria
     * @return Map of provider ID to metrics
     */
    Map<String, AnalyticsMetrics.ProviderMetrics> getMetricsByProvider(AnalyticsFilter filter);
    
    /**
     * Get aggregated metrics by country
     * 
     * @param filter Analytics filter criteria
     * @return Map of country code to metrics
     */
    Map<String, AnalyticsMetrics.CountryMetrics> getMetricsByCountry(AnalyticsFilter filter);
    
    /**
     * Get failure categorization with counts
     * Categorizes failures by reason (provider error, invalid recipient, rate limit, etc.)
     * 
     * @param filter Analytics filter criteria
     * @return Map of failure reason to count
     */
    Map<String, Long> getFailureReasons(AnalyticsFilter filter);
    
    /**
     * Calculate average delivery time per channel
     * 
     * @param filter Analytics filter criteria
     * @return Map of channel to average delivery time in seconds
     */
    Map<String, Double> getAverageDeliveryTimeByChannel(AnalyticsFilter filter);
    
    /**
     * Calculate average delivery time per provider
     * 
     * @param filter Analytics filter criteria
     * @return Map of provider ID to average delivery time in seconds
     */
    Map<String, Double> getAverageDeliveryTimeByProvider(AnalyticsFilter filter);
}
