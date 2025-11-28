package com.immortals.cache.observability;

import io.micrometer.core.instrument.*;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@SuppressWarnings("unused")
public class CacheMetrics {

    @Getter
    private final MeterRegistry registry;

    @Getter
    private final String cacheName;

    private final String namespace;

    private final Counter hits;
    private final Counter misses;
    private final Counter evictions;
    private final Counter puts;
    private final Counter removes;
    private final Counter errors;

    private final Timer getTimer;
    private final Timer putTimer;
    private final Timer removeTimer;
    private final Timer getAllTimer;
    private final Timer putAllTimer;

    private final AtomicLong cacheSize;
    private final AtomicLong memoryUsage;

    private Tags tags;

    public CacheMetrics(MeterRegistry registry, String cacheName, String namespace) {
        this.registry = registry;
        this.cacheName = cacheName;
        this.namespace = namespace;

        this.tags = Tags.of(
                "cache", cacheName,
                "namespace", namespace
        );

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
    }

    @PostConstruct
    public void initMetrics() {
        Gauge.builder("cache.hit.rate", this, CacheMetrics::getHitRate)
                .description("Cache hit rate (0.0 to 1.0)")
                .tags(tags)
                .register(registry);

        registry.gauge("cache_metric", this, CacheMetrics::getValue);
    }

    public void recordHit() {
        hits.increment();
        log.debug("Cache hit: cache={}, namespace={}", cacheName, namespace);
    }

    public void recordMiss() {
        misses.increment();
        log.debug("Cache miss: cache={}, namespace={}", cacheName, namespace);
    }

    public void recordEviction() {
        evictions.increment();
        log.debug("Cache eviction: cache={}, namespace={}", cacheName, namespace);
    }

    public void recordPut() {
        puts.increment();
    }

    public void recordRemove() {
        removes.increment();
    }

    public void recordError() {
        errors.increment();
        log.warn("Cache error: cache={}, namespace={}", cacheName, namespace);
    }

    public void recordGetLatency(Duration duration) {
        getTimer.record(duration);
    }

    public void recordPutLatency(Duration duration) {
        putTimer.record(duration);
    }

    public void recordRemoveLatency(Duration duration) {
        removeTimer.record(duration);
    }

    public void recordGetAllLatency(Duration duration) {
        getAllTimer.record(duration);
    }

    public void recordPutAllLatency(Duration duration) {
        putAllTimer.record(duration);
    }

    public void updateSize(long size) {
        cacheSize.set(size);
    }

    public void updateMemoryUsage(long bytes) {
        memoryUsage.set(bytes);
    }

    public double getHitRate() {
        double h = hits.count();
        double m = misses.count();
        double total = h + m;
        return total == 0 ? 0.0 : h / total;
    }

    public long getHitCount() {
        return (long) hits.count();
    }

    public long getMissCount() {
        return (long) misses.count();
    }

    public long getEvictionCount() {
        return (long) evictions.count();
    }

    public double getAverageGetLatency() {
        return getTimer.mean(TimeUnit.MILLISECONDS);
    }

    public double getAveragePutLatency() {
        return putTimer.mean(TimeUnit.MILLISECONDS);
    }

    public long getCurrentSize() {
        return cacheSize.get();
    }

    public double getValue() {
        return 1.0;
    }
}
