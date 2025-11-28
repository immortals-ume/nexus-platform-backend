package com.immortals.cache.features.autoconfigure;

import com.immortals.cache.core.CacheService;
import com.immortals.cache.core.CacheServiceFactory;
import com.immortals.cache.providers.caffeine.CaffeineConfiguration;
import com.immortals.cache.providers.caffeine.CaffeineProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for Caffeine cache provider.
 *
 * <p>This configuration is activated when:
 * <ul>
 *   <li>Caffeine is on the classpath</li>
 *   <li>Cache type is set to "caffeine"</li>
 *   <li>No custom cache provider bean is defined</li>
 * </ul>
 *
 * <p>Caffeine provides high-performance in-memory caching suitable for:
 * <ul>
 *   <li>Single-instance applications</li>
 *   <li>L1 cache in multi-level setups</li>
 *   <li>Development and testing environments</li>
 * </ul>
 *
 * <p>This AutoConfiguration orchestrates the CaffeineConfiguration class,
 * which handles the actual bean creation and configuration logic.
 *
 * <p>Requirements: 5.2, 6.1, 6.3
 *
 * @since 2.0.0
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = "com.github.benmanes.caffeine.cache.Caffeine")
@ConditionalOnProperty(
        name = "immortals.cache.type",
        havingValue = "caffeine"
)
@Import(CaffeineConfiguration.class)
public class CaffeineAutoConfiguration {

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

        log.info("Caffeine properties configured: maximumSize={}, ttl={}",
                properties.getMaximumSize(), properties.getTtl());

        return properties;
    }

    /**
     * Creates a factory that returns the singleton Caffeine cache service instance.
     *
     * <p>This factory always returns the same instance, ensuring that all namespaces
     * share the same underlying cache. Namespace isolation is handled by NamespacedCacheService
     * through key prefixing.
     *
     * @param cacheService the singleton Caffeine cache service bean
     * @return a factory that returns the singleton instance
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheServiceFactory<String, Object> baseCacheFactory(
            CacheService<String, Object> cacheService) {
        log.info("Creating Caffeine baseCacheFactory that returns singleton instance");
        return () -> cacheService;
    }
}
