package com.immortals.notification.service.configuration;

import com.immortals.notification.service.service.ProviderHealthService;
import com.immortals.platform.domain.notifications.domain.Notification;
import com.immortals.notification.service.application.usecase.port.NotificationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom health indicator for notification service
 * Checks:
 * - Database connectivity
 * - Kafka connectivity
 * - Redis connectivity
 * - At least one provider is healthy per channel
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final List<NotificationProvider> providers;
    private final ProviderHealthService providerHealthService;
    private final NotificationServiceProperties properties;

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        boolean overallHealthy = true;

        // Check database connectivity
        boolean databaseHealthy = checkDatabaseHealth();
        details.put("database", databaseHealthy ? "UP" : "DOWN");
        if (!databaseHealthy) {
            overallHealthy = false;
        }

        // Check Kafka connectivity
        boolean kafkaHealthy = checkKafkaHealth();
        details.put("kafka", kafkaHealthy ? "UP" : "DOWN");
        if (!kafkaHealthy) {
            overallHealthy = false;
        }

        // Check Redis connectivity
        boolean redisHealthy = checkRedisHealth();
        details.put("redis", redisHealthy ? "UP" : "DOWN");
        if (!redisHealthy) {
            overallHealthy = false;
        }

        // Check provider health per channel
        Map<String, Object> providerHealth = checkProviderHealth();
        details.put("providers", providerHealth);
        
        // Check if at least one provider is healthy per channel
        boolean providersHealthy = isAtLeastOneProviderHealthyPerChannel(providerHealth);
        if (!providersHealthy) {
            overallHealthy = false;
        }

        // Build health response
        Health.Builder healthBuilder = overallHealthy ? Health.up() : Health.down();
        
        return healthBuilder
                .withDetails(details)
                .build();
    }

    /**
     * Check database connectivity
     */
    private boolean checkDatabaseHealth() {
        try {
            try (Connection connection = dataSource.getConnection()) {
                return connection.isValid(properties.getHealthCheck().getDatabaseTimeoutSeconds());
            }
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return false;
        }
    }

    /**
     * Check Kafka connectivity
     */
    private boolean checkKafkaHealth() {
        try {
            // Try to get metrics from Kafka producer
            // If Kafka is down, this will throw an exception
            kafkaTemplate.metrics();
            return true;
        } catch (Exception e) {
            log.error("Kafka health check failed", e);
            return false;
        }
    }

    /**
     * Check Redis connectivity
     */
    private boolean checkRedisHealth() {
        try {
            String pong = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
            return properties.getHealthCheck().getRedisHealthCommand().equalsIgnoreCase(pong);
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return false;
        }
    }

    /**
     * Check provider health for all channels
     */
    private Map<String, Object> checkProviderHealth() {
        Map<String, Object> providerHealthMap = new HashMap<>();
        
        // Group providers by channel
        Map<Notification.NotificationType, List<NotificationProvider>> providersByChannel = new HashMap<>();
        for (NotificationProvider provider : providers) {
            Notification.NotificationType channel = provider.getChannel();
            providersByChannel.computeIfAbsent(channel, k -> new java.util.ArrayList<>()).add(provider);
        }
        
        // Check health for each channel
        for (Map.Entry<Notification.NotificationType, List<NotificationProvider>> entry : providersByChannel.entrySet()) {
            Notification.NotificationType channel = entry.getKey();
            List<NotificationProvider> channelProviders = entry.getValue();
            
            Map<String, String> channelHealth = new HashMap<>();
            int healthyCount = 0;
            
            for (NotificationProvider provider : channelProviders) {
                boolean isHealthy = providerHealthService.isHealthy(provider.getProviderId(), channel);
                channelHealth.put(provider.getProviderId(), isHealthy ? "HEALTHY" : "UNHEALTHY");
                if (isHealthy) {
                    healthyCount++;
                }
            }
            
            Map<String, Object> channelDetails = new HashMap<>();
            channelDetails.put("providers", channelHealth);
            channelDetails.put("healthyCount", healthyCount);
            channelDetails.put("totalCount", channelProviders.size());
            channelDetails.put("status", healthyCount > 0 ? "UP" : "DOWN");
            
            providerHealthMap.put(channel.name(), channelDetails);
        }
        
        return providerHealthMap;
    }

    /**
     * Check if at least one provider is healthy per channel
     */
    private boolean isAtLeastOneProviderHealthyPerChannel(Map<String, Object> providerHealth) {
        for (Map.Entry<String, Object> entry : providerHealth.entrySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> channelDetails = (Map<String, Object>) entry.getValue();
            Integer healthyCount = (Integer) channelDetails.get("healthyCount");
            
            if (healthyCount == null || healthyCount == 0) {
                log.warn("No healthy providers for channel: {}", entry.getKey());
                return false;
            }
        }
        
        return true;
    }
}
