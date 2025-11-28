package com.immortals.cache.features.autoconfigure;

import com.immortals.cache.core.CacheService;
import com.immortals.cache.core.CacheServiceFactory;
import com.immortals.cache.providers.caffeine.CaffeineProperties;
import com.immortals.cache.providers.multilevel.MultiLevelCacheProperties;
import com.immortals.cache.providers.multilevel.MultiLevelConfiguration;
import com.immortals.cache.providers.redis.RedisProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for multi-level cache provider.
 *
 * <p>This configuration is activated when:
 * <ul>
 *   <li>Caffeine and Redis are on the classpath</li>
 *   <li>Cache type is set to "multi-level"</li>
 *   <li>No custom cache provider bean is defined</li>
 * </ul>
 *
 * <p>Multi-level cache provides:
 * <ul>
 *   <li>L1 Cache: Caffeine (local in-memory, fast)</li>
 *   <li>L2 Cache: Redis (distributed, shared across instances)</li>
 *   <li>Eviction Publisher: Redis Pub/Sub for distributed invalidation</li>
 * </ul>
 *
 * <p>This is the single AutoConfiguration class for multi-level caching.
 * It orchestrates by importing MultiLevelCacheAutoConfiguration from cache-providers,
 * which handles the actual bean creation and configuration logic.
 *
 * <p>Requirements: 5.4, 6.1
 *
 * @since 2.0.0
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = {
        "com.github.benmanes.caffeine.cache.Caffeine",
        "org.springframework.data.redis.core.RedisTemplate"
})
@ConditionalOnProperty(name = "immortals.cache.type", havingValue = "multi-level")
@EnableConfigurationProperties({
        CacheProperties.class,
        RedisProperties.class,
        CaffeineProperties.class,
        MultiLevelCacheProperties.class
})
@Import(MultiLevelConfiguration.class)
public class MultiLevelAutoConfiguration {

    /**
     * Creates Caffeine properties from cache properties.
     *
     * @param cacheProperties main cache properties
     * @return Caffeine-specific properties
     */
    @Bean
    @ConditionalOnMissingBean
    public CaffeineProperties caffeineProperties(CacheProperties cacheProperties) {
        CaffeineProperties properties = new CaffeineProperties();

        CaffeineProperties caffeineConfig = cacheProperties.getCaffeine();
        properties.setMaximumSize(caffeineConfig.getMaximumSize());
        properties.setTtl(caffeineConfig.getTtl());
        properties.setRecordStats(caffeineConfig.getRecordStats());

        log.info("L1 (Caffeine) properties configured: maximumSize={}, ttl={}",
                properties.getMaximumSize(), properties.getTtl());

        return properties;
    }

    /**
     * Creates a factory that returns the singleton multi-level cache service instance.
     *
     * <p>This factory always returns the same instance, ensuring that all namespaces
     * share the same underlying cache. Namespace isolation is handled by NamespacedCacheService
     * through key prefixing.
     *
     * @param cacheService the singleton multi-level cache service bean
     * @return a factory that returns the singleton instance
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheServiceFactory<String, Object> baseCacheFactory(
            CacheService<String, Object> cacheService) {
        log.info("Creating multi-level baseCacheFactory that returns singleton instance");
        return () -> cacheService;
    }
}
