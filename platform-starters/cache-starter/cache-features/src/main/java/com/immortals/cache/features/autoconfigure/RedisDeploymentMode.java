package com.immortals.cache.features.autoconfigure;

/**
 * Enum representing the Redis deployment mode.
 *
 * <p>Defines the different ways Redis can be deployed and configured:
 * <ul>
 *   <li>STANDALONE - Single Redis instance (default)</li>
 *   <li>SENTINEL - High availability with Redis Sentinel</li>
 *   <li>CLUSTER - Distributed Redis Cluster</li>
 * </ul>
 *
 * @since 2.0.0
 */
public enum RedisDeploymentMode {
    /**
     * Standalone Redis instance.
     * Single point of failure, suitable for development and non-critical environments.
     */
    STANDALONE("standalone"),

    /**
     * Redis Sentinel deployment.
     * Provides high availability with automatic failover.
     */
    SENTINEL("sentinel"),

    /**
     * Redis Cluster deployment.
     * Distributed Redis with automatic sharding and high availability.
     */
    CLUSTER("cluster");

    private final String value;

    RedisDeploymentMode(String value) {
        this.value = value;
    }

    /**
     * Converts a string value to the corresponding deployment mode.
     *
     * @param value the string value
     * @return the corresponding RedisDeploymentMode
     * @throws IllegalArgumentException if the value is not recognized
     */
    public static RedisDeploymentMode fromValue(String value) {
        for (RedisDeploymentMode mode : RedisDeploymentMode.values()) {
            if (mode.value.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown Redis deployment mode: " + value);
    }

    /**
     * Gets the string value of the deployment mode.
     *
     * @return the deployment mode as a string
     */
    public String getValue() {
        return value;
    }
}
