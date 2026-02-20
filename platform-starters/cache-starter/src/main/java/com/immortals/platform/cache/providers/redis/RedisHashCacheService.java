package com.immortals.platform.cache.providers.redis;

import com.immortals.platform.cache.core.CacheStatistics;
import com.immortals.platform.common.exception.CacheConnectionException;
import com.immortals.platform.common.exception.CacheException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis Hash-based cache service providing hash operations for complex data structures.
 * 
 * <p>This service provides Redis hash operations which are useful for:
 * <ul>
 *   <li>Storing related data under a single key (e.g., user sessions, token storage)</li>
 *   <li>Atomic operations on hash fields</li>
 *   <li>Memory-efficient storage of structured data</li>
 * </ul>
 * 
 * @param <H> the type of hash keys
 * @param <HK> the type of hash field keys
 * @param <HV> the type of hash field values
 */
public class RedisHashCacheService<H, HK, HV> {
    private static final Logger log = LoggerFactory.getLogger(RedisHashCacheService.class);
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final HashOperations<String, HK, HV> hashOps;
    private final MeterRegistry meterRegistry;
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong evictions = new AtomicLong(0);

    public RedisHashCacheService(RedisTemplate<String, Object> redisTemplate, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.hashOps = redisTemplate.opsForHash();
        this.meterRegistry = meterRegistry;
        log.info("Redis hash cache service initialized");
    }

    /**
     * Put a value in a Redis hash with TTL.
     * 
     * @param hashKey the Redis hash key
     * @param fieldKey the field within the hash
     * @param value the value to store
     * @param ttl time-to-live for the entire hash
     */
    public void put(H hashKey, HK fieldKey, HV value, Duration ttl) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            String key = hashKey.toString();
            hashOps.put(key, fieldKey, value);
            
            if (ttl != null && !ttl.isZero()) {
                redisTemplate.expire(key, ttl.toMillis(), TimeUnit.MILLISECONDS);
            }
            
            meterRegistry.counter("cache.hash.put",
                    "provider", "redis",
                    "status", "success").increment();
                    
            log.debug("Stored hash value for key: {} field: {} with ttl: {}", hashKey, fieldKey, ttl);
        } catch (RedisConnectionFailureException e) {
            meterRegistry.counter("cache.hash.put",
                    "provider", "redis", 
                    "status", "error").increment();
            log.error("Redis connection failed for hash PUT key [{}] field [{}]: {}", hashKey, fieldKey, e.getMessage(), e);
            throw new CacheConnectionException("Failed to connect to Redis for hash PUT operation", 
                    hashKey.toString(), CacheException.CacheOperation.PUT, e);
        } catch (DataAccessException e) {
            meterRegistry.counter("cache.hash.put",
                    "provider", "redis",
                    "status", "error").increment();
            log.error("Redis hash PUT failed for key [{}] field [{}]: {}", hashKey, fieldKey, e.getMessage(), e);
            throw new CacheException("Failed to put value in hash cache", 
                    hashKey.toString(), CacheException.CacheOperation.PUT, e);
        } finally {
            sample.stop(Timer.builder("cache.hash.put.latency")
                    .tag("provider", "redis")
                    .register(meterRegistry));
        }
    }

    /**
     * Get a value from a hash field.
     */
    public HV get(H hashKey, HK fieldKey) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            HV value = hashOps.get(hashKey.toString(), fieldKey);
            
            if (value != null) {
                hits.incrementAndGet();
                meterRegistry.counter("cache.hash.hit", "provider", "redis").increment();
                log.debug("Hash cache hit for key: {} field: {}", hashKey, fieldKey);
            } else {
                misses.incrementAndGet();
                meterRegistry.counter("cache.hash.miss", "provider", "redis").increment();
                log.debug("Hash cache miss for key: {} field: {}", hashKey, fieldKey);
            }
            
            return value;
        } catch (RedisConnectionFailureException e) {
            misses.incrementAndGet();
            meterRegistry.counter("cache.hash.get",
                    "provider", "redis",
                    "status", "error").increment();
            log.error("Redis connection failed for hash GET key [{}] field [{}]: {}", hashKey, fieldKey, e.getMessage(), e);
            throw new CacheConnectionException("Failed to connect to Redis for hash GET operation", 
                    hashKey.toString(), CacheException.CacheOperation.GET, e);
        } catch (DataAccessException e) {
            misses.incrementAndGet();
            meterRegistry.counter("cache.hash.get",
                    "provider", "redis",
                    "status", "error").increment();
            log.error("Redis hash GET failed for key [{}] field [{}]: {}", hashKey, fieldKey, e.getMessage(), e);
            throw new CacheException("Failed to get value from hash cache", 
                    hashKey.toString(), CacheException.CacheOperation.GET, e);
        } finally {
            sample.stop(Timer.builder("cache.hash.get.latency")
                    .tag("provider", "redis")
                    .register(meterRegistry));
        }
    }

    /**
     * Remove a field from the hash.
     */
    public void remove(H hashKey, HK fieldKey) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            Long deleted = hashOps.delete(hashKey.toString(), fieldKey);
            
            if (deleted != null && deleted > 0) {
                evictions.incrementAndGet();
                meterRegistry.counter("cache.hash.eviction", "provider", "redis").increment();
            }
            
            log.debug("Removed hash field for key: {} field: {}", hashKey, fieldKey);
        } catch (RedisConnectionFailureException e) {
            meterRegistry.counter("cache.hash.remove",
                    "provider", "redis",
                    "status", "error").increment();
            log.error("Redis connection failed for hash DELETE key [{}] field [{}]: {}", hashKey, fieldKey, e.getMessage(), e);
            throw new CacheConnectionException("Failed to connect to Redis for hash REMOVE operation", 
                    hashKey.toString(), CacheException.CacheOperation.REMOVE, e);
        } catch (DataAccessException e) {
            meterRegistry.counter("cache.hash.remove",
                    "provider", "redis",
                    "status", "error").increment();
            log.error("Redis hash DELETE failed for key [{}] field [{}]: {}", hashKey, fieldKey, e.getMessage(), e);
            throw new CacheException("Failed to remove hash field", 
                    hashKey.toString(), CacheException.CacheOperation.REMOVE, e);
        } finally {
            sample.stop(Timer.builder("cache.hash.remove.latency")
                    .tag("provider", "redis")
                    .register(meterRegistry));
        }
    }

    /**
     * Check if a field exists in the hash.
     */
    public boolean containsKey(H hashKey, HK fieldKey) {
        try {
            return hashOps.hasKey(hashKey.toString(), fieldKey);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed for hash CONTAINS KEY check [{}] field [{}]: {}", 
                    hashKey, fieldKey, e.getMessage(), e);
            throw new CacheConnectionException("Failed to connect to Redis for hash CONTAINS_KEY operation", 
                    hashKey.toString(), CacheException.CacheOperation.CONTAINS_KEY, e);
        } catch (DataAccessException e) {
            log.error("Redis hash CONTAINS KEY check failed for key [{}] field [{}]: {}", 
                    hashKey, fieldKey, e.getMessage(), e);
            throw new CacheException("Failed to check hash field presence", 
                    hashKey.toString(), CacheException.CacheOperation.CONTAINS_KEY, e);
        }
    }

    /**
     * Get all fields and values from the hash.
     */
    public Map<HK, HV> getAll(H hashKey) {
        try {
            return hashOps.entries(hashKey.toString());
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed for hash GET ALL key [{}]: {}", hashKey, e.getMessage(), e);
            throw new CacheConnectionException("Failed to connect to Redis for hash GET_ALL operation", 
                    hashKey.toString(), CacheException.CacheOperation.GET_ALL, e);
        } catch (DataAccessException e) {
            log.error("Redis hash GET ALL failed for key [{}]: {}", hashKey, e.getMessage(), e);
            throw new CacheException("Failed to get all hash fields", 
                    hashKey.toString(), CacheException.CacheOperation.GET_ALL, e);
        }
    }

    /**
     * Atomically increment a hash field value.
     */
    public Long increment(H hashKey, HK fieldKey, long delta, Duration ttl) {
        try {
            String key = hashKey.toString();
            Long result = hashOps.increment(key, fieldKey, delta);
            
            // Set TTL only if this is the first increment (result equals delta)
            if (ttl != null && !ttl.isZero() && result.equals(delta)) {
                redisTemplate.expire(key, ttl.toMillis(), TimeUnit.MILLISECONDS);
            }
            
            log.debug("Incremented hash field key: {} field: {} by {}, new value: {}", 
                    hashKey, fieldKey, delta, result);
            return result;
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed for hash INCREMENT key [{}] field [{}]: {}", 
                    hashKey, fieldKey, e.getMessage(), e);
            throw new CacheConnectionException("Failed to connect to Redis for hash INCREMENT operation", 
                    hashKey.toString(), CacheException.CacheOperation.INCREMENT, e);
        } catch (DataAccessException e) {
            log.error("Redis hash INCREMENT failed for key [{}] field [{}]: {}", 
                    hashKey, fieldKey, e.getMessage(), e);
            throw new CacheException("Failed to increment hash field value", 
                    hashKey.toString(), CacheException.CacheOperation.INCREMENT, e);
        }
    }

    /**
     * Get cache statistics.
     */
    public CacheStatistics getStatistics() {
        long totalRequests = hits.get() + misses.get();
        double hitRate = totalRequests > 0 ? (double) hits.get() / totalRequests : 0.0;

        return CacheStatistics.builder()
                .namespace("redis-hash")
                .timestamp(Instant.now())
                .window(CacheStatistics.TimeWindow.ALL_TIME)
                .hitCount(hits.get())
                .missCount(misses.get())
                .hitRate(hitRate)
                .missRate(1.0 - hitRate)
                .evictionCount(evictions.get())
                .build();
    }

    public Long getHitCount() {
        return hits.get();
    }

    public Long getMissCount() {
        return misses.get();
    }

    public void resetMetrics() {
        hits.set(0);
        misses.set(0);
        evictions.set(0);
    }
}