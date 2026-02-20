package com.immortals.notification.service.service.impl;

import com.immortals.platform.domain.notifications.domain.Notification;
import com.immortals.platform.domain.notifications.domain.ProviderHealth;
import com.immortals.notification.service.application.usecase.port.NotificationProvider;
import com.immortals.notification.service.service.ProviderHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced implementation of provider health tracking with scheduled health checks
 * Uses in-memory storage with cache integration for distributed tracking
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProviderHealthServiceImpl implements ProviderHealthService {
    
    private final Map<String, ProviderHealth> healthMap = new ConcurrentHashMap<>();
    private final List<NotificationProvider> providers;
    private final NotificationMetricsService metricsService;
    
    @Override
    @Cacheable(value = "provider:health", key = "#providerId + ':' + #type.name()")
    public ProviderHealth getHealth(String providerId, Notification.NotificationType type) {
        String key = getKey(providerId, type);
        return healthMap.computeIfAbsent(key, k -> ProviderHealth.builder()
                .providerId(providerId)
                .type(type)
                .status(ProviderHealth.HealthStatus.HEALTHY)
                .successCount(0)
                .failureCount(0)
                .successRate(100.0)
                .lastCheckedAt(LocalDateTime.now())
                .build());
    }
    
    @Override
    @CachePut(value = "provider:health", key = "#providerId + ':' + #type.name()")
    public void recordSuccess(String providerId, Notification.NotificationType type) {
        var health = getHealth(providerId, type);
        health.recordSuccess();
        log.debug("Provider {} success rate: {}%", providerId, health.getSuccessRate());
        
        // Update cache
        updateHealthInCache(providerId, type, health);
        
        // Record metrics
        metricsService.recordProviderHealth(providerId, type, health.isHealthy());
    }
    
    @Override
    @CachePut(value = "provider:health", key = "#providerId + ':' + #type.name()")
    public void recordFailure(String providerId, Notification.NotificationType type) {
        var health = getHealth(providerId, type);
        health.recordFailure();
        log.warn("Provider {} success rate dropped to: {}%", providerId, health.getSuccessRate());
        
        // Update cache
        updateHealthInCache(providerId, type, health);
        
        // Record metrics
        metricsService.recordProviderHealth(providerId, type, health.isHealthy());
        
        // Disable provider if unhealthy
        if (!health.isHealthy()) {
            log.error("Provider {} is now UNHEALTHY with success rate: {}%. Consider disabling.", 
                    providerId, health.getSuccessRate());
        }
    }
    
    @Override
    public boolean isHealthy(String providerId, Notification.NotificationType type) {
        return getHealth(providerId, type).isHealthy();
    }
    
    @Override
    public boolean isHealthy(String providerId) {
        // Check if provider is healthy across all channels
        // A provider is considered healthy if it's healthy for at least one channel
        // or if no health data exists yet (new provider)
        return healthMap.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(providerId + ":"))
                .map(Map.Entry::getValue)
                .findAny()
                .map(ProviderHealth::isHealthy)
                .orElse(true); // Default to healthy if no data exists
    }
    
    /**
     * Scheduled health check for all providers
     * Runs every 5 minutes to verify provider availability
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void performHealthChecks() {
        log.info("Starting scheduled health checks for {} providers", providers.size());
        
        for (NotificationProvider provider : providers) {
            try {
                boolean healthy = provider.isHealthy();
                String providerId = provider.getProviderId();
                Notification.NotificationType channel = provider.getChannel();
                
                ProviderHealth health = getHealth(providerId, channel);
                health.setLastCheckedAt(LocalDateTime.now());
                
                if (healthy) {
                    log.debug("Provider {} health check: HEALTHY", providerId);
                } else {
                    log.warn("Provider {} health check: UNHEALTHY", providerId);
                    // Mark as degraded if health check fails
                    health.setStatus(ProviderHealth.HealthStatus.DEGRADED);
                }
                
                // Update cache
                updateHealthInCache(providerId, channel, health);
                
            } catch (Exception e) {
                log.error("Error performing health check for provider {}: {}", 
                        provider.getProviderId(), e.getMessage(), e);
            }
        }
        
        log.info("Completed scheduled health checks");
    }
    
    /**
     * Update health status in cache
     */
    private void updateHealthInCache(String providerId, Notification.NotificationType type, ProviderHealth health) {
        String key = getKey(providerId, type);
        healthMap.put(key, health);
    }
    
    /**
     * Get all provider health statuses
     * Useful for monitoring and analytics
     */
    public Map<String, ProviderHealth> getAllHealthStatuses() {
        return Map.copyOf(healthMap);
    }
    
    /**
     * Reset health statistics for a provider
     * Useful for testing or after provider maintenance
     */
    public void resetHealth(String providerId, Notification.NotificationType type) {
        String key = getKey(providerId, type);
        healthMap.remove(key);
        log.info("Reset health statistics for provider: {} channel: {}", providerId, type);
    }
    
    private String getKey(String providerId, Notification.NotificationType type) {
        return providerId + ":" + type.name();
    }
}
