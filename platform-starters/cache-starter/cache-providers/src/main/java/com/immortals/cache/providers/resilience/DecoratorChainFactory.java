package com.immortals.cache.providers.resilience;

import com.immortals.cache.core.CacheService;
import io.micrometer.core.instrument.MeterRegistry;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Factory for building decorator chains for cache services.
 * Applies decorators in the correct order:
 * 1. Metrics decorator (outermost - records all operations)
 * 2. Circuit breaker decorator (prevents cascading failures)
 * 3. Stampede protection decorator (prevents thundering herd)
 * 4. Base cache service (innermost)
 */
public class DecoratorChainFactory {
    
    private static final Logger log = LoggerFactory.getLogger(DecoratorChainFactory.class);
    
    private final MeterRegistry meterRegistry;
    private final RedissonClient redissonClient;
    private final Duration stampedeTimeout;
    private final Duration computationTimeout;
    private final int circuitBreakerFailureThreshold;
    private final Duration circuitBreakerWaitDuration;
    
    /**
     * Creates a decorator chain factory.
     * 
     * @param meterRegistry meter registry for metrics
     * @param redissonClient optional Redisson client for distributed locks
     * @param stampedeTimeout timeout for stampede protection lock acquisition
     * @param computationTimeout timeout for value computation
     * @param circuitBreakerFailureThreshold failure rate threshold in percentage
     * @param circuitBreakerWaitDuration duration to wait before attempting to close circuit
     */
    public DecoratorChainFactory(MeterRegistry meterRegistry,
                                RedissonClient redissonClient,
                                Duration stampedeTimeout,
                                Duration computationTimeout,
                                int circuitBreakerFailureThreshold,
                                Duration circuitBreakerWaitDuration) {
        this.meterRegistry = meterRegistry;
        this.redissonClient = redissonClient;
        this.stampedeTimeout = stampedeTimeout;
        this.computationTimeout = computationTimeout;
        this.circuitBreakerFailureThreshold = circuitBreakerFailureThreshold;
        this.circuitBreakerWaitDuration = circuitBreakerWaitDuration;
    }
    
    /**
     * Builds a complete decorator chain for a cache service.
     * 
     * @param baseCache the base cache service
     * @param namespace cache namespace
     * @param enableMetrics whether to enable metrics decorator
     * @param enableStampedeProtection whether to enable stampede protection
     * @param enableCircuitBreaker whether to enable circuit breaker
     * @param fallbackCache optional fallback cache for circuit breaker
     * @return decorated cache service
     */
    public <K, V> CacheService<K, V> buildDecoratorChain(
            CacheService<K, V> baseCache,
            String namespace,
            boolean enableMetrics,
            boolean enableStampedeProtection,
            boolean enableCircuitBreaker,
            CacheService<K, V> fallbackCache) {
        
        CacheService<K, V> decorated = baseCache;
        
        // Apply stampede protection first (innermost, after base cache)
        if (enableStampedeProtection && redissonClient != null) {
            decorated = new StampedeProtectionDecorator<>(
                    decorated,
                    redissonClient,
                    namespace,
                    stampedeTimeout,
                    computationTimeout,
                    meterRegistry
            );
            log.debug("Applied stampede protection decorator for namespace: {}", namespace);
        }
        
        // Apply circuit breaker
        if (enableCircuitBreaker) {
            decorated = new CircuitBreakerDecorator<>(
                    decorated,
                    fallbackCache,
                    namespace,
                    circuitBreakerFailureThreshold,
                    circuitBreakerWaitDuration,
                    meterRegistry
            );
            log.debug("Applied circuit breaker decorator for namespace: {}", namespace);
        }
        
        // Apply metrics last (outermost - records all operations)
        if (enableMetrics && meterRegistry != null) {
            decorated = new MetricsDecorator<>(
                    decorated,
                    meterRegistry,
                    namespace
            );
            log.debug("Applied metrics decorator for namespace: {}", namespace);
        }
        
        return decorated;
    }
    
    /**
     * Builds a decorator chain with all features enabled.
     * 
     * @param baseCache the base cache service
     * @param namespace cache namespace
     * @param fallbackCache optional fallback cache for circuit breaker
     * @return fully decorated cache service
     */
    public <K, V> CacheService<K, V> buildFullDecoratorChain(
            CacheService<K, V> baseCache,
            String namespace,
            CacheService<K, V> fallbackCache) {
        
        return buildDecoratorChain(
                baseCache,
                namespace,
                true,  // enableMetrics
                true,  // enableStampedeProtection
                true,  // enableCircuitBreaker
                fallbackCache
        );
    }
    
    /**
     * Builds a decorator chain with only metrics enabled.
     * 
     * @param baseCache the base cache service
     * @param namespace cache namespace
     * @return cache service with metrics decorator
     */
    public <K, V> CacheService<K, V> buildMetricsOnlyChain(
            CacheService<K, V> baseCache,
            String namespace) {
        
        return buildDecoratorChain(
                baseCache,
                namespace,
                true,   // enableMetrics
                false,  // enableStampedeProtection
                false,  // enableCircuitBreaker
                null
        );
    }
}
