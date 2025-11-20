package com.immortals.usermanagementservice.service.cache;

import java.time.Duration;
import java.util.Map;

public interface CacheService<H, HK, HV> {

    /**
     * Put a value in a Redis hash.
     * @param hashKey the Redis hash key
     * @param fieldKey the field within the hash
     * @param value the value to store
     * @param ttl time-to-live for the entire hash (not individual fields)
     * @param lockingKey optional locking key for concurrency control
     */
    void put(H hashKey, HK fieldKey, HV value, Duration ttl, String lockingKey);

    /**
     * Get a value from a hash field.
     */
    HV get(H hashKey, HK fieldKey, String lockingKey);

    /**
     * Remove a field from the hash.
     */
    void remove(H hashKey, HK fieldKey, String lockingKey);

    /**
     * Check if a field exists in the hash.
     */
    boolean containsKey(H hashKey, HK fieldKey, String lockingKey);

    /**
     * Get all fields and values from the hash.
     */
    Map<HK, HV> getAll(H hashKey, String lockingKey);

    /**
     * Get cache hit count.
     */
    default Long getHitCount() { return 0L; }

    /**
     * Get cache miss count.
     */
    default Long getMissCount() { return 0L; }

    /**
     * Increment a numeric hash field and optionally set TTL on first increment.
     */
    Long increment(H hashKey, HK fieldKey, String lockingKey, Duration ttl);
}
