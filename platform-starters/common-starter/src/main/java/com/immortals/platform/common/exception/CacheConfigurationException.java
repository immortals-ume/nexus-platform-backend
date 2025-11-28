package com.immortals.platform.common.exception;

import lombok.Getter;

import java.io.Serial;

/**
 * Exception thrown when cache configuration is invalid or incomplete.
 *
 * <p>This exception is typically thrown during application startup when the cache
 * configuration is validated. It indicates that the cache service cannot be initialized
 * due to missing or invalid configuration properties.
 *
 * <p>Common causes include:
 * <ul>
 *   <li>Missing required configuration properties (host, port)</li>
 *   <li>Invalid configuration values (negative timeouts, invalid pool sizes)</li>
 *   <li>Conflicting configuration options</li>
 *   <li>Missing dependencies for enabled features</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * if (properties.getHost() == null) {
 *     throw new CacheConfigurationException(
 *         "Cache host must be configured",
 *         "immortals.cache.redis.host"
 *     );
 * }
 * }</pre>
 *
 * @since 2.0.0
 */
@Getter
public class CacheConfigurationException extends CacheException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * -- GETTER --
     *  Returns the configuration property that is invalid.
     *
     */
    private final String configProperty;
    /**
     * -- GETTER --
     *  Returns the invalid value that was provided.
     *
     */
    private final transient Object invalidValue;

    /**
     * Creates a new CacheConfigurationException with the specified message.
     *
     * @param message the error message
     */
    public CacheConfigurationException(String message) {
        super(message);
        this.configProperty = null;
        this.invalidValue = null;
    }

    /**
     * Creates a new CacheConfigurationException with the specified message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public CacheConfigurationException(String message, Throwable cause) {
        super(message, cause);
        this.configProperty = null;
        this.invalidValue = null;
    }

    /**
     * Creates a new CacheConfigurationException with the specific configuration property.
     *
     * @param message the error message
     * @param configProperty the configuration property that is invalid
     */
    public CacheConfigurationException(String message, String configProperty) {
        super(buildDetailedMessage(message, configProperty, null));
        this.configProperty = configProperty;
        this.invalidValue = null;
    }

    /**
     * Creates a new CacheConfigurationException with the specific configuration property and value.
     *
     * @param message the error message
     * @param configProperty the configuration property that is invalid
     * @param invalidValue the invalid value that was provided
     */
    public CacheConfigurationException(String message, String configProperty, Object invalidValue) {
        super(buildDetailedMessage(message, configProperty, invalidValue));
        this.configProperty = configProperty;
        this.invalidValue = invalidValue;
    }

    /**
     * Builds a detailed error message with configuration guidance.
     */
    private static String buildDetailedMessage(String message, String configProperty, Object invalidValue) {
        StringBuilder sb = new StringBuilder(message);

        if (configProperty != null) {
            sb.append("\n\nConfiguration Property: ").append(configProperty);
        }

        if (invalidValue != null) {
            sb.append("\nProvided Value: ").append(invalidValue);
        }

        sb.append("\n\nConfiguration Guide:");
        sb.append("\n1. Review application.yml or application.properties");
        sb.append("\n2. Check environment variables for overrides");
        sb.append("\n3. Verify configuration property names and values");
        sb.append("\n4. Ensure required dependencies are included");

        sb.append("\n\nRequired Configuration:");
        sb.append("\nimmortals:");
        sb.append("\n  cache:");
        sb.append("\n    type: redis              # Cache type: caffeine, redis, multi-level");
        sb.append("\n    redis:");
        sb.append("\n      host: localhost        # Redis server host");
        sb.append("\n      port: 6379            # Redis server port");
        sb.append("\n      password: ${REDIS_PASSWORD:}  # Optional password");
        sb.append("\n      database: 0           # Redis database index");

        sb.append("\n\nOptional Configuration:");
        sb.append("\n      command-timeout: 5s   # Command timeout");
        sb.append("\n      default-ttl: 1h       # Default TTL");
        sb.append("\n      use-ssl: false        # Enable SSL/TLS");

        sb.append("\n\nCaffeine Configuration:");
        sb.append("\n    caffeine:");
        sb.append("\n      maximum-size: 10000   # Maximum cache entries");
        sb.append("\n      expire-after-write: 1h  # Expiration time");

        sb.append("\n\nMulti-Level Configuration:");
        sb.append("\n    type: multi-level");
        sb.append("\n    caffeine:");
        sb.append("\n      maximum-size: 5000");
        sb.append("\n    redis:");
        sb.append("\n      host: localhost");

        sb.append("\n\nFeature Configuration:");
        sb.append("\n    features:");
        sb.append("\n      compression:");
        sb.append("\n        enabled: true");
        sb.append("\n        threshold: 1024");
        sb.append("\n      encryption:");
        sb.append("\n        enabled: true");
        sb.append("\n        key: ${ENCRYPTION_KEY}");

        sb.append("\n\nResilience Configuration:");
        sb.append("\n    resilience:");
        sb.append("\n      circuit-breaker:");
        sb.append("\n        enabled: true");
        sb.append("\n        failure-rate-threshold: 50");
        sb.append("\n      stampede-protection: true");

        sb.append("\n\nValidation Rules:");
        sb.append("\n- host: must not be null or empty");
        sb.append("\n- port: must be between 1 and 65535");
        sb.append("\n- maximum-size: must be positive");
        sb.append("\n- timeouts: must be positive durations");
        sb.append("\n- threshold: must be non-negative");

        sb.append("\n\nTroubleshooting:");
        sb.append("\n- Enable debug logging: logging.level.com.immortals.cache=DEBUG");
        sb.append("\n- Check for typos in property names");
        sb.append("\n- Verify property values match expected types");
        sb.append("\n- Review Spring Boot configuration documentation");
        sb.append("\n- Check for conflicting configuration sources");

        return sb.toString();
    }
}
