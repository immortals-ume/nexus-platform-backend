package com.immortals.platform.domain.notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Analytics metrics response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsMetrics {
    
    private Long totalSent;
    private Long totalDelivered;
    private Long totalFailed;
    private Long totalRead;
    private Double deliveryRate;
    private Double failureRate;
    private Double readRate;
    private Double averageDeliveryTimeSeconds;
    
    // Breakdown by channel
    private Map<String, ChannelMetrics> channelMetrics;
    
    // Breakdown by provider
    private Map<String, ProviderMetrics> providerMetrics;
    
    // Breakdown by country
    private Map<String, CountryMetrics> countryMetrics;
    
    // Failure categorization
    private Map<String, Long> failureReasons;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChannelMetrics {
        private String channel;
        private Long sent;
        private Long delivered;
        private Long failed;
        private Long read;
        private Double deliveryRate;
        private Double failureRate;
        private Double averageDeliveryTimeSeconds;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderMetrics {
        private String providerId;
        private Long sent;
        private Long delivered;
        private Long failed;
        private Double deliveryRate;
        private Double failureRate;
        private Double averageDeliveryTimeSeconds;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountryMetrics {
        private String countryCode;
        private Long sent;
        private Long delivered;
        private Long failed;
        private Double deliveryRate;
    }
}
