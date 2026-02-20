package com.immortals.platform.common.featureswitch;

import com.immortals.platform.common.config.FeatureSwitchProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing feature switches
 */
@RequiredArgsConstructor
@Slf4j
public class FeatureSwitchService {
    
    private final FeatureSwitchProperties properties;
    
    public boolean isEnabled(String featureName) {
        if (!properties.isEnabled()) {
            return false;
        }
        
        Boolean enabled = properties.getSwitches().get(featureName);
        return enabled != null ? enabled : false;
    }
    
    public boolean isEnabled(String featureName, boolean defaultValue) {
        if (!properties.isEnabled()) {
            return defaultValue;
        }
        
        Boolean enabled = properties.getSwitches().get(featureName);
        return enabled != null ? enabled : defaultValue;
    }
}