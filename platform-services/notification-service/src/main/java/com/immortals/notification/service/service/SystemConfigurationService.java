package com.immortals.notification.service.service;

import com.immortals.platform.domain.notifications.SystemConfiguration;

import java.util.List;
import java.util.Optional;

public interface SystemConfigurationService {

    Optional<SystemConfiguration> getConfiguration(String key);

    String getConfigValue(String key, String defaultValue);

    Integer getConfigValueAsInt(String key, Integer defaultValue);

    Boolean getConfigValueAsBoolean(String key, Boolean defaultValue);

    Double getConfigValueAsDouble(String key, Double defaultValue);

    List<SystemConfiguration> getConfigurationsByCategory(String category);

    List<SystemConfiguration> getAllActiveConfigurations();

    SystemConfiguration saveConfiguration(SystemConfiguration configuration);

    SystemConfiguration updateConfiguration(String key, String value);

    void deleteConfiguration(String key);

    void refreshCache();
}
