package com.immortals.platform.common.exception;

import java.io.Serial;

/**
 * Exception thrown when serialization or deserialization of cache values fails.
 * 
 * <p>This exception indicates that the cache value cannot be converted to/from the format
 * required for storage in the cache (typically JSON or Java serialization).
 * 
 * <p>Common causes include:
 * <ul>
 *   <li>Missing no-arg constructor in cached objects</li>
 *   <li>Non-serializable fields in cached objects</li>
 *   <li>Version mismatches between serialized and deserialized classes</li>
 *   <li>Circular references in object graphs</li>
 *   <li>Incompatible type changes in cached objects</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * try {
 *     cacheService.put("user:123", userObject);
 * } catch (CacheSerializationException e) {
 *     log.error("Failed to serialize user object: {}", e.getMessage());
 *     // Handle serialization failure
 * }
 * }</pre>
 * 
 * @since 2.0.0
 */
public class CacheSerializationException extends CacheException {

    @Serial
    private static final long serialVersionUID = 1L;
    private static final String ERROR_CODE = "CACHE_SERIALIZATION_ERROR";

    private final Class<?> valueType;
    private final SerializationDirection direction;

    /**
     * Creates a new CacheSerializationException with the specified message.
     *
     * @param message the error message
     */
    public CacheSerializationException(String message) {
        super(message);
        this.valueType = null;
        this.direction = SerializationDirection.UNKNOWN;
    }

    /**
     * Creates a new CacheSerializationException with the specified message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public CacheSerializationException(String message, Throwable cause) {
        super(message, cause);
        this.valueType = null;
        this.direction = SerializationDirection.UNKNOWN;
    }

    /**
     * Creates a new CacheSerializationException with detailed type information.
     *
     * @param message the error message
     * @param cacheKey the cache key being accessed
     * @param operation the cache operation being performed
     * @param valueType the type of value being serialized/deserialized
     * @param direction whether this is serialization or deserialization
     * @param cause the underlying cause
     */
    public CacheSerializationException(String message, String cacheKey, CacheException.CacheOperation operation,
                                      Class<?> valueType, SerializationDirection direction, Throwable cause) {
        super(buildDetailedMessage(message, cacheKey, valueType, direction), cacheKey, operation, cause);
        this.valueType = valueType;
        this.direction = direction;
    }

    /**
     * Returns the type of value that failed to serialize/deserialize.
     *
     * @return the value type, or null if not available
     */
    public Class<?> getValueType() {
        return valueType;
    }

    /**
     * Returns the direction of serialization (to/from cache).
     *
     * @return the serialization direction
     */
    public SerializationDirection getDirection() {
        return direction;
    }

    /**
     * Builds a detailed error message with troubleshooting guidance.
     */
    private static String buildDetailedMessage(String message, String cacheKey,
                                              Class<?> valueType, SerializationDirection direction) {
        String typeName = valueType != null ? valueType.getSimpleName() : "Unknown";
        String directionStr = direction == SerializationDirection.SERIALIZATION ? "serializing" : "deserializing";

        return String.format(
            "%s (Key: '%s', Type: %s, Direction: %s)" +
            "\n\nCommon Causes:" +
            "\n1. Missing no-arg constructor in %s class" +
            "\n2. Non-serializable fields (e.g., streams, connections)" +
            "\n3. Circular references in object graph" +
            "\n4. Class version mismatch (serialVersionUID)" +
            "\n5. Missing Jackson annotations for complex types" +
            "\n6. Incompatible type changes after deployment" +
            "\n\nSolutions:" +
            "\n1. Ensure class has a no-arg constructor" +
            "\n2. Mark non-serializable fields as transient or @JsonIgnore" +
            "\n3. Use @JsonIdentityInfo to handle circular references" +
            "\n4. Add explicit serialVersionUID to classes" +
            "\n5. Use @JsonTypeInfo for polymorphic types" +
            "\n6. Implement custom serializer/deserializer if needed" +
            "\n7. Clear cache after incompatible class changes" +
            "\n\nBest Practices:" +
            "\n- Use simple DTOs for caching instead of complex domain objects" +
            "\n- Avoid caching objects with transient state" +
            "\n- Version your cached data structures" +
            "\n- Implement cache eviction strategy for schema changes" +
            "\n- Test serialization in unit tests" +
            "\n\nDebugging:" +
            "\n- Enable debug logging: logging.level.com.fasterxml.jackson=DEBUG" +
            "\n- Test serialization manually: objectMapper.writeValueAsString(object)" +
            "\n- Check for circular references using object graph visualization" +
            "\n- Review class structure for serialization compatibility",
            message,
            cacheKey,
            typeName,
            directionStr,
            typeName
        );
    }

    /**
     * Enumeration of serialization directions.
     */
    public enum SerializationDirection {
        /** Serializing object to cache (write operation) */
        SERIALIZATION,

        /** Deserializing object from cache (read operation) */
        DESERIALIZATION,

        /** Direction unknown or not applicable */
        UNKNOWN
    }
}
