package com.immortals.usermanagementservice.manager;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class RWLockTokenLockManager implements TokenLockManager {

    private final ConcurrentHashMap<String, ReadWriteLock> locks = new ConcurrentHashMap<>();

    private ReadWriteLock getLock(String key) {
        return locks.computeIfAbsent(key, k -> new ReentrantReadWriteLock());
    }

    @Override
    public void acquireRead(String key, Duration duration) {
        try {
            boolean acquired = getLock(key).readLock().tryLock(duration.toMillis(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new RuntimeException("Failed to acquire read lock within timeout for key: " + key);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Read lock interrupted for key: " + key, e);
        }
    }

    @Override
    public void releaseRead(String key) {
        getLock(key).readLock().unlock();
    }

    @Override
    public void acquireWrite(String key) {
        acquireWrite(key, Duration.ofSeconds(1));
    }

    public void acquireWrite(String key, Duration timeout) {
        try {
            boolean acquired = getLock(key).writeLock().tryLock(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new RuntimeException("Failed to acquire write lock within timeout for key: " + key);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Write lock interrupted for key: " + key, e);
        }
    }

    @Override
    public void releaseWrite(String key) {
        getLock(key).writeLock().unlock();
    }
}
