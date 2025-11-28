package com.example.discovery.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Utility class for sanitizing user inputs to prevent injection attacks
 * Detects and sanitizes potentially malicious patterns including:
 * - SQL injection attempts
 * - XSS (Cross-Site Scripting) attempts
 * - Command injection attempts
 * - Path traversal attempts
 * - LDAP injection attempts
 */
@Slf4j
@Component
public class InputSanitizer {
    
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "('.*(--|;|\\*|/\\*|\\*/|xp_|sp_|exec|execute|select|insert|update|delete|drop|create|alter|union|script).*')|" +
        "(\\b(select|insert|update|delete|drop|create|alter|union|exec|execute|script)\\b)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(<script[^>]*>.*?</script>)|" +
        "(<iframe[^>]*>.*?</iframe>)|" +
        "(javascript:)|" +
        "(on\\w+\\s*=)|" +
        "(<img[^>]*onerror[^>]*>)|" +
        "(<svg[^>]*onload[^>]*>)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile(
        "(;\\s*(rm|cat|ls|wget|curl|nc|bash|sh|cmd|powershell)\\s)|" +
        "(\\||&&|`|\\$\\()",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
        "(\\.\\./)|(\\.\\\\)|" +
        "(%2e%2e/)|(%2e%2e\\\\)|" +
        "(\\.\\.%2f)|(\\.\\.%5c)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern LDAP_INJECTION_PATTERN = Pattern.compile(
        "(\\*\\)|\\(\\||\\(&)|" +
        "(\\)\\(cn=)|" +
        "(\\*\\)\\(\\|\\(objectclass=\\*\\)\\))",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern JNDI_INJECTION_PATTERN = Pattern.compile(
        "(\\$\\{jndi:)|" +
        "(\\$\\{ldap:)|" +
        "(\\$\\{rmi:)|" +
        "(\\$\\{dns:)",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Validates input for malicious patterns
     * @param input The input string to validate
     * @return true if input is safe, false if potentially malicious
     */
    public boolean isSafe(String input) {
        if (input == null || input.isBlank()) {
            return true;
        }
        
        return !containsMaliciousPattern(input);
    }
    
    /**
     * Checks if input contains any malicious patterns
     * @param input The input string to check
     * @return true if malicious pattern detected, false otherwise
     */
    public boolean containsMaliciousPattern(String input) {
        if (input == null) {
            return false;
        }
        
        if (SQL_INJECTION_PATTERN.matcher(input).find()) {
            log.warn("SQL injection pattern detected in input");
            return true;
        }
        
        if (XSS_PATTERN.matcher(input).find()) {
            log.warn("XSS pattern detected in input");
            return true;
        }
        
        if (COMMAND_INJECTION_PATTERN.matcher(input).find()) {
            log.warn("Command injection pattern detected in input");
            return true;
        }
        
        if (PATH_TRAVERSAL_PATTERN.matcher(input).find()) {
            log.warn("Path traversal pattern detected in input");
            return true;
        }
        
        if (LDAP_INJECTION_PATTERN.matcher(input).find()) {
            log.warn("LDAP injection pattern detected in input");
            return true;
        }
        
        if (JNDI_INJECTION_PATTERN.matcher(input).find()) {
            log.warn("JNDI injection pattern detected in input");
            return true;
        }
        
        return false;
    }
    
    /**
     * Sanitizes input by removing potentially dangerous characters
     * @param input The input string to sanitize
     * @return Sanitized string
     */
    public String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        
        String sanitized = input.replaceAll("<[^>]*>", "");
        
        sanitized = sanitized.replaceAll("(?i)<script[^>]*>.*?</script>", "");
        
        sanitized = sanitized.replaceAll("(?i)on\\w+\\s*=\\s*[\"'][^\"']*[\"']", "");
        
        sanitized = sanitized.replaceAll("(?i)javascript:", "");
        
        sanitized = sanitized.replace("\0", "");
        
        return sanitized;
    }
    
    /**
     * Validates and throws exception if input is malicious
     * @param input The input to validate
     * @param fieldName The name of the field being validated
     * @throws IllegalArgumentException if input contains malicious patterns
     */
    public void validateOrThrow(String input, String fieldName) {
        if (!isSafe(input)) {
            throw new IllegalArgumentException(
                String.format("Invalid input for field '%s': potentially malicious pattern detected", fieldName)
            );
        }
    }
}
