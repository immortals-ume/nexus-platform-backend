package com.immortals.notification.service.infra.mapper;

import com.immortals.platform.domain.notifications.domain.Notification;
import com.immortals.platform.domain.notifications.entity.ProviderConfig;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapper for converting between ProviderConfig and ProviderConfig domain model
 */
@Component
public class ProviderConfigMapper {
    
    /**
     * Convert entity to domain model
     */
    public com.immortals.platform.domain.notifications.domain.ProviderConfig toDomain(ProviderConfig entity) {
        if (entity == null) {
            return null;
        }
        
        return com.immortals.platform.domain.notifications.domain.ProviderConfig.builder()
                .id(entity.getId())
                .providerId(entity.getProviderId())
                .providerName(entity.getProviderName())
                .channel(parseChannel(entity.getChannel()))
                .supportedCountries(entity.getSupportedCountries() != null ? 
                        Arrays.asList(entity.getSupportedCountries()) : List.of())
                .priority(entity.getPriority())
                .enabled(entity.getEnabled())
                .credentials(entity.getCredentials())
                .configuration(entity.getConfiguration())
                .rateLimitConfig(mapToRateLimitConfig(entity.getRateLimitConfig()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
    /**
     * Convert domain model to entity
     */
    public ProviderConfig toEntity(com.immortals.platform.domain.notifications.domain.ProviderConfig domain) {
        if (domain == null) {
            return null;
        }
        
        return ProviderConfig.builder()
                .providerId(domain.getProviderId())
                .providerName(domain.getProviderName())
                .channel(domain.getChannel() != null ? domain.getChannel().name() : null)
                .supportedCountries(domain.getSupportedCountries() != null ? 
                        domain.getSupportedCountries().toArray(new String[0]) : null)
                .priority(domain.getPriority())
                .enabled(domain.isEnabled())
                .credentials(domain.getCredentials())
                .configuration(domain.getConfiguration())
                .rateLimitConfig(rateLimitConfigToMap(domain.getRateLimitConfig()))
                .build();
    }
    
    /**
     * Parse channel string to NotificationType enum
     */
    private Notification.NotificationType parseChannel(String channel) {
        if (channel == null) {
            return null;
        }
        try {
            return Notification.NotificationType.valueOf(channel);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Convert rate limit config map to domain model
     */
    private com.immortals.platform.domain.notifications.domain.ProviderConfig.RateLimitConfig mapToRateLimitConfig(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        
        return com.immortals.platform.domain.notifications.domain.ProviderConfig.RateLimitConfig.builder()
                .requestsPerSecond(getIntegerValue(map, "requestsPerSecond"))
                .requestsPerMinute(getIntegerValue(map, "requestsPerMinute"))
                .requestsPerHour(getIntegerValue(map, "requestsPerHour"))
                .requestsPerDay(getIntegerValue(map, "requestsPerDay"))
                .build();
    }
    
    /**
     * Convert rate limit config domain model to map
     */
    public Map<String, Object> rateLimitConfigToMap(com.immortals.platform.domain.notifications.domain.ProviderConfig.RateLimitConfig config) {
        if (config == null) {
            return null;
        }
        
        Map<String, Object> map = new HashMap<>();
        if (config.getRequestsPerSecond() != null) {
            map.put("requestsPerSecond", config.getRequestsPerSecond());
        }
        if (config.getRequestsPerMinute() != null) {
            map.put("requestsPerMinute", config.getRequestsPerMinute());
        }
        if (config.getRequestsPerHour() != null) {
            map.put("requestsPerHour", config.getRequestsPerHour());
        }
        if (config.getRequestsPerDay() != null) {
            map.put("requestsPerDay", config.getRequestsPerDay());
        }
        return map;
    }
    
    /**
     * Safely get integer value from map
     */
    private Integer getIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
