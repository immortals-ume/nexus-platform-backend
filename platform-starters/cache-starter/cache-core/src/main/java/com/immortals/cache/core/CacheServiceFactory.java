package com.immortals.cache.core;

/**
 * Factory for creating cache service instances.
 * Supports creating new instances for each namespace.
 * 
 * @param <K> the type of keys
 * @param <V> the type of values
 */
public interface CacheServiceFactory<K, V> {
    
    /**
     * Creates a new cache service instance.
     * Each call creates a fresh instance.
     * 
     * @return a new cache service instance
     */
    CacheService<K, V> createCacheService();
}
