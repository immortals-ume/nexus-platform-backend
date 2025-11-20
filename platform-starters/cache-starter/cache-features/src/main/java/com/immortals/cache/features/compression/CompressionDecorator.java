package com.immortals.cache.features.compression;

import com.immortals.cache.core.CacheService;
import com.immortals.cache.core.CacheStatistics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.io.*;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Decorator that adds transparent compression to a cache service.
 * Only compresses values that exceed the configured size threshold.
 */
public class CompressionDecorator<K, V> implements CacheService<K, V> {
    
    private final CacheService<K, byte[]> delegate;
    private final CompressionStrategy compressionStrategy;
    private final int compressionThreshold;
    private final Counter compressionCounter;
    private final Counter decompressionCounter;
    private final Counter bypassCounter;
    
    /**
     * Creates a compression decorator.
     * 
     * @param delegate the underlying cache service
     * @param compressionStrategy the compression strategy to use
     * @param compressionThreshold minimum size in bytes to trigger compression
     * @param meterRegistry meter registry for metrics (optional)
     */
    public CompressionDecorator(CacheService<K, byte[]> delegate, 
                                CompressionStrategy compressionStrategy,
                                int compressionThreshold,
                                MeterRegistry meterRegistry) {
        this.delegate = delegate;
        this.compressionStrategy = compressionStrategy;
        this.compressionThreshold = compressionThreshold;
        
        if (meterRegistry != null) {
            this.compressionCounter = Counter.builder("cache.compression.operations")
                .tag("operation", "compress")
                .tag("algorithm", compressionStrategy.getAlgorithm())
                .description("Number of compression operations")
                .register(meterRegistry);
            
            this.decompressionCounter = Counter.builder("cache.compression.operations")
                .tag("operation", "decompress")
                .tag("algorithm", compressionStrategy.getAlgorithm())
                .description("Number of decompression operations")
                .register(meterRegistry);
            
            this.bypassCounter = Counter.builder("cache.compression.bypass")
                .tag("algorithm", compressionStrategy.getAlgorithm())
                .description("Number of times compression was bypassed due to size threshold")
                .register(meterRegistry);
        } else {
            this.compressionCounter = null;
            this.decompressionCounter = null;
            this.bypassCounter = null;
        }
    }
    
    @Override
    public void put(K key, V value) {
        byte[] serialized = serialize(value);
        byte[] stored = compressIfNeeded(serialized);
        delegate.put(key, stored);
    }
    
    @Override
    public void put(K key, V value, Duration ttl) {
        byte[] serialized = serialize(value);
        byte[] stored = compressIfNeeded(serialized);
        delegate.put(key, stored, ttl);
    }
    
    @Override
    public Optional<V> get(K key) {
        return delegate.get(key)
            .map(this::decompressIfNeeded)
            .map(this::deserialize);
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
        Map<K, byte[]> compressed = entries.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> compressIfNeeded(serialize(e.getValue()))
            ));
        delegate.putAll(compressed);
    }
    
    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        return delegate.getAll(keys).entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> deserialize(decompressIfNeeded(e.getValue()))
            ));
    }
    
    @Override
    public boolean putIfAbsent(K key, V value) {
        byte[] serialized = serialize(value);
        byte[] stored = compressIfNeeded(serialized);
        return delegate.putIfAbsent(key, stored);
    }
    
    @Override
    public boolean putIfAbsent(K key, V value, Duration ttl) {
        byte[] serialized = serialize(value);
        byte[] stored = compressIfNeeded(serialized);
        return delegate.putIfAbsent(key, stored, ttl);
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
    
    /**
     * Compresses data if it exceeds the threshold.
     * Adds a marker byte to indicate whether data is compressed.
     */
    private byte[] compressIfNeeded(byte[] data) {
        if (data.length < compressionThreshold) {
            if (bypassCounter != null) {
                bypassCounter.increment();
            }
            // Prefix with 0 to indicate uncompressed
            return prependMarker(data, (byte) 0);
        }
        
        byte[] compressed = compressionStrategy.compress(data);
        if (compressionCounter != null) {
            compressionCounter.increment();
        }
        
        // Prefix with 1 to indicate compressed
        return prependMarker(compressed, (byte) 1);
    }
    
    /**
     * Decompresses data if it was compressed (based on marker byte).
     */
    private byte[] decompressIfNeeded(byte[] data) {
        if (data == null || data.length == 0) {
            return data;
        }
        
        byte marker = data[0];
        byte[] actualData = Arrays.copyOfRange(data, 1, data.length);
        
        if (marker == 1) {
            if (decompressionCounter != null) {
                decompressionCounter.increment();
            }
            return compressionStrategy.decompress(actualData);
        }
        
        return actualData;
    }
    
    private byte[] prependMarker(byte[] data, byte marker) {
        byte[] result = new byte[data.length + 1];
        result[0] = marker;
        System.arraycopy(data, 0, result, 1, data.length);
        return result;
    }
    
    private byte[] serialize(V value) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(value);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new CompressionException("Failed to serialize value", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private V deserialize(byte[] data) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (V) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new CompressionException("Failed to deserialize value", e);
        }
    }
}
