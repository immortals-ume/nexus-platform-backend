package com.immortals.platform.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Feature switch configuration properties
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "common.features")
public class FeatureSwitchProperties {
    
    private boolean enabled;
    private Map<String, Boolean> switches = new HashMap<>();
    private String source;
}