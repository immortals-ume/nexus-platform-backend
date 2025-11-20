package com.immortals.cache.observability;

import com.immortals.cache.core.CacheService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AOP aspect that automatically instruments all CacheService implementations
 * with comprehensive observability features including metrics, tracing, and logging.
 * 
 * This aspect intercepts all cache operations and:
 * - Records metrics (hits, misses, latencies)
 * - Adds distributed tracing spans
 * - Logs operations with correlation IDs
 * 
 * @since 2.0.0
 */
@Aspect
@Component
@Slf4j
public class CacheObservabilityAspect {
    
    private final MeterRegistry meterRegistry;
    private final CacheTracingService tracingService;
    private final CacheStructuredLogger structuredLogger;
    private final Map<String, CacheMetrics> metricsCache;
    
    public CacheObservabilityAspect(
            MeterRegistry meterRegistry,
            Optional<CacheTracingService> tracingService,
            CacheStructuredLogger structuredLogger) {
        this.meterRegistry = meterRegistry;
        this.tracingService = tracingService.orElse(null);
        this.structuredLogger = structuredLogger;
        this.metricsCache = new ConcurrentHashMap<>();
    }
    
    /**
     * Pointcut for all CacheService.get() methods.
     */
    @Pointcut("execution(* com.immortals.cache.core.CacheService.get(..))")
    public void getCacheOperation() {}
    
    /**
     * Pointcut for all CacheService.put() methods.
     */
    @Pointcut("execution(* com.immortals.cache.core.CacheService.put(..))")
    public void putCacheOperation() {}
    
    /**
     * Pointcut for all CacheService.remove() methods.
     */
    @Pointcut("execution(* com.immortals.cache.core.CacheService.remove(..))")
    public void removeCacheOperation() {}
    
    /**
     * Pointcut for all CacheService.getAll() methods.
     */
    @Pointcut("execution(* com.immortals.cache.core.CacheService.getAll(..))")
    public void getAllCacheOperation() {}
    
    /**
     * Pointcut for all CacheService.putAll() methods.
     */
    @Pointcut("execution(* com.immortals.cache.core.CacheService.putAll(..))")
    public void putAllCacheOperation() {}
    
    /**
     * Pointcut for all CacheService.clear() methods.
     */
    @Pointcut("execution(* com.immortals.cache.core.CacheService.clear(..))")
    public void clearCacheOperation() {}
    
    /**
     * Intercepts get operations to record metrics, tracing, and logging.
     */
    @Around("getCacheOperation()")
    public Object aroundGet(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Object key = args.length > 0 ? args[0] : null;
        String cacheName = getCacheName(joinPoint);
        String namespace = getNamespace(joinPoint);
        
        CacheMetrics metrics = getOrCreateMetrics(cacheName, namespace);
        Instant start = Instant.now();
        
        // Start tracing span
        Object span = null;
        if (tracingService != null) {
            span = tracingService.startSpan("cache.get", cacheName, namespace);
            tracingService.addAttribute(span, "cache.key", String.valueOf(key));
        }
        
        try {
            Object result = joinPoint.proceed();
            Duration duration = Duration.between(start, Instant.now());
            
            // Record metrics
            if (result instanceof Optional) {
                Optional<?> optResult = (Optional<?>) result;
                if (optResult.isPresent()) {
                    metrics.recordHit();
                    if (tracingService != null) {
                        tracingService.addAttribute(span, "cache.hit", true);
                    }
                    structuredLogger.logCacheHit(cacheName, namespace, key, duration);
                } else {
                    metrics.recordMiss();
                    if (tracingService != null) {
                        tracingService.addAttribute(span, "cache.hit", false);
                    }
                    structuredLogger.logCacheMiss(cacheName, namespace, key, duration);
                }
            }
            
            metrics.recordGetLatency(duration);
            
            if (tracingService != null) {
                tracingService.endSpan(span);
            }
            
            return result;
        } catch (Throwable e) {
            metrics.recordError();
            if (tracingService != null) {
                tracingService.recordError(span, e);
                tracingService.endSpan(span);
            }
            structuredLogger.logCacheError(cacheName, namespace, "get", key, e);
            throw e;
        }
    }
    
    /**
     * Intercepts put operations to record metrics, tracing, and logging.
     */
    @Around("putCacheOperation()")
    public Object aroundPut(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Object key = args.length > 0 ? args[0] : null;
        String cacheName = getCacheName(joinPoint);
        String namespace = getNamespace(joinPoint);
        
        CacheMetrics metrics = getOrCreateMetrics(cacheName, namespace);
        Instant start = Instant.now();
        
        // Start tracing span
        Object span = null;
        if (tracingService != null) {
            span = tracingService.startSpan("cache.put", cacheName, namespace);
            tracingService.addAttribute(span, "cache.key", String.valueOf(key));
        }
        
        try {
            Object result = joinPoint.proceed();
            Duration duration = Duration.between(start, Instant.now());
            
            metrics.recordPut();
            metrics.recordPutLatency(duration);
            
            structuredLogger.logCachePut(cacheName, namespace, key, duration);
            
            if (tracingService != null) {
                tracingService.endSpan(span);
            }
            
            return result;
        } catch (Throwable e) {
            metrics.recordError();
            if (tracingService != null) {
                tracingService.recordError(span, e);
                tracingService.endSpan(span);
            }
            structuredLogger.logCacheError(cacheName, namespace, "put", key, e);
            throw e;
        }
    }
    
    /**
     * Intercepts remove operations to record metrics, tracing, and logging.
     */
    @Around("removeCacheOperation()")
    public Object aroundRemove(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Object key = args.length > 0 ? args[0] : null;
        String cacheName = getCacheName(joinPoint);
        String namespace = getNamespace(joinPoint);
        
        CacheMetrics metrics = getOrCreateMetrics(cacheName, namespace);
        Instant start = Instant.now();
        
        // Start tracing span
        Object span = null;
        if (tracingService != null) {
            span = tracingService.startSpan("cache.remove", cacheName, namespace);
            tracingService.addAttribute(span, "cache.key", String.valueOf(key));
        }
        
        try {
            Object result = joinPoint.proceed();
            Duration duration = Duration.between(start, Instant.now());
            
            metrics.recordRemove();
            metrics.recordRemoveLatency(duration);
            
            structuredLogger.logCacheRemove(cacheName, namespace, key, duration);
            
            if (tracingService != null) {
                tracingService.endSpan(span);
            }
            
            return result;
        } catch (Throwable e) {
            metrics.recordError();
            if (tracingService != null) {
                tracingService.recordError(span, e);
                tracingService.endSpan(span);
            }
            structuredLogger.logCacheError(cacheName, namespace, "remove", key, e);
            throw e;
        }
    }
    
    /**
     * Intercepts getAll operations to record metrics, tracing, and logging.
     */
    @Around("getAllCacheOperation()")
    public Object aroundGetAll(ProceedingJoinPoint joinPoint) throws Throwable {
        String cacheName = getCacheName(joinPoint);
        String namespace = getNamespace(joinPoint);
        
        CacheMetrics metrics = getOrCreateMetrics(cacheName, namespace);
        Instant start = Instant.now();
        
        // Start tracing span
        Object span = null;
        if (tracingService != null) {
            span = tracingService.startSpan("cache.getAll", cacheName, namespace);
        }
        
        try {
            Object result = joinPoint.proceed();
            Duration duration = Duration.between(start, Instant.now());
            
            metrics.recordGetAllLatency(duration);
            
            structuredLogger.logCacheBatchOperation(cacheName, namespace, "getAll", duration);
            
            if (tracingService != null) {
                tracingService.endSpan(span);
            }
            
            return result;
        } catch (Throwable e) {
            metrics.recordError();
            if (tracingService != null) {
                tracingService.recordError(span, e);
                tracingService.endSpan(span);
            }
            structuredLogger.logCacheError(cacheName, namespace, "getAll", null, e);
            throw e;
        }
    }
    
    /**
     * Intercepts putAll operations to record metrics, tracing, and logging.
     */
    @Around("putAllCacheOperation()")
    public Object aroundPutAll(ProceedingJoinPoint joinPoint) throws Throwable {
        String cacheName = getCacheName(joinPoint);
        String namespace = getNamespace(joinPoint);
        
        CacheMetrics metrics = getOrCreateMetrics(cacheName, namespace);
        Instant start = Instant.now();
        
        // Start tracing span
        Object span = null;
        if (tracingService != null) {
            span = tracingService.startSpan("cache.putAll", cacheName, namespace);
        }
        
        try {
            Object result = joinPoint.proceed();
            Duration duration = Duration.between(start, Instant.now());
            
            metrics.recordPutAllLatency(duration);
            
            structuredLogger.logCacheBatchOperation(cacheName, namespace, "putAll", duration);
            
            if (tracingService != null) {
                tracingService.endSpan(span);
            }
            
            return result;
        } catch (Throwable e) {
            metrics.recordError();
            if (tracingService != null) {
                tracingService.recordError(span, e);
                tracingService.endSpan(span);
            }
            structuredLogger.logCacheError(cacheName, namespace, "putAll", null, e);
            throw e;
        }
    }
    
    /**
     * Intercepts clear operations to record logging.
     */
    @Around("clearCacheOperation()")
    public Object aroundClear(ProceedingJoinPoint joinPoint) throws Throwable {
        String cacheName = getCacheName(joinPoint);
        String namespace = getNamespace(joinPoint);
        
        CacheMetrics metrics = getOrCreateMetrics(cacheName, namespace);
        
        // Start tracing span
        Object span = null;
        if (tracingService != null) {
            span = tracingService.startSpan("cache.clear", cacheName, namespace);
        }
        
        try {
            Object result = joinPoint.proceed();
            
            structuredLogger.logCacheClear(cacheName, namespace);
            
            if (tracingService != null) {
                tracingService.endSpan(span);
            }
            
            return result;
        } catch (Throwable e) {
            metrics.recordError();
            if (tracingService != null) {
                tracingService.recordError(span, e);
                tracingService.endSpan(span);
            }
            structuredLogger.logCacheError(cacheName, namespace, "clear", null, e);
            throw e;
        }
    }
    
    /**
     * Gets or creates metrics for a cache.
     */
    private CacheMetrics getOrCreateMetrics(String cacheName, String namespace) {
        String key = cacheName + ":" + namespace;
        return metricsCache.computeIfAbsent(key, 
            k -> new CacheMetrics(meterRegistry, cacheName, namespace));
    }
    
    /**
     * Extracts cache name from the target object.
     */
    private String getCacheName(ProceedingJoinPoint joinPoint) {
        Object target = joinPoint.getTarget();
        if (target instanceof CacheService) {
            return target.getClass().getSimpleName();
        }
        return "unknown";
    }
    
    /**
     * Extracts namespace from the target object.
     * This is a simplified implementation - in practice, you might want to
     * use a more sophisticated approach to extract the namespace.
     */
    private String getNamespace(ProceedingJoinPoint joinPoint) {
        // Default namespace - can be enhanced to extract from context or target
        return "default";
    }
}
