package com.immortals.cache.features.encryption;

import com.immortals.cache.core.CacheService;
import com.immortals.cache.core.CacheStatistics;

import java.io.*;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Decorator that adds transparent encryption to a cache service.
 * All values are encrypted before storage and decrypted on retrieval.
 */
public class EncryptionDecorator<K, V> implements CacheService<K, V> {

    private final CacheService<K, byte[]> delegate;
    private final EncryptionStrategy encryptionStrategy;

    /**
     * Creates an encryption decorator.
     *
     * @param delegate           the underlying cache service
     * @param encryptionStrategy the encryption strategy to use
     */
    public EncryptionDecorator(CacheService<K, byte[]> delegate,
                               EncryptionStrategy encryptionStrategy) {
        this.delegate = delegate;
        this.encryptionStrategy = encryptionStrategy;

        encryptionStrategy.validateConfiguration();
    }

    @Override
    public void put(K key, V value) {
        byte[] serialized = serialize(value);
        byte[] encrypted = encryptionStrategy.encrypt(serialized);
        delegate.put(key, encrypted);
    }

    @Override
    public void put(K key, V value, Duration ttl) {
        byte[] serialized = serialize(value);
        byte[] encrypted = encryptionStrategy.encrypt(serialized);
        delegate.put(key, encrypted, ttl);
    }

    @Override
    public Optional<V> get(K key) {
        return delegate.get(key)
                .map(encryptionStrategy::decrypt)
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
        Map<K, byte[]> encrypted = entries.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> encryptionStrategy.encrypt(serialize(e.getValue()))
                ));
        delegate.putAll(encrypted);
    }

    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        return delegate.getAll(keys)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> deserialize(encryptionStrategy.decrypt(e.getValue()))
                ));
    }

    @Override
    public boolean putIfAbsent(K key, V value) {
        byte[] serialized = serialize(value);
        byte[] encrypted = encryptionStrategy.encrypt(serialized);
        return delegate.putIfAbsent(key, encrypted);
    }

    @Override
    public boolean putIfAbsent(K key, V value, Duration ttl) {
        byte[] serialized = serialize(value);
        byte[] encrypted = encryptionStrategy.encrypt(serialized);
        return delegate.putIfAbsent(key, encrypted, ttl);
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

    private byte[] serialize(V value) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(value);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new EncryptionException("Failed to serialize value", e);
        }
    }

    @SuppressWarnings("unchecked")
    private V deserialize(byte[] data) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (V) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new EncryptionException("Failed to deserialize value", e);
        }
    }
}
