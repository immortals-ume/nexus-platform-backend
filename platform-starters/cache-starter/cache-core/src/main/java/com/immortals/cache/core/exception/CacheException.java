package com.immortals.cache.core.exception;

/**
 * Base exception for all cache-related errors.
 * 
 * <p>All cache-specific exceptions should extend this class to maintain
 * a consistent exception hierarchy across the platform.
 * 
 * @since 2.0.0
 */
public class CacheException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    private final String cacheKey;
    private final CacheOperation operation;

    /**
     * Creates a new CacheException with the specified message.
     *
     * @param message the error message
     */
    public CacheException(String message) {
        super(message);
        this.cacheKey = null;
        this.operation = null;
    }

    /**
     * Creates a new CacheException with the specified message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public CacheException(String message, Throwable cause) {
        super(message, cause);
        this.cacheKey = null;
        this.operation = null;
    }

    /**
     * Creates a new CacheException with cache operation context.
     *
     * @param message the error message
     * @param cacheKey the cache key being accessed
     * @param operation the cache operation being performed
     */
    public CacheException(String message, String cacheKey, CacheOperation operation) {
        super(message);
        this.cacheKey = cacheKey;
        this.operation = operation;
    }

    /**
     * Creates a new CacheException with full context.
     *
     * @param message the error message
     * @param cacheKey the cache key being accessed
     * @param operation the cache operation being performed
     * @param cause the underlying cause
     */
    public CacheException(String message, String cacheKey, CacheOperation operation, Throwable cause) {
        super(message, cause);
        this.cacheKey = cacheKey;
        this.operation = operation;
    }

    /**
     * Returns the cache key associated with this exception.
     *
     * @return the cache key, or null if not available
     */
    public String getCacheKey() {
        return cacheKey;
    }

    /**
     * Returns the cache operation associated with this exception.
     *
     * @return the cache operation, or null if not available
     */
    public CacheOperation getOperation() {
        return operation;
    }

    /**
     * Enumeration of cache operations for better error context.
     */
    public enum CacheOperation {
        /** Get operation - retrieving a value from cache */
        GET,
        
        /** Put operation - storing a value in cache */
        PUT,
        
        /** Remove operation - removing a value from cache */
        REMOVE,
        
        /** Clear operation - clearing all entries from cache */
        CLEAR,
        
        /** Increment operation - incrementing a numeric value */
        INCREMENT,
        
        /** Decrement operation - decrementing a numeric value */
        DECREMENT,
        
        /** Lock acquire operation - acquiring a distributed lock */
        LOCK_ACQUIRE,
        
        /** Lock release operation - releasing a distributed lock */
        LOCK_RELEASE,
        
        /** Hash get operation - retrieving a hash field */
        HASH_GET,
        
        /** Hash put operation - storing a hash field */
        HASH_PUT,
        
        /** Hash delete operation - deleting a hash field */
        HASH_DELETE,
        
        /** Compute operation - computing a value */
        COMPUTE,
        
        /** Batch get operation - retrieving multiple values */
        GET_ALL,
        
        /** Batch put operation - storing multiple values */
        PUT_ALL,
        
        /** Contains key operation - checking if key exists */
        CONTAINS_KEY
    }
}
