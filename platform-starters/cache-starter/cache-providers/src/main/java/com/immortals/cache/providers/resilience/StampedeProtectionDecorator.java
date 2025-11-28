package com.immortals.cache.providers.resilience;

import com.immortals.cache.core.CacheService;
import com.immortals.cache.providers.redis.StampedeProtectionHelper;
import io.micrometer.core.instrument.MeterRegistry;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

/**
 * Decorator that adds stampede protection to a cache service.
 * Prevents cache stampede (thundering herd) by using distributed locks
 * to serialize cache regeneration for the same key.
 * 
 * @param <K> key type
 * @param <V> value type
 */
public class StampedeProtectionDecorator<K, V> extends CacheDecorator<K, V> {

    private static final Logger log = LoggerFactory.getLogger(StampedeProtectionDecorator.class);

    private final StampedeProtectionHelper stampedeHelper;
    private final String namespace;

    /**
     * Creates a stampede protection decorator.
     * 
     * @param delegate the underlying cache service
     * @param redissonClient Redisson client for distributed locks
     * @param namespace cache namespace
     * @param lockTimeout timeout for lock acquisition
     * @param computationTimeout timeout for value computation
     * @param meterRegistry meter registry for metrics
     */
    public StampedeProtectionDecorator(CacheService<K, V> delegate,
                                      RedissonClient redissonClient,
                                      String namespace,
                                      Duration lockTimeout,
                                      Duration computationTimeout,
                                      MeterRegistry meterRegistry) {
        super(delegate);
        this.namespace = namespace;
        this.stampedeHelper = new StampedeProtectionHelper(
                redissonClient,
                lockTimeout,
                computationTimeout,
                meterRegistry
        );

        log.debug("Stampede protection decorator initialized for namespace: {} (lockTimeout: {}, computationTimeout: {})",
                namespace, lockTimeout, computationTimeout);
    }

    /**
     * Gets a value from cache with stampede protection.
     * If the value is not in cache, uses distributed lock to prevent
     * multiple threads from computing the same value simultaneously.
     * 
     * @param key the cache key
     * @param valueLoader function to compute the value if not cached
     * @return the cached or computed value
     */
    public Optional<V> getWithLoader(K key, Function<K, V> valueLoader) {
        return stampedeHelper.executeWithStampedeProtection(
                key,
                () -> delegate.get(key),
                valueLoader,
                value -> delegate.put(key, value)
        );
    }

    /**
     * Gets a value from cache with stampede protection and TTL.
     * 
     * @param key the cache key
     * @param valueLoader function to compute the value if not cached
     * @param ttl time-to-live for the cached value
     * @return the cached or computed value
     */
    public Optional<V> getWithLoader(K key, Function<K, V> valueLoader, Duration ttl) {
        return stampedeHelper.executeWithStampedeProtection(
                key,
                () -> delegate.get(key),
                valueLoader,
                value -> delegate.put(key, value, ttl)
        );
    }
}
