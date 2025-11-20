package com.immortals.cache.features.annotations;

import java.lang.annotation.*;
import java.time.Duration;

/**
 * Annotation for read-through caching operations.
 * Methods annotated with @Cacheable will first check the cache before executing.
 * If a cached value exists, it is returned without executing the method.
 * 
 * Example:
 * <pre>
 * {@code
 * @Cacheable(namespace = "users", key = "#userId")
 * public User findUser(String userId) {
 *     return userRepository.findById(userId);
 * }
 * }
 * </pre>
 * 
 * @since 2.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cacheable {
    
    /**
     * The namespace (cache name) to use for this operation.
     * Required attribute.
     * 
     * @return the namespace
     */
    String namespace();
    
    /**
     * SpEL expression for generating the cache key.
     * Can reference method parameters using #paramName syntax.
     * 
     * Examples:
     * - "#userId"
     * - "#user.id"
     * - "#p0" (first parameter)
     * - "'user:' + #userId"
     * 
     * @return the key expression
     */
    String key() default "";
    
    /**
     * SpEL expression to conditionally enable caching.
     * The method result is cached only if this expression evaluates to true.
     * 
     * Example: "#userId != null"
     * 
     * @return the condition expression
     */
    String condition() default "";
    
    /**
     * SpEL expression to conditionally skip caching based on the result.
     * The result is NOT cached if this expression evaluates to true.
     * Can reference the method result using #result.
     * 
     * Example: "#result == null"
     * 
     * @return the unless expression
     */
    String unless() default "";
    
    /**
     * Time-to-live for the cached entry in seconds.
     * If not specified, uses the namespace default TTL.
     * 
     * @return TTL in seconds, or -1 for no TTL
     */
    long ttl() default -1;
    
    /**
     * Whether to compress the cached value.
     * If not specified, uses the namespace default compression setting.
     * 
     * @return true to enable compression
     */
    boolean compress() default false;
    
    /**
     * Whether to encrypt the cached value.
     * If not specified, uses the namespace default encryption setting.
     * 
     * @return true to enable encryption
     */
    boolean encrypt() default false;
    
    /**
     * Whether to enable stampede protection for this cache operation.
     * When enabled, concurrent requests for the same cache miss will be
     * synchronized to prevent multiple executions.
     * 
     * @return true to enable stampede protection
     */
    boolean stampedeProtection() default false;
}
