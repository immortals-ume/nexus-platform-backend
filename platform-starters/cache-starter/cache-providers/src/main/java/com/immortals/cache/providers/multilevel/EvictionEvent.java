package com.immortals.cache.providers.multilevel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Event representing a cache eviction operation.
 * Published to messaging systems for distributed cache invalidation.
 * 
 * @since 2.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvictionEvent implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    private String namespace;
    private String key;
    private String pattern;
    private EvictionType type;
    private String source;
    private Instant timestamp;
    
    /**
     * Type of eviction operation.
     */
    public enum EvictionType {
        /** Evict a single key */
        SINGLE_KEY,
        /** Evict keys matching a pattern */
        PATTERN,
        /** Clear all keys in the namespace */
        CLEAR_ALL
    }
    
    /**
     * Create a single key eviction event.
     */
    public static EvictionEvent singleKey(String namespace, String key, String source) {
        return new EvictionEvent(namespace, key, null, EvictionType.SINGLE_KEY, source, Instant.now());
    }
    
    /**
     * Create a pattern-based eviction event.
     */
    public static EvictionEvent pattern(String namespace, String pattern, String source) {
        return new EvictionEvent(namespace, null, pattern, EvictionType.PATTERN, source, Instant.now());
    }
    
    /**
     * Create a clear all event.
     */
    public static EvictionEvent clearAll(String namespace, String source) {
        return new EvictionEvent(namespace, null, null, EvictionType.CLEAR_ALL, source, Instant.now());
    }
}
