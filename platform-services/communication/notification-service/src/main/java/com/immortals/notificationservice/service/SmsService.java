package com.immortals.notificationservice.service;

/**
 * Service interface for sending SMS notifications
 */
public interface SmsService {
    
    /**
     * Send an SMS notification
     * 
     * @param to recipient phone number
     * @param message SMS message content
     * @return true if SMS was sent successfully, false otherwise
     */
    boolean sendSms(String to, String message);
}
