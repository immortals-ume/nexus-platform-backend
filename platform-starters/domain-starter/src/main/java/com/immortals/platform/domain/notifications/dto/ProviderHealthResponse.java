package com.immortals.platform.domain.notifications.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for provider health check
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Provider health check response")
public class ProviderHealthResponse {
    
    @Schema(description = "Provider unique identifier", example = "TWILIO")
    private String providerId;
    
    @Schema(description = "Provider display name", example = "Twilio SMS Provider")
    private String providerName;
    
    @Schema(description = "Whether provider is healthy", example = "true")
    private Boolean healthy;
    
    @Schema(description = "Health check status", example = "UP")
    private String status;
    
    @Schema(description = "Health check message")
    private String message;
    
    @Schema(description = "Success count")
    private Long successCount;
    
    @Schema(description = "Failure count")
    private Long failureCount;
    
    @Schema(description = "Success rate percentage", example = "98.5")
    private Double successRate;
    
    @Schema(description = "Last health check timestamp")
    private LocalDateTime lastCheckedAt;
}
