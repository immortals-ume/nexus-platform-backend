package com.immortals.notification.service.service.impl;

import com.immortals.notification.service.infra.mapper.ProviderConfigMapper;
import com.immortals.notification.service.repository.ProviderConfigRepository;
import com.immortals.notification.service.service.ProviderConfigService;
import com.immortals.platform.cache.service.CacheService;
import com.immortals.platform.common.exception.ResourceNotFoundException;
import com.immortals.platform.domain.notifications.domain.model.Notification;
import com.immortals.platform.domain.notifications.domain.model.ProviderConfig;
import com.immortals.platform.domain.notifications.entity.ProviderConfigEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of ProviderConfigService
 * Manages provider configurations with caching support
 * Requirements: 2.1, 2.5
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderConfigServiceImpl implements ProviderConfigService {
    
    private final ProviderConfigRepository providerConfigRepository;
    private final ProviderConfigMapper providerConfigMapper;
    private final CacheService cacheService;
    
    private static final String CACHE_KEY_PREFIX = "provider:config:";
    private static final String CACHE_KEY_ALL = "provider:config:all";
    private static final int CACHE_TTL_SECONDS = 300; // 5 minutes
    
    @Override
    @Cacheable(value = "providerConfigs", key = "'all'")
    public List<ProviderConfig> getAllProviders() {
        log.debug("Fetching all provider configurations");
        return providerConfigRepository.findAll().stream()
                .map(providerConfigMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    @Cacheable(value = "providerConfigs", key = "'enabled'")
    public List<ProviderConfig> getAllEnabledProviders() {
        log.debug("Fetching all enabled provider configurations");
        return providerConfigRepository.findAllEnabled().stream()
                .map(providerConfigMapper::toDomain)
                .sorted(Comparator.comparing(ProviderConfig::getPriority))
                .collect(Collectors.toList());
    }
    
    @Override
    @Cacheable(value = "providerConfigs", key = "#providerId")
    public Optional<ProviderConfig> getProviderById(String providerId) {
        log.debug("Fetching provider configuration for providerId: {}", providerId);
        return providerConfigRepository.findByProviderId(providerId)
                .map(providerConfigMapper::toDomain);
    }
    
    @Override
    @Cacheable(value = "providerConfigs", key = "'channel:' + #channel.name()")
    public List<ProviderConfig> getEnabledProvidersByChannel(Notification.NotificationType channel) {
        log.debug("Fetching enabled providers for channel: {}", channel);
        return providerConfigRepository.findEnabledByChannel(channel.name()).stream()
                .map(providerConfigMapper::toDomain)
                .sorted(Comparator.comparing(ProviderConfig::getPriority))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ProviderConfig> getEnabledProvidersByChannelAndCountry(
            Notification.NotificationType channel, 
            String countryCode) {
        log.debug("Fetching enabled providers for channel: {} and country: {}", channel, countryCode);
        
        return getEnabledProvidersByChannel(channel).stream()
                .filter(config -> config.supportsCountry(countryCode))
                .sorted(Comparator.comparing(ProviderConfig::getPriority))
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "providerConfigs", allEntries = true)
    public ProviderConfig updateProvider(String providerId, ProviderConfig config) {
        log.info("Updating provider configuration for providerId: {}", providerId);
        
        ProviderConfigEntity entity = providerConfigRepository.findByProviderId(providerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Provider not found with id: " + providerId));
        
        // Update fields
        if (config.getProviderName() != null) {
            entity.setProviderName(config.getProviderName());
        }
        if (config.getPriority() != null) {
            entity.setPriority(config.getPriority());
        }
        if (config.getSupportedCountries() != null) {
            entity.setSupportedCountries(config.getSupportedCountries().toArray(new String[0]));
        }
        if (config.getCredentials() != null) {
            entity.setCredentials(config.getCredentials());
        }
        if (config.getConfiguration() != null) {
            entity.setConfiguration(config.getConfiguration());
        }
        if (config.getRateLimitConfig() != null) {
            entity.setRateLimitConfig(providerConfigMapper.rateLimitConfigToMap(config.getRateLimitConfig()));
        }
        
        entity.setUpdatedAt(LocalDateTime.now());
        
        ProviderConfigEntity saved = providerConfigRepository.save(entity);
        
        // Clear cache
        clearProviderCache(providerId);
        
        log.info("Provider configuration updated successfully for providerId: {}", providerId);
        return providerConfigMapper.toDomain(saved);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "providerConfigs", allEntries = true)
    public ProviderConfig toggleProvider(String providerId, boolean enabled) {
        log.info("Toggling provider {} to enabled={}", providerId, enabled);
        
        ProviderConfigEntity entity = providerConfigRepository.findByProviderId(providerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Provider not found with id: " + providerId));
        
        entity.setEnabled(enabled);
        entity.setUpdatedAt(LocalDateTime.now());
        
        ProviderConfigEntity saved = providerConfigRepository.save(entity);
        
        // Clear cache
        clearProviderCache(providerId);
        
        log.info("Provider {} toggled to enabled={}", providerId, enabled);
        return providerConfigMapper.toDomain(saved);
    }
    
    @Override
    @CacheEvict(value = "providerConfigs", allEntries = true)
    public void refreshConfigurations() {
        log.info("Refreshing provider configurations from config-service");
        // Clear all provider caches
        cacheService.evictAll("providerConfigs");
        log.info("Provider configurations cache cleared");
    }
    
    @Override
    public boolean isProviderEnabled(String providerId) {
        return providerConfigRepository.existsByProviderIdAndEnabled(providerId);
    }
    
    /**
     * Clear cache for a specific provider
     */
    private void clearProviderCache(String providerId) {
        String cacheKey = CACHE_KEY_PREFIX + providerId;
        cacheService.evict("providerConfigs", providerId);
        cacheService.evict("providerConfigs", "all");
        cacheService.evict("providerConfigs", "enabled");
        log.debug("Cleared cache for provider: {}", providerId);
    }
}
