package com.immortals.platform.domain.notifications.dto;

import com.immortals.platform.domain.notifications.domain.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO from provider after notification delivery attempt
 * Contains delivery status, provider tracking ID, and error details if any
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderResponse {
    
    private boolean success;
    private String providerId;
    private String providerMessageId;
    private Notification.DeliveryStatus deliveryStatus;
    private String errorMessage;
    private String errorCode;
    private LocalDateTime sentAt;
    private Map<String, Object> metadata;
    
    /**
     * Create a successful response
     */
    public static ProviderResponse success(String providerId, String providerMessageId) {
        return ProviderResponse.builder()
                .success(true)
                .providerId(providerId)
                .providerMessageId(providerMessageId)
                .deliveryStatus(Notification.DeliveryStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create a failed response
     */
    public static ProviderResponse failure(String providerId, String errorMessage, String errorCode) {
        return ProviderResponse.builder()
                .success(false)
                .providerId(providerId)
                .deliveryStatus(Notification.DeliveryStatus.FAILED)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .sentAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * Check if the response indicates a retryable error
     */
    public boolean isRetryable() {
        if (success) {
            return false;
        }
        
        // Non-retryable error codes
        if (errorCode != null) {
            String code = errorCode.toUpperCase();
            // 4xx errors are typically non-retryable (client errors)
            if (code.startsWith("4") || 
                code.contains("INVALID") || 
                code.contains("UNAUTHORIZED") ||
                code.contains("FORBIDDEN") ||
                code.contains("NOT_FOUND")) {
                return false;
            }
        }
        
        // 5xx errors and network errors are retryable
        return true;
    }
}
