package com.immortals.cache.features.stampede;

import com.immortals.cache.core.CacheService;
import com.immortals.cache.core.CacheStatistics;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Decorator that adds stampede protection to a cache service.
 * Prevents cache stampede by using distributed locks to ensure only one thread
 * computes the value when a cache miss occurs.
 */
@Slf4j
public class StampedeProtectionDecorator<K, V> implements CacheService<K, V> {

    private static final String LOCK_PREFIX = "cache:lock:";
    private final CacheService<K, V> delegate;
    private final RedissonClient redissonClient;
    private final String namespace;
    private final Duration lockTimeout;

    /**
     * Creates a stampede protection decorator.
     *
     * @param delegate       the underlying cache service
     * @param redissonClient the Redisson client for distributed locks
     * @param namespace      the cache namespace
     * @param lockTimeout    timeout for acquiring the lock
     */
    public StampedeProtectionDecorator(CacheService<K, V> delegate,
                                       RedissonClient redissonClient,
                                       String namespace,
                                       Duration lockTimeout) {
        this.delegate = delegate;
        this.redissonClient = redissonClient;
        this.namespace = namespace;
        this.lockTimeout = lockTimeout;
    }

    @Override
    public void put(K key, V value) {
        delegate.put(key, value);
    }

    @Override
    public void put(K key, V value, Duration ttl) {
        delegate.put(key, value, ttl);
    }

    @Override
    public Optional<V> get(K key) {
        Optional<V> cached = delegate.get(key);
        if (cached.isPresent()) {
            return cached;
        }

        String lockKey = LOCK_PREFIX + namespace + ":" + key;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(lockTimeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                log.warn("Failed to acquire stampede protection lock for key: {}", key);
                return Optional.empty();
            }

            try {
                cached = delegate.get(key);
                if (cached.isPresent()) {
                    log.debug("Value computed by another thread for key: {}", key);
                    return cached;
                }

                return Optional.empty();
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread()
                    .interrupt();
            log.error("Interrupted while acquiring stampede protection lock for key: {}", key, e);
            return Optional.empty();
        }
    }

    @Override
    public void remove(K key) {
        delegate.remove(key);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public boolean containsKey(K key) {
        return delegate.containsKey(key);
    }

    @Override
    public void putAll(Map<K, V> entries) {
        delegate.putAll(entries);
    }

    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        return delegate.getAll(keys);
    }

    @Override
    public boolean putIfAbsent(K key, V value) {
        return delegate.putIfAbsent(key, value);
    }

    @Override
    public boolean putIfAbsent(K key, V value, Duration ttl) {
        return delegate.putIfAbsent(key, value, ttl);
    }

    @Override
    public Long increment(K key, long delta) {
        return delegate.increment(key, delta);
    }

    @Override
    public Long decrement(K key, long delta) {
        return delegate.decrement(key, delta);
    }

    @Override
    public CacheStatistics getStatistics() {
        return delegate.getStatistics();
    }
}
