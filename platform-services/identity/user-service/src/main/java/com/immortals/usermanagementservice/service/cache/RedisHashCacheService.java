package com.immortals.usermanagementservice.service.cache;

import com.immortals.usermanagementservice.manager.TokenLockManager;
import com.immortals.usermanagementservice.service.exception.CacheException;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class RedisHashCacheService<H, HK, HV> implements CacheService<H, HK, HV> {

    private final RedisTemplate<H, Object> redisTemplate;
    private final HashOperations<H, HK, HV> hashOps;
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final ReentrantLock metricsLock = new ReentrantLock();
    private final TokenLockManager tokenLockManager;

    public RedisHashCacheService(@Qualifier("redisTemplate") RedisTemplate<H, Object> redisTemplate,
                                 TokenLockManager tokenLockManager) {
        this.redisTemplate = redisTemplate;
        this.hashOps = redisTemplate.opsForHash();
        this.tokenLockManager = tokenLockManager;
    }

    @Override
    public void put(H hashKey, HK fieldKey, HV value, Duration ttl, String lockingKey) throws CacheException {
        try {
            if (lockingKey != null) tokenLockManager.acquireWrite(lockingKey);
            hashOps.put(hashKey, fieldKey, value);
            if (ttl != null) redisTemplate.expire(hashKey, ttl);
        } catch (DataAccessException | InterruptedException e) {
            log.error("Redis Hash PUT failed for hash [{}] field [{}]: {}", hashKey, fieldKey, e.getMessage(), e);
            throw new CacheException("Failed to put value in hash cache", e);
        } finally {
            if (lockingKey != null) tokenLockManager.releaseWrite(lockingKey);
        }
    }

    @Override
    public HV get(H hashKey, HK fieldKey, String lockingKey) {
        try {
            if (lockingKey != null) tokenLockManager.acquireRead(lockingKey, Duration.ofSeconds(1));
            HV value = hashOps.get(hashKey, fieldKey);
            if (value != null) hits.incrementAndGet();
            else misses.incrementAndGet();
            return value;
        } catch (DataAccessException | InterruptedException e) {
            log.error("Redis Hash GET failed for hash [{}] field [{}]: {}", hashKey, fieldKey, e.getMessage(), e);
            throw new CacheException("Failed to get value from hash cache", e);
        } finally {
            if (lockingKey != null) tokenLockManager.releaseRead(lockingKey);
        }
    }

    @Override
    public void remove(H hashKey, HK fieldKey, String lockingKey) {
        try {
            if (lockingKey != null) tokenLockManager.acquireWrite(lockingKey);
            hashOps.delete(hashKey, fieldKey);
        } catch (DataAccessException | InterruptedException e) {
            log.error("Redis Hash DELETE failed for hash [{}] field [{}]: {}", hashKey, fieldKey, e.getMessage(), e);
            throw new CacheException("Failed to remove hash field", e);
        } finally {
            if (lockingKey != null) tokenLockManager.releaseWrite(lockingKey);
        }
    }

    @Override
    public boolean containsKey(H hashKey, HK fieldKey, String lockingKey) {
        try {
            if (lockingKey != null) tokenLockManager.acquireRead(lockingKey, Duration.ofSeconds(1));
            boolean exists = hashOps.hasKey(hashKey, fieldKey);
            if (exists) hits.incrementAndGet();
            else misses.incrementAndGet();
            return exists;
        } catch (DataAccessException | InterruptedException e) {
            log.error("Redis Hash CONTAINS KEY check failed for hash [{}] field [{}]: {}", hashKey, fieldKey, e.getMessage(), e);
            throw new CacheException("Failed to check hash field presence", e);
        } finally {
            if (lockingKey != null) tokenLockManager.releaseRead(lockingKey);
        }
    }

    @Override
    public Map<HK, HV> getAll(H hashKey, String lockingKey) {
        try {
            if (lockingKey != null) tokenLockManager.acquireRead(lockingKey, Duration.ofSeconds(1));
            Map<HK, HV> entries = hashOps.entries(hashKey);
            if (entries == null || entries.isEmpty()) misses.incrementAndGet();
            else hits.incrementAndGet();
            return entries;
        } catch (DataAccessException | InterruptedException e) {
            log.error("Redis Hash GET ALL failed for hash [{}]: {}", hashKey, e.getMessage(), e);
            throw new CacheException("Failed to get all hash fields", e);
        } finally {
            if (lockingKey != null) tokenLockManager.releaseRead(lockingKey);
        }
    }

    @Override
    public Long increment(H hashKey, HK fieldKey, String lockingKey, Duration ttl) {
        try {
            if (lockingKey != null) tokenLockManager.acquireWrite(lockingKey);
            Long attempts = redisTemplate.opsForHash().increment(hashKey, fieldKey, 1L);
            if (attempts == 1 && ttl != null) {
                redisTemplate.expire(hashKey, ttl);
            }
            return attempts;
        } catch (DataAccessException | InterruptedException e) {
            log.error("Redis Hash INCREMENT failed for hash [{}] field [{}]: {}", hashKey, fieldKey, e.getMessage(), e);
            throw new CacheException("Failed to update hash fields", e);
        } finally {
            if (lockingKey != null) tokenLockManager.releaseWrite(lockingKey);
        }
    }

    @Override
    public Long getHitCount() {
        return hits.get();
    }

    @Override
    public Long getMissCount() {
        return misses.get();
    }

    public void resetMetrics() {
        metricsLock.lock();
        try {
            hits.set(0);
            misses.set(0);
        } finally {
            metricsLock.unlock();
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down RedisHashCacheService. Hits: {}, Misses: {}", hits.get(), misses.get());
    }
}
