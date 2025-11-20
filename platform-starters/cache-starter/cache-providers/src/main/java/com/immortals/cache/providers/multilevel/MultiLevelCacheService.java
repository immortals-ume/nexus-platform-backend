package com.immortals.cache.providers.multilevel;

import com.immortals.cache.core.CacheService;
import com.immortals.cache.core.CacheStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Multi-level cache service implementation combining L1 (local) and L2 (distributed) caches.
 * 
 * <p>Implements a two-tier caching strategy:
 * <ul>
 *   <li>L1: Fast local in-memory cache (e.g., Caffeine)</li>
 *   <li>L2: Distributed cache (e.g., Redis)</li>
 * </ul>
 * 
 * <p>Read strategy: Check L1 first, then L2, populate L1 on L2 hits
 * <p>Write strategy: Write-through to both L1 and L2
 * <p>Eviction strategy: Evict from both levels, publish eviction events for distributed invalidation
 * <p>Fallback strategy: Serve from L1 when L2 is unavailable
 * 
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of cached values
 * @since 2.0.0
 */
public class MultiLevelCacheService<K, V> implements CacheService<K, V> {
    
    private static final Logger log = LoggerFactory.getLogger(MultiLevelCacheService.class);

    private final CacheService<K, V> l1Cache;
    private final CacheService<K, V> l2Cache;
    private final EvictionPublisher evictionPublisher;
    private final String namespace;
    
    // Statistics tracking
    private final AtomicLong l1Hits = new AtomicLong(0);
    private final AtomicLong l1Misses = new AtomicLong(0);
    private final AtomicLong l2Hits = new AtomicLong(0);
    private final AtomicLong l2Misses = new AtomicLong(0);
    private final AtomicLong l2Failures = new AtomicLong(0);
    private final AtomicLong fallbackCount = new AtomicLong(0);

    public MultiLevelCacheService(CacheService<K, V> l1Cache, 
                                  CacheService<K, V> l2Cache,
                                  EvictionPublisher evictionPublisher,
                                  String namespace) {
        this.l1Cache = l1Cache;
        this.l2Cache = l2Cache;
        this.evictionPublisher = evictionPublisher;
        this.namespace = namespace;
        
        log.info("Multi-level cache service initialized for namespace: {} with L1 and L2", namespace);
    }

    // ========== Read Operations ==========

    /**
     * Get operation with L1-first lookup strategy and L2 fallback.
     * 
     * <p>Flow:
     * <ol>
     *   <li>Check L1 cache</li>
     *   <li>If L1 miss, check L2 cache</li>
     *   <li>If L2 hit, populate L1 cache</li>
     *   <li>If L2 fails, serve from L1 only (fallback mode)</li>
     * </ol>
     */
    @Override
    public Optional<V> get(K key) {
        // Try L1 first
        Optional<V> value = getFromL1(key);
        if (value.isPresent()) {
            l1Hits.incrementAndGet();
            log.debug("L1 cache hit for key: {} in namespace: {}", key, namespace);
            return value;
        }
        
        l1Misses.incrementAndGet();
        log.debug("L1 cache miss for key: {} in namespace: {}", key, namespace);
        
        // Try L2 with fallback handling
        try {
            value = getFromL2(key);
            if (value.isPresent()) {
                l2Hits.incrementAndGet();
                log.debug("L2 cache hit for key: {} in namespace: {}, populating L1", key, namespace);
                
                // Populate L1 on L2 hit
                populateL1(key, value.get());
                return value;
            }
            
            l2Misses.incrementAndGet();
            log.debug("L2 cache miss for key: {} in namespace: {}", key, namespace);
        } catch (Exception e) {
            l2Failures.incrementAndGet();
            fallbackCount.incrementAndGet();
            log.warn("L2 cache failure for key: {} in namespace: {}, falling back to L1 only. Error: {}", 
                    key, namespace, e.getMessage());
            

            return Optional.empty();
        }
        
        return Optional.empty();
    }

    private Optional<V> getFromL1(K key) {
        try {
            return l1Cache.get(key);
        } catch (Exception e) {
            log.error("L1 cache get failed for key: {} in namespace: {}", key, namespace, e);
            return Optional.empty();
        }
    }

    private Optional<V> getFromL2(K key) {
        return l2Cache.get(key);
    }

    private void populateL1(K key, V value) {
        try {
            l1Cache.put(key, value);
        } catch (Exception e) {
            log.error("Failed to populate L1 cache for key: {} in namespace: {}", key, namespace, e);
        }
    }

    // ========== Write Operations ==========

    /**
     * Put operation with write-through to both levels and fallback handling.
     * 
     * <p>Writes to both L1 and L2. If L2 fails, still writes to L1 (degraded mode).
     */
    @Override
    public void put(K key, V value) {
        // Write to L1 first (always succeeds or throws)
        try {
            l1Cache.put(key, value);
            log.debug("Stored value in L1 cache for key: {} in namespace: {}", key, namespace);
        } catch (Exception e) {
            log.error("Failed to put value in L1 cache for key: {} in namespace: {}", key, namespace, e);
            throw new RuntimeException("Failed to put value in L1 cache", e);
        }
        
        // Try to write to L2 with fallback handling
        try {
            l2Cache.put(key, value);
            log.debug("Stored value in L2 cache for key: {} in namespace: {}", key, namespace);
        } catch (Exception e) {
            l2Failures.incrementAndGet();
            fallbackCount.incrementAndGet();
            log.warn("L2 cache put failed for key: {} in namespace: {}, operating in degraded mode (L1 only). Error: {}", 
                    key, namespace, e.getMessage());
            // Continue - value is in L1
        }
    }

    /**
     * Put operation with TTL, write-through to both levels.
     */
    @Override
    public void put(K key, V value, Duration ttl) {
        // Write to L1 first
        try {
            l1Cache.put(key, value, ttl);
            log.debug("Stored value with TTL in L1 cache for key: {} in namespace: {}", key, namespace);
        } catch (Exception e) {
            log.error("Failed to put value with TTL in L1 cache for key: {} in namespace: {}", key, namespace, e);
            throw new RuntimeException("Failed to put value with TTL in L1 cache", e);
        }
        
        // Try to write to L2 with fallback handling
        try {
            l2Cache.put(key, value, ttl);
            log.debug("Stored value with TTL in L2 cache for key: {} in namespace: {}", key, namespace);
        } catch (Exception e) {
            l2Failures.incrementAndGet();
            fallbackCount.incrementAndGet();
            log.warn("L2 cache put with TTL failed for key: {} in namespace: {}, operating in degraded mode (L1 only). Error: {}", 
                    key, namespace, e.getMessage());
            // Continue - value is in L1
        }
    }

    // ========== Remove Operations ==========

    /**
     * Remove operation, evicts from both levels and publishes eviction event.
     */
    @Override
    public void remove(K key) {
        // Evict from L1
        try {
            l1Cache.remove(key);
            log.debug("Removed key from L1 cache: {} in namespace: {}", key, namespace);
        } catch (Exception e) {
            log.error("Failed to remove key from L1 cache: {} in namespace: {}", key, namespace, e);
        }
        
        // Try to evict from L2 with fallback handling
        try {
            l2Cache.remove(key);
            log.debug("Removed key from L2 cache: {} in namespace: {}", key, namespace);
            
            // Publish eviction event for distributed invalidation
            publishEviction(key);
        } catch (Exception e) {
            l2Failures.incrementAndGet();
            log.warn("L2 cache remove failed for key: {} in namespace: {}, L1 eviction completed. Error: {}", 
                    key, namespace, e.getMessage());
            // Continue - key is removed from L1
        }
    }

    /**
     * Clear operation, clears both levels.
     */
    @Override
    public void clear() {
        // Clear L1
        try {
            l1Cache.clear();
            log.info("Cleared L1 cache for namespace: {}", namespace);
        } catch (Exception e) {
            log.error("Failed to clear L1 cache for namespace: {}", namespace, e);
        }
        
        // Try to clear L2 with fallback handling
        try {
            l2Cache.clear();
            log.info("Cleared L2 cache for namespace: {}", namespace);
            
            // Publish clear all event
            publishClearAll();
        } catch (Exception e) {
            l2Failures.incrementAndGet();
            log.warn("L2 cache clear failed for namespace: {}, L1 clear completed. Error: {}", 
                    namespace, e.getMessage());
            // Continue - L1 is cleared
        }
    }

    // ========== Query Operations ==========

    /**
     * Check if key exists in either L1 or L2.
     */
    @Override
    public boolean containsKey(K key) {
        // Check L1 first
        try {
            if (l1Cache.containsKey(key)) {
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to check key presence in L1 cache: {} in namespace: {}", key, namespace, e);
        }
        
        // Check L2 with fallback handling
        try {
            return l2Cache.containsKey(key);
        } catch (Exception e) {
            l2Failures.incrementAndGet();
            log.warn("L2 cache containsKey failed for key: {} in namespace: {}, returning false. Error: {}", 
                    key, namespace, e.getMessage());
            return false;
        }
    }

    // ========== Batch Operations ==========

    @Override
    public void putAll(Map<K, V> entries) {
        // Write to L1
        try {
            l1Cache.putAll(entries);
            log.debug("Stored {} entries in L1 cache for namespace: {}", entries.size(), namespace);
        } catch (Exception e) {
            log.error("Failed to put multiple entries in L1 cache for namespace: {}", namespace, e);
            throw new RuntimeException("Failed to put multiple entries in L1 cache", e);
        }
        
        // Try to write to L2 with fallback handling
        try {
            l2Cache.putAll(entries);
            log.debug("Stored {} entries in L2 cache for namespace: {}", entries.size(), namespace);
        } catch (Exception e) {
            l2Failures.incrementAndGet();
            fallbackCount.incrementAndGet();
            log.warn("L2 cache putAll failed for namespace: {}, operating in degraded mode (L1 only). Error: {}", 
                    namespace, e.getMessage());
            // Continue - entries are in L1
        }
    }

    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        Map<K, V> result = new HashMap<>();
        
        // Get all from L1 first
        try {
            Map<K, V> l1Results = l1Cache.getAll(keys);
            result.putAll(l1Results);
            
            // If all keys found in L1, return
            if (l1Results.size() == keys.size()) {
                l1Hits.addAndGet(keys.size());
                return result;
            }
            
            l1Hits.addAndGet(l1Results.size());
            l1Misses.addAndGet(keys.size() - l1Results.size());
        } catch (Exception e) {
            log.error("Failed to get multiple entries from L1 cache for namespace: {}", namespace, e);
        }
        
        // Get missing keys from L2 with fallback handling
        try {
            Map<K, V> l2Results = l2Cache.getAll(keys);
            
            // Populate L1 with L2 results
            if (l2Results != null && !l2Results.isEmpty()) {
                try {
                    l1Cache.putAll(l2Results);
                    l2Hits.addAndGet(l2Results.size());
                } catch (Exception e) {
                    log.error("Failed to populate L1 cache with L2 results for namespace: {}", namespace, e);
                }
            }
            
            result.putAll(l2Results);
            l2Misses.addAndGet(keys.size() - result.size());
        } catch (Exception e) {
            l2Failures.incrementAndGet();
            fallbackCount.incrementAndGet();
            log.warn("L2 cache getAll failed for namespace: {}, returning L1 results only. Error: {}", 
                    namespace, e.getMessage());
            // Continue - return what we have from L1
        }
        
        return result;
    }

    // ========== Conditional Operations ==========

    @Override
    public boolean putIfAbsent(K key, V value) {
        // Check L1 first
        try {
            if (l1Cache.containsKey(key)) {
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to check key presence in L1 cache: {} in namespace: {}", key, namespace, e);
        }
        
        // Try L2 with fallback handling
        try {
            boolean success = l2Cache.putIfAbsent(key, value);
            if (success) {
                // Also put in L1
                try {
                    l1Cache.put(key, value);
                } catch (Exception e) {
                    log.error("Failed to populate L1 cache after putIfAbsent for key: {} in namespace: {}", 
                            key, namespace, e);
                }
            }
            return success;
        } catch (Exception e) {
            l2Failures.incrementAndGet();
            fallbackCount.incrementAndGet();
            log.warn("L2 cache putIfAbsent failed for key: {} in namespace: {}, attempting L1 only. Error: {}", 
                    key, namespace, e.getMessage());
            
            // Fallback: try L1 only
            try {
                return l1Cache.putIfAbsent(key, value);
            } catch (Exception ex) {
                log.error("L1 cache putIfAbsent also failed for key: {} in namespace: {}", key, namespace, ex);
                return false;
            }
        }
    }

    @Override
    public boolean putIfAbsent(K key, V value, Duration ttl) {
        // Check L1 first
        try {
            if (l1Cache.containsKey(key)) {
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to check key presence in L1 cache: {} in namespace: {}", key, namespace, e);
        }
        
        // Try L2 with fallback handling
        try {
            boolean success = l2Cache.putIfAbsent(key, value, ttl);
            if (success) {
                // Also put in L1
                try {
                    l1Cache.put(key, value, ttl);
                } catch (Exception e) {
                    log.error("Failed to populate L1 cache after putIfAbsent with TTL for key: {} in namespace: {}", 
                            key, namespace, e);
                }
            }
            return success;
        } catch (Exception e) {
            l2Failures.incrementAndGet();
            fallbackCount.incrementAndGet();
            log.warn("L2 cache putIfAbsent with TTL failed for key: {} in namespace: {}, attempting L1 only. Error: {}", 
                    key, namespace, e.getMessage());
            
            // Fallback: try L1 only
            try {
                return l1Cache.putIfAbsent(key, value, ttl);
            } catch (Exception ex) {
                log.error("L1 cache putIfAbsent with TTL also failed for key: {} in namespace: {}", key, namespace, ex);
                return false;
            }
        }
    }

    // ========== Atomic Operations (Delegated to L2) ==========

    @Override
    public Long increment(K key, long delta) {
        try {
            // Atomic operations only supported in L2 (Redis)
            Long result = l2Cache.increment(key, delta);
            
            // Invalidate L1 to ensure consistency
            try {
                l1Cache.remove(key);
            } catch (Exception e) {
                log.error("Failed to invalidate L1 cache after increment for key: {} in namespace: {}", 
                        key, namespace, e);
            }
            
            return result;
        } catch (Exception e) {
            l2Failures.incrementAndGet();
            log.error("L2 cache increment failed for key: {} in namespace: {}", key, namespace, e);
            throw new UnsupportedOperationException("Increment operation requires L2 cache (Redis)", e);
        }
    }

    @Override
    public Long decrement(K key, long delta) {
        try {
            // Atomic operations only supported in L2 (Redis)
            Long result = l2Cache.decrement(key, delta);
            
            // Invalidate L1 to ensure consistency
            try {
                l1Cache.remove(key);
            } catch (Exception e) {
                log.error("Failed to invalidate L1 cache after decrement for key: {} in namespace: {}", 
                        key, namespace, e);
            }
            
            return result;
        } catch (Exception e) {
            l2Failures.incrementAndGet();
            log.error("L2 cache decrement failed for key: {} in namespace: {}", key, namespace, e);
            throw new UnsupportedOperationException("Decrement operation requires L2 cache (Redis)", e);
        }
    }

    // ========== Eviction Publishing ==========

    private void publishEviction(K key) {
        if (evictionPublisher != null) {
            try {
                evictionPublisher.publishKeyEviction(namespace, key.toString());
            } catch (Exception e) {
                log.error("Failed to publish eviction event for key: {} in namespace: {}", key, namespace, e);
            }
        }
    }

    private void publishClearAll() {
        if (evictionPublisher != null) {
            try {
                evictionPublisher.publishClearAll(namespace);
            } catch (Exception e) {
                log.error("Failed to publish clear all event for namespace: {}", namespace, e);
            }
        }
    }

    // ========== Statistics ==========

    @Override
    public CacheStatistics getStatistics() {
        long totalHits = l1Hits.get() + l2Hits.get();
        long totalMisses = l1Misses.get() + l2Misses.get();
        long totalRequests = totalHits + totalMisses;
        double hitRate = totalRequests > 0 ? (double) totalHits / totalRequests : 0.0;
        double missRate = totalRequests > 0 ? (double) totalMisses / totalRequests : 0.0;
        
        return CacheStatistics.builder()
                .namespace(namespace)
                .timestamp(Instant.now())
                .window(CacheStatistics.TimeWindow.ALL_TIME)
                .hitCount(totalHits)
                .missCount(totalMisses)
                .hitRate(hitRate)
                .missRate(missRate)
                .currentSize(0L) // Would need to query both caches
                .maxSize(0L)
                .fillPercentage(0.0)
                .evictionCount(0L)
                .evictionRate(0.0)
                .avgGetLatency(0.0)
                .p50GetLatency(0.0)
                .p95GetLatency(0.0)
                .p99GetLatency(0.0)
                .maxGetLatency(0.0)
                .avgPutLatency(0.0)
                .p50PutLatency(0.0)
                .p95PutLatency(0.0)
                .p99PutLatency(0.0)
                .avgRemoveLatency(0.0)
                .getOpsPerSecond(0.0)
                .putOpsPerSecond(0.0)
                .removeOpsPerSecond(0.0)
                .memoryUsage(null)
                .maxMemory(null)
                .build();
    }
    
    /**
     * Get L1-specific statistics.
     */
    public CacheStatistics getL1Statistics() {
        long totalRequests = l1Hits.get() + l1Misses.get();
        double hitRate = totalRequests > 0 ? (double) l1Hits.get() / totalRequests : 0.0;
        
        return CacheStatistics.builder()
                .namespace(namespace + "-L1")
                .timestamp(Instant.now())
                .window(CacheStatistics.TimeWindow.ALL_TIME)
                .hitCount(l1Hits.get())
                .missCount(l1Misses.get())
                .hitRate(hitRate)
                .missRate(1.0 - hitRate)
                .build();
    }
    
    /**
     * Get L2-specific statistics.
     */
    public CacheStatistics getL2Statistics() {
        long totalRequests = l2Hits.get() + l2Misses.get();
        double hitRate = totalRequests > 0 ? (double) l2Hits.get() / totalRequests : 0.0;
        
        return CacheStatistics.builder()
                .namespace(namespace + "-L2")
                .timestamp(Instant.now())
                .window(CacheStatistics.TimeWindow.ALL_TIME)
                .hitCount(l2Hits.get())
                .missCount(l2Misses.get())
                .hitRate(hitRate)
                .missRate(1.0 - hitRate)
                .build();
    }
    
    /**
     * Get fallback statistics.
     */
    public long getL2FailureCount() {
        return l2Failures.get();
    }
    
    public long getFallbackCount() {
        return fallbackCount.get();
    }
}
