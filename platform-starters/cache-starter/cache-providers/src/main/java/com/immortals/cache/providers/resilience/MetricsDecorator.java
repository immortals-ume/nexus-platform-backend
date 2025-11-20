package com.immortals.cache.providers.resilience;

import com.immortals.cache.core.CacheService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Decorator that adds comprehensive metrics tracking to a cache service.
 * Records operation latency, hit/miss rates, and eviction metrics.
 * 
 * @param <K> key type
 * @param <V> value type
 */
public class MetricsDecorator<K, V> extends CacheDecorator<K, V> {
    
    private static final Logger log = LoggerFactory.getLogger(MetricsDecorator.class);
    
    private final MeterRegistry meterRegistry;
    private final String namespace;
    
    private final Counter hitCounter;
    private final Counter missCounter;
    private final Counter putCounter;
    private final Counter removeCounter;
    private final Counter evictionCounter;
    private final Timer getTimer;
    private final Timer putTimer;
    private final Timer removeTimer;
    
    /**
     * Creates a metrics decorator.
     * 
     * @param delegate the underlying cache service
     * @param meterRegistry meter registry for metrics
     * @param namespace cache namespace for metric tags
     */
    public MetricsDecorator(CacheService<K, V> delegate, 
                           MeterRegistry meterRegistry,
                           String namespace) {
        super(delegate);
        this.meterRegistry = meterRegistry;
        this.namespace = namespace;
        
        // Initialize counters
        this.hitCounter = Counter.builder("cache.hits")
                .tag("namespace", namespace)
                .description("Number of cache hits")
                .register(meterRegistry);
        
        this.missCounter = Counter.builder("cache.misses")
                .tag("namespace", namespace)
                .description("Number of cache misses")
                .register(meterRegistry);
        
        this.putCounter = Counter.builder("cache.puts")
                .tag("namespace", namespace)
                .description("Number of cache put operations")
                .register(meterRegistry);
        
        this.removeCounter = Counter.builder("cache.removes")
                .tag("namespace", namespace)
                .description("Number of cache remove operations")
                .register(meterRegistry);
        
        this.evictionCounter = Counter.builder("cache.evictions")
                .tag("namespace", namespace)
                .description("Number of cache evictions")
                .register(meterRegistry);
        
        // Initialize timers
        this.getTimer = Timer.builder("cache.get.duration")
                .tag("namespace", namespace)
                .description("Duration of cache get operations")
                .register(meterRegistry);
        
        this.putTimer = Timer.builder("cache.put.duration")
                .tag("namespace", namespace)
                .description("Duration of cache put operations")
                .register(meterRegistry);
        
        this.removeTimer = Timer.builder("cache.remove.duration")
                .tag("namespace", namespace)
                .description("Duration of cache remove operations")
                .register(meterRegistry);
        
        log.debug("Metrics decorator initialized for namespace: {}", namespace);
    }
    
    @Override
    public void put(K key, V value) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            delegate.put(key, value);
            putCounter.increment();
        } finally {
            sample.stop(putTimer);
        }
    }
    
    @Override
    public void put(K key, V value, Duration ttl) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            delegate.put(key, value, ttl);
            putCounter.increment();
        } finally {
            sample.stop(putTimer);
        }
    }
    
    @Override
    public Optional<V> get(K key) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            Optional<V> result = delegate.get(key);
            if (result.isPresent()) {
                hitCounter.increment();
                log.debug("Cache hit for key: {} in namespace: {}", key, namespace);
            } else {
                missCounter.increment();
                log.debug("Cache miss for key: {} in namespace: {}", key, namespace);
            }
            return result;
        } finally {
            sample.stop(getTimer);
        }
    }
    
    @Override
    public void remove(K key) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            delegate.remove(key);
            removeCounter.increment();
            evictionCounter.increment();
        } finally {
            sample.stop(removeTimer);
        }
    }
    
    @Override
    public void clear() {
        delegate.clear();
        log.debug("Cache cleared for namespace: {}", namespace);
    }
    
    @Override
    public boolean putIfAbsent(K key, V value) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            boolean result = delegate.putIfAbsent(key, value);
            if (result) {
                putCounter.increment();
            } else {
                hitCounter.increment();
            }
            return result;
        } finally {
            sample.stop(putTimer);
        }
    }
    
    @Override
    public boolean putIfAbsent(K key, V value, Duration ttl) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            boolean result = delegate.putIfAbsent(key, value, ttl);
            if (result) {
                putCounter.increment();
            } else {
                hitCounter.increment();
            }
            return result;
        } finally {
            sample.stop(putTimer);
        }
    }
}
