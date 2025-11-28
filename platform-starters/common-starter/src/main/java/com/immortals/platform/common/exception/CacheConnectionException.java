package com.immortals.platform.common.exception.exception;

import lombok.Getter;

import java.io.Serial;

/**
 * Exception thrown when a connection to the cache service (Redis) cannot be established or is lost.
 * 
 * <p>This exception indicates network connectivity issues, Redis server unavailability,
 * or configuration problems preventing successful connection to the cache.
 * 
 * <p>This is typically a transient error that may resolve itself, but could also indicate
 * more serious infrastructure issues requiring immediate attention.
 * 
 * <p>Example usage:
 * <pre>{@code
 * try {
 *     cacheService.put("key", "value");
 * } catch (CacheConnectionException e) {
 *     log.error("Failed to connect to cache: {}", e.getMessage());
 *     // Implement retry logic or alert operations team
 * }
 * }</pre>
 * 
 * @since 2.0.0
 */
@Getter
public class CacheConnectionException extends CacheException {
    
    @Serial
    private static final long serialVersionUID = 1L;
    private static final String ERROR_CODE = "CACHE_CONNECTION_ERROR";

    /**
     * -- GETTER --
     *  Returns the cache server host that failed to connect.
     *
     * @return the host, or null if not available
     */
    private final String host;
    /**
     * -- GETTER --
     *  Returns the cache server port.
     *
     * @return the port, or null if not available
     */
    private final Integer port;
    /**
     * -- GETTER --
     *  Indicates whether this error is potentially retryable.
     *
     * @return true if the operation can be retried, false otherwise
     */
    private final boolean retryable;

    /**
     * Creates a new CacheConnectionException with the specified message.
     *
     * @param message the error message
     */
    public CacheConnectionException(String message) {
        super(message);
        this.host = null;
        this.port = null;
        this.retryable = true;
    }

    /**
     * Creates a new CacheConnectionException with the specified message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public CacheConnectionException(String message, Throwable cause) {
        super(message, cause);
        this.host = null;
        this.port = null;
        this.retryable = true;
    }

    /**
     * Creates a new CacheConnectionException with detailed connection information.
     *
     * @param message the error message
     * @param host the cache server host that failed to connect
     * @param port the cache server port
     * @param cause the underlying cause
     */
    public CacheConnectionException(String message, String host, Integer port, Throwable cause) {
        super(buildDetailedMessage(message, host, port), cause);
        this.host = host;
        this.port = port;
        this.retryable = true;
    }

    /**
     * Creates a new CacheConnectionException with cache key context.
     *
     * @param message the error message
     * @param cacheKey the cache key being accessed
     * @param operation the cache operation being performed
     * @param cause the underlying cause
     */
    public CacheConnectionException(String message, String cacheKey, CacheOperation operation, Throwable cause) {
        super(message, cacheKey, operation, cause);
        this.host = null;
        this.port = null;
        this.retryable = true;
    }

    /**
     * Builds a detailed error message with connection information and troubleshooting guidance.
     */
    private static String buildDetailedMessage(String message, String host, Integer port) {
        return String.format(
            "%s (Host: %s:%d)" +
            "\n\nImmediate Actions:" +
            "\n1. Verify cache server is running: redis-cli -h %s -p %d ping" +
            "\n2. Check network connectivity: telnet %s %d" +
            "\n3. Review firewall rules and security groups" +
            "\n4. Verify authentication credentials" +
            "\n\nCommon Causes:" +
            "\n- Cache server is down or restarting" +
            "\n- Network connectivity issues" +
            "\n- Firewall blocking connections" +
            "\n- Incorrect host/port configuration" +
            "\n- Server at maximum connections" +
            "\n- Authentication failure (wrong password/ACL)" +
            "\n\nTroubleshooting:" +
            "\n- Check cache server logs for errors" +
            "\n- Verify connection pool configuration" +
            "\n- Review application.yml cache configuration" +
            "\n- Check server metrics (CPU, memory, connections)" +
            "\n- Test connection using redis-cli" +
            "\n- Review network latency and packet loss" +
            "\n\nRetry Strategy:" +
            "\n- Implement exponential backoff with jitter" +
            "\n- Set maximum retry attempts (e.g., 3-5)" +
            "\n- Consider circuit breaker pattern for repeated failures" +
            "\n- Implement fallback to L1 cache or primary data source",
            message,
            host,
            port,
            host,
            port,
            host,
            port
        );
    }
}
