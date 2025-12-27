package com.immortals.platform.domain.notifications.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for updating provider configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Provider configuration update request")
public class ProviderConfigRequest {
    
    @Schema(description = "Provider display name", example = "Twilio SMS Provider")
    private String providerName;
    
    @Schema(description = "Provider priority (lower is higher priority)", example = "10")
    @Min(value = 1, message = "Priority must be at least 1")
    private Integer priority;
    
    @Schema(description = "List of supported country codes", example = "[\"US\", \"CA\", \"GB\"]")
    private List<String> supportedCountries;
    
    @Schema(description = "Provider credentials (API keys, tokens)")
    private Map<String, String> credentials;
    
    @Schema(description = "Provider-specific configuration")
    private Map<String, Object> configuration;
    
    @Schema(description = "Rate limit configuration")
    private RateLimitConfigDto rateLimitConfig;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Rate limit configuration")
    public static class RateLimitConfigDto {
        
        @Schema(description = "Maximum requests per second", example = "10")
        @Min(value = 1, message = "Requests per second must be at least 1")
        private Integer requestsPerSecond;
        
        @Schema(description = "Maximum requests per minute", example = "100")
        @Min(value = 1, message = "Requests per minute must be at least 1")
        private Integer requestsPerMinute;
        
        @Schema(description = "Maximum requests per hour", example = "1000")
        @Min(value = 1, message = "Requests per hour must be at least 1")
        private Integer requestsPerHour;
        
        @Schema(description = "Maximum requests per day", example = "10000")
        @Min(value = 1, message = "Requests per day must be at least 1")
        private Integer requestsPerDay;
    }
}
