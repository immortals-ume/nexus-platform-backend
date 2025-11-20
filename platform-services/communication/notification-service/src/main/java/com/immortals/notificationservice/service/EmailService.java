package com.immortals.notificationservice.service;

/**
 * Service interface for sending email notifications
 */
public interface EmailService {
    
    /**
     * Send an email notification
     * 
     * @param to recipient email address
     * @param subject email subject
     * @param body email body content
     * @return true if email was sent successfully, false otherwise
     */
    boolean sendEmail(String to, String subject, String body);
    
    /**
     * Send an email notification with HTML content
     * 
     * @param to recipient email address
     * @param subject email subject
     * @param htmlBody HTML email body content
     * @return true if email was sent successfully, false otherwise
     */
    boolean sendHtmlEmail(String to, String subject, String htmlBody);
}
