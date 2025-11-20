package com.immortals.notificationservice.service;

import com.immortals.notificationservice.api.dto.WebhookPayload;
import com.immortals.notificationservice.domain.model.DeliveryStatus;

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
}
