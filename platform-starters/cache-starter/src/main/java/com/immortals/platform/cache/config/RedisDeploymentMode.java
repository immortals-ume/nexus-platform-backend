package com.immortals.platform.cache.config;

/**
 * Redis deployment modes supported by the cache starter.
 *
 * @since 2.0.0
 */
public enum RedisDeploymentMode {
    
    /**
     * Single Redis instance.
     */
    STANDALONE,
    
    /**
     * Redis Sentinel for high availability.
     */
    SENTINEL,
    
    /**
     * Redis Cluster for horizontal scaling.
     */
    CLUSTER
}