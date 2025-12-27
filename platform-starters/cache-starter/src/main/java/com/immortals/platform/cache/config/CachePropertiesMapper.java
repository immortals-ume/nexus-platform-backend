package com.immortals.platform.cache.config;

import com.immortals.platform.cache.providers.redis.RedisProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for mapping cache properties to provider-specific properties.
 *
 * @since 2.0.0
 */
@Slf4j
public final class CachePropertiesMapper {

    private CachePropertiesMapper() {
        // Utility class
    }

    /**
     * Maps cache properties to Redis properties.
     *
     * @param cacheProperties cache properties
     * @return Redis properties
     */
    public static RedisProperties mapToRedisProperties(CacheProperties cacheProperties) {
        log.debug("Mapping cache properties to Redis properties");
        
        RedisProperties redisProperties = cacheProperties.getRedisProperties();
        
        // Apply defaults if not set
        if (redisProperties.getHost() == null) {
            redisProperties.setHost("localhost");
        }
        if (redisProperties.getPort() == null) {
            redisProperties.setPort(6379);
        }
        if (redisProperties.getCommandTimeout() == null) {
            redisProperties.setCommandTimeout(java.time.Duration.ofSeconds(2));
        }
        
        log.debug("Mapped Redis properties: host={}, port={}", 
                 redisProperties.getHost(), redisProperties.getPort());
        
        return redisProperties;
    }

    /**
     * Validates Redis configuration.
     *
     * @param cacheProperties cache properties
     * @throws IllegalArgumentException if configuration is invalid
     */
    public static void validateRedisConfiguration(CacheProperties cacheProperties) {
        RedisProperties redisProperties = cacheProperties.getRedisProperties();
        
        if (redisProperties == null) {
            throw new IllegalArgumentException("Redis properties cannot be null");
        }
        
        // Additional validation can be added here
        log.debug("Redis configuration validation passed");
    }
}