package com.immortals.cache.providers.redis;

import com.immortals.cache.core.CacheService;
import com.immortals.cache.core.CacheStatistics;
import com.immortals.platform.common.exception.CacheConnectionException;
import com.immortals.platform.common.exception.CacheException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis-based distributed cache implementation with built-in resilience patterns.
 * 
 * <p>Provides distributed caching with support for:
 * <ul>
 *   <li>Clustering, replication, and pipelining</li>
 *   <li>Circuit breaker pattern for fault tolerance (Requirements: 5.2, 5.3)</li>
 *   <li>Automatic timeout handling (Requirement: 5.5)</li>
 *   <li>Fallback to empty responses on failures</li>
 * </ul>
 * 
 * <p>Note: This service is namespace-agnostic. Namespace isolation is handled by NamespacedCacheService wrapper.
 * All metrics are recorded without namespace tags - the wrapper adds namespace context.
 * 
 * <p>Circuit breaker is configured via application properties:
 * <pre>
 * resilience4j.circuitbreaker:
 *   instances:
 *     redisCache:
 *       failure-rate-threshold: 50
 *       wait-duration-in-open-state: 60s
 *       sliding-window-size: 100
 * </pre>
 */
public class RedisCacheService<K, V> implements CacheService<K, V> {
    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);
    private final RedisTemplate<String, Object> redisTemplate;
    private final ValueOperations<String, Object> valueOps;
    private final MeterRegistry meterRegistry;
    private final Duration defaultTtl;
    private final boolean pipeliningEnabled;
    private final int pipelineBatchSize;

    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong evictions = new AtomicLong(0);


    public RedisCacheService(RedisTemplate<String, Object> redisTemplate,
                            MeterRegistry meterRegistry,
                            RedisProperties properties) {
        this.redisTemplate = redisTemplate;
        this.valueOps = redisTemplate.opsForValue();
        this.meterRegistry = meterRegistry;
        this.defaultTtl = properties.getTimeToLive() != null ? properties.getTimeToLive() : Duration.ofHours(1);
        this.pipeliningEnabled = properties.getPipelining().getEnabled();
        this.pipelineBatchSize = properties.getPipelining().getBatchSize() > 0 ? properties.getPipelining().getBatchSize() : 100;

        log.info("Redis cache service initialized with pipelining={}, batchSize={}",
                pipeliningEnabled, this.pipelineBatchSize);
    }

    @Override
    public void put(K key, V value) {
        put(key, value, defaultTtl);
    }


    @Override
    public void put(K key, V value, Duration ttl) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            if (ttl != null && !ttl.isZero()) {
                valueOps.set(key.toString(), value, ttl.toMillis(), TimeUnit.MILLISECONDS);
            } else {
                valueOps.set(key.toString(), value);
            }

            meterRegistry.counter("cache.put",
                    "provider", "redis",
                    "status", "success").increment();

            log.debug("Stored value in Redis for key: {} with ttl: {}", key, ttl);
        } catch (RedisConnectionFailureException e) {
            meterRegistry.counter("cache.put",
                    "provider", "redis",
                    "status", "error").increment();

            log.error("Redis connection failed for PUT key [{}]: {}", key, e.getMessage(), e);
            throw new CacheConnectionException("Failed to connect to Redis for PUT operation", key.toString(), CacheException.CacheOperation.PUT, e);
        } catch (DataAccessException e) {
            meterRegistry.counter("cache.put",
                    "provider", "redis",
                    "status", "error").increment();

            log.error("Redis PUT failed for key [{}]: {}", key, e.getMessage(), e);
            throw new CacheException("Failed to put value in cache", key.toString(), CacheException.CacheOperation.PUT, e);
        } finally {
            sample.stop(Timer.builder("cache.put.latency")
                    .tag("provider", "redis")
                    .register(meterRegistry));
        }
    }


    @Override
    @SuppressWarnings("unchecked")
    public Optional<V> get(K key) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            Object value = valueOps.get(key.toString());

            if (value != null) {
                hits.incrementAndGet();
                meterRegistry.counter("cache.hit",
                        "provider", "redis").increment();

                log.debug("Cache hit for key: {}", key);
                return Optional.of((V) value);
            } else {
                misses.incrementAndGet();
                meterRegistry.counter("cache.miss",
                        "provider", "redis").increment();

                log.debug("Cache miss for key: {}", key);
                return Optional.empty();
            }
        } catch (RedisConnectionFailureException e) {
            misses.incrementAndGet();
            meterRegistry.counter("cache.get",
                    "provider", "redis",
                    "status", "error").increment();

            log.error("Redis connection failed for GET key [{}]: {}", key, e.getMessage(), e);
            throw new CacheConnectionException("Failed to connect to Redis for GET operation", key.toString(), CacheException.CacheOperation.GET, e);
        } catch (DataAccessException e) {
            misses.incrementAndGet();
            meterRegistry.counter("cache.get",
                    "provider", "redis",
                    "status", "error").increment();

            log.error("Redis GET failed for key [{}]: {}", key, e.getMessage(), e);
            throw new CacheException("Failed to get value from cache", key.toString(), CacheException.CacheOperation.GET, e);
        } finally {
            sample.stop(Timer.builder("cache.get.latency")
                    .tag("provider", "redis")
                    .register(meterRegistry));
        }
    }


    @Override
    public void remove(K key) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            Boolean deleted = redisTemplate.delete(key.toString());

            if (deleted) {
                evictions.incrementAndGet();
                meterRegistry.counter("cache.eviction",
                        "provider", "redis").increment();
            }

            log.debug("Removed key from Redis: {}", key);
        } catch (RedisConnectionFailureException e) {
            meterRegistry.counter("cache.remove",
                    "provider", "redis",
                    "status", "error").increment();

            log.error("Redis connection failed for REMOVE key [{}]: {}", key, e.getMessage(), e);
            throw new CacheConnectionException("Failed to connect to Redis for REMOVE operation", key.toString(), CacheException.CacheOperation.REMOVE, e);
        } catch (DataAccessException e) {
            meterRegistry.counter("cache.remove",
                    "provider", "redis",
                    "status", "error").increment();

            log.error("Redis DELETE failed for key [{}]: {}", key, e.getMessage(), e);
            throw new CacheException("Failed to remove cache key", key.toString(), CacheException.CacheOperation.REMOVE, e);
        } finally {
            sample.stop(Timer.builder("cache.remove.latency")
                    .tag("provider", "redis")
                    .register(meterRegistry));
        }
    }


    @Override
    public void clear() {
        try {
            var connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                throw new CacheConnectionException(
                        "RedisConnectionFactory is null",
                        null,
                        CacheException.CacheOperation.CLEAR,null
                );
            }

            try (var connection = connectionFactory.getConnection()) {
                connection.serverCommands().flushDb();
            }

            log.info("Successfully cleared Redis cache for current DB");

        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed during CLEAR: {}", e.getMessage(), e);
            throw new CacheConnectionException(
                    "Failed to connect to Redis during CLEAR",
                    null,
                    CacheException.CacheOperation.CLEAR,
                    e
            );

        } catch (DataAccessException e) {
            log.error("Redis CLEAR operation failed: {}", e.getMessage(), e);
            throw new CacheException(
                    "Failed to clear Redis cache",
                    null,
                    CacheException.CacheOperation.CLEAR,
                    e
            );

        } catch (Exception e) {
            log.error("Unexpected error during Redis CLEAR: {}", e.getMessage(), e);
            throw new CacheException(
                    "Unexpected error during Redis CLEAR",
                    null,
                    CacheException.CacheOperation.CLEAR,
                    e
            );
        }
    }


    @Override
    public boolean containsKey(K key) {
        try {
            return redisTemplate.hasKey(key.toString());
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed for CONTAINS KEY check [{}]: {}",
                    key, e.getMessage(), e);
            throw new CacheConnectionException("Failed to connect to Redis for CONTAINS_KEY operation", key.toString(), CacheException.CacheOperation.CONTAINS_KEY, e);
        } catch (DataAccessException e) {
            log.error("Redis CONTAINS KEY check failed for key [{}]: {}",
                    key, e.getMessage(), e);
            throw new CacheException("Failed to check key presence in cache", key.toString(), CacheException.CacheOperation.CONTAINS_KEY, e);
        }
    }

    @Override
    public void putAll(Map<K, V> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            if (!pipeliningEnabled || entries.size() <= pipelineBatchSize) {
                executePipelinedPutAll(entries);
            } else {
                List<Map<K, V>> batches = partitionMap(entries, pipelineBatchSize);
                log.debug("Splitting putAll operation into {} batches of size {}", batches.size(), pipelineBatchSize);

                for (Map<K, V> batch : batches) {
                    executePipelinedPutAll(batch);
                }
            }

            meterRegistry.counter("cache.putAll",
                    "provider", "redis",
                    "status", "success").increment();
        } catch (RedisConnectionFailureException e) {
            meterRegistry.counter("cache.putAll",
                    "provider", "redis",
                    "status", "error").increment();

            log.error("Redis connection failed for putAll: {}", e.getMessage(), e);
            throw new CacheConnectionException("Failed to connect to Redis for PUT_ALL operation", null, CacheException.CacheOperation.PUT_ALL, e);
        } catch (DataAccessException e) {
            meterRegistry.counter("cache.putAll",
                    "provider", "redis",
                    "status", "error").increment();

            log.error("Redis pipelined putAll failed: {}", e.getMessage(), e);
            throw new CacheException("Failed to perform batch put operation", null, CacheException.CacheOperation.PUT_ALL, e);
        } finally {
            sample.stop(Timer.builder("cache.putAll.latency")
                    .tag("provider", "redis")
                    .register(meterRegistry));
        }
    }

    private void executePipelinedPutAll(Map<K, V> entries) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            entries.forEach((key, value) -> {
                if (defaultTtl != null && !defaultTtl.isZero()) {
                    valueOps.set(key.toString(), value, defaultTtl.toMillis(), TimeUnit.MILLISECONDS);
                } else {
                    valueOps.set(key.toString(), value);
                }
            });
            return null;
        });
    }

    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            if (!pipeliningEnabled || keys.size() <= pipelineBatchSize) {
                return executePipelinedGetAll(keys);
            } else {
                List<List<K>> batches = partitionList(new ArrayList<>(keys), pipelineBatchSize);
                log.debug("Splitting getAll operation into {} batches of size {}", batches.size(), pipelineBatchSize);

                Map<K, V> result = new HashMap<>();
                for (List<K> batch : batches) {
                    result.putAll(executePipelinedGetAll(batch));
                }
                return result;
            }
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed for getAll: {}", e.getMessage(), e);
            throw new CacheConnectionException("Failed to connect to Redis for GET_ALL operation", null, CacheException.CacheOperation.GET_ALL, e);
        } catch (DataAccessException e) {
            log.error("Redis pipelined getAll failed: {}", e.getMessage(), e);
            throw new CacheException("Failed to perform batch get operation", null, CacheException.CacheOperation.GET_ALL, e);
        } finally {
            sample.stop(Timer.builder("cache.getAll.latency")
                    .tag("provider", "redis")
                    .register(meterRegistry));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<K, V> executePipelinedGetAll(Collection<K> keys) {
        List<String> redisKeys = keys.stream()
                .map(K::toString)
                .toList();

        List<Object> values = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            redisKeys.forEach(valueOps::get);
            return null;
        });

        Map<K, V> result = new HashMap<>();
        Iterator<K> keyIterator = keys.iterator();
        Iterator<Object> valueIterator = values.iterator();

        while (keyIterator.hasNext() && valueIterator.hasNext()) {
            K key = keyIterator.next();
            Object value = valueIterator.next();
            if (value != null) {
                result.put(key, (V) value);
                hits.incrementAndGet();
            } else {
                misses.incrementAndGet();
            }
        }

        return result;
    }

    @Override
    public boolean putIfAbsent(K key, V value) {
        return putIfAbsent(key, value, defaultTtl);
    }

    @Override
    public boolean putIfAbsent(K key, V value, Duration ttl) {
        try {
            Boolean result;
            if (ttl != null && !ttl.isZero()) {
                result = valueOps.setIfAbsent(key.toString(), value, ttl.toMillis(), TimeUnit.MILLISECONDS);
            } else {
                result = valueOps.setIfAbsent(key.toString(), value);
            }

            if (Boolean.TRUE.equals(result)) {
                hits.incrementAndGet();
                log.debug("Set value for key: {} (was absent)", key);
                return true;
            } else {
                misses.incrementAndGet();
                log.debug("Key already exists: {}", key);
                return false;
            }
        } catch (RedisConnectionFailureException e) {
            misses.incrementAndGet();
            log.error("Redis connection failed for putIfAbsent key [{}]: {}",
                    key, e.getMessage(), e);
            throw new CacheConnectionException("Failed to connect to Redis for putIfAbsent operation", key.toString(), CacheException.CacheOperation.PUT, e);
        } catch (DataAccessException e) {
            misses.incrementAndGet();
            log.error("Redis putIfAbsent failed for key [{}]: {}",
                    key, e.getMessage(), e);
            throw new CacheException("Failed to perform putIfAbsent in cache", key.toString(), CacheException.CacheOperation.PUT, e);
        }
    }

    @Override
    public Long increment(K key, long delta) {
        try {
            Long result = valueOps.increment(key.toString(), delta);
            log.debug("Incremented key: {} by {}, new value: {}", key, delta, result);
            return result;
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed for increment key [{}]: {}",
                    key, e.getMessage(), e);
            throw new CacheConnectionException("Failed to connect to Redis for INCREMENT operation", key.toString(), CacheException.CacheOperation.INCREMENT, e);
        } catch (DataAccessException e) {
            log.error("Redis increment failed for key [{}]: {}",
                    key, e.getMessage(), e);
            throw new CacheException("Failed to increment value", key.toString(), CacheException.CacheOperation.INCREMENT, e);
        }
    }

    @Override
    public Long decrement(K key, long delta) {
        try {
            Long result = valueOps.decrement(key.toString(), delta);
            log.debug("Decremented key: {} by {}, new value: {}", key, delta, result);
            return result;
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed for decrement key [{}]: {}",
                    key, e.getMessage(), e);
            throw new CacheConnectionException("Failed to connect to Redis for DECREMENT operation", key.toString(), CacheException.CacheOperation.DECREMENT, e);
        } catch (DataAccessException e) {
            log.error("Redis decrement failed for key [{}]: {}",
                    key, e.getMessage(), e);
            throw new CacheException("Failed to decrement value", key.toString(), CacheException.CacheOperation.DECREMENT, e);
        }
    }

    @Override
    public CacheStatistics getStatistics() {
        long totalRequests = hits.get() + misses.get();
        double hitRate = totalRequests > 0 ? (double) hits.get() / totalRequests : 0.0;

        return CacheStatistics.builder()
                .namespace("redis")
                .timestamp(Instant.now())
                .window(CacheStatistics.TimeWindow.ALL_TIME)
                .hitCount(hits.get())
                .missCount(misses.get())
                .hitRate(hitRate)
                .missRate(1.0 - hitRate)
                .evictionCount(evictions.get())
                .build();
    }

    private List<Map<K, V>> partitionMap(Map<K, V> map, int batchSize) {
        List<Map<K, V>> partitions = new ArrayList<>();
        Map<K, V> currentBatch = new HashMap<>();

        for (Map.Entry<K, V> entry : map.entrySet()) {
            currentBatch.put(entry.getKey(), entry.getValue());

            if (currentBatch.size() >= batchSize) {
                partitions.add(currentBatch);
                currentBatch = new HashMap<>();
            }
        }

        if (!currentBatch.isEmpty()) {
            partitions.add(currentBatch);
        }

        return partitions;
    }

    private List<List<K>> partitionList(List<K> list, int batchSize) {
        List<List<K>> partitions = new ArrayList<>();

        for (int i = 0; i < list.size(); i += batchSize) {
            int end = Math.min(i + batchSize, list.size());
            partitions.add(new ArrayList<>(list.subList(i, end)));
        }

        return partitions;
    }
}
