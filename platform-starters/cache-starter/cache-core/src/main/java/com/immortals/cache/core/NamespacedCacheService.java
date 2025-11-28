package com.immortals.cache.core;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Wrapper that adds namespace prefixing to cache keys to prevent collisions.
 * 
 * <p>This decorator ensures that keys from different namespaces never collide
 * by prefixing all keys with the namespace identifier.
 * 
 * <p>Key format: {namespace}:{key}
 * 
 * <p>This is particularly important when multiple namespaces share the same
 * underlying cache storage (e.g., a single Redis instance).
 * 
 * <p>Requirements: 8.1, 8.2, 8.3
 * 
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of cached values
 * @since 2.0.0
 */
public class NamespacedCacheService<K, V> implements CacheService<K, V> {

    private static final String NAMESPACE_SEPARATOR = ":";

    private final CacheService<String, V> delegate;
    private final String namespace;

    /**
     * Creates a namespaced cache service.
     * 
     * @param delegate the underlying cache service
     * @param namespace the namespace identifier
     */
    public NamespacedCacheService(CacheService<String, V> delegate, String namespace) {
        if (delegate == null) {
            throw new IllegalArgumentException("Delegate cache service cannot be null");
        }
        if (namespace == null || namespace.trim().isEmpty()) {
            throw new IllegalArgumentException("Namespace cannot be null or empty");
        }

        this.delegate = delegate;
        this.namespace = namespace;
    }

    @Override
    public void put(K key, V value) {
        String namespacedKey = buildNamespacedKey(key);
        delegate.put(namespacedKey, value);
    }

    @Override
    public void put(K key, V value, Duration ttl) {
        String namespacedKey = buildNamespacedKey(key);
        delegate.put(namespacedKey, value, ttl);
    }

    @Override
    public Optional<V> get(K key) {
        String namespacedKey = buildNamespacedKey(key);
        return delegate.get(namespacedKey);
    }

    @Override
    public void remove(K key) {
        String namespacedKey = buildNamespacedKey(key);
        delegate.remove(namespacedKey);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public boolean containsKey(K key) {
        String namespacedKey = buildNamespacedKey(key);
        return delegate.containsKey(namespacedKey);
    }

    @Override
    public void putAll(Map<K, V> entries) {
        Map<String, V> namespacedEntries = entries.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> buildNamespacedKey(e.getKey()),
                        Map.Entry::getValue
                ));
        delegate.putAll(namespacedEntries);
    }

    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        Collection<String> namespacedKeys = keys.stream()
                .map(this::buildNamespacedKey)
                .collect(Collectors.toList());

        Map<String, V> namespacedResults = delegate.getAll(namespacedKeys);

        return namespacedResults.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> extractOriginalKey(e.getKey()),
                        Map.Entry::getValue
                ));
    }

    @Override
    public boolean putIfAbsent(K key, V value) {
        String namespacedKey = buildNamespacedKey(key);
        return delegate.putIfAbsent(namespacedKey, value);
    }

    @Override
    public boolean putIfAbsent(K key, V value, Duration ttl) {
        String namespacedKey = buildNamespacedKey(key);
        return delegate.putIfAbsent(namespacedKey, value, ttl);
    }

    @Override
    public Long increment(K key, long delta) {
        String namespacedKey = buildNamespacedKey(key);
        return delegate.increment(namespacedKey, delta);
    }

    @Override
    public Long decrement(K key, long delta) {
        String namespacedKey = buildNamespacedKey(key);
        return delegate.decrement(namespacedKey, delta);
    }

    @Override
    public CacheStatistics getStatistics() {
        return delegate.getStatistics();
    }

    /**
     * Builds a namespaced key by prefixing with the namespace.
     * 
     * @param key the original key
     * @return the namespaced key
     */
    private String buildNamespacedKey(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        return namespace + NAMESPACE_SEPARATOR + key.toString();
    }

    /**
     * Extracts the original key from a namespaced key.
     * 
     * @param namespacedKey the namespaced key
     * @return the original key
     */
    @SuppressWarnings("unchecked")
    private K extractOriginalKey(String namespacedKey) {
        String prefix = namespace + NAMESPACE_SEPARATOR;
        if (namespacedKey.startsWith(prefix)) {
            return (K) namespacedKey.substring(prefix.length());
        }
        return (K) namespacedKey;
    }

    /**
     * Gets the namespace for this cache service.
     * 
     * @return the namespace identifier
     */
    public String getNamespace() {
        return namespace;
    }
}
