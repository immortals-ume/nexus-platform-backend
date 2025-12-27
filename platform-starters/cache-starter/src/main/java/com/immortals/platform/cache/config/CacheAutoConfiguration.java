package com.immortals.platform.cache.config;

import com.immortals.platform.cache.core.CacheConfiguration;
import com.immortals.platform.cache.core.CacheService;
import com.immortals.platform.cache.core.CacheServiceFactory;
import com.immortals.platform.cache.core.DefaultUnifiedCacheManager;
import com.immortals.platform.cache.config.CacheProperties;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Main auto-configuration for cache starter.
 *
 * <p>This configuration provides the core cache infrastructure and is activated when:
 * <ul>
 *   <li>CacheService is on the classpath</li>
 *   <li>No custom cache configuration is provided</li>
 * </ul>
 *
 * <p>This class provides:
 * <ul>
 *   <li>Core cache properties binding</li>
 *   <li>Unified cache manager for namespace management</li>
 *   <li>Base infrastructure for all cache providers</li>
 * </ul>
 *
 * <p>Requirements: 5.1, 5.2, 5.3
 *
 * @since 2.0.0
 */
@Slf4j
@Configuration
@ConditionalOnClass(CacheService.class)
@EnableConfigurationProperties(CacheProperties.class)
public class CacheAutoConfiguration {

    /**
     * Creates the unified cache manager that handles namespace management.
     *
     * <p>The unified cache manager provides:
     * <ul>
     *   <li>Namespace isolation through key prefixing</li>
     *   <li>Decorator chain application</li>
     *   <li>Consistent API across all cache providers</li>
     * </ul>
     *
     * @param cacheServiceFactory factory for creating cache service instances
     * @param cacheProperties cache configuration properties
     * @param meterRegistry meter registry for metrics
     * @param decoratorChainBuilder builder for applying decorators
     * @return configured unified cache manager
     */
    @Bean
    @ConditionalOnMissingBean
    public DefaultUnifiedCacheManager unifiedCacheManager(
            CacheServiceFactory<String, Object> cacheServiceFactory,
            CacheProperties cacheProperties,
            MeterRegistry meterRegistry,
            ObjectProvider<DefaultUnifiedCacheManager.DecoratorChainBuilder> decoratorChainBuilder) {
        
        log.info("Creating unified cache manager with type: {}", cacheProperties.getType());
        
        DefaultUnifiedCacheManager.DecoratorChainBuilder builder = 
            decoratorChainBuilder.getIfAvailable(() -> {
                log.debug("Using default decorator chain builder");
                return new DefaultDecoratorChainBuilder(meterRegistry, null, null, 0, null, false);
            });
        
        CacheService<?, ?> sharedCacheInstance = cacheServiceFactory.createCacheService();
        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        DefaultUnifiedCacheManager manager = new DefaultUnifiedCacheManager(
            sharedCacheInstance,
            cacheConfiguration,
            builder
        );
        
        log.info("Unified cache manager created successfully");
        return manager;
    }

    /**
     * Logs cache configuration summary at startup.
     *
     * @param cacheProperties cache properties
     * @return configuration summary bean
     */
    @Bean
    public CacheConfigurationSummary cacheConfigurationSummary(CacheProperties cacheProperties) {
        log.info("=== Cache Configuration Summary ===");
        log.info("Cache Type: {}", cacheProperties.getType());
        log.info("Default TTL: {}", cacheProperties.getDefaultTtl());
        log.info("Enabled: {}", cacheProperties.getEnabled());
        log.info("Namespaces configured: {}", cacheProperties.getNamespaces().size());
        log.info("===================================");
        
        return new CacheConfigurationSummary();
    }

    /**
     * Marker class for cache configuration summary.
     */
    public static class CacheConfigurationSummary {
    }
}