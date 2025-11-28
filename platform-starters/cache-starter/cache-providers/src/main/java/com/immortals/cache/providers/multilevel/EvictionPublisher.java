package com.immortals.cache.providers.multilevel;

/**
 * Publisher interface for distributed cache eviction events.
 * Implementations use messaging systems (e.g., Redis Pub/Sub) to notify
 * other application instances about cache invalidations.
 * 
 * @since 2.0.0
 */
public interface EvictionPublisher {

    /**
     * Publish a single key eviction event for a specific namespace.
     * 
     * @param namespace the cache namespace
     * @param key the cache key to evict
     */
    void publishKeyEviction(String namespace, String key);

    /**
     * Publish a pattern-based eviction event for a specific namespace.
     * 
     * @param namespace the cache namespace
     * @param pattern the key pattern to match for eviction
     */
    void publishPatternEviction(String namespace, String pattern);

    /**
     * Publish a clear all caches event for a specific namespace.
     * 
     * @param namespace the cache namespace
     */
    void publishClearAll(String namespace);

    /**
     * Get the unique instance ID of this publisher.
     * Used to avoid processing self-published events.
     * 
     * @return the instance ID
     */
    String getInstanceId();
}
