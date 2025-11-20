package com.immortals.cache.providers.caffeine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.immortals.cache.core.CacheService;
import com.immortals.cache.core.CacheStatistics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Caffeine-based in-memory cache implementation.
 * Provides high-performance local caching with configurable size limits, TTL, and eviction policies.
 * 
 * <p>Note: This service is namespace-agnostic. Namespace isolation is handled by NamespacedCacheService wrapper.
 * All metrics are recorded without namespace tags - the wrapper adds namespace context.
 */
public class L1CacheService<K, V> implements CacheService<K, V> {
    private static final Logger log = LoggerFactory.getLogger(L1CacheService.class);

    private final Cache<K, V> cache;
    private final MeterRegistry meterRegistry;
    
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);

    public L1CacheService(CaffeineProperties properties, MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.cache = buildCache(properties);
        
        log.info("Caffeine cache service initialized with maxSize: {}, ttl: {}", 
                 properties.getMaximumSize(), properties.getTtl());
    }

    private Cache<K, V> buildCache(CaffeineProperties properties) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .maximumSize(properties.getMaximumSize())
                .recordStats();
        
        if (properties.getTtl() != null && !properties.getTtl().isZero()) {
            builder.expireAfterWrite(properties.getTtl().toMillis(), TimeUnit.MILLISECONDS);
        }
        
        return builder.build();
    }

    @Override
    public void put(K key, V value) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            cache.put(key, value);
            log.debug("Stored value in cache for key: {}", key);
        } finally {
            sample.stop(Timer.builder("cache.put")
                    .tag("provider", "caffeine")
                    .register(meterRegistry));
        }
    }

    @Override
    public void put(K key, V value, Duration ttl) {
        log.debug("Per-key TTL not supported in Caffeine, using global TTL for key: {}", key);
        put(key, value);
    }

    @Override
    public Optional<V> get(K key) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            V value = cache.getIfPresent(key);
            if (value != null) {
                hits.incrementAndGet();
                meterRegistry.counter("cache.hit", "provider", "caffeine").increment();
                log.debug("Cache hit for key: {}", key);
                return Optional.of(value);
            } else {
                misses.incrementAndGet();
                meterRegistry.counter("cache.miss", "provider", "caffeine").increment();
                log.debug("Cache miss for key: {}", key);
                return Optional.empty();
            }
        } finally {
            sample.stop(Timer.builder("cache.get")
                    .tag("provider", "caffeine")
                    .register(meterRegistry));
        }
    }

    @Override
    public void remove(K key) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            cache.invalidate(key);
            meterRegistry.counter("cache.eviction", "provider", "caffeine").increment();
            log.debug("Removed key from cache: {}", key);
        } finally {
            sample.stop(Timer.builder("cache.remove")
                    .tag("provider", "caffeine")
                    .register(meterRegistry));
        }
    }

    @Override
    public void clear() {
        cache.invalidateAll();
        log.info("Cleared cache");
    }

    @Override
    public boolean containsKey(K key) {
        return cache.getIfPresent(key) != null;
    }

    @Override
    public void putAll(Map<K, V> entries) {
        cache.putAll(entries);
        log.debug("Stored {} entries in cache", entries.size());
    }

    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        return cache.getAllPresent(keys);
    }

    @Override
    public boolean putIfAbsent(K key, V value) {
        V existing = cache.getIfPresent(key);
        if (existing != null) {
            return false;
        }
        cache.put(key, value);
        return true;
    }

    @Override
    public boolean putIfAbsent(K key, V value, Duration ttl) {
        return putIfAbsent(key, value);
    }

    @Override
    public Long increment(K key, long delta) {
        throw new UnsupportedOperationException("Atomic increment not supported in Caffeine cache");
    }

    @Override
    public Long decrement(K key, long delta) {
        throw new UnsupportedOperationException("Atomic decrement not supported in Caffeine cache");
    }

    @Override
    public CacheStatistics getStatistics() {
        CacheStats stats = cache.stats();
        long totalRequests = hits.get() + misses.get();
        double hitRate = totalRequests > 0 ? (double) hits.get() / totalRequests : 0.0;
        
        return CacheStatistics.builder()
                .namespace("caffeine")
                .timestamp(Instant.now())
                .window(CacheStatistics.TimeWindow.ALL_TIME)
                .hitCount(hits.get())
                .missCount(misses.get())
                .hitRate(hitRate)
                .missRate(1.0 - hitRate)
                .currentSize(cache.estimatedSize())
                .evictionCount(stats.evictionCount())
                .build();
    }
}
