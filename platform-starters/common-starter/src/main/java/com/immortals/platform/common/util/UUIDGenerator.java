package com.immortals.platform.common.util;

import java.util.UUID;

/**
 * Utility class for generating UUIDs.
 * Provides methods for generating correlation IDs and other unique identifiers.
 */
public final class UUIDGenerator {

    private UUIDGenerator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Generates a random UUID as a string
     */
    public static String generate() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generates a random UUID
     */
    public static UUID generateUUID() {
        return UUID.randomUUID();
    }

    /**
     * Generates a correlation ID for request tracing
     */
    public static String generateCorrelationId() {
        return generate();
    }

    /**
     * Generates a correlation ID with a prefix
     */
    public static String generateCorrelationId(String prefix) {
        return prefix + "-" + generate();
    }

    /**
     * Generates a short correlation ID (first 8 characters of UUID)
     */
    public static String generateShortCorrelationId() {
        return generate().substring(0, 8);
    }

    /**
     * Generates a short correlation ID with a prefix
     */
    public static String generateShortCorrelationId(String prefix) {
        return prefix + "-" + generateShortCorrelationId();
    }

    /**
     * Parses a string to UUID, returns null if invalid
     */
    public static UUID parseUUID(String uuidString) {
        if (StringUtils.isEmpty(uuidString)) {
            return null;
        }
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Validates if a string is a valid UUID
     */
    public static boolean isValidUUID(String uuidString) {
        return parseUUID(uuidString) != null;
    }
}
