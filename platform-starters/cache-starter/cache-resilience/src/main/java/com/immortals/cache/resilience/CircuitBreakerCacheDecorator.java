package com.immortals.cache.resilience;

import com.immortals.cache.core.CacheService;
import com.immortals.cache.core.CacheStatistics;
import com.immortals.cache.core.exception.CacheException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Circuit breaker decorator for cache operations.
 * 
 * <p>Implements the circuit breaker pattern to prevent cascading failures when the cache
 * (typically Redis) is experiencing issues. When too many failures occur, the circuit breaker
 * opens and subsequent requests are served from the fallback cache (typically L1/Caffeine)
 * without attempting to access the failing cache.
 * 
 * <p>Circuit breaker states:
 * <ul>
 *   <li>CLOSED: Normal operation, all requests go through</li>
 *   <li>OPEN: Too many failures, requests fail fast and use fallback</li>
 *   <li>HALF_OPEN: Testing if the cache has recovered</li>
 * </ul>
 * 
 * <p>Requirements: 5.2, 5.3
 * 
 * @param <K> the type of cache keys
 * @param <V> the type of cache values
 * @since 2.0.0
 */
@Slf4j
public class CircuitBreakerCacheDecorator<K, V> implements CacheService<K, V> {
    
    private final CacheService<K, V> delegate;
    private final CacheService<K, V> fallbackCache;
    private final CircuitBreaker circuitBreaker;
    private final String namespace;
    
    private final Counter circuitBreakerOpenCount;
    private final Counter fallbackCount;
    private final AtomicLong circuitBreakerState;
    
    /**
     * Creates a circuit breaker decorator with default configuration.
     * 
     * @param delegate the cache to protect with circuit breaker
     * @param fallbackCache the fallback cache to use when circuit is open (typically L1)
     * @param namespace the cache namespace
     * @param meterRegistry meter registry for metrics
     */
    public CircuitBreakerCacheDecorator(
            CacheService<K, V> delegate,
            CacheService<K, V> fallbackCache,
            String namespace,
            MeterRegistry meterRegistry) {
        
        this(delegate, fallbackCache, namespace, meterRegistry, createDefaultConfig());
    }
    
    /**
     * Creates a circuit breaker decorator with custom configuration.
     * 
     * @param delegate the cache to protect with circuit breaker
     * @param fallbackCache the fallback cache to use when circuit is open (typically L1)
     * @param namespace the cache namespace
     * @param meterRegistry meter registry for metrics
     * @param config circuit breaker configuration
     */
    public CircuitBreakerCacheDecorator(
            CacheService<K, V> delegate,
            CacheService<K, V> fallbackCache,
            String namespace,
            MeterRegistry meterRegistry,
            CircuitBreakerConfig config) {
        
        this.delegate = delegate;
        this.fallbackCache = fallbackCache;
        this.namespace = namespace;
        
        // Create circuit breaker with configuration
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        this.circuitBreaker = registry.circuitBreaker("cache-" + namespace);
        
        // Initialize metrics
        this.circuitBreakerState = new AtomicLong(0); // 0=CLOSED, 1=OPEN, 2=HALF_OPEN
        
        this.circuitBreakerOpenCount = Counter.builder("cache.circuit_breaker.open")
                .tag("namespace", namespace)
                .description("Number of times circuit breaker opened")
                .register(meterRegistry);
        
        this.fallbackCount = Counter.builder("cache.circuit_breaker.fallback")
                .tag("namespace", namespace)
                .description("Number of times fallback cache was used")
                .register(meterRegistry);
        
        Gauge.builder("cache.circuit_breaker.state", circuitBreakerState, AtomicLong::get)
                .tag("namespace", namespace)
                .description("Circuit breaker state (0=CLOSED, 1=OPEN, 2=HALF_OPEN)")
                .register(meterRegistry);
        
        // Register event listeners
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> {
                    log.info("Circuit breaker state transition for namespace {}: {} -> {}",
                            namespace, event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState());
                    
                    switch (event.getStateTransition().getToState()) {
                        case CLOSED:
                            circuitBreakerState.set(0);
                            break;
                        case OPEN:
                            circuitBreakerState.set(1);
                            circuitBreakerOpenCount.increment();
                            break;
                        case HALF_OPEN:
                            circuitBreakerState.set(2);
                            break;
                    }
                })
                .onError(event -> {
                    log.warn("Circuit breaker recorded error for namespace {}: {}",
                            namespace, event.getThrowable().getMessage());
                })
                .onSuccess(event -> {
                    log.debug("Circuit breaker recorded success for namespace {}", namespace);
                });
        
        log.info("Circuit breaker decorator initialized for namespace: {} with config: " +
                "failureRateThreshold={}%, waitDurationInOpenState={}s, " +
                "permittedNumberOfCallsInHalfOpenState={}, slidingWindowSize={}",
                namespace,
                config.getFailureRateThreshold(),
                config.getWaitDurationInOpenState().getSeconds(),
                config.getPermittedNumberOfCallsInHalfOpenState(),
                config.getSlidingWindowSize());
    }
    
    @Override
    public void put(K key, V value) {
        executeWithCircuitBreaker(
                () -> {
                    delegate.put(key, value);
                    return null;
                },
                () -> {
                    if (fallbackCache != null) {
                        fallbackCache.put(key, value);
                    }
                    return null;
                },
                "put",
                key
        );
    }
    
    @Override
    public void put(K key, V value, Duration ttl) {
        executeWithCircuitBreaker(
                () -> {
                    delegate.put(key, value, ttl);
                    return null;
                },
                () -> {
                    if (fallbackCache != null) {
                        fallbackCache.put(key, value, ttl);
                    }
                    return null;
                },
                "put-with-ttl",
                key
        );
    }
    
    @Override
    public Optional<V> get(K key) {
        return executeWithCircuitBreaker(
                () -> delegate.get(key),
                () -> fallbackCache != null ? fallbackCache.get(key) : Optional.empty(),
                "get",
                key
        );
    }
    
    @Override
    public void remove(K key) {
        executeWithCircuitBreaker(
                () -> {
                    delegate.remove(key);
                    return null;
                },
                () -> {
                    if (fallbackCache != null) {
                        fallbackCache.remove(key);
                    }
                    return null;
                },
                "remove",
                key
        );
    }
    
    @Override
    public void clear() {
        executeWithCircuitBreaker(
                () -> {
                    delegate.clear();
                    return null;
                },
                () -> {
                    if (fallbackCache != null) {
                        fallbackCache.clear();
                    }
                    return null;
                },
                "clear",
                null
        );
    }
    
    @Override
    public boolean containsKey(K key) {
        return executeWithCircuitBreaker(
                () -> delegate.containsKey(key),
                () -> fallbackCache != null && fallbackCache.containsKey(key),
                "containsKey",
                key
        );
    }
    
    @Override
    public void putAll(Map<K, V> entries) {
        executeWithCircuitBreaker(
                () -> {
                    delegate.putAll(entries);
                    return null;
                },
                () -> {
                    if (fallbackCache != null) {
                        fallbackCache.putAll(entries);
                    }
                    return null;
                },
                "putAll",
                null
        );
    }
    
    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        return executeWithCircuitBreaker(
                () -> delegate.getAll(keys),
                () -> fallbackCache != null ? fallbackCache.getAll(keys) : Map.of(),
                "getAll",
                null
        );
    }
    
    @Override
    public boolean putIfAbsent(K key, V value) {
        return executeWithCircuitBreaker(
                () -> delegate.putIfAbsent(key, value),
                () -> fallbackCache != null && fallbackCache.putIfAbsent(key, value),
                "putIfAbsent",
                key
        );
    }
    
    @Override
    public boolean putIfAbsent(K key, V value, Duration ttl) {
        return executeWithCircuitBreaker(
                () -> delegate.putIfAbsent(key, value, ttl),
                () -> fallbackCache != null && fallbackCache.putIfAbsent(key, value, ttl),
                "putIfAbsent-with-ttl",
                key
        );
    }
    
    @Override
    public Long increment(K key, long delta) {
        return executeWithCircuitBreaker(
                () -> delegate.increment(key, delta),
                () -> fallbackCache != null ? fallbackCache.increment(key, delta) : null,
                "increment",
                key
        );
    }
    
    @Override
    public Long decrement(K key, long delta) {
        return executeWithCircuitBreaker(
                () -> delegate.decrement(key, delta),
                () -> fallbackCache != null ? fallbackCache.decrement(key, delta) : null,
                "decrement",
                key
        );
    }
    
    @Override
    public CacheStatistics getStatistics() {
        // Statistics should always be available, even if circuit is open
        try {
            return delegate.getStatistics();
        } catch (Exception e) {
            log.warn("Failed to get statistics from delegate cache for namespace: {}, " +
                    "returning fallback statistics", namespace, e);
            return fallbackCache != null ? fallbackCache.getStatistics() : CacheStatistics.empty();
        }
    }
    
    /**
     * Executes an operation with circuit breaker protection.
     * 
     * @param operation the operation to execute
     * @param fallback the fallback operation if circuit is open
     * @param operationName the operation name for logging
     * @param key the cache key (for logging)
     * @return the result of the operation or fallback
     */
    private <T> T executeWithCircuitBreaker(
            CircuitBreakerOperation<T> operation,
            CircuitBreakerOperation<T> fallback,
            String operationName,
            K key) {
        
        try {
            return circuitBreaker.executeSupplier(() -> {
                try {
                    return operation.execute();
                } catch (CacheException e) {
                    // Let circuit breaker track cache exceptions
                    throw e;
                } catch (Exception e) {
                    // Wrap other exceptions
                    throw new CacheException(
                            "Cache operation failed: " + operationName,
                            "CACHE_OPERATION_ERROR",
                            key != null ? key.toString() : null,
                            null,
                            e
                    );
                }
            });
        } catch (Exception e) {
            // Circuit breaker is open or operation failed
            log.debug("Circuit breaker triggered for {} operation on namespace: {}, key: {}. " +
                    "Using fallback cache. Error: {}",
                    operationName, namespace, key, e.getMessage());
            
            fallbackCount.increment();
            
            try {
                return fallback.execute();
            } catch (Exception fallbackError) {
                log.error("Fallback cache also failed for {} operation on namespace: {}, key: {}. " +
                        "Error: {}", operationName, namespace, key, fallbackError.getMessage());
                
                // Return safe defaults
                return getSafeDefault();
            }
        }
    }
    
    /**
     * Returns a safe default value based on the expected return type.
     */
    @SuppressWarnings("unchecked")
    private <T> T getSafeDefault() {
        // This will return null for most types, which is acceptable
        // Callers should handle null/empty appropriately
        return null;
    }
    
    /**
     * Creates default circuit breaker configuration.
     */
    private static CircuitBreakerConfig createDefaultConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Open circuit if 50% of calls fail
                .waitDurationInOpenState(Duration.ofSeconds(60)) // Wait 60s before trying again
                .permittedNumberOfCallsInHalfOpenState(10) // Allow 10 test calls in half-open state
                .slidingWindowSize(100) // Track last 100 calls
                .minimumNumberOfCalls(10) // Need at least 10 calls before calculating failure rate
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
    }
    
    /**
     * Functional interface for circuit breaker operations.
     */
    @FunctionalInterface
    private interface CircuitBreakerOperation<T> {
        T execute() throws Exception;
    }
}
