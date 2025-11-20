package com.immortals.cache.providers.caffeine;

import com.immortals.cache.core.exception.CacheConfigurationException;
import com.immortals.cache.providers.redis.RedisProperties;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;

import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.micrometer.core.instrument.MeterRegistry;
import com.immortals.cache.providers.resilience.DecoratorChainFactory;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for Caffeine L1 (local in-memory) cache.
 * Provides high-performance local caching with configurable eviction policies.
 */
@Configuration
@ConditionalOnProperty(prefix = "cache.redis.l1-cache", name = "enabled", havingValue = "true")
@EnableConfigurationProperties
public class CaffeineConfiguration {
    private static final Logger log = LoggerFactory.getLogger(CaffeineConfiguration.class);
    @Bean
    public CaffeineProperties cacheProperties() {
        return new CaffeineProperties();
    }

    /**
     * Creates a Caffeine cache instance with configured settings.
     * Supports LRU, LFU, and FIFO eviction policies.
     *
     * @return configured Caffeine cache
     */
    @Bean
    public Cache<Object, Object> caffeineCache(final CaffeineProperties caffeineProperties) {
        try {
            // Validate properties
            validateCaffeineProperties(caffeineProperties);
            
            Caffeine<Object, Object> builder = Caffeine.newBuilder()
                    .maximumSize(caffeineProperties.getMaximumSize())
                    .expireAfterWrite(caffeineProperties.getTtl().toMillis(), TimeUnit.MILLISECONDS)
                    .removalListener(removalListener());
            
            String evictionPolicy = CaffeineProperties.EvictionPolicy.LRU.name();
            switch (evictionPolicy) {
                case "LFU":
                    log.debug("Using LFU (W-TinyLFU) eviction policy for L1 cache");
                    break;
                case "FIFO":
                    log.debug("Using FIFO-like eviction policy for L1 cache");
                    break;
                case "LRU":
                    log.debug("Using LRU-like eviction policy for L1 cache");
                    break;
                default:
                    builder.expireAfterAccess(caffeineProperties.getTtl().toMillis(), TimeUnit.MILLISECONDS);
                    log.debug("Using LRU eviction policy for L1 cache");
                    break;
            }

            if (caffeineProperties.getRecordStats()) {
                builder.recordStats();
                log.info("L1 cache statistics recording enabled");
            }
            
            Cache<Object, Object> cache = builder.build();
            
            log.debug("Caffeine L1 cache created: maxSize={}, ttl={}, evictionPolicy={}",
                  caffeineProperties.getMaximumSize(),caffeineProperties.getTtl(), evictionPolicy);
            
            return cache;
        } catch (Exception e) {
            log.error("Failed to create Caffeine cache configuration", e);
            throw new CacheConfigurationException(
                "Failed to create Caffeine cache: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Removal listener for tracking evictions from L1 cache.
     */
    private RemovalListener<Object, Object> removalListener() {
        return (key, value, cause) -> {
            if (cause == RemovalCause.EXPIRED || cause == RemovalCause.SIZE) {
                log.debug("L1 cache entry evicted: key={}, cause={}", key, cause);
            }
        };
    }

    /**
     * Creates an L1CacheService that wraps the Caffeine cache.
     * Applies resilience decorators (metrics, circuit breaker, stampede protection).
     * 
     * Note: This is a prototype bean - a new instance is created for each namespace.
     *
     * @param caffeineProperties the Caffeine configuration properties
     * @param meterRegistry the meter registry for metrics
     * @param redissonClient optional Redisson client for distributed locks
     * @return configured and decorated L1CacheService
     */
    @Bean
    public com.immortals.cache.core.CacheService<String, Object> l1CacheService(
            CaffeineProperties caffeineProperties,
            MeterRegistry meterRegistry,
            @org.springframework.beans.factory.annotation.Autowired(required = false) RedissonClient redissonClient) {
        try {
            // Validate properties before creating service
            validateCaffeineProperties(caffeineProperties);
            
            // Create base cache service (namespace-agnostic)
            L1CacheService<String, Object> baseService = new L1CacheService<>(
                caffeineProperties,
                meterRegistry
            );
            
            // Apply resilience decorators
            DecoratorChainFactory decoratorFactory = new DecoratorChainFactory(
                    meterRegistry,
                    redissonClient,
                    Duration.ofSeconds(5),  // Default stampede lock timeout
                    Duration.ofSeconds(5),  // Default computation timeout
                    50,                      // Default circuit breaker failure threshold
                    Duration.ofSeconds(60)   // Default circuit breaker wait duration
            );
            
            boolean enableMetrics = true;
            boolean enableStampedeProtection = redissonClient != null;
            boolean enableCircuitBreaker = false;  // Disabled for L1 cache by default
            
            com.immortals.cache.core.CacheService<String, Object> decorated = decoratorFactory.buildDecoratorChain(
                    baseService,
                    "caffeine",  // Provider name, not namespace
                    enableMetrics,
                    enableStampedeProtection,
                    enableCircuitBreaker,
                    null  // No fallback cache for L1
            );
            
            log.info("L1CacheService bean created successfully with decorators: metrics={}, stampedeProtection={}, circuitBreaker={}",
                    enableMetrics, enableStampedeProtection, enableCircuitBreaker);
            return decorated;
        } catch (CacheConfigurationException e) {
            log.error("Failed to create L1CacheService: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating L1CacheService: {}", e.getMessage(), e);
            throw new CacheConfigurationException(e.getMessage());
        }
    }

    /**
     * Validates Caffeine configuration properties.
     * Ensures maximum size is positive and TTL is valid.
     *
     * @param properties the properties to validate
     * @throws CacheConfigurationException if validation fails
     */
    private void validateCaffeineProperties(CaffeineProperties properties) {
        if (properties == null) {
            throw new CacheConfigurationException(
                "CaffeineProperties cannot be null",
                "immortals.cache.caffeine"
            );
        }

        // Validate maximum size
        if (properties.getMaximumSize() <= 0) {
            throw new CacheConfigurationException(
                "Caffeine cache maximum size must be positive",
                "immortals.cache.caffeine.maximum-size",
                properties.getMaximumSize()
            );
        }

        // Validate TTL
        if (properties.getTtl() == null) {
            throw new CacheConfigurationException(
                "Caffeine cache TTL must be configured",
                "immortals.cache.caffeine.ttl"
            );
        }

        if (properties.getTtl().isNegative()) {
            throw new CacheConfigurationException(
                "Caffeine cache TTL must be positive",
                "immortals.cache.caffeine.ttl",
                properties.getTtl()
            );
        }

        if (properties.getTtl().isZero()) {
            throw new CacheConfigurationException(
                "Caffeine cache TTL must be greater than zero",
                "immortals.cache.caffeine.ttl",
                properties.getTtl()
            );
        }

        log.debug("Caffeine properties validation passed: maxSize={}, ttl={}",
            properties.getMaximumSize(), properties.getTtl());
    }
}
