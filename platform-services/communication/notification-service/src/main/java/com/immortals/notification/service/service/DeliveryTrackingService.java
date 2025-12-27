package com.immortals.notification.service.service;

import com.immortals.notification.service.api.dto.WebhookPayload;
import com.immortals.notification.service.domain.model.DeliveryStatus;

import java.util.Map;

/**
 * Service for tracking notification delivery status
 */
public interface DeliveryTrackingService {
    
    /**
     * Process webhook from provider
     */
    void processWebhook(String providerId, WebhookPayload payload);
    
    /**
     * Get delivery status for notification
     */
    DeliveryStatus getDeliveryStatus(String notificationId);
    
    /**
     * Update delivery status
     */
    void updateStatus(String notificationId, DeliveryStatus.Status status, String details);
    
    /**
     * Update delivery status from provider webhook
     * 
     * @param providerId Provider identifier (e.g., TWILIO, GUPSHUP)
     * @param providerMessageId Provider's message tracking ID
     * @param status Delivery status
     * @param errorCode Error code if failed
     * @param errorMessage Error message if failed
     * @param metadata Additional webhook metadata
     */
    void updateDeliveryStatus(
        String providerId,
        String providerMessageId,
        String status,
        String errorCode,
        String errorMessage,
        Map<String, String> metadata
    );
}
