package com.immortals.authapp.service.cache;

import com.immortals.authapp.manager.TokenLockManager;
import com.immortals.authapp.service.exception.CacheException;
import jakarta.annotation.PreDestroy;
import jakarta.validation.constraints.NotNull;
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

    public RedisHashCacheService(@Qualifier("redisTemplate") RedisTemplate<H, Object> redisTemplate, TokenLockManager tokenLockManager) {
        this.redisTemplate = redisTemplate;
        this.hashOps = redisTemplate.opsForHash();
        this.tokenLockManager = tokenLockManager;
    }

    @Override
    public void put(H key, HK hashKey, HV value, Duration ttl, @NotNull String lockingKey) throws CacheException {
        try {
            if (lockingKey != null) tokenLockManager.acquireWrite(lockingKey);
            hashOps.put(key, hashKey, value);
            if (ttl != null) redisTemplate.expire(key, ttl);
        } catch (DataAccessException | InterruptedException e) {
            log.error("Redis Hash PUT failed for key [{}] field [{}]: {}", key, hashKey, e.getMessage(), e);
            throw new CacheException("Failed to put value in hash cache", e);
        } finally {
            if (lockingKey != null) tokenLockManager.releaseWrite(lockingKey);
        }
    }

    @Override
    public HV get(H key, HK hashKey, String lockingKey) {
        try {
            if (lockingKey != null) tokenLockManager.acquireRead(lockingKey, Duration.ofSeconds(1));
            HV value = hashOps.get(key, hashKey);
            if (value != null) hits.incrementAndGet();
            else misses.incrementAndGet();
            return value;
        } catch (DataAccessException | InterruptedException e) {
            log.error("Redis Hash GET failed for key [{}] field [{}]: {}", key, hashKey, e.getMessage(), e);
            throw new CacheException("Failed to get value from hash cache", e);
        } finally {
            if (lockingKey != null) tokenLockManager.releaseRead(lockingKey);
        }
    }

    @Override
    public void remove(H key, HK hashKey, String lockingKey) {
        try {
            if (lockingKey != null) tokenLockManager.acquireRead(lockingKey, Duration.ofSeconds(1));
            hashOps.delete(key, hashKey);
        } catch (DataAccessException | InterruptedException e) {
            log.error("Redis Hash DELETE failed for key [{}] field [{}]: {}", key, hashKey, e.getMessage(), e);
            throw new CacheException("Failed to remove hash field", e);
        } finally {
            if (lockingKey != null) tokenLockManager.releaseRead(lockingKey);
        }
    }

    @Override
    public boolean containsKey(H key, HK hashKey, String lockingKey) {
        try {
            if (lockingKey != null) tokenLockManager.acquireRead(lockingKey, Duration.ofSeconds(1));
            return hashOps.hasKey(key, hashKey);
        } catch (DataAccessException | InterruptedException e) {
            log.error("Redis Hash CONTAINS KEY check failed for key [{}] field [{}]: {}", key, hashKey, e.getMessage(), e);
            throw new CacheException("Failed to check hash field presence", e);
        } finally {
            if (lockingKey != null) tokenLockManager.releaseRead(lockingKey);
        }
    }

    @Override
    public Map<HK, HV> getAll(H key, String lockingKey) {
        try {
            if (lockingKey != null) tokenLockManager.acquireRead(lockingKey, Duration.ofSeconds(1));
            return hashOps.entries(key);
        } catch (DataAccessException | InterruptedException e) {
            log.error("Redis Hash GET ALL failed for key [{}]: {}", key, e.getMessage(), e);
            throw new CacheException("Failed to get all hash fields", e);
        } finally {
            if (lockingKey != null) tokenLockManager.releaseRead(lockingKey);
        }
    }

    @Override
    public Long increment(H hashKey, HV fieldKey, String lockingKey, Duration ttl) {
        try {
            if (lockingKey != null) tokenLockManager.acquireRead(lockingKey, Duration.ofSeconds(1));
            Long attempts = redisTemplate.opsForHash()
                    .increment(hashKey, fieldKey, 1L);
            if (attempts == 1) {
                redisTemplate.expire(hashKey, ttl);
            }
            return attempts;
        } catch (DataAccessException | InterruptedException e) {
            throw new CacheException("Failed to update  hash fields", e);
        } finally {
            if (lockingKey != null) tokenLockManager.releaseRead(lockingKey);
        }
    }


    public Long getHitCount() {
        return hits.get();
    }

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
