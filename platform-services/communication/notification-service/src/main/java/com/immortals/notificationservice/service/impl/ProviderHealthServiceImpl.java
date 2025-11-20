package com.immortals.notificationservice.service.impl;

import com.immortals.notificationservice.domain.model.Notification;
import com.immortals.notificationservice.domain.model.ProviderHealth;
import com.immortals.notificationservice.service.ProviderHealthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of provider health tracking
 * In production, this should be backed by Redis for distributed tracking
 */
@Service
@Slf4j
public class ProviderHealthServiceImpl implements ProviderHealthService {
    
    private final Map<String, ProviderHealth> healthMap = new ConcurrentHashMap<>();
    
    @Override
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
    public void recordSuccess(String providerId, Notification.NotificationType type) {
        var health = getHealth(providerId, type);
        health.recordSuccess();
        log.debug("Provider {} success rate: {}%", providerId, health.getSuccessRate());
    }
    
    @Override
    public void recordFailure(String providerId, Notification.NotificationType type) {
        var health = getHealth(providerId, type);
        health.recordFailure();
        log.warn("Provider {} success rate dropped to: {}%", providerId, health.getSuccessRate());
    }
    
    @Override
    public boolean isHealthy(String providerId, Notification.NotificationType type) {
        return getHealth(providerId, type).isHealthy();
    }
    
    private String getKey(String providerId, Notification.NotificationType type) {
        return providerId + ":" + type.name();
    }
}
