package com.immortals.notification.service.controller;

import com.immortals.notification.service.service.SystemConfigurationService;
import com.immortals.platform.domain.dto.ApiResponse;
import com.immortals.platform.domain.notifications.SystemConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications/config")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "System Configuration", description = "Dynamic system configuration management APIs")
public class SystemConfigurationController {

    private final SystemConfigurationService configurationService;

    @GetMapping
    @Operation(summary = "Get all active configurations", description = "Retrieve all active system configurations")
    public ResponseEntity<ApiResponse<List<SystemConfiguration>>> getAllConfigurations() {
        log.info("Fetching all active configurations");
        List<SystemConfiguration> configurations = configurationService.getAllActiveConfigurations();
        return ResponseEntity.ok(ApiResponse.success(configurations, "Configurations retrieved successfully"));
    }

    @GetMapping("/{key}")
    @Operation(summary = "Get configuration by key", description = "Retrieve a specific configuration by its key")
    public ResponseEntity<ApiResponse<SystemConfiguration>> getConfiguration(
            @Parameter(description = "Configuration key") @PathVariable String key) {
        log.info("Fetching configuration: key={}", key);
        
        SystemConfiguration config = configurationService.getConfiguration(key)
                .orElseThrow(() -> new com.immortals.platform.common.exception.ResourceNotFoundException(
                        "Configuration not found: " + key));
        
        return ResponseEntity.ok(ApiResponse.success(config, "Configuration retrieved successfully"));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get configurations by category", description = "Retrieve all configurations in a specific category")
    public ResponseEntity<ApiResponse<List<SystemConfiguration>>> getConfigurationsByCategory(
            @Parameter(description = "Configuration category") @PathVariable String category) {
        log.info("Fetching configurations by category: {}", category);
        
        List<SystemConfiguration> configurations = configurationService.getConfigurationsByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(configurations, "Configurations retrieved successfully"));
    }

    @PostMapping
    @Operation(summary = "Create new configuration", description = "Create a new system configuration")
    public ResponseEntity<ApiResponse<SystemConfiguration>> createConfiguration(
            @Valid @RequestBody SystemConfiguration configuration) {
        log.info("Creating new configuration: key={}", configuration.getConfigKey());
        
        SystemConfiguration saved = configurationService.saveConfiguration(configuration);
        return ResponseEntity.ok(ApiResponse.success(saved, "Configuration created successfully"));
    }

    @PutMapping("/{key}")
    @Operation(summary = "Update configuration value", description = "Update the value of an existing configuration")
    public ResponseEntity<ApiResponse<SystemConfiguration>> updateConfiguration(
            @Parameter(description = "Configuration key") @PathVariable String key,
            @Parameter(description = "New configuration value") @RequestParam String value) {
        log.info("Updating configuration: key={}", key);
        
        SystemConfiguration updated = configurationService.updateConfiguration(key, value);
        return ResponseEntity.ok(ApiResponse.success(updated, "Configuration updated successfully"));
    }

    @DeleteMapping("/{key}")
    @Operation(summary = "Delete configuration", description = "Soft delete a configuration (marks as inactive)")
    public ResponseEntity<ApiResponse<Void>> deleteConfiguration(
            @Parameter(description = "Configuration key") @PathVariable String key) {
        log.info("Deleting configuration: key={}", key);
        
        configurationService.deleteConfiguration(key);
        return ResponseEntity.ok(ApiResponse.success(null, "Configuration deleted successfully"));
    }

    @PostMapping("/refresh-cache")
    @Operation(summary = "Refresh configuration cache", description = "Clear and refresh the configuration cache")
    public ResponseEntity<ApiResponse<Void>> refreshCache() {
        log.info("Refreshing configuration cache");
        
        configurationService.refreshCache();
        return ResponseEntity.ok(ApiResponse.success(null, "Configuration cache refreshed successfully"));
    }
}
