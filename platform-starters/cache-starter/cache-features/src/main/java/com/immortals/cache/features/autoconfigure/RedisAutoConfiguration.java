package com.immortals.cache.features.autoconfigure;

import com.immortals.cache.core.CacheService;
import com.immortals.cache.core.CacheServiceFactory;
import com.immortals.cache.providers.redis.CacheClusterConfiguration;
import com.immortals.cache.providers.redis.CacheSentinelConfiguration;
import com.immortals.cache.providers.redis.CacheStandaloneConfiguration;
import com.immortals.cache.providers.redis.RedisProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for Redis cache provider.
 *
 * <p>This configuration is activated when:
 * <ul>
 *   <li>Spring Data Redis and Lettuce are on the classpath</li>
 *   <li>Cache type is set to "redis"</li>
 *   <li>No custom cache provider bean is defined</li>
 * </ul>
 *
 * <p>This class orchestrates the Redis configuration by:
 * <ul>
 *   <li>Conditionally importing the appropriate Redis configuration (Standalone, Sentinel, or Cluster) based on properties</li>
 *   <li>Extracting Redis properties from CacheProperties</li>
 *   <li>Creating a Supplier that delegates to the selected configuration's redisCacheService()</li>
 * </ul>
 *
 * <p>Supported deployment modes:
 * <ul>
 *   <li>Standalone - Single Redis instance (default)</li>
 *   <li>Sentinel - High availability with Redis Sentinel</li>
 *   <li>Cluster - Distributed Redis Cluster</li>
 * </ul>
 *
 * <p>Requirements: 5.3, 6.1, 6.4
 *
 * @since 2.0.0
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = {"org.springframework.data.redis.core.RedisTemplate", "io.lettuce.core.RedisClient"})
@ConditionalOnProperty(name = "immortals.cache.type", havingValue = "redis")
@Import({
        CacheStandaloneConfiguration.class,
        CacheSentinelConfiguration.class,
        CacheClusterConfiguration.class
})
public class RedisAutoConfiguration {

    /**
     * Creates Redis properties from cache properties.
     *
     * <p>Extracts Redis-specific configuration from the main CacheProperties
     * and applies defaults for any missing values. Determines deployment mode
     * (standalone, sentinel, or cluster) based on configuration.
     *
     * @param cacheProperties main cache properties
     * @return Redis-specific properties
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisProperties redisProperties(CacheProperties cacheProperties) {
        log.info("Mapping cache properties to Redis properties");

        try {
            CachePropertiesMapper.validateRedisConfiguration(cacheProperties);
            RedisProperties properties = CachePropertiesMapper.mapToRedisProperties(cacheProperties);

            RedisDeploymentMode deploymentMode = determineDeploymentMode(properties);
            log.info("Redis properties configured: host={}, port={}, database={}, useSsl={}, deploymentMode={}",
                    properties.getHost(), properties.getPort(), properties.getDatabase(), properties.getUseSsl(), deploymentMode.getValue());

            return properties;
        } catch (IllegalStateException e) {
            log.error("Failed to configure Redis properties: {}", e.getMessage());
            throw e;
        }
    }


    /**
     * Determines the Redis deployment mode based on configuration properties.
     *
     * @param properties Redis properties
     * @return deployment mode enum value
     */
    private RedisDeploymentMode determineDeploymentMode(RedisProperties properties) {
        if (properties.getCluster() != null && !properties.getCluster()
                .getNodes()
                .isEmpty()) {
            return RedisDeploymentMode.CLUSTER;
        } else if (properties.getSentinel() != null && properties.getSentinel()
                .getMaster() != null) {
            return RedisDeploymentMode.SENTINEL;
        } else {
            return RedisDeploymentMode.STANDALONE;
        }
    }

    /**
     * Creates a factory that returns the singleton Redis cache service instance.
     *
     * <p>This factory always returns the same instance, ensuring that all namespaces
     * share the same underlying cache. Namespace isolation is handled by NamespacedCacheService
     * through key prefixing.
     *
     * @param cacheService the singleton Redis cache service bean
     * @return a factory that returns the singleton instance
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheServiceFactory<String, Object> baseCacheFactory(
            CacheService<String, Object> cacheService) {
        log.info("Creating Redis baseCacheFactory that returns singleton instance");
        return () -> cacheService;
    }
}
