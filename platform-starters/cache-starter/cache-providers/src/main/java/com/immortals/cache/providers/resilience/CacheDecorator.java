package com.immortals.cache.providers.resilience;

import com.immortals.cache.core.CacheService;
import com.immortals.cache.core.CacheStatistics;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Base abstract class for cache decorators.
 * Provides a template for implementing decorator pattern with delegation.
 * 
 * @param <K> key type
 * @param <V> value type
 */
public abstract class CacheDecorator<K, V> implements CacheService<K, V> {
    
    protected final CacheService<K, V> delegate;
    
    /**
     * Creates a cache decorator.
     * 
     * @param delegate the underlying cache service to decorate
     */
    protected CacheDecorator(CacheService<K, V> delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public void put(K key, V value) {
        delegate.put(key, value);
    }
    
    @Override
    public void put(K key, V value, Duration ttl) {
        delegate.put(key, value, ttl);
    }
    
    @Override
    public Optional<V> get(K key) {
        return delegate.get(key);
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
        delegate.putAll(entries);
    }
    
    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        return delegate.getAll(keys);
    }
    
    @Override
    public boolean putIfAbsent(K key, V value) {
        return delegate.putIfAbsent(key, value);
    }
    
    @Override
    public boolean putIfAbsent(K key, V value, Duration ttl) {
        return delegate.putIfAbsent(key, value, ttl);
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
