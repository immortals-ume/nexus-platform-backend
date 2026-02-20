package com.immortals.notification.service.service.impl;

import com.immortals.platform.domain.notifications.domain.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility service for extracting country codes from phone numbers and emails
 */
@Component
@Slf4j
public class CountryCodeExtractor {
    
    // Pattern to extract country code from phone number (e.g., +1, +91, +44)
    private static final Pattern PHONE_COUNTRY_CODE_PATTERN = Pattern.compile("^\\+?(\\d{1,3})");
    
    /**
     * Extract country code from recipient based on notification type
     * 
     * @param recipient the recipient (phone number or email)
     * @param type the notification type
     * @return country code (e.g., "US", "IN", "GB") or default "*"
     */
    public String extractCountryCode(String recipient, Notification.NotificationType type) {
        if (recipient == null || recipient.isBlank()) {
            log.warn("Empty recipient, using default country code");
            return "*";
        }
        
        return switch (type) {
            case SMS, WHATSAPP, PUSH_NOTIFICATION -> extractFromPhoneNumber(recipient);
            case EMAIL -> extractFromEmail(recipient);
        };
    }
    
    /**
     * Extract country code from phone number
     * Maps common country calling codes to ISO country codes
     */
    private String extractFromPhoneNumber(String phoneNumber) {
        Matcher matcher = PHONE_COUNTRY_CODE_PATTERN.matcher(phoneNumber.trim());
        
        if (matcher.find()) {
            String callingCode = matcher.group(1);
            String countryCode = mapCallingCodeToCountry(callingCode);
            log.debug("Extracted country code: {} from phone: {}", countryCode, phoneNumber);
            return countryCode;
        }
        
        log.warn("Could not extract country code from phone: {}, using default", phoneNumber);
        return "*"; // Default/global
    }
    
    /**
     * Extract country code from email domain
     * This is a simplified implementation - in production, you might use a more sophisticated approach
     */
    private String extractFromEmail(String email) {
        // For emails, we typically can't reliably determine country from domain
        // Return global/default
        log.debug("Using default country code for email: {}", email);
        return "*";
    }
    
    /**
     * Map calling code to ISO country code
     * This is a simplified mapping - in production, use a comprehensive library
     */
    private String mapCallingCodeToCountry(String callingCode) {
        return switch (callingCode) {
            case "1" -> "US";      // United States/Canada
            case "44" -> "GB";     // United Kingdom
            case "91" -> "IN";     // India
            case "92" -> "PK";     // Pakistan
            case "880" -> "BD";    // Bangladesh
            case "94" -> "LK";     // Sri Lanka
            case "61" -> "AU";     // Australia
            case "64" -> "NZ";     // New Zealand
            case "81" -> "JP";     // Japan
            case "82" -> "KR";     // South Korea
            case "86" -> "CN";     // China
            case "33" -> "FR";     // France
            case "49" -> "DE";     // Germany
            case "39" -> "IT";     // Italy
            case "34" -> "ES";     // Spain
            case "7" -> "RU";      // Russia
            case "55" -> "BR";     // Brazil
            case "52" -> "MX";     // Mexico
            case "27" -> "ZA";     // South Africa
            case "234" -> "NG";    // Nigeria
            case "254" -> "KE";    // Kenya
            case "20" -> "EG";     // Egypt
            case "971" -> "AE";    // UAE
            case "966" -> "SA";    // Saudi Arabia
            case "65" -> "SG";     // Singapore
            case "60" -> "MY";     // Malaysia
            case "62" -> "ID";     // Indonesia
            case "63" -> "PH";     // Philippines
            case "66" -> "TH";     // Thailand
            case "84" -> "VN";     // Vietnam
            default -> "*";        // Global/default
        };
    }
}
