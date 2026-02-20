package com.immortals.notification.service.service;

import com.immortals.platform.domain.notifications.domain.ProviderConfig;
import com.immortals.platform.domain.notifications.domain.Notification;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing provider configurations
 * Loads provider configs from database and config-service
 * Supports dynamic configuration updates
 * Requirements: 2.1, 2.5
 */
public interface ProviderConfigService {
    
    /**
     * Get all provider configurations
     */
    List<ProviderConfig> getAllProviders();
    
    /**
     * Get all enabled provider configurations
     */
    List<ProviderConfig> getAllEnabledProviders();
    
    /**
     * Get provider configuration by provider ID
     */
    Optional<ProviderConfig> getProviderById(String providerId);
    
    /**
     * Get enabled providers for a specific channel
     */
    List<ProviderConfig> getEnabledProvidersByChannel(Notification.NotificationType channel);
    
    /**
     * Get enabled providers for a specific channel and country
     */
    List<ProviderConfig> getEnabledProvidersByChannelAndCountry(
            Notification.NotificationType channel, 
            String countryCode
    );
    
    /**
     * Update provider configuration
     */
    ProviderConfig updateProvider(String providerId, ProviderConfig config);
    
    /**
     * Enable or disable a provider
     */
    ProviderConfig toggleProvider(String providerId, boolean enabled);
    
    /**
     * Refresh provider configurations from config-service
     */
    void refreshConfigurations();
    
    /**
     * Check if provider exists and is enabled
     */
    boolean isProviderEnabled(String providerId);
}
