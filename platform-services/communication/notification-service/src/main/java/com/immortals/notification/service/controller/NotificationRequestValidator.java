package com.immortals.notification.service.controller;

import com.immortals.platform.common.util.StringUtils;
import com.immortals.platform.common.util.ValidationUtils;
import com.immortals.platform.domain.notifications.dto.SendNotificationRequest;
import org.springframework.stereotype.Component;

/**
 * Custom validator for notification requests
 * Uses common-starter's ValidationUtils for validation
 * Requirements: 1.1
 */
@Component
public class NotificationRequestValidator {

    /**
     * Validate SendNotificationRequest
     * Performs custom validation beyond standard JSR-303 annotations
     */
    public void validate(SendNotificationRequest request) {
        // Validate that either message or templateCode is provided
        if (StringUtils.isBlank(request.getMessage()) && StringUtils.isBlank(request.getTemplateCode())) {
            throw new IllegalArgumentException("Either message or templateCode must be provided");
        }

        // Validate EMAIL-specific requirements
        if ("EMAIL".equalsIgnoreCase(request.getType())) {
            ValidationUtils.requireNonBlank(request.getSubject(), "Subject");
            
            // Validate email format
            if (!isValidEmail(request.getRecipient())) {
                throw new IllegalArgumentException("Recipient must be a valid email address for EMAIL notifications");
            }
        }

        // Validate SMS/WHATSAPP-specific requirements
        if ("SMS".equalsIgnoreCase(request.getType()) || "WHATSAPP".equalsIgnoreCase(request.getType())) {
            // Validate phone number format (basic validation)
            if (!isValidPhoneNumber(request.getRecipient())) {
                throw new IllegalArgumentException("Recipient must be a valid phone number for SMS/WhatsApp notifications");
            }
        }

        // Validate template variables are provided if template code is specified
        if (StringUtils.isNotBlank(request.getTemplateCode()) && 
            (request.getTemplateVariables() == null || request.getTemplateVariables().isEmpty())) {
            // This is just a warning - template might not require variables
            // Log warning but don't fail validation
        }

        // Validate scheduled time is in the future
        if (request.getScheduledAt() != null && request.getScheduledAt().isBefore(java.time.LocalDateTime.now())) {
            throw new IllegalArgumentException("Scheduled time must be in the future");
        }

        // Validate country code format if provided
        if (StringUtils.isNotBlank(request.getCountryCode())) {
            ValidationUtils.requireLengthBetween(request.getCountryCode(), 2, 3, "Country code");
        }

        // Validate locale format if provided
        if (StringUtils.isNotBlank(request.getLocale())) {
            if (!isValidLocale(request.getLocale())) {
                throw new IllegalArgumentException("Locale must be in format: language_COUNTRY (e.g., en_US)");
            }
        }
    }

    /**
     * Basic email validation
     */
    private boolean isValidEmail(String email) {
        if (StringUtils.isBlank(email)) {
            return false;
        }
        // Simple email pattern validation
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * Basic phone number validation
     * Accepts formats: +1234567890, 1234567890, +1-234-567-8900, etc.
     */
    private boolean isValidPhoneNumber(String phone) {
        if (StringUtils.isBlank(phone)) {
            return false;
        }
        // Remove common phone number formatting characters
        String cleaned = phone.replaceAll("[\\s\\-\\(\\)]", "");
        // Check if it starts with + and contains only digits after that, or just digits
        return cleaned.matches("^\\+?[0-9]{10,15}$");
    }

    /**
     * Validate locale format (e.g., en_US, fr_FR)
     */
    private boolean isValidLocale(String locale) {
        if (StringUtils.isBlank(locale)) {
            return false;
        }
        // Locale format: language_COUNTRY (e.g., en_US)
        return locale.matches("^[a-z]{2}_[A-Z]{2}$");
    }
}
