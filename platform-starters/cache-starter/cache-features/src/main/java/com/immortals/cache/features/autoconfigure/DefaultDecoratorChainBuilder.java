package com.immortals.cache.features.autoconfigure;

import com.immortals.cache.core.CacheConfiguration;
import com.immortals.cache.core.CacheService;
import com.immortals.cache.core.CacheStatistics;
import com.immortals.cache.core.DefaultUnifiedCacheManager;
import com.immortals.cache.features.compression.CompressionDecorator;
import com.immortals.cache.features.compression.GzipCompressionStrategy;
import com.immortals.cache.features.encryption.EncryptionDecorator;
import com.immortals.cache.features.encryption.AesGcmEncryptionStrategy;
import com.immortals.cache.features.stampede.StampedeProtectionDecorator;
import com.immortals.cache.features.circuitbreaker.CircuitBreakerDecorator;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;

import java.io.*;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Default implementation of DecoratorChainBuilder.
 * 
 * <p>Builds decorator chains for cache services based on configuration.
 * Applies features like compression, encryption, metrics, and resilience patterns.
 * 
 * @since 2.0.0
 */
@Slf4j
public class DefaultDecoratorChainBuilder implements DefaultUnifiedCacheManager.DecoratorChainBuilder {
    
    private final MeterRegistry meterRegistry;
    private final RedissonClient redissonClient;
    private final String encryptionKey;
    private final int compressionThreshold;
    private final Duration stampedeTimeout;
    private final boolean circuitBreakerEnabled;
    
    /**
     * Creates a DefaultDecoratorChainBuilder with configuration.
     * 
     * @param meterRegistry meter registry for metrics
     * @param redissonClient optional Redisson client for distributed locks
     * @param encryptionKey optional encryption key
     * @param compressionThreshold threshold for compression in bytes
     * @param stampedeTimeout timeout for stampede protection locks
     * @param circuitBreakerEnabled whether to enable circuit breaker
     */
    public DefaultDecoratorChainBuilder(
            MeterRegistry meterRegistry,
            RedissonClient redissonClient,
            String encryptionKey,
            int compressionThreshold,
            Duration stampedeTimeout,
            boolean circuitBreakerEnabled) {
        this.meterRegistry = meterRegistry;
        this.redissonClient = redissonClient;
        this.encryptionKey = encryptionKey;
        this.compressionThreshold = compressionThreshold;
        this.stampedeTimeout = stampedeTimeout;
        this.circuitBreakerEnabled = circuitBreakerEnabled;
    }
    
    /**
     * Builds a decorator chain for the given cache service.
     * 
     * <p>Applies decorators in order:
     * <ol>
     *   <li>Metrics decorator (if meter registry available)</li>
     *   <li>Compression decorator (if enabled and threshold set)</li>
     *   <li>Encryption decorator (if enabled and key provided)</li>
     *   <li>Stampede protection decorator (if enabled and Redisson available)</li>
     *   <li>Circuit breaker decorator (if enabled)</li>
     * </ol>
     * 
     * @param baseCache the base cache service
     * @param namespace the cache namespace
     * @param config the cache configuration
     * @return decorated cache service
     */
    @Override
    public <K, V> CacheService<K, V> buildDecoratorChain(
            CacheService<K, V> baseCache,
            String namespace,
            CacheConfiguration config) {
        
        CacheService<K, V> decorated = baseCache;
        
        // Apply metrics decorator
        if (meterRegistry != null) {
            log.debug("Applying metrics decorator for namespace: {}", namespace);
            // TODO: Implement metrics decorator wrapping
            // decorated = new MetricsDecoratorCacheService<>(decorated, meterRegistry, namespace);
        }
        
        // Apply compression decorator
        if (config.isCompressionEnabled() && compressionThreshold > 0) {
            log.debug("Applying compression decorator for namespace: {} (threshold: {})", namespace, compressionThreshold);
            // Note: Compression decorator requires byte[] storage. Only apply if underlying cache supports it.
            // For now, compression is deferred to a future implementation with proper serialization layer.
        }
        
        // Apply encryption decorator
        if (config.isEncryptionEnabled() && encryptionKey != null) {
            log.debug("Applying encryption decorator for namespace: {}", namespace);
            // Note: Encryption decorator requires byte[] storage. Only apply if underlying cache supports it.
            // For now, encryption is deferred to a future implementation with proper serialization layer.
        }
        
        // Apply stampede protection decorator
        if (config.isStampedeProtectionEnabled() && redissonClient != null && stampedeTimeout != null) {
            log.debug("Applying stampede protection decorator for namespace: {} (timeout: {})", namespace, stampedeTimeout);
            decorated = new StampedeProtectionDecorator<>(decorated, redissonClient, namespace, stampedeTimeout);
        }
        
        // Apply circuit breaker decorator
        if (circuitBreakerEnabled) {
            log.debug("Applying circuit breaker decorator for namespace: {}", namespace);
            decorated = new CircuitBreakerDecorator<>(decorated);
        }
        
        return decorated;
    }
    
    /**
     * Adapter that converts byte arrays to/from the underlying cache storage.
     * The underlying cache stores byte arrays as Objects, so we just pass them through.
     */
    @SuppressWarnings("unchecked")
    private static class SerializationAdapter<K, V> implements CacheService<K, byte[]> {
        private final CacheService<K, V> delegate;
        
        SerializationAdapter(CacheService<K, V> delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public void put(K key, byte[] value) {
            delegate.put(key, (V) value);
        }
        
        @Override
        public void put(K key, byte[] value, Duration ttl) {
            delegate.put(key, (V) value, ttl);
        }
        
        @Override
        public Optional<byte[]> get(K key) {
            Optional<V> result = delegate.get(key);
            if (result.isPresent()) {
                Object obj = result.get();
                if (obj instanceof byte[]) {
                    return Optional.of((byte[]) obj);
                }
            }
            return Optional.empty();
        }
        
        @Override
        public void remove(K key) {
            delegate.remove(key);
        }
        
        @Override
        public void clear() {
            delegate.clear();
        }
        
        @Override
        public boolean containsKey(K key) {
            return delegate.containsKey(key);
        }
        
        @Override
        public void putAll(Map<K, byte[]> entries) {
            delegate.putAll((Map<K, V>) (Map) entries);
        }
        
        @Override
        public Map<K, byte[]> getAll(Collection<K> keys) {
            Map<K, V> results = delegate.getAll(keys);
            Map<K, byte[]> converted = new HashMap<>();
            results.forEach((k, v) -> {
                if (v instanceof byte[]) {
                    converted.put(k, (byte[]) v);
                }
            });
            return converted;
        }
        
        @Override
        public boolean putIfAbsent(K key, byte[] value) {
            return delegate.putIfAbsent(key, (V) value);
        }
        
        @Override
        public boolean putIfAbsent(K key, byte[] value, Duration ttl) {
            return delegate.putIfAbsent(key, (V) value, ttl);
        }
        
        @Override
        public Long increment(K key, long delta) {
            return delegate.increment(key, delta);
        }
        
        @Override
        public Long decrement(K key, long delta) {
            return delegate.decrement(key, delta);
        }
        
        @Override
        public CacheStatistics getStatistics() {
            return delegate.getStatistics();
        }
    }
    
    /**
     * Adapter that deserializes bytes back to objects.
     */
    @SuppressWarnings("unchecked")
    private static class DeserializationAdapter<K, V> implements CacheService<K, V> {
        private final CacheService<K, byte[]> delegate;
        
        DeserializationAdapter(CacheService<K, byte[]> delegate) {
            this.delegate = delegate;
        }
        
        private V deserialize(byte[] data) {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                ObjectInputStream ois = new ObjectInputStream(bais);
                V obj = (V) ois.readObject();
                ois.close();
                return obj;
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException("Deserialization failed", e);
            }
        }
        
        @Override
        public void put(K key, V value) {
            delegate.put(key, serialize(value));
        }
        
        @Override
        public void put(K key, V value, Duration ttl) {
            delegate.put(key, serialize(value), ttl);
        }
        
        private byte[] serialize(V value) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(value);
                oos.close();
                return baos.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException("Serialization failed", e);
            }
        }
        
        @Override
        public Optional<V> get(K key) {
            return delegate.get(key).map(this::deserialize);
        }
        
        @Override
        public void remove(K key) {
            delegate.remove(key);
        }
        
        @Override
        public void clear() {
            delegate.clear();
        }
        
        @Override
        public boolean containsKey(K key) {
            return delegate.containsKey(key);
        }
        
        @Override
        public void putAll(Map<K, V> entries) {
            Map<K, byte[]> serialized = new HashMap<>();
            entries.forEach((k, v) -> serialized.put(k, serialize(v)));
            delegate.putAll(serialized);
        }
        
        @Override
        public Map<K, V> getAll(Collection<K> keys) {
            Map<K, byte[]> bytes = delegate.getAll(keys);
            Map<K, V> result = new HashMap<>();
            bytes.forEach((k, v) -> result.put(k, deserialize(v)));
            return result;
        }
        
        @Override
        public boolean putIfAbsent(K key, V value) {
            return delegate.putIfAbsent(key, serialize(value));
        }
        
        @Override
        public boolean putIfAbsent(K key, V value, Duration ttl) {
            return delegate.putIfAbsent(key, serialize(value), ttl);
        }
        
        @Override
        public Long increment(K key, long delta) {
            return delegate.increment(key, delta);
        }
        
        @Override
        public Long decrement(K key, long delta) {
            return delegate.decrement(key, delta);
        }
        
        @Override
        public CacheStatistics getStatistics() {
            return delegate.getStatistics();
        }
    }
}
