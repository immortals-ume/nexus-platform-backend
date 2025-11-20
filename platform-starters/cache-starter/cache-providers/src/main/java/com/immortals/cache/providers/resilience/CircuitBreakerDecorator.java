package com.immortals.cache.providers.resilience;

import com.immortals.cache.core.CacheService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Decorator that adds circuit breaker pattern to a cache service.
 * Prevents cascading failures by stopping requests when the cache service
 * experiences too many failures.
 * 
 * @param <K> key type
 * @param <V> value type
 */
public class CircuitBreakerDecorator<K, V> extends CacheDecorator<K, V> {
    
    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerDecorator.class);
    
    private final CircuitBreaker circuitBreaker;
    private final CacheService<K, V> fallbackCache;
    private final String namespace;
    
    /**
     * Creates a circuit breaker decorator.
     * 
     * @param delegate the underlying cache service
     * @param fallbackCache optional fallback cache (e.g., L1 cache) when circuit is open
     * @param namespace cache namespace
     * @param failureRateThreshold failure rate threshold in percentage (0-100)
     * @param waitDuration duration to wait before attempting to close the circuit
     * @param meterRegistry meter registry for metrics
     */
    public CircuitBreakerDecorator(CacheService<K, V> delegate,
                                  CacheService<K, V> fallbackCache,
                                  String namespace,
                                  int failureRateThreshold,
                                  Duration waitDuration,
                                  MeterRegistry meterRegistry) {
        super(delegate);
        this.fallbackCache = fallbackCache;
        this.namespace = namespace;
        
        // Configure circuit breaker
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRateThreshold)
                .waitDurationInOpenState(waitDuration)
                .recordExceptions(Exception.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build();
        
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        this.circuitBreaker = registry.circuitBreaker(namespace);
        
        // Register metrics
        if (meterRegistry != null) {
            io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics
                    .ofCircuitBreakerRegistry(registry)
                    .bindTo(meterRegistry);
        }
        
        log.debug("Circuit breaker decorator initialized for namespace: {} (failureRateThreshold: {}%, waitDuration: {})",
                namespace, failureRateThreshold, waitDuration);
    }
    
    @Override
    public void put(K key, V value) {
        try {
            circuitBreaker.executeRunnable(() -> delegate.put(key, value));
        } catch (Exception e) {
            log.warn("Circuit breaker open or error during put for namespace: {}", namespace, e);
            throw e;
        }
    }
    
    @Override
    public void put(K key, V value, Duration ttl) {
        try {
            circuitBreaker.executeRunnable(() -> delegate.put(key, value, ttl));
        } catch (Exception e) {
            log.warn("Circuit breaker open or error during put with TTL for namespace: {}", namespace, e);
            throw e;
        }
    }
    
    @Override
    public Optional<V> get(K key) {
        try {
            return circuitBreaker.executeSupplier(() -> delegate.get(key));
        } catch (Exception e) {
            log.warn("Circuit breaker open or error during get for namespace: {}, attempting fallback", namespace, e);
            
            // Try fallback cache if available
            if (fallbackCache != null) {
                try {
                    Optional<V> fallbackResult = fallbackCache.get(key);
                    if (fallbackResult.isPresent()) {
                        log.debug("Fallback cache hit for key: {} in namespace: {}", key, namespace);
                        return fallbackResult;
                    }
                } catch (Exception fallbackError) {
                    log.error("Fallback cache also failed for key: {} in namespace: {}", key, namespace, fallbackError);
                }
            }
            
            return Optional.empty();
        }
    }
    
    @Override
    public void remove(K key) {
        try {
            circuitBreaker.executeRunnable(() -> delegate.remove(key));
        } catch (Exception e) {
            log.warn("Circuit breaker open or error during remove for namespace: {}", namespace, e);
            throw e;
        }
    }
    
    @Override
    public void clear() {
        try {
            circuitBreaker.executeRunnable(delegate::clear);
        } catch (Exception e) {
            log.warn("Circuit breaker open or error during clear for namespace: {}", namespace, e);
            throw e;
        }
    }
    
    @Override
    public boolean containsKey(K key) {
        try {
            return circuitBreaker.executeSupplier(() -> delegate.containsKey(key));
        } catch (Exception e) {
            log.warn("Circuit breaker open or error during containsKey for namespace: {}", namespace, e);
            
            // Try fallback cache if available
            if (fallbackCache != null) {
                try {
                    return fallbackCache.containsKey(key);
                } catch (Exception fallbackError) {
                    log.error("Fallback cache also failed for containsKey in namespace: {}", namespace, fallbackError);
                }
            }
            
            return false;
        }
    }
    
    @Override
    public void putAll(Map<K, V> entries) {
        try {
            circuitBreaker.executeRunnable(() -> delegate.putAll(entries));
        } catch (Exception e) {
            log.warn("Circuit breaker open or error during putAll for namespace: {}", namespace, e);
            throw e;
        }
    }
    
    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        try {
            return circuitBreaker.executeSupplier(() -> delegate.getAll(keys));
        } catch (Exception e) {
            log.warn("Circuit breaker open or error during getAll for namespace: {}", namespace, e);
            
            // Try fallback cache if available
            if (fallbackCache != null) {
                try {
                    return fallbackCache.getAll(keys);
                } catch (Exception fallbackError) {
                    log.error("Fallback cache also failed for getAll in namespace: {}", namespace, fallbackError);
                }
            }
            
            return Map.of();
        }
    }
    
    @Override
    public boolean putIfAbsent(K key, V value) {
        try {
            return circuitBreaker.executeSupplier(() -> delegate.putIfAbsent(key, value));
        } catch (Exception e) {
            log.warn("Circuit breaker open or error during putIfAbsent for namespace: {}", namespace, e);
            throw e;
        }
    }
    
    @Override
    public boolean putIfAbsent(K key, V value, Duration ttl) {
        try {
            return circuitBreaker.executeSupplier(() -> delegate.putIfAbsent(key, value, ttl));
        } catch (Exception e) {
            log.warn("Circuit breaker open or error during putIfAbsent with TTL for namespace: {}", namespace, e);
            throw e;
        }
    }
    
    /**
     * Returns the current state of the circuit breaker.
     * 
     * @return the circuit breaker state (CLOSED, OPEN, HALF_OPEN)
     */
    public CircuitBreaker.State getCircuitBreakerState() {
        return circuitBreaker.getState();
    }
    
    /**
     * Returns metrics about the circuit breaker.
     * 
     * @return circuit breaker metrics
     */
    public CircuitBreaker.Metrics getCircuitBreakerMetrics() {
        return circuitBreaker.getMetrics();
    }
}
