package com.immortals.notification.service.service.impl;

import com.immortals.platform.domain.notifications.domain.Notification;
import com.immortals.platform.domain.notifications.domain.NotificationPriority;
import com.immortals.notification.service.application.usecase.port.NotificationProvider;
import com.immortals.notification.service.service.ProviderHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Service responsible for routing notifications to appropriate providers
 * Implements provider selection logic based on channel, country, priority, and health
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProviderRouter {
    
    private final List<NotificationProvider> providers;
    private final ProviderHealthService providerHealthService;
    
    /**
     * Select the best provider for a notification based on channel, country, priority, and health
     * 
     * @param channel the notification channel
     * @param countryCode the recipient's country code
     * @param priority the notification priority
     * @return the selected provider, or empty if no suitable provider found
     */
    public Optional<NotificationProvider> selectProvider(
            Notification.NotificationType channel,
            String countryCode,
            NotificationPriority priority) {
        
        log.debug("Selecting provider for channel: {}, country: {}, priority: {}", 
                channel, countryCode, priority);
        
        return providers.stream()
                // Filter by channel and country support
                .filter(provider -> provider.supports(channel, countryCode))
                // Filter by health status
                .filter(provider -> providerHealthService.isHealthy(provider.getProviderId()))
                // Sort by priority (lower number = higher priority)
                .sorted(Comparator.comparingInt(NotificationProvider::getPriority))
                // Get the first (highest priority) provider
                .findFirst()
                .map(provider -> {
                    log.info("Selected provider: {} for channel: {}, country: {}", 
                            provider.getProviderId(), channel, countryCode);
                    return provider;
                });
    }
    
    /**
     * Get a failover provider when the primary provider fails
     * Excludes the failed provider from selection
     * 
     * @param channel the notification channel
     * @param countryCode the recipient's country code
     * @param excludeProviderId the provider ID to exclude (the one that failed)
     * @return the failover provider, or empty if no alternative found
     */
    public Optional<NotificationProvider> getFailoverProvider(
            Notification.NotificationType channel,
            String countryCode,
            String excludeProviderId) {
        
        log.debug("Finding failover provider for channel: {}, country: {}, excluding: {}", 
                channel, countryCode, excludeProviderId);
        
        return providers.stream()
                // Exclude the failed provider
                .filter(provider -> !provider.getProviderId().equals(excludeProviderId))
                // Filter by channel and country support
                .filter(provider -> provider.supports(channel, countryCode))
                // Filter by health status
                .filter(provider -> providerHealthService.isHealthy(provider.getProviderId()))
                // Sort by priority
                .sorted(Comparator.comparingInt(NotificationProvider::getPriority))
                // Get the first available alternative
                .findFirst()
                .map(provider -> {
                    log.info("Selected failover provider: {} for channel: {}, country: {}", 
                            provider.getProviderId(), channel, countryCode);
                    return provider;
                });
    }
    
    /**
     * Get all providers that support a specific channel and country
     * Useful for analytics and monitoring
     * 
     * @param channel the notification channel
     * @param countryCode the recipient's country code
     * @return list of supporting providers, sorted by priority
     */
    public List<NotificationProvider> getAvailableProviders(
            Notification.NotificationType channel,
            String countryCode) {
        
        return providers.stream()
                .filter(provider -> provider.supports(channel, countryCode))
                .filter(provider -> providerHealthService.isHealthy(provider.getProviderId()))
                .sorted(Comparator.comparingInt(NotificationProvider::getPriority))
                .toList();
    }
    
    /**
     * Get a provider by its ID
     * 
     * @param providerId the provider identifier
     * @return the provider, or empty if not found
     */
    public Optional<NotificationProvider> getProviderById(String providerId) {
        return providers.stream()
                .filter(provider -> provider.getProviderId().equals(providerId))
                .findFirst();
    }
}
