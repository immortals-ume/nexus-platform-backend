package com.immortals.platform.domain.notifications.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Domain model for provider configuration
 * Defines provider settings, routing rules, and rate limits
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderConfig {
    
    private Long id;
    private String providerId;  // TWILIO, GUPSHUP, AWS_SNS, MAILCHIMP
    private String providerName;
    private Notification.NotificationType channel;
    private List<String> supportedCountries;  // Country codes (e.g., US, IN, GB)
    private Integer priority;  // Lower = higher priority
    private boolean enabled;
    private Map<String, String> credentials;  // API keys, tokens, secrets
    private Map<String, Object> configuration;  // Provider-specific settings
    private RateLimitConfig rateLimitConfig;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Rate limit configuration for provider
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RateLimitConfig {
        private Integer requestsPerSecond;
        private Integer requestsPerMinute;
        private Integer requestsPerHour;
        private Integer requestsPerDay;
    }
    
    /**
     * Check if provider supports a specific country
     */
    public boolean supportsCountry(String countryCode) {
        if (supportedCountries == null || supportedCountries.isEmpty()) {
            return false;
        }
        // "*" means global support
        if (supportedCountries.contains("*")) {
            return true;
        }
        return supportedCountries.contains(countryCode);
    }
    
    /**
     * Check if provider supports a specific channel
     */
    public boolean supportsChannel(Notification.NotificationType channelType) {
        return this.channel == channelType;
    }
    
    /**
     * Check if provider is available for use
     */
    public boolean isAvailable() {
        return enabled;
    }
}
