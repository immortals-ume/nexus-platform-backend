package com.immortals.cache.core;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Core cache service interface providing unified caching operations.
 * All cache implementations must implement this interface.
 * 
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of cached values
 */
public interface CacheService<K, V> {

    /**
     * Associates the specified value with the specified key in this cache.
     * 
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     */
    void put(K key, V value);

    /**
     * Associates the specified value with the specified key in this cache with a TTL.
     * 
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @param ttl the time-to-live duration for this entry
     */
    void put(K key, V value, Duration ttl);

    /**
     * Returns the value associated with the key in this cache, or empty if there is no cached value.
     * 
     * @param key the key whose associated value is to be returned
     * @return an Optional containing the value if present, or empty otherwise
     */
    Optional<V> get(K key);

    /**
     * Removes the mapping for a key from this cache if it is present.
     * 
     * @param key the key whose mapping is to be removed from the cache
     */
    void remove(K key);

    /**
     * Removes all mappings from the cache.
     */
    void clear();

    /**
     * Returns true if this cache contains a mapping for the specified key.
     * 
     * @param key the key whose presence in this cache is to be tested
     * @return true if this cache contains a mapping for the specified key
     */
    boolean containsKey(K key);

    /**
     * Copies all of the mappings from the specified map to this cache.
     * 
     * @param entries mappings to be stored in this cache
     */
    void putAll(Map<K, V> entries);

    /**
     * Returns a map of the values associated with the keys in this cache.
     * 
     * @param keys the keys whose associated values are to be returned
     * @return a map containing the key-value pairs for the requested keys
     */
    Map<K, V> getAll(Collection<K> keys);

    /**
     * If the specified key is not already associated with a value, associates it with the given value.
     * 
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return true if the value was set, false if the key already existed
     */
    boolean putIfAbsent(K key, V value);

    /**
     * If the specified key is not already associated with a value, associates it with the given value and TTL.
     * 
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @param ttl the time-to-live duration for this entry
     * @return true if the value was set, false if the key already existed
     */
    boolean putIfAbsent(K key, V value, Duration ttl);

    /**
     * Atomically increments the value associated with the key by the given delta.
     * Note: This operation is only supported by certain cache implementations (e.g., Redis).
     * 
     * @param key the key whose value is to be incremented
     * @param delta the amount to increment by
     * @return the new value after incrementing
     * @throws UnsupportedOperationException if this operation is not supported by the implementation
     */
    Long increment(K key, long delta);

    /**
     * Atomically decrements the value associated with the key by the given delta.
     * Note: This operation is only supported by certain cache implementations (e.g., Redis).
     * 
     * @param key the key whose value is to be decremented
     * @param delta the amount to decrement by
     * @return the new value after decrementing
     * @throws UnsupportedOperationException if this operation is not supported by the implementation
     */
    Long decrement(K key, long delta);

    /**
     * Returns statistics about this cache's performance.
     * 
     * @return cache statistics
     */
    CacheStatistics getStatistics();
}
