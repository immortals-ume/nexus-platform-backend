package com.immortals.notification.service.service.impl;

import com.immortals.platform.domain.notifications.dto.WebhookPayload;
import com.immortals.platform.domain.notifications.domain.model.DeliveryStatus;
import com.immortals.platform.domain.notifications.entity.NotificationLog;
import com.immortals.notification.service.repository.NotificationLogRepository;
import com.immortals.notification.service.repository.NotificationRepository;
import com.immortals.notification.service.service.DeliveryTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Implementation of DeliveryTrackingService
 * Handles tracking and updating notification delivery status from provider webhooks
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryTrackingServiceImpl implements DeliveryTrackingService {
    
    private final NotificationRepository notificationRepository;
    private final NotificationLogRepository notificationLogRepository;
    
    @Override
    public void processWebhook(String providerId, WebhookPayload payload) {
        log.info("Processing webhook from provider: {}", providerId);
        // Implementation for generic webhook processing
    }
    
    @Override
    public DeliveryStatus getDeliveryStatus(String notificationId) {
        // Implementation for getting delivery status
        return null;
    }
    
    @Override
    public void updateStatus(String notificationId, DeliveryStatus.Status status, String details) {
        log.info("Updating status for notification: {} to {}", notificationId, status);
        // Implementation for updating status
    }
    
    @Override
    @Transactional
    public void updateDeliveryStatus(
        String providerId,
        String providerMessageId,
        String status,
        String errorCode,
        String errorMessage,
        Map<String, String> metadata
    ) {
        try {
            log.info("Updating delivery status - Provider: {}, MessageId: {}, Status: {}", 
                providerId, providerMessageId, status);
            
            // For now, we'll log the webhook data
            // In a complete implementation, we would:
            // 1. Find the notification by providerMessageId
            // 2. Update its delivery status
            // 3. Log the status change
            
            // Create notification log entry
            NotificationLog logEntry = new NotificationLog();
            logEntry.setEventId("webhook-" + providerMessageId);
            logEntry.setNotificationType("WEBHOOK");
            logEntry.setRecipient("N/A");
            logEntry.setProviderId(providerId);
            logEntry.setProviderMessageId(providerMessageId);
            logEntry.setStatus("WEBHOOK_RECEIVED");
            logEntry.setDeliveryStatus(status);
            logEntry.setErrorMessage(errorMessage);
            logEntry.setMessage(String.format("Webhook received - Status: %s, MessageId: %s", 
                status, providerMessageId));
            
            // Add metadata
            Map<String, String> logMetadata = new java.util.HashMap<>();
            logMetadata.put("providerMessageId", providerMessageId);
            if (errorCode != null) {
                logMetadata.put("errorCode", errorCode);
            }
            if (errorMessage != null) {
                logMetadata.put("errorMessage", errorMessage);
            }
            if (metadata != null) {
                logMetadata.putAll(metadata);
            }
            logEntry.setMetadata(logMetadata);
            
            notificationLogRepository.save(logEntry);
            
            log.info("Delivery status updated successfully for provider message: {}", providerMessageId);
            
        } catch (Exception e) {
            log.error("Error updating delivery status for provider message: {}", providerMessageId, e);
            throw e;
        }
    }
}
