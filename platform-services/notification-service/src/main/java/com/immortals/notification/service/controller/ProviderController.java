package com.immortals.notification.service.controller;

import com.immortals.platform.domain.notifications.dto.ProviderConfigRequest;
import com.immortals.platform.domain.notifications.dto.ProviderConfigResponse;
import com.immortals.platform.domain.notifications.dto.ProviderHealthResponse;
import com.immortals.notification.service.service.ProviderConfigService;
import com.immortals.notification.service.service.ProviderHealthService;
import com.immortals.platform.domain.dto.ApiResponse;
import com.immortals.platform.domain.notifications.domain.ProviderConfig;
import com.immortals.platform.domain.notifications.domain.ProviderHealth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API controller for provider management
 * Provides endpoints for listing, configuring, and monitoring notification providers
 * Requirements: 11.4
 */
@RestController
@RequestMapping("/api/v1/notifications/providers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Provider Management", description = "Provider configuration and monitoring APIs")
public class ProviderController {
    
    private final ProviderConfigService providerConfigService;
    private final ProviderHealthService providerHealthService;
    
    /**
     * GET /api/v1/notifications/providers - List all providers
     * Requirements: 11.4
     */
    @GetMapping
    @Operation(summary = "List all notification providers", 
            description = "Get a list of all configured notification providers with their status")
    public ResponseEntity<ApiResponse<List<ProviderConfigResponse>>> getAllProviders(
            @Parameter(description = "Filter by enabled status")
            @RequestParam(required = false) Boolean enabled) {
        
        log.info("Fetching all providers, enabled filter: {}", enabled);
        
        List<ProviderConfig> providers;
        if (Boolean.TRUE.equals(enabled)) {
            providers = providerConfigService.getAllEnabledProviders();
        } else {
            providers = providerConfigService.getAllProviders();
        }
        
        List<ProviderConfigResponse> responses = providers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(
                responses,
                "Providers retrieved successfully"
        ));
    }
    
    /**
     * GET /api/v1/notifications/providers/{id} - Get provider details
     * Requirements: 11.4
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get provider details", 
            description = "Get detailed configuration and status of a specific provider")
    public ResponseEntity<ApiResponse<ProviderConfigResponse>> getProviderById(
            @Parameter(description = "Provider ID (e.g., TWILIO, GUPSHUP)")
            @PathVariable String id) {
        
        log.info("Fetching provider details for id: {}", id);
        
        ProviderConfig provider = providerConfigService.getProviderById(id)
                .orElseThrow(() -> new com.immortals.platform.common.exception.ResourceNotFoundException(
                        "Provider not found with id: " + id));
        
        ProviderConfigResponse response = mapToResponse(provider);
        
        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Provider details retrieved successfully"
        ));
    }
    
    /**
     * PUT /api/v1/notifications/providers/{id} - Update provider configuration
     * Requirements: 11.4
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update provider configuration", 
            description = "Update configuration settings for a specific provider")
    public ResponseEntity<ApiResponse<ProviderConfigResponse>> updateProvider(
            @Parameter(description = "Provider ID (e.g., TWILIO, GUPSHUP)")
            @PathVariable String id,
            @Valid @RequestBody ProviderConfigRequest request) {
        
        log.info("Updating provider configuration for id: {}", id);
        
        // Convert request to domain model
        ProviderConfig config = mapToDomain(request);
        
        // Update provider
        ProviderConfig updated = providerConfigService.updateProvider(id, config);
        
        ProviderConfigResponse response = mapToResponse(updated);
        
        log.info("Provider {} updated successfully", id);
        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Provider configuration updated successfully"
        ));
    }
    
    /**
     * POST /api/v1/notifications/providers/{id}/health - Manual health check
     * Requirements: 11.4
     */
    @PostMapping("/{id}/health")
    @Operation(summary = "Perform manual health check", 
            description = "Trigger a manual health check for a specific provider")
    public ResponseEntity<ApiResponse<ProviderHealthResponse>> checkProviderHealth(
            @Parameter(description = "Provider ID (e.g., TWILIO, GUPSHUP)")
            @PathVariable String id) {
        
        log.info("Performing manual health check for provider: {}", id);
        
        // Verify provider exists
        ProviderConfig provider = providerConfigService.getProviderById(id)
                .orElseThrow(() -> new com.immortals.platform.common.exception.ResourceNotFoundException(
                        "Provider not found with id: " + id));
        
        // Check health for all channels the provider supports
        boolean isHealthy = providerHealthService.isHealthy(id);
        
        // Get detailed health info if available
        ProviderHealth health = null;
        if (provider.getChannel() != null) {
            health = providerHealthService.getHealth(id, provider.getChannel());
        }
        
        ProviderHealthResponse response = ProviderHealthResponse.builder()
                .providerId(id)
                .providerName(provider.getProviderName())
                .healthy(isHealthy)
                .status(isHealthy ? "UP" : "DOWN")
                .message(isHealthy ? "Provider is healthy" : "Provider is unhealthy")
                .successCount(health != null ? health.getSuccessCount() : 0L)
                .failureCount(health != null ? health.getFailureCount() : 0L)
                .successRate(health != null ? health.getSuccessRate() : 0.0)
                .lastCheckedAt(LocalDateTime.now())
                .build();
        
        log.info("Health check completed for provider {}: {}", id, isHealthy ? "HEALTHY" : "UNHEALTHY");
        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Health check completed"
        ));
    }
    
    /**
     * POST /api/v1/notifications/providers/{id}/toggle - Enable/disable provider
     */
    @PostMapping("/{id}/toggle")
    @Operation(summary = "Enable or disable provider", 
            description = "Toggle provider enabled status")
    public ResponseEntity<ApiResponse<ProviderConfigResponse>> toggleProvider(
            @Parameter(description = "Provider ID (e.g., TWILIO, GUPSHUP)")
            @PathVariable String id,
            @Parameter(description = "Enable or disable")
            @RequestParam boolean enabled) {
        
        log.info("Toggling provider {} to enabled={}", id, enabled);
        
        ProviderConfig updated = providerConfigService.toggleProvider(id, enabled);
        
        ProviderConfigResponse response = mapToResponse(updated);
        
        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Provider " + (enabled ? "enabled" : "disabled") + " successfully"
        ));
    }
    
    /**
     * POST /api/v1/notifications/providers/refresh - Refresh configurations
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh provider configurations", 
            description = "Reload provider configurations from config-service")
    public ResponseEntity<ApiResponse<Void>> refreshConfigurations() {
        
        log.info("Refreshing provider configurations");
        
        providerConfigService.refreshConfigurations();
        
        return ResponseEntity.ok(ApiResponse.success(
                null,
                "Provider configurations refreshed successfully"
        ));
    }
    
    /**
     * Map ProviderConfig domain model to response DTO
     */
    private ProviderConfigResponse mapToResponse(ProviderConfig config) {
        // Check health status
        boolean isHealthy = providerHealthService.isHealthy(config.getProviderId());
        
        // Mask sensitive credentials
        // Don't expose credentials in response for security
        
        return ProviderConfigResponse.builder()
                .providerId(config.getProviderId())
                .providerName(config.getProviderName())
                .channel(config.getChannel() != null ? config.getChannel().name() : null)
                .supportedCountries(config.getSupportedCountries())
                .priority(config.getPriority())
                .enabled(config.isEnabled())
                .healthy(isHealthy)
                .configuration(config.getConfiguration())
                .rateLimitConfig(mapRateLimitConfigToDto(config.getRateLimitConfig()))
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
    
    /**
     * Map request DTO to ProviderConfig domain model
     */
    private ProviderConfig mapToDomain(ProviderConfigRequest request) {
        return ProviderConfig.builder()
                .providerName(request.getProviderName())
                .priority(request.getPriority())
                .supportedCountries(request.getSupportedCountries())
                .credentials(request.getCredentials())
                .configuration(request.getConfiguration())
                .rateLimitConfig(mapRateLimitConfigToDomain(request.getRateLimitConfig()))
                .build();
    }
    
    /**
     * Map rate limit config to DTO
     */
    private ProviderConfigResponse.RateLimitConfigDto mapRateLimitConfigToDto(
            ProviderConfig.RateLimitConfig config) {
        if (config == null) {
            return null;
        }
        return ProviderConfigResponse.RateLimitConfigDto.builder()
                .requestsPerSecond(config.getRequestsPerSecond())
                .requestsPerMinute(config.getRequestsPerMinute())
                .requestsPerHour(config.getRequestsPerHour())
                .requestsPerDay(config.getRequestsPerDay())
                .build();
    }
    
    /**
     * Map rate limit config DTO to domain model
     */
    private ProviderConfig.RateLimitConfig mapRateLimitConfigToDomain(
            ProviderConfigRequest.RateLimitConfigDto dto) {
        if (dto == null) {
            return null;
        }
        return ProviderConfig.RateLimitConfig.builder()
                .requestsPerSecond(dto.getRequestsPerSecond())
                .requestsPerMinute(dto.getRequestsPerMinute())
                .requestsPerHour(dto.getRequestsPerHour())
                .requestsPerDay(dto.getRequestsPerDay())
                .build();
    }
}
