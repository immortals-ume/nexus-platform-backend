package com.immortals.notification.service.application.usecase.port;

import com.immortals.platform.domain.notifications.domain.Notification;
import com.immortals.platform.domain.notifications.dto.ProviderRequest;
import com.immortals.platform.domain.notifications.dto.ProviderResponse;

/**
 * Port interface for notification delivery providers (Hexagonal Architecture)
 * Enhanced to support multi-provider routing, health checks, and status tracking
 */
public interface NotificationProvider {
    
    /**
     * Get unique provider identifier (e.g., TWILIO, GUPSHUP, AWS_SNS, MAILCHIMP)
     */
    String getProviderId();
    
    /**
     * Get the notification channel this provider supports
     */
    Notification.NotificationType getChannel();
    
    /**
     * Check if this provider supports the given channel and country code
     * 
     * @param channel the notification channel
     * @param countryCode the recipient's country code (e.g., US, IN, GB)
     * @return true if supported, false otherwise
     */
    boolean supports(Notification.NotificationType channel, String countryCode);
    
    /**
     * Send notification through the provider
     * 
     * @param request the provider request containing notification details
     * @return ProviderResponse with delivery status and provider message ID
     */
    ProviderResponse send(ProviderRequest request);
    
    /**
     * Check delivery status of a notification
     * 
     * @param providerMessageId the provider's tracking ID
     * @return current delivery status
     */
    Notification.DeliveryStatus checkStatus(String providerMessageId);
    
    /**
     * Check if the provider is healthy and available
     * 
     * @return true if healthy, false otherwise
     */
    boolean isHealthy();
    
    /**
     * Get provider priority (lower is higher priority)
     */
    default int getPriority() {
        return 100;
    }
    
    /**
     * Legacy method for backward compatibility
     * @deprecated Use send(ProviderRequest) instead
     */
    @Deprecated
    default boolean send(Notification notification) {
        ProviderRequest request = ProviderRequest.fromNotification(notification);
        ProviderResponse response = send(request);
        return response.isSuccess();
    }
    
    /**
     * Legacy method for backward compatibility
     * @deprecated Use supports(NotificationType, String) instead
     */
    @Deprecated
    default boolean supports(Notification.NotificationType type) {
        return getChannel() == type;
    }
}
