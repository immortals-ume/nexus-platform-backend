package com.immortals.cache.resilience;

import com.immortals.cache.core.CacheService;
import com.immortals.cache.core.CacheStatistics;
import com.immortals.cache.core.exception.CacheException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * Timeout decorator for cache operations.
 * 
 * <p>Wraps cache operations with timeout handling to prevent operations from blocking
 * indefinitely. If an operation exceeds the configured timeout, it is cancelled and
 * an empty result is returned.
 * 
 * <p>Timeout handling strategy:
 * <ul>
 *   <li>Log warning when timeout occurs</li>
 *   <li>Record timeout metrics</li>
 *   <li>Return empty Optional/safe default</li>
 *   <li>Cancel the operation if possible</li>
 * </ul>
 * 
 * <p>Requirements: 5.5
 * 
 * @param <K> the type of cache keys
 * @param <V> the type of cache values
 * @since 2.0.0
 */
@Slf4j
public class TimeoutCacheDecorator<K, V> implements CacheService<K, V> {
    
    private final CacheService<K, V> delegate;
    private final Duration timeout;
    private final String namespace;
    private final ExecutorService executorService;
    
    private final Counter timeoutCount;
    private final Timer operationTimer;
    
    /**
     * Creates a timeout decorator with specified timeout.
     * 
     * @param delegate the cache to wrap with timeout handling
     * @param timeout the timeout duration for cache operations
     * @param namespace the cache namespace
     * @param meterRegistry meter registry for metrics
     */
    public TimeoutCacheDecorator(
            CacheService<K, V> delegate,
            Duration timeout,
            String namespace,
            MeterRegistry meterRegistry) {
        
        this.delegate = delegate;
        this.timeout = timeout;
        this.namespace = namespace;
        
        // Create executor service for timeout handling
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setName("cache-timeout-" + namespace + "-" + thread.getId());
            thread.setDaemon(true);
            return thread;
        });
        
        // Initialize metrics
        this.timeoutCount = Counter.builder("cache.timeouts")
                .tag("namespace", namespace)
                .description("Number of cache operation timeouts")
                .register(meterRegistry);
        
        this.operationTimer = Timer.builder("cache.operation.duration")
                .tag("namespace", namespace)
                .description("Duration of cache operations")
                .register(meterRegistry);
        
        log.info("Timeout decorator initialized for namespace: {} with timeout: {}ms",
                namespace, timeout.toMillis());
    }
    
    @Override
    public void put(K key, V value) {
        executeWithTimeout(
                () -> {
                    delegate.put(key, value);
                    return null;
                },
                "put",
                key
        );
    }
    
    @Override
    public void put(K key, V value, Duration ttl) {
        executeWithTimeout(
                () -> {
                    delegate.put(key, value, ttl);
                    return null;
                },
                "put-with-ttl",
                key
        );
    }
    
    @Override
    public Optional<V> get(K key) {
        return executeWithTimeout(
                () -> delegate.get(key),
                "get",
                key
        ).orElse(Optional.empty());
    }
    
    @Override
    public void remove(K key) {
        executeWithTimeout(
                () -> {
                    delegate.remove(key);
                    return null;
                },
                "remove",
                key
        );
    }
    
    @Override
    public void clear() {
        executeWithTimeout(
                () -> {
                    delegate.clear();
                    return null;
                },
                "clear",
                null
        );
    }
    
    @Override
    public boolean containsKey(K key) {
        return executeWithTimeout(
                () -> delegate.containsKey(key),
                "containsKey",
                key
        ).orElse(false);
    }
    
    @Override
    public void putAll(Map<K, V> entries) {
        executeWithTimeout(
                () -> {
                    delegate.putAll(entries);
                    return null;
                },
                "putAll",
                null
        );
    }
    
    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        return executeWithTimeout(
                () -> delegate.getAll(keys),
                "getAll",
                null
        ).orElse(Map.of());
    }
    
    @Override
    public boolean putIfAbsent(K key, V value) {
        return executeWithTimeout(
                () -> delegate.putIfAbsent(key, value),
                "putIfAbsent",
                key
        ).orElse(false);
    }
    
    @Override
    public boolean putIfAbsent(K key, V value, Duration ttl) {
        return executeWithTimeout(
                () -> delegate.putIfAbsent(key, value, ttl),
                "putIfAbsent-with-ttl",
                key
        ).orElse(false);
    }
    
    @Override
    public Long increment(K key, long delta) {
        return executeWithTimeout(
                () -> delegate.increment(key, delta),
                "increment",
                key
        ).orElse(null);
    }
    
    @Override
    public Long decrement(K key, long delta) {
        return executeWithTimeout(
                () -> delegate.decrement(key, delta),
                "decrement",
                key
        ).orElse(null);
    }
    
    @Override
    public CacheStatistics getStatistics() {
        // Statistics should be fast, but still apply timeout
        return executeWithTimeout(
                () -> delegate.getStatistics(),
                "getStatistics",
                null
        ).orElse(CacheStatistics.empty());
    }
    
    /**
     * Executes an operation with timeout handling.
     * 
     * @param operation the operation to execute
     * @param operationName the operation name for logging
     * @param key the cache key (for logging)
     * @return Optional containing the result, or empty if timeout occurred
     */
    private <T> Optional<T> executeWithTimeout(
            Callable<T> operation,
            String operationName,
            K key) {
        
        Future<T> future = executorService.submit(operation);
        
        Timer.Sample sample = Timer.start();
        
        try {
            T result = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            sample.stop(operationTimer);
            return Optional.ofNullable(result);
            
        } catch (TimeoutException e) {
            sample.stop(operationTimer);
            timeoutCount.increment();
            
            // Cancel the operation
            future.cancel(true);
            
            log.warn(
                    "Cache operation timeout for {} on namespace: {}, key: {}. " +
                    "Operation exceeded timeout of {}ms. " +
                    "Consider increasing timeout or investigating performance issues.",
                    operationName,
                    namespace,
                    key,
                    timeout.toMillis()
            );
            
            return Optional.empty();
            
        } catch (InterruptedException e) {
            sample.stop(operationTimer);
            Thread.currentThread().interrupt();
            
            log.warn(
                    "Cache operation interrupted for {} on namespace: {}, key: {}",
                    operationName,
                    namespace,
                    key
            );
            
            return Optional.empty();
            
        } catch (ExecutionException e) {
            sample.stop(operationTimer);
            
            Throwable cause = e.getCause();
            
            if (cause instanceof CacheException) {
                // Re-throw cache exceptions
                throw (CacheException) cause;
            } else {
                // Wrap other exceptions
                throw new CacheException(
                        "Cache operation failed: " + operationName,
                        "CACHE_OPERATION_ERROR",
                        key != null ? key.toString() : null,
                        null,
                        cause
                );
            }
        }
    }
    
    /**
     * Shuts down the executor service.
     * Should be called when the cache is no longer needed.
     */
    public void shutdown() {
        log.info("Shutting down timeout decorator executor for namespace: {}", namespace);
        executorService.shutdown();
        
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
