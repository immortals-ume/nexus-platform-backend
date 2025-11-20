package com.immortals.cache.features.circuitbreaker;

import com.immortals.cache.core.CacheService;
import com.immortals.cache.core.CacheStatistics;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

/**
 * Decorator that adds circuit breaker pattern to a cache service.
 * Prevents cascading failures by failing fast when the cache service is degraded.
 */
@Slf4j
public class CircuitBreakerDecorator<K, V> implements CacheService<K, V> {
    
    private final CacheService<K, V> delegate;
    private final CircuitBreaker circuitBreaker;
    private static final String CIRCUIT_BREAKER_NAME = "cache-service";
    
    /**
     * Creates a circuit breaker decorator with default configuration.
     * 
     * @param delegate the underlying cache service
     */
    public CircuitBreakerDecorator(CacheService<K, V> delegate) {
        this.delegate = delegate;
        this.circuitBreaker = createCircuitBreaker();
    }
    
    /**
     * Creates a circuit breaker decorator with custom configuration.
     * 
     * @param delegate the underlying cache service
     * @param config the circuit breaker configuration
     */
    public CircuitBreakerDecorator(CacheService<K, V> delegate, CircuitBreakerConfig config) {
        this.delegate = delegate;
        this.circuitBreaker = CircuitBreakerRegistry.ofDefaults().circuitBreaker(CIRCUIT_BREAKER_NAME, config);
    }
    
    private static CircuitBreaker createCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50.0f)
            .slowCallRateThreshold(50.0f)
            .slowCallDurationThreshold(Duration.ofSeconds(2))
            .waitDurationInOpenState(Duration.ofSeconds(10))
            .permittedNumberOfCallsInHalfOpenState(3)
            .minimumNumberOfCalls(5)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .build();
        
        return CircuitBreakerRegistry.ofDefaults().circuitBreaker(CIRCUIT_BREAKER_NAME, config);
    }
    
    @Override
    public void put(K key, V value) {
        executeWithCircuitBreaker(() -> {
            delegate.put(key, value);
            return null;
        });
    }
    
    @Override
    public void put(K key, V value, Duration ttl) {
        executeWithCircuitBreaker(() -> {
            delegate.put(key, value, ttl);
            return null;
        });
    }
    
    @Override
    public Optional<V> get(K key) {
        return executeWithCircuitBreaker(() -> delegate.get(key));
    }
    
    @Override
    public void remove(K key) {
        executeWithCircuitBreaker(() -> {
            delegate.remove(key);
            return null;
        });
    }
    
    @Override
    public void clear() {
        executeWithCircuitBreaker(() -> {
            delegate.clear();
            return null;
        });
    }
    
    @Override
    public boolean containsKey(K key) {
        return executeWithCircuitBreaker(() -> delegate.containsKey(key));
    }
    
    @Override
    public void putAll(Map<K, V> entries) {
        executeWithCircuitBreaker(() -> {
            delegate.putAll(entries);
            return null;
        });
    }
    
    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        return executeWithCircuitBreaker(() -> delegate.getAll(keys));
    }
    
    @Override
    public boolean putIfAbsent(K key, V value) {
        return executeWithCircuitBreaker(() -> delegate.putIfAbsent(key, value));
    }
    
    @Override
    public boolean putIfAbsent(K key, V value, Duration ttl) {
        return executeWithCircuitBreaker(() -> delegate.putIfAbsent(key, value, ttl));
    }
    
    @Override
    public Long increment(K key, long delta) {
        return executeWithCircuitBreaker(() -> delegate.increment(key, delta));
    }
    
    @Override
    public Long decrement(K key, long delta) {
        return executeWithCircuitBreaker(() -> delegate.decrement(key, delta));
    }
    
    @Override
    public CacheStatistics getStatistics() {
        return executeWithCircuitBreaker(() -> delegate.getStatistics());
    }
    
    /**
     * Executes a supplier with circuit breaker protection.
     */
    private <T> T executeWithCircuitBreaker(Supplier<T> supplier) {
        try {
            return circuitBreaker.executeSupplier(supplier);
        } catch (Exception e) {
            log.error("Circuit breaker triggered for cache operation", e);
            throw new CircuitBreakerException("Cache service is unavailable due to circuit breaker", e);
        }
    }
}
