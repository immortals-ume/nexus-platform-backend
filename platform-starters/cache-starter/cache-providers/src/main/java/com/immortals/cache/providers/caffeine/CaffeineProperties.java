package com.immortals.cache.providers.caffeine;


import com.immortals.platform.common.exception.CacheConfigurationException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for Caffeine cache.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "cache.caffeine")
public class CaffeineProperties {

    /**
     * Maximum number of entries the cache may contain.
     */
    private long maximumSize;

    /**
     * Time-to-live duration for cache entries.
     */
    private Duration ttl;

    /**
     * Eviction policy (LRU is default for Caffeine).
     */
    private EvictionPolicy evictionPolicy;

    /**
     * Whether to enable statistics recording.
     */
    private Boolean recordStats;

    public enum EvictionPolicy {
        LRU,
        LFU,
        SIZE
    }

    /**
     * Validates the Caffeine properties.
     * 
     * @throws CacheConfigurationException if validation fails
     */
    public void validate() {
        if (maximumSize <= 0) {
            throw new CacheConfigurationException(
                "Caffeine cache maximum size must be positive",
                "immortals.cache.caffeine.maximum-size",
                maximumSize
            );
        }
    }
}
