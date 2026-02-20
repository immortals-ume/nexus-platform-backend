package com.immortals.notification.service.service.impl;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * Utility class for masking sensitive data in logs
 * Masks phone numbers, emails, and other PII
 */
@Slf4j
public class SensitiveDataMasker {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "\\+?[0-9]{1,4}?[-.\\s]?\\(?[0-9]{1,3}?\\)?[-.\\s]?[0-9]{1,4}[-.\\s]?[0-9]{1,4}[-.\\s]?[0-9]{1,9}"
    );

    /**
     * Mask email address
     * Example: john.doe@example.com -> j***e@e***e.com
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "***";
        }

        try {
            int atIndex = email.indexOf('@');
            if (atIndex <= 0) {
                return maskGeneric(email);
            }

            String localPart = email.substring(0, atIndex);
            String domainPart = email.substring(atIndex + 1);

            String maskedLocal = maskGeneric(localPart);
            String maskedDomain = maskDomain(domainPart);

            return maskedLocal + "@" + maskedDomain;
        } catch (Exception e) {
            log.warn("Error masking email, using generic masking", e);
            return maskGeneric(email);
        }
    }

    /**
     * Mask phone number
     * Example: +1234567890 -> +***7890
     */
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return "***";
        }

        try {
            // Remove all non-digit characters except +
            String cleaned = phoneNumber.replaceAll("[^0-9+]", "");
            
            if (cleaned.length() < 4) {
                return "***";
            }

            // Keep country code (if present) and last 4 digits
            String prefix = cleaned.startsWith("+") ? "+" : "";
            String lastFour = cleaned.substring(cleaned.length() - 4);
            
            return prefix + "***" + lastFour;
        } catch (Exception e) {
            log.warn("Error masking phone number, using generic masking", e);
            return "***";
        }
    }

    /**
     * Mask domain name
     * Example: example.com -> e***e.com
     */
    private static String maskDomain(String domain) {
        if (domain == null || domain.length() < 4) {
            return "***";
        }

        int dotIndex = domain.lastIndexOf('.');
        if (dotIndex <= 0) {
            return maskGeneric(domain);
        }

        String domainName = domain.substring(0, dotIndex);
        String tld = domain.substring(dotIndex);

        return maskGeneric(domainName) + tld;
    }

    /**
     * Generic masking - show first and last character
     * Example: sensitive -> s***e
     */
    private static String maskGeneric(String value) {
        if (value == null || value.length() < 2) {
            return "***";
        }

        if (value.length() == 2) {
            return value.charAt(0) + "*";
        }

        return value.charAt(0) + "***" + value.charAt(value.length() - 1);
    }

    /**
     * Mask all emails in a string
     */
    public static String maskEmailsInString(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        return EMAIL_PATTERN.matcher(text).replaceAll(matchResult -> {
            String email = matchResult.group();
            return maskEmail(email);
        });
    }

    /**
     * Mask all phone numbers in a string
     */
    public static String maskPhoneNumbersInString(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        return PHONE_PATTERN.matcher(text).replaceAll(matchResult -> {
            String phone = matchResult.group();
            return maskPhoneNumber(phone);
        });
    }

    /**
     * Mask all sensitive data (emails and phone numbers) in a string
     */
    public static String maskAllSensitiveData(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String masked = maskEmailsInString(text);
        masked = maskPhoneNumbersInString(masked);
        
        return masked;
    }

    /**
     * Determine if a string is an email or phone number and mask accordingly
     */
    public static String maskRecipient(String recipient) {
        if (recipient == null || recipient.isEmpty()) {
            return "***";
        }

        if (recipient.contains("@")) {
            return maskEmail(recipient);
        } else {
            return maskPhoneNumber(recipient);
        }
    }
}
