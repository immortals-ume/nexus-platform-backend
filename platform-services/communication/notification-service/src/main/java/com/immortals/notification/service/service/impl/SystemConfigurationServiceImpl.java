package com.immortals.notification.service.service.impl;

import com.immortals.platform.domain.notifications.SystemConfiguration;
import com.immortals.notification.service.repository.SystemConfigurationRepository;
import com.immortals.notification.service.service.SystemConfigurationService;
import com.immortals.platform.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SystemConfigurationServiceImpl implements SystemConfigurationService {

    private final SystemConfigurationRepository repository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "system:config", key = "#key")
    public Optional<SystemConfiguration> getConfiguration(String key) {
        return repository.findByConfigKeyAndIsActiveTrue(key);
    }

    @Override
    @Transactional(readOnly = true)
    public String getConfigValue(String key, String defaultValue) {
        return getConfiguration(key)
                .map(SystemConfiguration::getConfigValue)
                .orElse(defaultValue);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getConfigValueAsInt(String key, Integer defaultValue) {
        return getConfiguration(key)
                .map(config -> {
                    try {
                        return Integer.parseInt(config.getConfigValue());
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse config {} as integer, using default", key);
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean getConfigValueAsBoolean(String key, Boolean defaultValue) {
        return getConfiguration(key)
                .map(config -> Boolean.parseBoolean(config.getConfigValue()))
                .orElse(defaultValue);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getConfigValueAsDouble(String key, Double defaultValue) {
        return getConfiguration(key)
                .map(config -> {
                    try {
                        return Double.parseDouble(config.getConfigValue());
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse config {} as double, using default", key);
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemConfiguration> getConfigurationsByCategory(String category) {
        return repository.findByCategoryAndIsActiveTrue(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemConfiguration> getAllActiveConfigurations() {
        return repository.findByIsActiveTrue();
    }

    @Override
    @Transactional
    @CacheEvict(value = "system:config", key = "#configuration.configKey")
    public SystemConfiguration saveConfiguration(SystemConfiguration configuration) {
        log.info("Saving configuration: key={}", configuration.getConfigKey());
        return repository.save(configuration);
    }

    @Override
    @Transactional
    @CacheEvict(value = "system:config", key = "#key")
    public SystemConfiguration updateConfiguration(String key, String value) {
        log.info("Updating configuration: key={}, value={}", key, value);
        
        SystemConfiguration config = repository.findByConfigKeyAndIsActiveTrue(key)
                .orElseThrow(() -> new ResourceNotFoundException("Configuration not found: " + key));
        
        config.setConfigValue(value);
        return repository.save(config);
    }

    @Override
    @Transactional
    @CacheEvict(value = "system:config", key = "#key")
    public void deleteConfiguration(String key) {
        log.info("Deleting configuration: key={}", key);
        
        SystemConfiguration config = repository.findByConfigKeyAndIsActiveTrue(key)
                .orElseThrow(() -> new ResourceNotFoundException("Configuration not found: " + key));
        
        config.setIsActive(false);
        repository.save(config);
    }

    @Override
    @CacheEvict(value = "system:config", allEntries = true)
    public void refreshCache() {
        log.info("Refreshing system configuration cache");
    }
}
