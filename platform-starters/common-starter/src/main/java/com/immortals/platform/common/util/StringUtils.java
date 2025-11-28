package com.immortals.platform.common.util;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Utility class for string manipulation operations.
 * Provides common string processing functions.
 */
public final class StringUtils {

    private StringUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[1-9]\\d{1,14}$"
    );

    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9]+$"
    );

    /**
     * Checks if a string is null or empty
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * Checks if a string is null, empty, or contains only whitespace
     */
    public static boolean isBlank(String str) {
        return str == null || str.isBlank();
    }

    /**
     * Checks if a string is not null and not empty
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * Checks if a string is not null, not empty, and contains non-whitespace characters
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * Trims a string, returns null if the input is null
     */
    public static String trim(String str) {
        return str == null ? null : str.trim();
    }

    /**
     * Trims a string, returns empty string if the input is null
     */
    public static String trimToEmpty(String str) {
        return str == null ? "" : str.trim();
    }

    /**
     * Converts a string to lowercase, returns null if the input is null
     */
    public static String toLowerCase(String str) {
        return str == null ? null : str.toLowerCase();
    }

    /**
     * Converts a string to uppercase, returns null if the input is null
     */
    public static String toUpperCase(String str) {
        return str == null ? null : str.toUpperCase();
    }

    /**
     * Capitalizes the first character of a string
     */
    public static String capitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Converts a string to camelCase
     */
    public static String toCamelCase(String str) {
        if (isEmpty(str)) {
            return str;
        }
        String[] words = str.split("[\\s_-]+");
        StringBuilder result = new StringBuilder(words[0].toLowerCase());
        for (int i = 1; i < words.length; i++) {
            result.append(capitalize(words[i].toLowerCase()));
        }
        return result.toString();
    }

    /**
     * Converts a string to snake_case
     */
    public static String toSnakeCase(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.replaceAll("([a-z])([A-Z])", "$1_$2")
                .replaceAll("[\\s-]+", "_")
                .toLowerCase();
    }

    /**
     * Converts a string to kebab-case
     */
    public static String toKebabCase(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.replaceAll("([a-z])([A-Z])", "$1-$2")
                .replaceAll("[\\s_]+", "-")
                .toLowerCase();
    }

    /**
     * Truncates a string to a maximum length
     */
    public static String truncate(String str, int maxLength) {
        if (isEmpty(str) || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength);
    }

    /**
     * Truncates a string to a maximum length and adds ellipsis
     */
    public static String truncateWithEllipsis(String str, int maxLength) {
        if (isEmpty(str) || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * Joins a collection of strings with a delimiter
     */
    public static String join(Collection<String> strings, String delimiter) {
        if (strings == null || strings.isEmpty()) {
            return "";
        }
        return String.join(delimiter, strings);
    }

    /**
     * Joins an array of strings with a delimiter
     */
    public static String join(String[] strings, String delimiter) {
        if (strings == null || strings.length == 0) {
            return "";
        }
        return String.join(delimiter, strings);
    }

    /**
     * Validates if a string is a valid email address
     */
    public static boolean isValidEmail(String email) {
        if (isEmpty(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validates if a string is a valid phone number (E.164 format)
     */
    public static boolean isValidPhone(String phone) {
        if (isEmpty(phone)) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * Checks if a string contains only alphanumeric characters
     */
    public static boolean isAlphanumeric(String str) {
        if (isEmpty(str)) {
            return false;
        }
        return ALPHANUMERIC_PATTERN.matcher(str).matches();
    }

    /**
     * Masks a string, showing only the first and last n characters
     */
    public static String mask(String str, int visibleChars, char maskChar) {
        if (isEmpty(str) || str.length() <= visibleChars * 2) {
            return str;
        }
        String start = str.substring(0, visibleChars);
        String end = str.substring(str.length() - visibleChars);
        String masked = String.valueOf(maskChar).repeat(str.length() - visibleChars * 2);
        return start + masked + end;
    }

    /**
     * Masks an email address
     */
    public static String maskEmail(String email) {
        if (isEmpty(email) || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];

        if (localPart.length() <= 2) {
            return mask(localPart, 1, '*') + "@" + domain;
        }
        return mask(localPart, 2, '*') + "@" + domain;
    }

    /**
     * Removes all whitespace from a string
     */
    public static String removeWhitespace(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.replaceAll("\\s+", "");
    }

    /**
     * Replaces multiple consecutive whitespace characters with a single space
     */
    public static String normalizeWhitespace(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.replaceAll("\\s+", " ").trim();
    }

    /**
     * Checks if a string contains another string (case-insensitive)
     */
    public static boolean containsIgnoreCase(String str, String searchStr) {
        if (str == null || searchStr == null) {
            return false;
        }
        return str.toLowerCase().contains(searchStr.toLowerCase());
    }

    /**
     * Returns a default value if the string is null or empty
     */
    public static String defaultIfEmpty(String str, String defaultValue) {
        return isEmpty(str) ? defaultValue : str;
    }

    /**
     * Returns a default value if the string is null or blank
     */
    public static String defaultIfBlank(String str, String defaultValue) {
        return isBlank(str) ? defaultValue : str;
    }
}
