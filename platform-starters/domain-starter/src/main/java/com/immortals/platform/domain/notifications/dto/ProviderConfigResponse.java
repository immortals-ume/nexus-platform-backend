package com.immortals.platform.domain.notifications.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for provider configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Provider configuration response")
public class ProviderConfigResponse {
    
    @Schema(description = "Provider unique identifier", example = "TWILIO")
    private String providerId;
    
    @Schema(description = "Provider display name", example = "Twilio SMS Provider")
    private String providerName;
    
    @Schema(description = "Notification channel", example = "SMS")
    private String channel;
    
    @Schema(description = "List of supported country codes", example = "[\"US\", \"CA\", \"GB\"]")
    private List<String> supportedCountries;
    
    @Schema(description = "Provider priority (lower is higher priority)", example = "10")
    private Integer priority;
    
    @Schema(description = "Whether provider is enabled", example = "true")
    private Boolean enabled;
    
    @Schema(description = "Provider health status", example = "true")
    private Boolean healthy;
    
    @Schema(description = "Provider-specific configuration (credentials masked)")
    private Map<String, Object> configuration;
    
    @Schema(description = "Rate limit configuration")
    private RateLimitConfigDto rateLimitConfig;
    
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;
    
    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Rate limit configuration")
    public static class RateLimitConfigDto {
        
        @Schema(description = "Maximum requests per second", example = "10")
        private Integer requestsPerSecond;
        
        @Schema(description = "Maximum requests per minute", example = "100")
        private Integer requestsPerMinute;
        
        @Schema(description = "Maximum requests per hour", example = "1000")
        private Integer requestsPerHour;
        
        @Schema(description = "Maximum requests per day", example = "10000")
        private Integer requestsPerDay;
    }
}
