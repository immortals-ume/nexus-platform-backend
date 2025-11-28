package com.immortals.cache.features.autoconfigure;

import com.immortals.cache.core.CacheConfiguration;
import com.immortals.cache.core.CacheService;
import com.immortals.cache.core.DefaultUnifiedCacheManager;
import com.immortals.cache.core.UnifiedCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for the cache service.
 *
 * <p>This configuration automatically sets up the cache infrastructure based on
 * the configured cache type and available dependencies on the classpath.
 *
 * <p>Supported cache types:
 * <ul>
 *   <li>caffeine - In-memory cache using Caffeine</li>
 *   <li>redis - Distributed cache using Redis</li>
 *   <li>multi-level - Two-tier cache combining Caffeine (L1) and Redis (L2)</li>
 * </ul>
 *
 * <p>Configuration is driven by properties under the "immortals.cache" prefix.
 *
 * <p>Example configuration:
 * <pre>
 * immortals:
 *   cache:
 *     type: multi-level
 *     default-ttl: 1h
 *     caffeine:
 *       maximum-size: 10000
 *     redis:
 *       host: localhost
 *       port: 6379
 * </pre>
 *
 * <p>Requirements: 10.1, 10.2, 11.1
 *
 * @since 2.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnProperty(name = "immortals.cache.enabled", havingValue = "true", matchIfMissing = true)
@Import({
        CaffeineAutoConfiguration.class,
        RedisAutoConfiguration.class,
        MultiLevelAutoConfiguration.class
})
public class CacheAutoConfiguration {

    /**
     * Creates the UnifiedCacheManager bean.
     *
     * <p>This is the main entry point for cache operations.
     *
     * @param cacheService          the cache service bean (Caffeine, Redis, or Multi-level)
     * @param properties            cache configuration properties
     * @param decoratorChainBuilder builder for applying decorators to cache services
     * @return configured UnifiedCacheManager
     */
    @Bean
    @ConditionalOnMissingBean
    public UnifiedCacheManager unifiedCacheManager(
            CacheService<?, ?> cacheService,
            CacheProperties properties,
            DefaultUnifiedCacheManager.DecoratorChainBuilder decoratorChainBuilder) {

        log.info("Initializing UnifiedCacheManager with cache type: {}", properties.getType());

        CacheConfiguration defaultConfig = createDefaultConfiguration(properties);

        log.debug("Creating DefaultUnifiedCacheManager");
        UnifiedCacheManager manager = new DefaultUnifiedCacheManager(
                cacheService,
                defaultConfig,
                decoratorChainBuilder
        );

        log.info("UnifiedCacheManager initialized successfully");

        return manager;
    }

    /**
     * Creates default cache configuration from properties.
     *
     * @param properties cache properties
     * @return default cache configuration
     */
    private CacheConfiguration createDefaultConfiguration(CacheProperties properties) {
        CacheConfiguration config = new CacheConfiguration();
        config.setTtl(properties.getDefaultTtl());
        config.setCompressionEnabled(properties.getFeatures()
                .getCompression()
                .isEnabled());
        config.setEncryptionEnabled(properties.getFeatures()
                .getEncryption()
                .isEnabled());
        config.setStampedeProtectionEnabled(properties.getResilience()
                .getStampedeProtection()
                .isEnabled());
        config.setCircuitBreakerEnabled(properties.getResilience()
                .getCircuitBreaker()
                .isEnabled());

        log.debug("Created default configuration: {}", config);

        return config;
    }

}
