package com.immortals.cache.features.annotations;

import com.immortals.cache.core.CacheService;
import com.immortals.cache.core.UnifiedCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Optional;

/**
 * AOP aspect that processes cache annotations (@Cacheable, @CachePut, @CacheEvict).
 * Intercepts annotated methods and applies caching logic with namespace support.
 * 
 * @since 2.0.0
 */
@Aspect
@Component
@Slf4j
public class CacheAspect {
    
    private final UnifiedCacheManager cacheManager;
    private final KeyGenerator keyGenerator;
    private final ExpressionEvaluator expressionEvaluator;
    private final ParameterNameDiscoverer parameterNameDiscoverer;
    
    public CacheAspect(UnifiedCacheManager cacheManager) {
        this.cacheManager = cacheManager;
        this.keyGenerator = new KeyGenerator();
        this.expressionEvaluator = new ExpressionEvaluator();
        this.parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    }
    
    /**
     * Handles @Cacheable annotation.
     * Checks cache before method execution and caches the result if not found.
     */
    @Around("@annotation(cacheable)")
    public Object handleCacheable(ProceedingJoinPoint joinPoint, Cacheable cacheable) throws Throwable {
        Method method = getMethod(joinPoint);
        Object[] args = joinPoint.getArgs();
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
        
        // Evaluate condition before execution
        if (!expressionEvaluator.evaluateCondition(cacheable.condition(), method, args, paramNames)) {
            log.debug("Cacheable condition not met for method: {}", method.getName());
            return joinPoint.proceed();
        }
        
        // Generate cache key
        String cacheKey = keyGenerator.generateKey(cacheable.key(), method, args, paramNames);
        
        // Get cache service for namespace
        CacheService<String, Object> cacheService = getCacheService(cacheable.namespace());
        
        // Try to get from cache
        Optional<Object> cachedValue = cacheService.get(cacheKey);
        if (cachedValue.isPresent()) {
            log.debug("Cache hit for key: {} in namespace: {}", cacheKey, cacheable.namespace());
            return cachedValue.get();
        }
        
        log.debug("Cache miss for key: {} in namespace: {}", cacheKey, cacheable.namespace());
        
        // Execute method with stampede protection if enabled
        Object result;
        if (cacheable.stampedeProtection()) {
            result = executeWithStampedeProtection(joinPoint, cacheService, cacheKey);
        } else {
            result = joinPoint.proceed();
        }
        
        // Evaluate unless condition after execution
        if (expressionEvaluator.evaluateUnless(cacheable.unless(), method, args, paramNames, result)) {
            log.debug("Cacheable unless condition met, not caching result for key: {}", cacheKey);
            return result;
        }
        
        // Cache the result
        if (cacheable.ttl() > 0) {
            cacheService.put(cacheKey, result, Duration.ofSeconds(cacheable.ttl()));
        } else {
            cacheService.put(cacheKey, result);
        }
        
        log.debug("Cached result for key: {} in namespace: {}", cacheKey, cacheable.namespace());
        return result;
    }
    
    /**
     * Handles @CachePut annotation.
     * Always executes the method and updates the cache with the result.
     */
    @Around("@annotation(cachePut)")
    public Object handleCachePut(ProceedingJoinPoint joinPoint, CachePut cachePut) throws Throwable {
        Method method = getMethod(joinPoint);
        Object[] args = joinPoint.getArgs();
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
        
        // Evaluate condition before execution
        if (!expressionEvaluator.evaluateCondition(cachePut.condition(), method, args, paramNames)) {
            log.debug("CachePut condition not met for method: {}", method.getName());
            return joinPoint.proceed();
        }
        
        // Execute method
        Object result = joinPoint.proceed();
        
        // Evaluate unless condition after execution
        if (expressionEvaluator.evaluateUnless(cachePut.unless(), method, args, paramNames, result)) {
            log.debug("CachePut unless condition met, not caching result");
            return result;
        }
        
        // Generate cache key
        String cacheKey = keyGenerator.generateKey(cachePut.key(), method, args, paramNames);
        
        // Get cache service for namespace
        CacheService<String, Object> cacheService = getCacheService(cachePut.namespace());
        
        // Update cache
        if (cachePut.ttl() > 0) {
            cacheService.put(cacheKey, result, Duration.ofSeconds(cachePut.ttl()));
        } else {
            cacheService.put(cacheKey, result);
        }
        
        log.debug("Updated cache for key: {} in namespace: {}", cacheKey, cachePut.namespace());
        return result;
    }
    
    /**
     * Handles @CacheEvict annotation.
     * Removes entries from cache before or after method execution.
     */
    @Around("@annotation(cacheEvict)")
    public Object handleCacheEvict(ProceedingJoinPoint joinPoint, CacheEvict cacheEvict) throws Throwable {
        Method method = getMethod(joinPoint);
        Object[] args = joinPoint.getArgs();
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
        
        // Evaluate condition
        if (!expressionEvaluator.evaluateCondition(cacheEvict.condition(), method, args, paramNames)) {
            log.debug("CacheEvict condition not met for method: {}", method.getName());
            return joinPoint.proceed();
        }
        
        // Get cache service for namespace
        CacheService<String, Object> cacheService = getCacheService(cacheEvict.namespace());
        
        // Evict before invocation if specified
        if (cacheEvict.beforeInvocation()) {
            performEviction(cacheService, cacheEvict, method, args, paramNames);
        }
        
        // Execute method
        Object result = joinPoint.proceed();
        
        // Evict after invocation if not done before
        if (!cacheEvict.beforeInvocation()) {
            performEviction(cacheService, cacheEvict, method, args, paramNames);
        }
        
        return result;
    }
    
    /**
     * Performs cache eviction based on annotation settings.
     */
    private void performEviction(CacheService<String, Object> cacheService, CacheEvict cacheEvict,
                                Method method, Object[] args, String[] paramNames) {
        if (cacheEvict.allEntries()) {
            cacheService.clear();
            log.debug("Cleared all entries in namespace: {}", cacheEvict.namespace());
        } else {
            String cacheKey = keyGenerator.generateKey(cacheEvict.key(), method, args, paramNames);
            cacheService.remove(cacheKey);
            log.debug("Evicted cache entry for key: {} in namespace: {}", cacheKey, cacheEvict.namespace());
        }
    }
    
    /**
     * Executes method with stampede protection using putIfAbsent.
     */
    private Object executeWithStampedeProtection(ProceedingJoinPoint joinPoint, 
                                                CacheService<String, Object> cacheService,
                                                String cacheKey) throws Throwable {
        // Double-check pattern: check cache again before executing
        Optional<Object> cachedValue = cacheService.get(cacheKey);
        if (cachedValue.isPresent()) {
            return cachedValue.get();
        }
        
        // Execute method
        Object result = joinPoint.proceed();
        
        // Use putIfAbsent to handle concurrent executions
        cacheService.putIfAbsent(cacheKey, result);
        
        return result;
    }
    
    /**
     * Gets the cache service for the specified namespace.
     */
    private CacheService<String, Object> getCacheService(String namespace) {
        return cacheManager.getCache(namespace);
    }
    
    /**
     * Extracts the method from the join point.
     */
    private Method getMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getMethod();
    }
}
