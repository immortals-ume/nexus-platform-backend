package com.immortals.cache.providers.multilevel;

import com.immortals.cache.core.exception.CacheConfigurationException;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for multi-level cache.
 * 
 * @since 2.0.0
 */
@Data
@ConfigurationProperties(prefix = "immortals.cache.multi-level")
public class MultiLevelCacheProperties {
    
    /**
     * Enable multi-level caching.
     */
    private boolean enabled = false;
    
    /**
     * Enable distributed eviction notifications.
     */
    private boolean evictionEnabled = true;
    
    /**
     * Enable fallback to L1 when L2 fails.
     */
    private boolean fallbackEnabled = true;
    
    /**
     * Log fallback events.
     */
    private boolean logFallbacks = true;
    
    /**
     * Eviction publisher type (e.g., "redis").
     */
    private String evictionPublisher;
    
    /**
     * Validates the multi-level cache properties.
     * 
     * @throws CacheConfigurationException if validation fails
     */
    public void validate() {
        if (evictionEnabled && (evictionPublisher == null || evictionPublisher.trim().isEmpty())) {
            throw new CacheConfigurationException(
                "Eviction publisher must be configured when eviction is enabled",
                "immortals.cache.multi-level.eviction-publisher",
                evictionPublisher
            );
        }
    }
}
