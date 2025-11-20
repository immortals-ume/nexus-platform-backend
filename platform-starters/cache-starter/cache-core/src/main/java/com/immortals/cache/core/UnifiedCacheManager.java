package com.immortals.cache.core;

import java.util.Collection;
import java.util.Map;

/**
 * Central facade for managing multiple cache instances.
 * Provides namespace isolation and configuration management.
 * 
 * <p>This interface serves as the single entry point for all cache operations,
 * allowing applications to manage multiple cache namespaces with different
 * configurations and implementations.</p>
 */
public interface UnifiedCacheManager {
    
    /**
     * Get or create a cache instance for the given namespace with default configuration.
     * 
     * @param namespace the namespace identifier for the cache
     * @param <K> the type of keys maintained by this cache
     * @param <V> the type of cached values
     * @return a cache service instance for the specified namespace
     */
    <K, V> CacheService<K, V> getCache(String namespace);
    
    /**
     * Get or create a cache instance for the given namespace with specific configuration.
     * 
     * @param namespace the namespace identifier for the cache
     * @param config the configuration to apply to this cache
     * @param <K> the type of keys maintained by this cache
     * @param <V> the type of cached values
     * @return a cache service instance for the specified namespace
     */
    <K, V> CacheService<K, V> getCache(String namespace, CacheConfiguration config);
    
    /**
     * Remove a cache namespace and all its entries.
     * 
     * @param namespace the namespace identifier to remove
     */
    void removeCache(String namespace);
    
    /**
     * Get all registered cache namespace names.
     * 
     * @return a collection of all cache namespace names
     */
    Collection<String> getCacheNames();
    
    /**
     * Get aggregated statistics across all caches.
     * 
     * @return a map of namespace names to their statistics
     */
    Map<String, CacheStatistics> getAllStatistics();
}
