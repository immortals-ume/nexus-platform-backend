package com.immortals.cache.observability;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Structured logging for cache operations with correlation IDs.
 * Logs cache operations at appropriate levels with consistent formatting.
 * 
 * @since 2.0.0
 */
@Component
@Slf4j
public class CacheStructuredLogger {
    
    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String CACHE_NAME_KEY = "cacheName";
    private static final String NAMESPACE_KEY = "namespace";
    private static final String OPERATION_KEY = "operation";
    private static final String KEY_KEY = "key";
    private static final String DURATION_MS_KEY = "durationMs";
    
    /**
     * Logs a cache hit operation.
     * 
     * @param cacheName the cache name
     * @param namespace the namespace
     * @param key the cache key
     * @param duration the operation duration
     */
    public void logCacheHit(String cacheName, String namespace, Object key, Duration duration) {
        String correlationId = getOrCreateCorrelationId();
        
        try {
            MDC.put(CORRELATION_ID_KEY, correlationId);
            MDC.put(CACHE_NAME_KEY, cacheName);
            MDC.put(NAMESPACE_KEY, namespace);
            MDC.put(OPERATION_KEY, "get");
            MDC.put(KEY_KEY, String.valueOf(key));
            MDC.put(DURATION_MS_KEY, String.valueOf(duration.toMillis()));
            
            log.debug("Cache hit - cache: {}, namespace: {}, key: {}, duration: {}ms",
                cacheName, namespace, key, duration.toMillis());
        } finally {
            clearMDC();
        }
    }
    
    /**
     * Logs a cache miss operation.
     * 
     * @param cacheName the cache name
     * @param namespace the namespace
     * @param key the cache key
     * @param duration the operation duration
     */
    public void logCacheMiss(String cacheName, String namespace, Object key, Duration duration) {
        String correlationId = getOrCreateCorrelationId();
        
        try {
            MDC.put(CORRELATION_ID_KEY, correlationId);
            MDC.put(CACHE_NAME_KEY, cacheName);
            MDC.put(NAMESPACE_KEY, namespace);
            MDC.put(OPERATION_KEY, "get");
            MDC.put(KEY_KEY, String.valueOf(key));
            MDC.put(DURATION_MS_KEY, String.valueOf(duration.toMillis()));
            
            log.debug("Cache miss - cache: {}, namespace: {}, key: {}, duration: {}ms",
                cacheName, namespace, key, duration.toMillis());
        } finally {
            clearMDC();
        }
    }
    
    /**
     * Logs a cache put operation.
     * 
     * @param cacheName the cache name
     * @param namespace the namespace
     * @param key the cache key
     * @param duration the operation duration
     */
    public void logCachePut(String cacheName, String namespace, Object key, Duration duration) {
        String correlationId = getOrCreateCorrelationId();
        
        try {
            MDC.put(CORRELATION_ID_KEY, correlationId);
            MDC.put(CACHE_NAME_KEY, cacheName);
            MDC.put(NAMESPACE_KEY, namespace);
            MDC.put(OPERATION_KEY, "put");
            MDC.put(KEY_KEY, String.valueOf(key));
            MDC.put(DURATION_MS_KEY, String.valueOf(duration.toMillis()));
            
            log.debug("Cache put - cache: {}, namespace: {}, key: {}, duration: {}ms",
                cacheName, namespace, key, duration.toMillis());
        } finally {
            clearMDC();
        }
    }
    
    /**
     * Logs a cache remove operation.
     * 
     * @param cacheName the cache name
     * @param namespace the namespace
     * @param key the cache key
     * @param duration the operation duration
     */
    public void logCacheRemove(String cacheName, String namespace, Object key, Duration duration) {
        String correlationId = getOrCreateCorrelationId();
        
        try {
            MDC.put(CORRELATION_ID_KEY, correlationId);
            MDC.put(CACHE_NAME_KEY, cacheName);
            MDC.put(NAMESPACE_KEY, namespace);
            MDC.put(OPERATION_KEY, "remove");
            MDC.put(KEY_KEY, String.valueOf(key));
            MDC.put(DURATION_MS_KEY, String.valueOf(duration.toMillis()));
            
            log.debug("Cache remove - cache: {}, namespace: {}, key: {}, duration: {}ms",
                cacheName, namespace, key, duration.toMillis());
        } finally {
            clearMDC();
        }
    }
    
    /**
     * Logs a cache clear operation.
     * 
     * @param cacheName the cache name
     * @param namespace the namespace
     */
    public void logCacheClear(String cacheName, String namespace) {
        String correlationId = getOrCreateCorrelationId();
        
        try {
            MDC.put(CORRELATION_ID_KEY, correlationId);
            MDC.put(CACHE_NAME_KEY, cacheName);
            MDC.put(NAMESPACE_KEY, namespace);
            MDC.put(OPERATION_KEY, "clear");
            
            log.info("Cache cleared - cache: {}, namespace: {}", cacheName, namespace);
        } finally {
            clearMDC();
        }
    }
    
    /**
     * Logs a batch cache operation.
     * 
     * @param cacheName the cache name
     * @param namespace the namespace
     * @param operation the operation name (getAll, putAll)
     * @param duration the operation duration
     */
    public void logCacheBatchOperation(String cacheName, String namespace, String operation, Duration duration) {
        String correlationId = getOrCreateCorrelationId();
        
        try {
            MDC.put(CORRELATION_ID_KEY, correlationId);
            MDC.put(CACHE_NAME_KEY, cacheName);
            MDC.put(NAMESPACE_KEY, namespace);
            MDC.put(OPERATION_KEY, operation);
            MDC.put(DURATION_MS_KEY, String.valueOf(duration.toMillis()));
            
            log.debug("Cache batch operation - cache: {}, namespace: {}, operation: {}, duration: {}ms",
                cacheName, namespace, operation, duration.toMillis());
        } finally {
            clearMDC();
        }
    }
    
    /**
     * Logs a cache operation error.
     * 
     * @param cacheName the cache name
     * @param namespace the namespace
     * @param operation the operation name
     * @param key the cache key (may be null)
     * @param error the error that occurred
     */
    public void logCacheError(String cacheName, String namespace, String operation, Object key, Throwable error) {
        String correlationId = getOrCreateCorrelationId();
        
        try {
            MDC.put(CORRELATION_ID_KEY, correlationId);
            MDC.put(CACHE_NAME_KEY, cacheName);
            MDC.put(NAMESPACE_KEY, namespace);
            MDC.put(OPERATION_KEY, operation);
            if (key != null) {
                MDC.put(KEY_KEY, String.valueOf(key));
            }
            
            log.warn("Cache operation error - cache: {}, namespace: {}, operation: {}, key: {}, error: {}",
                cacheName, namespace, operation, key, error.getMessage(), error);
        } finally {
            clearMDC();
        }
    }
    
    /**
     * Logs a cache eviction event.
     * 
     * @param cacheName the cache name
     * @param namespace the namespace
     * @param key the evicted key
     * @param reason the eviction reason
     */
    public void logCacheEviction(String cacheName, String namespace, Object key, String reason) {
        String correlationId = getOrCreateCorrelationId();
        
        try {
            MDC.put(CORRELATION_ID_KEY, correlationId);
            MDC.put(CACHE_NAME_KEY, cacheName);
            MDC.put(NAMESPACE_KEY, namespace);
            MDC.put(OPERATION_KEY, "eviction");
            MDC.put(KEY_KEY, String.valueOf(key));
            
            log.debug("Cache eviction - cache: {}, namespace: {}, key: {}, reason: {}",
                cacheName, namespace, key, reason);
        } finally {
            clearMDC();
        }
    }
    
    /**
     * Gets or creates a correlation ID for the current thread.
     * 
     * @return correlation ID
     */
    private String getOrCreateCorrelationId() {
        String correlationId = MDC.get(CORRELATION_ID_KEY);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }
    
    /**
     * Clears MDC context.
     */
    private void clearMDC() {
        MDC.remove(CACHE_NAME_KEY);
        MDC.remove(NAMESPACE_KEY);
        MDC.remove(OPERATION_KEY);
        MDC.remove(KEY_KEY);
        MDC.remove(DURATION_MS_KEY);
        // Keep correlation ID for the thread
    }
}
