package com.immortals.cache.providers.redis;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Helper class for stampede protection using distributed locks.
 * 
 * <p>This implementation prevents cache stampede by:
 * <ul>
 *   <li>Using distributed locks to serialize cache regeneration for the same key</li>
 *   <li>Double-checking the cache after acquiring the lock to avoid redundant computation</li>
 *   <li>Handling computation failures gracefully with proper exception propagation</li>
 *   <li>Supporting configurable computation timeout to prevent hanging operations</li>
 *   <li>Tracking comprehensive metrics for monitoring and alerting</li>
 * </ul>
 * 
 * <p>Implements double-check locking pattern:
 * <ol>
 *   <li>Check cache (fast path)</li>
 *   <li>If miss, acquire distributed lock</li>
 *   <li>Double-check cache after acquiring lock</li>
 *   <li>If still missing, compute value with timeout protection</li>
 * </ol>
 * 
 * <p>Requirement: 5.4
 * 
 * <p>Based on existing DefaultStampedeProtectionService implementation from src-783
 */
public class StampedeProtectionHelper {
    private static final Logger log = LoggerFactory.getLogger(StampedeProtectionHelper.class);


    private static final String LOCK_PREFIX = "cache:stampede:";

    private final RedissonClient redissonClient;
    private final Duration lockTimeout;
    private final Duration computationTimeout;
    private final Counter stampedeActivated;
    private final Counter doubleCheckHits;
    private final Counter computationSuccess;
    private final Counter computationFailure;
    private final Counter computationTime;
    private final Timer lockWaitTimer;
    private final Timer computationTimer;
    private final Timer totalTimer;
    private final ExecutorService executorService;

    public StampedeProtectionHelper(
            RedissonClient redissonClient,
            Duration lockTimeout,
            Duration computationTimeout,
            MeterRegistry meterRegistry) {
        this.redissonClient = redissonClient;
        this.lockTimeout = lockTimeout;
        this.computationTimeout = computationTimeout;

        this.stampedeActivated = Counter.builder("cache.stampede.protection.activated")
                .description("Number of times stampede protection was activated")
                .register(meterRegistry);

        this.doubleCheckHits = Counter.builder("cache.stampede.protection.double_check_hit")
                .description("Number of times double-check found cached value")
                .register(meterRegistry);

        this.computationSuccess = Counter.builder("cache.stampede.protection.computation_success")
                .description("Number of successful value computations")
                .register(meterRegistry);

        this.computationFailure = Counter.builder("cache.stampede.protection.computation_failure")
                .description("Number of failed value computations")
                .register(meterRegistry);

        this.computationTime = Counter.builder("cache.stampede.protection.computation_timeout")
                .description("Number of computation timeouts")
                .register(meterRegistry);

        this.lockWaitTimer = Timer.builder("cache.stampede.lock.wait")
                .description("Time spent waiting for stampede protection lock")
                .register(meterRegistry);

        this.computationTimer = Timer.builder("cache.stampede.computation")
                .description("Time spent computing values")
                .register(meterRegistry);

        this.totalTimer = Timer.builder("cache.stampede.total")
                .description("Total time for stampede-protected operations")
                .register(meterRegistry);

        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setName("stampede-protection-" + thread.getName());
            thread.setDaemon(true);
            return thread;
        });

        log.info("Stampede protection helper initialized with lock timeout: {}, computation timeout: {}",
                lockTimeout, computationTimeout);
    }

    /**
     * Execute a cache operation with stampede protection.
     * 
     * @param key the cache key
     * @param cacheCheck supplier to check if value exists in cache
     * @param valueLoader function to compute the value if not cached
     * @param cacheSetter consumer to store the computed value in cache
     * @param <K> key type
     * @param <V> value type
     * @return the cached or computed value
     */
    public <K, V> Optional<V> executeWithStampedeProtection(
            K key,
            Supplier<Optional<V>> cacheCheck,
            Function<K, V> valueLoader,
            java.util.function.Consumer<V> cacheSetter) {

        Timer.Sample overallSample = Timer.start();

        try {
            Optional<V> cached = cacheCheck.get();
            if (cached.isPresent()) {
                log.debug("Cache hit for key '{}', no stampede protection needed", key);
                return cached;
            }

            stampedeActivated.increment();
            log.debug("Cache miss for key '{}', activating stampede protection", key);

            String lockKey = LOCK_PREFIX + key;
            RLock lock = redissonClient.getLock(lockKey);

            Timer.Sample lockWaitSample = Timer.start();

            try {
                boolean acquired = lock.tryLock(lockTimeout.toMillis(), TimeUnit.MILLISECONDS);

                if (!acquired) {
                    log.warn("Failed to acquire stampede protection lock for key '{}' within {}ms",
                            key, lockTimeout.toMillis());
                    return Optional.empty();
                }

                lockWaitSample.stop(lockWaitTimer);

                try {
                    cached = cacheCheck.get();
                    if (cached.isPresent()) {
                        doubleCheckHits.increment();
                        log.debug("Double-check hit for key '{}', another thread computed the value", key);
                        return cached;
                    }

                    log.debug("Computing value for key '{}' with stampede protection", key);
                    V value = computeWithTimeout(key, valueLoader);

                    if (value != null) {
                        cacheSetter.accept(value);
                        computationSuccess.increment();
                        log.debug("Successfully computed and cached value for key '{}'", key);
                        return Optional.of(value);
                    } else {
                        log.warn("Value loader returned null for key '{}', not caching", key);
                        return Optional.empty();
                    }

                } finally {
                    lock.unlock();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting for stampede protection lock for key '{}'", key);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error in stampede protection for key '{}'", key, e);
            return Optional.empty();
        } finally {
            overallSample.stop(totalTimer);
        }
    }

    /**
     * Computes a value with timeout protection to prevent hanging operations.
     * 
     * @param key the cache key
     * @param valueLoader the function to compute the value
     * @return the computed value
     * @throws RedisCacheException if computation times out or fails
     */
    private <K, V> V computeWithTimeout(K key, Function<K, V> valueLoader) {
        Timer.Sample computationSample = Timer.start();

        Future<V> future = executorService.submit(() -> {
            try {
                return valueLoader.apply(key);
            } catch (Exception e) {
                log.error("Error computing value for key '{}'", key, e);
                throw new RedisCacheException("Failed to compute value for key: " + key, e);
            }
        });

        try {
            V result = future.get(computationTimeout.toMillis(), TimeUnit.MILLISECONDS);
            computationSample.stop(computationTimer);
            return result;

        } catch (TimeoutException e) {
            future.cancel(true);
            computationTime.increment();
            log.error("Computation timeout for key '{}' after {} ms", key, computationTimeout.toMillis());
            throw new RedisCacheException(
                    String.format("Computation timeout for key '%s' after %d ms", key, computationTimeout.toMillis()), e);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            log.error("Computation interrupted for key '{}'", key);
            throw new RedisCacheException("Computation interrupted for key: " + key, e);

        } catch (ExecutionException e) {
            computationFailure.increment();
            Throwable cause = e.getCause();
            log.error("Computation failed for key '{}'", key, cause);
            throw new RedisCacheException("Computation failed for key: " + key, cause);
        } finally {
            computationSample.stop(computationTimer);
        }
    }
}
