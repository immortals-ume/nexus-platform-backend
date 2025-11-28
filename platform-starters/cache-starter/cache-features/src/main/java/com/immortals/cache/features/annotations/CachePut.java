package com.immortals.cache.features.annotations;

import java.lang.annotation.*;

/**
 * Annotation for write-through caching operations.
 * Methods annotated with @CachePut will always execute and update the cache
 * with the method result.
 * <p>
 * Unlike @Cacheable, this annotation does not skip method execution.
 * It's useful for update operations where you want to refresh the cache.
 * <p>
 * Example:
 * <pre>
 * {@code
 * @CachePut(namespace = "users", key = "#user.id")
 * public User updateUser(User user) {
 *     return userRepository.save(user);
 * }
 * }
 * </pre>
 *
 * @since 2.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CachePut {

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
     * <p>
     * Examples:
     * - "#user.id"
     * - "#userId"
     * - "'user:' + #user.id"
     *
     * @return the key expression
     */
    String key() default "";

    /**
     * SpEL expression to conditionally enable cache update.
     * The cache is updated only if this expression evaluates to true.
     * <p>
     * Example: "#user != null"
     *
     * @return the condition expression
     */
    String condition() default "";

    /**
     * SpEL expression to conditionally skip cache update based on the result.
     * The cache is NOT updated if this expression evaluates to true.
     * Can reference the method result using #result.
     * <p>
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
}
