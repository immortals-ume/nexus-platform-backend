package com.immortals.platform.domain.util;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Immutable phone number with country code support.
 * Automatically normalizes and validates phone number format.
 * Supports parsing from various formats and multiple output formats.
 */
public record PhoneNumber(
    @NotBlank String countryCode,
    @NotBlank @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits") String number
) {
    /**
     * Compact constructor with validation and normalization
     */
    public PhoneNumber {
        if (countryCode == null || countryCode.isBlank()) {
            throw new IllegalArgumentException("Country code cannot be null or blank");
        }
        if (number == null || number.isBlank()) {
            throw new IllegalArgumentException("Phone number cannot be null or blank");
        }
        
        // Normalize country code (remove + if present)
        countryCode = countryCode.trim().replaceAll("^\\+", "");
        
        // Normalize number (remove all non-digits)
        number = number.replaceAll("[^0-9]", "");
        
        if (!countryCode.matches("^[0-9]{1,3}$")) {
            throw new IllegalArgumentException("Country code must be 1-3 digits");
        }
        if (!number.matches("^[0-9]{10}$")) {
            throw new IllegalArgumentException("Phone number must be exactly 10 digits");
        }
    }

    /**
     * Create US phone number
     */
    public static PhoneNumber us(String number) {
        return new PhoneNumber("1", number);
    }

    /**
     * Create phone number from full string (e.g., "+1-555-123-4567")
     */
    public static PhoneNumber parse(String fullNumber) {
        if (fullNumber == null || fullNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number cannot be null or blank");
        }
        
        // Remove all non-digits
        String digits = fullNumber.replaceAll("[^0-9]", "");
        
        if (digits.length() < 10) {
            throw new IllegalArgumentException("Phone number too short");
        }
        
        // Extract country code and number
        String countryCode;
        String number;
        
        if (digits.length() == 10) {
            // Assume US number
            countryCode = "1";
            number = digits;
        } else if (digits.length() == 11 && digits.startsWith("1")) {
            // US number with country code
            countryCode = "1";
            number = digits.substring(1);
        } else {
            // International number - take first 1-3 digits as country code
            int codeLength = Math.min(3, digits.length() - 10);
            countryCode = digits.substring(0, codeLength);
            number = digits.substring(codeLength);
        }
        
        return new PhoneNumber(countryCode, number);
    }

    /**
     * Get formatted phone number with country code
     */
    public String getFormatted() {
        return String.format("+%s-%s-%s-%s", 
            countryCode,
            number.substring(0, 3),
            number.substring(3, 6),
            number.substring(6));
    }

    /**
     * Get formatted phone number without country code
     */
    public String getFormattedLocal() {
        return String.format("%s-%s-%s", 
            number.substring(0, 3),
            number.substring(3, 6),
            number.substring(6));
    }

    /**
     * Get full number with country code (no formatting)
     */
    public String getFullNumber() {
        return "+" + countryCode + number;
    }

    /**
     * Check if this is a US phone number
     */
    public boolean isUSNumber() {
        return "1".equals(countryCode);
    }

    @Override
    public String toString() {
        return getFormatted();
    }
}
