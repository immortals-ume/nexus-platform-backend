package com.immortals.cache.features.annotations;

import java.lang.annotation.*;

/**
 * Annotation for cache invalidation operations.
 * Methods annotated with @CacheEvict will remove entries from the cache.
 * 
 * Can evict a single entry by key or clear the entire namespace.
 * 
 * Example:
 * <pre>
 * {@code
 * @CacheEvict(namespace = "users", key = "#userId")
 * public void deleteUser(String userId) {
 *     userRepository.deleteById(userId);
 * }
 * 
 * @CacheEvict(namespace = "users", allEntries = true)
 * public void deleteAllUsers() {
 *     userRepository.deleteAll();
 * }
 * }
 * </pre>
 * 
 * @since 2.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheEvict {
    
    /**
     * The namespace (cache name) to use for this operation.
     * Required attribute.
     * 
     * @return the namespace
     */
    String namespace();
    
    /**
     * SpEL expression for generating the cache key to evict.
     * Ignored if allEntries is true.
     * 
     * Examples:
     * - "#userId"
     * - "#user.id"
     * - "'user:' + #userId"
     * 
     * @return the key expression
     */
    String key() default "";
    
    /**
     * SpEL expression to conditionally enable cache eviction.
     * The cache is evicted only if this expression evaluates to true.
     * 
     * Example: "#userId != null"
     * 
     * @return the condition expression
     */
    String condition() default "";
    
    /**
     * Whether to evict all entries in the namespace.
     * When true, the entire cache namespace is cleared.
     * When false, only the entry matching the key is evicted.
     * 
     * @return true to clear all entries
     */
    boolean allEntries() default false;
    
    /**
     * Whether to perform eviction before method execution.
     * 
     * When true: Cache is evicted before the method runs.
     * When false: Cache is evicted after the method completes successfully.
     * 
     * Use beforeInvocation=true for delete operations to ensure cache
     * consistency even if the method throws an exception.
     * 
     * @return true to evict before method execution
     */
    boolean beforeInvocation() default false;
}
