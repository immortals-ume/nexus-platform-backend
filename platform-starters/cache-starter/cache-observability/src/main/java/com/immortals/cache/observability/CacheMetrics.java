package com.immortals.cache.observability;

import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive metrics collection for cache operations using Micrometer.
 * Tracks hits, misses, evictions, operation latencies, and calculates hit rates.
 * 
 * @since 2.0.0
 */
@Slf4j
public class CacheMetrics {
    
    private final MeterRegistry registry;
    private final String cacheName;
    private final String namespace;
    
    // Counters
    private final Counter hits;
    private final Counter misses;
    private final Counter evictions;
    private final Counter puts;
    private final Counter removes;
    private final Counter errors;
    
    // Timers for latency tracking
    private final Timer getTimer;
    private final Timer putTimer;
    private final Timer removeTimer;
    private final Timer getAllTimer;
    private final Timer putAllTimer;
    
    // Gauges
    private final AtomicLong cacheSize;
    private final AtomicLong memoryUsage;
    
    /**
     * Creates a new CacheMetrics instance.
     * 
     * @param registry the Micrometer registry
     * @param cacheName the name of the cache
     * @param namespace the namespace of the cache
     */
    public CacheMetrics(MeterRegistry registry, String cacheName, String namespace) {
        this.registry = registry;
        this.cacheName = cacheName;
        this.namespace = namespace;
        
        Tags tags = Tags.of(
            "cache", cacheName,
            "namespace", namespace
        );
        
        // Initialize counters
        this.hits = Counter.builder("cache.hits")
            .description("Number of cache hits")
            .tags(tags)
            .register(registry);
            
        this.misses = Counter.builder("cache.misses")
            .description("Number of cache misses")
            .tags(tags)
            .register(registry);
            
        this.evictions = Counter.builder("cache.evictions")
            .description("Number of cache evictions")
            .tags(tags)
            .register(registry);
            
        this.puts = Counter.builder("cache.puts")
            .description("Number of cache put operations")
            .tags(tags)
            .register(registry);
            
        this.removes = Counter.builder("cache.removes")
            .description("Number of cache remove operations")
            .tags(tags)
            .register(registry);
            
        this.errors = Counter.builder("cache.errors")
            .description("Number of cache operation errors")
            .tags(tags)
            .register(registry);
        
        // Initialize timers
        this.getTimer = Timer.builder("cache.get")
            .description("Cache get operation latency")
            .tags(tags)
            .register(registry);
            
        this.putTimer = Timer.builder("cache.put")
            .description("Cache put operation latency")
            .tags(tags)
            .register(registry);
            
        this.removeTimer = Timer.builder("cache.remove")
            .description("Cache remove operation latency")
            .tags(tags)
            .register(registry);
            
        this.getAllTimer = Timer.builder("cache.getAll")
            .description("Cache getAll operation latency")
            .tags(tags)
            .register(registry);
            
        this.putAllTimer = Timer.builder("cache.putAll")
            .description("Cache putAll operation latency")
            .tags(tags)
            .register(registry);
        
        // Initialize gauges
        this.cacheSize = new AtomicLong(0);
        Gauge.builder("cache.size", cacheSize, AtomicLong::get)
            .description("Current number of entries in cache")
            .tags(tags)
            .register(registry);
            
        this.memoryUsage = new AtomicLong(0);
        Gauge.builder("cache.memory.usage", memoryUsage, AtomicLong::get)
            .description("Estimated memory usage in bytes")
            .tags(tags)
            .baseUnit("bytes")
            .register(registry);
        
        // Register hit rate gauge
        Gauge.builder("cache.hit.rate", this, CacheMetrics::getHitRate)
            .description("Cache hit rate (0.0 to 1.0)")
            .tags(tags)
            .register(registry);
    }
    
    /**
     * Records a cache hit.
     */
    public void recordHit() {
        hits.increment();
        log.debug("Cache hit recorded for cache: {}, namespace: {}", cacheName, namespace);
    }
    
    /**
     * Records a cache miss.
     */
    public void recordMiss() {
        misses.increment();
        log.debug("Cache miss recorded for cache: {}, namespace: {}", cacheName, namespace);
    }
    
    /**
     * Records a cache eviction.
     */
    public void recordEviction() {
        evictions.increment();
        log.debug("Cache eviction recorded for cache: {}, namespace: {}", cacheName, namespace);
    }
    
    /**
     * Records a cache put operation.
     */
    public void recordPut() {
        puts.increment();
    }
    
    /**
     * Records a cache remove operation.
     */
    public void recordRemove() {
        removes.increment();
    }
    
    /**
     * Records a cache operation error.
     */
    public void recordError() {
        errors.increment();
        log.warn("Cache error recorded for cache: {}, namespace: {}", cacheName, namespace);
    }
    
    /**
     * Records the latency of a get operation.
     * 
     * @param duration the duration of the operation
     */
    public void recordGetLatency(Duration duration) {
        getTimer.record(duration);
    }
    
    /**
     * Records the latency of a put operation.
     * 
     * @param duration the duration of the operation
     */
    public void recordPutLatency(Duration duration) {
        putTimer.record(duration);
    }
    
    /**
     * Records the latency of a remove operation.
     * 
     * @param duration the duration of the operation
     */
    public void recordRemoveLatency(Duration duration) {
        removeTimer.record(duration);
    }
    
    /**
     * Records the latency of a getAll operation.
     * 
     * @param duration the duration of the operation
     */
    public void recordGetAllLatency(Duration duration) {
        getAllTimer.record(duration);
    }
    
    /**
     * Records the latency of a putAll operation.
     * 
     * @param duration the duration of the operation
     */
    public void recordPutAllLatency(Duration duration) {
        putAllTimer.record(duration);
    }
    
    /**
     * Updates the current cache size.
     * 
     * @param size the current size
     */
    public void updateSize(long size) {
        cacheSize.set(size);
    }
    
    /**
     * Updates the estimated memory usage.
     * 
     * @param bytes the memory usage in bytes
     */
    public void updateMemoryUsage(long bytes) {
        memoryUsage.set(bytes);
    }
    
    /**
     * Calculates and returns the current hit rate.
     * 
     * @return hit rate between 0.0 and 1.0
     */
    public double getHitRate() {
        double totalHits = hits.count();
        double totalMisses = misses.count();
        double total = totalHits + totalMisses;
        
        if (total == 0) {
            return 0.0;
        }
        
        return totalHits / total;
    }
    
    /**
     * Gets the total number of hits.
     * 
     * @return hit count
     */
    public long getHitCount() {
        return (long) hits.count();
    }
    
    /**
     * Gets the total number of misses.
     * 
     * @return miss count
     */
    public long getMissCount() {
        return (long) misses.count();
    }
    
    /**
     * Gets the total number of evictions.
     * 
     * @return eviction count
     */
    public long getEvictionCount() {
        return (long) evictions.count();
    }
    
    /**
     * Gets the average get operation latency.
     * 
     * @return average latency in milliseconds
     */
    public double getAverageGetLatency() {
        return getTimer.mean(TimeUnit.MILLISECONDS);
    }
    
    /**
     * Gets the average put operation latency.
     * 
     * @return average latency in milliseconds
     */
    public double getAveragePutLatency() {
        return putTimer.mean(TimeUnit.MILLISECONDS);
    }
    
    /**
     * Gets the current cache size.
     * 
     * @return cache size
     */
    public long getCurrentSize() {
        return cacheSize.get();
    }
    
    /**
     * Gets the cache name.
     * 
     * @return cache name
     */
    public String getCacheName() {
        return cacheName;
    }
    
    /**
     * Gets the namespace.
     * 
     * @return namespace
     */
    public String getNamespace() {
        return namespace;
    }
}
