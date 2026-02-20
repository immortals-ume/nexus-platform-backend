package com.immortals.platform.common.autoconfigure;


import com.immortals.platform.common.config.FeatureSwitchProperties;
import com.immortals.platform.common.featureswitch.FeatureSwitchService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for feature switches
 */
@AutoConfiguration
@ConditionalOnProperty(name = "commons.features.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(FeatureSwitchProperties.class)
public class SwitchesAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FeatureSwitchService featureSwitchService(FeatureSwitchProperties properties) {
        return new FeatureSwitchService(properties);
    }
}