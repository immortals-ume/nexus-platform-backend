package com.immortals.platform.security.sanitization;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Input sanitizer for XSS and injection attack prevention.
 */
@Slf4j
@Component
public class InputSanitizer {

    // XSS patterns
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern IFRAME_PATTERN = Pattern.compile("<iframe[^>]*>.*?</iframe>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern OBJECT_PATTERN = Pattern.compile("<object[^>]*>.*?</object>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern EMBED_PATTERN = Pattern.compile("<embed[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE);
    private static final Pattern ONERROR_PATTERN = Pattern.compile("onerror\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern ONLOAD_PATTERN = Pattern.compile("onload\\s*=", Pattern.CASE_INSENSITIVE);

    // SQL injection patterns
    private static final Pattern SQL_COMMENT_PATTERN = Pattern.compile("--");
    private static final Pattern SQL_UNION_PATTERN = Pattern.compile("\\bunion\\b.*\\bselect\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_DROP_PATTERN = Pattern.compile("\\bdrop\\b.*\\btable\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_DELETE_PATTERN = Pattern.compile("\\bdelete\\b.*\\bfrom\\b", Pattern.CASE_INSENSITIVE);

    /**
     * Sanitize input to prevent XSS attacks.
     */
    public String sanitizeXSS(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String sanitized = input;
        
        // Remove dangerous HTML tags
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = IFRAME_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = OBJECT_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = EMBED_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove javascript: protocol
        sanitized = JAVASCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove event handlers
        sanitized = ONERROR_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = ONLOAD_PATTERN.matcher(sanitized).replaceAll("");
        
        // Encode special characters
        sanitized = sanitized.replace("<", "&lt;")
                             .replace(">", "&gt;")
                             .replace("\"", "&quot;")
                             .replace("'", "&#x27;")
                             .replace("/", "&#x2F;");

        if (!sanitized.equals(input)) {
            log.warn("XSS attempt detected and sanitized");
        }

        return sanitized;
    }

    /**
     * Check for SQL injection patterns.
     */
    public boolean containsSQLInjection(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        boolean hasSQLInjection = SQL_COMMENT_PATTERN.matcher(input).find() ||
                                 SQL_UNION_PATTERN.matcher(input).find() ||
                                 SQL_DROP_PATTERN.matcher(input).find() ||
                                 SQL_DELETE_PATTERN.matcher(input).find();

        if (hasSQLInjection) {
            log.warn("SQL injection attempt detected in input");
        }

        return hasSQLInjection;
    }

    /**
     * Sanitize input for safe use in SQL queries.
     * Note: This is a basic sanitizer. Always use parameterized queries!
     */
    public String sanitizeSQL(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Escape single quotes
        String sanitized = input.replace("'", "''");
        
        // Remove SQL comments
        sanitized = SQL_COMMENT_PATTERN.matcher(sanitized).replaceAll("");

        return sanitized;
    }

    /**
     * Sanitize filename to prevent path traversal attacks.
     */
    public String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return filename;
        }

        // Remove path traversal attempts
        String sanitized = filename.replace("../", "")
                                  .replace("..\\", "")
                                  .replace("/", "")
                                  .replace("\\", "");

        // Remove null bytes
        sanitized = sanitized.replace("\0", "");

        if (!sanitized.equals(filename)) {
            log.warn("Path traversal attempt detected in filename");
        }

        return sanitized;
    }

    /**
     * Validate and sanitize URL.
     */
    public boolean isValidUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        // Check for valid URL pattern
        Pattern urlPattern = Pattern.compile(
                "^(https?://)" +                           // Protocol
                "([a-zA-Z0-9.-]+)" +                       // Domain
                "(:[0-9]{1,5})?" +                         // Optional port
                "(/[a-zA-Z0-9._~:/?#\\[\\]@!$&'()*+,;=-]*)?" + // Optional path
                "$"
        );

        return urlPattern.matcher(url).matches();
    }

    /**
     * Sanitize general text input.
     */
    public String sanitizeText(String input) {
        if (input == null) {
            return null;
        }

        // Trim whitespace
        String sanitized = input.trim();
        
        // Remove control characters except newline and tab
        sanitized = sanitized.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "");
        
        // Normalize whitespace
        sanitized = sanitized.replaceAll("\\s+", " ");

        return sanitized;
    }
}
