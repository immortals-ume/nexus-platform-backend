package com.immortals.platform.common.util;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utility class for common validation patterns.
 * Provides validation methods for various data types and formats.
 */
public final class ValidationUtils {

    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$"
    );

    /**
     * Validates that an object is not null
     */
    public static void requireNonNull(Object obj, String fieldName) {
        if (obj == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
    }

    /**
     * Validates that a string is not null or empty
     */
    public static void requireNonEmpty(String str, String fieldName) {
        if (StringUtils.isEmpty(str)) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }

    /**
     * Validates that a string is not null or blank
     */
    public static void requireNonBlank(String str, String fieldName) {
        if (StringUtils.isBlank(str)) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
    }

    /**
     * Validates that a collection is not null or empty
     */
    public static void requireNonEmpty(Collection<?> collection, String fieldName) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }

    /**
     * Validates that a map is not null or empty
     */
    public static void requireNonEmpty(Map<?, ?> map, String fieldName) {
        if (map == null || map.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }

    /**
     * Validates that a number is positive
     */
    public static void requirePositive(Number number, String fieldName) {
        if (number == null || number.doubleValue() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }

    /**
     * Validates that a number is non-negative
     */
    public static void requireNonNegative(Number number, String fieldName) {
        if (number == null || number.doubleValue() < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
    }

    /**
     * Validates that a number is within a range (inclusive)
     */
    public static void requireInRange(Number number, Number min, Number max, String fieldName) {
        if (number == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        double value = number.doubleValue();
        double minValue = min.doubleValue();
        double maxValue = max.doubleValue();

        if (value < minValue || value > maxValue) {
            throw new IllegalArgumentException(
                    String.format("%s must be between %s and %s", fieldName, min, max)
            );
        }
    }

    /**
     * Validates that a string length is within a range
     */
    public static void requireLengthBetween(String str, int minLength, int maxLength, String fieldName) {
        if (str == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        int length = str.length();
        if (length < minLength || length > maxLength) {
            throw new IllegalArgumentException(
                    String.format("%s length must be between %d and %d characters",
                            fieldName, minLength, maxLength)
            );
        }
    }

    /**
     * Validates that a string has a minimum length
     */
    public static void requireMinLength(String str, int minLength, String fieldName) {
        if (str == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        if (str.length() < minLength) {
            throw new IllegalArgumentException(
                    String.format("%s must be at least %d characters", fieldName, minLength)
            );
        }
    }

    /**
     * Validates that a string has a maximum length
     */
    public static void requireMaxLength(String str, int maxLength, String fieldName) {
        if (str == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        if (str.length() > maxLength) {
            throw new IllegalArgumentException(
                    String.format("%s must be at most %d characters", fieldName, maxLength)
            );
        }
    }

    /**
     * Validates that a string is a valid email address
     */
    public static void requireValidEmail(String email, String fieldName) {
        if (!StringUtils.isValidEmail(email)) {
            throw new IllegalArgumentException(fieldName + " must be a valid email address");
        }
    }

    /**
     * Validates that a string is a valid phone number
     */
    public static void requireValidPhone(String phone, String fieldName) {
        if (!StringUtils.isValidPhone(phone)) {
            throw new IllegalArgumentException(fieldName + " must be a valid phone number");
        }
    }

    /**
     * Validates that a string is a valid UUID
     */
    public static void requireValidUuid(String uuid, String fieldName) {
        if (StringUtils.isEmpty(uuid) || !UUID_PATTERN.matcher(uuid).matches()) {
            throw new IllegalArgumentException(fieldName + " must be a valid UUID");
        }
    }

    /**
     * Validates that a string is a valid URL
     */
    public static void requireValidUrl(String url, String fieldName) {
        if (StringUtils.isEmpty(url) || !URL_PATTERN.matcher(url).matches()) {
            throw new IllegalArgumentException(fieldName + " must be a valid URL");
        }
    }

    /**
     * Validates that a string is a valid IPv4 address
     */
    public static void requireValidIpv4(String ip, String fieldName) {
        if (StringUtils.isEmpty(ip) || !IPV4_PATTERN.matcher(ip).matches()) {
            throw new IllegalArgumentException(fieldName + " must be a valid IPv4 address");
        }
    }

    /**
     * Validates that a string matches a pattern
     */
    public static void requirePattern(String str, Pattern pattern, String fieldName, String patternDescription) {
        if (StringUtils.isEmpty(str) || !pattern.matcher(str).matches()) {
            throw new IllegalArgumentException(
                    String.format("%s must match pattern: %s", fieldName, patternDescription)
            );
        }
    }

    /**
     * Validates that a condition is true
     */
    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Checks if a string is a valid UUID (returns boolean)
     */
    public static boolean isValidUuid(String uuid) {
        return StringUtils.isNotEmpty(uuid) && UUID_PATTERN.matcher(uuid).matches();
    }

    /**
     * Checks if a string is a valid URL (returns boolean)
     */
    public static boolean isValidUrl(String url) {
        return StringUtils.isNotEmpty(url) && URL_PATTERN.matcher(url).matches();
    }

    /**
     * Checks if a string is a valid IPv4 address (returns boolean)
     */
    public static boolean isValidIpv4(String ip) {
        return StringUtils.isNotEmpty(ip) && IPV4_PATTERN.matcher(ip).matches();
    }

    /**
     * Validates that a collection size is within a range
     */
    public static void requireSizeBetween(Collection<?> collection, int minSize, int maxSize, String fieldName) {
        if (collection == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        int size = collection.size();
        if (size < minSize || size > maxSize) {
            throw new IllegalArgumentException(
                    String.format("%s size must be between %d and %d", fieldName, minSize, maxSize)
            );
        }
    }
}
