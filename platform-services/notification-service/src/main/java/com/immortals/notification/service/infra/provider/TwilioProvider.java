package com.immortals.notification.service.infra.provider;

import com.immortals.platform.domain.notifications.domain.Notification;
import com.immortals.notification.service.application.usecase.port.NotificationProvider;
import com.immortals.platform.domain.notifications.dto.ProviderRequest;
import com.immortals.platform.domain.notifications.dto.ProviderResponse;
import com.immortals.platform.domain.notifications.entity.ProviderConfig;
import com.immortals.notification.service.repository.ProviderConfigRepository;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Twilio provider implementation for SMS and WhatsApp notifications
 * Supports SMS and WhatsApp channels with Twilio API
 */
@Slf4j
@Component
public class TwilioProvider implements NotificationProvider {
    
    private static final String PROVIDER_ID = "TWILIO";
    private static final String WHATSAPP_PREFIX = "whatsapp:";
    
    private final ProviderConfigRepository providerConfigRepository;
    private ProviderConfig config;
    private boolean initialized = false;
    
    public TwilioProvider(ProviderConfigRepository providerConfigRepository) {
        this.providerConfigRepository = providerConfigRepository;
        initializeProvider();
    }
    
    /**
     * Initialize Twilio provider with configuration from database
     */
    private void initializeProvider() {
        try {
            Optional<ProviderConfig> configOpt = providerConfigRepository.findByProviderId(PROVIDER_ID);
            if (configOpt.isPresent()) {
                this.config = configOpt.get();
                
                // Initialize Twilio SDK
                String accountSid = config.getCredentials().get("account-sid");
                String authToken = config.getCredentials().get("auth-token");
                
                if (accountSid != null && authToken != null) {
                    Twilio.init(accountSid, authToken);
                    initialized = true;
                    log.info("Twilio provider initialized successfully");
                } else {
                    log.error("Twilio credentials missing in configuration");
                }
            } else {
                log.warn("Twilio provider configuration not found in database");
            }
        } catch (Exception e) {
            log.error("Failed to initialize Twilio provider", e);
        }
    }
    
    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }
    
    @Override
    public Notification.NotificationType getChannel() {
        // Twilio supports both SMS and WhatsApp, return SMS as primary
        return Notification.NotificationType.SMS;
    }
    
    @Override
    public boolean supports(Notification.NotificationType channel, String countryCode) {
        if (!initialized || config == null || !config.isAvailable()) {
            return false;
        }
        
        // Check if channel is supported (SMS or WhatsApp)
        if (channel != Notification.NotificationType.SMS && 
            channel != Notification.NotificationType.WHATSAPP) {
            return false;
        }
        
        // Check if country is supported
        if (countryCode == null) {
            return true; // Allow if no country code specified
        }
        
        return config.supportsCountry(countryCode);
    }
    
    @Override
    public ProviderResponse send(ProviderRequest request) {
        if (!initialized) {
            return ProviderResponse.failure(PROVIDER_ID, "Provider not initialized", "PROVIDER_NOT_INITIALIZED");
        }
        
        try {
            log.info("Sending {} notification via Twilio to {}", request.getChannel(), request.getRecipient());
            
            // Get from number based on channel type
            String fromNumber = getFromNumber(request.getChannel());
            if (fromNumber == null) {
                return ProviderResponse.failure(PROVIDER_ID, "From number not configured", "MISSING_FROM_NUMBER");
            }
            
            // Format recipient based on channel
            String toNumber = formatRecipient(request.getRecipient(), request.getChannel());
            
            // Create and send message
            Message message = Message.creator(
                new PhoneNumber(toNumber),
                new PhoneNumber(fromNumber),
                request.getMessage()
            ).create();
            
            log.info("Twilio message sent successfully. SID: {}, Status: {}", 
                message.getSid(), message.getStatus());
            
            // Build metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("twilioSid", message.getSid());
            metadata.put("twilioStatus", message.getStatus().toString());
            metadata.put("numSegments", message.getNumSegments());
            
            return ProviderResponse.builder()
                .success(true)
                .providerId(PROVIDER_ID)
                .providerMessageId(message.getSid())
                .deliveryStatus(mapTwilioStatus(message.getStatus().toString()))
                .metadata(metadata)
                .build();
                
        } catch (ApiException e) {
            log.error("Twilio API error: {} - {}", e.getCode(), e.getMessage(), e);
            return ProviderResponse.failure(
                PROVIDER_ID, 
                e.getMessage(), 
                String.valueOf(e.getCode())
            );
        } catch (Exception e) {
            log.error("Unexpected error sending Twilio notification", e);
            return ProviderResponse.failure(
                PROVIDER_ID, 
                "Unexpected error: " + e.getMessage(), 
                "INTERNAL_ERROR"
            );
        }
    }
    
    @Override
    public Notification.DeliveryStatus checkStatus(String providerMessageId) {
        if (!initialized) {
            return Notification.DeliveryStatus.PENDING;
        }
        
        try {
            Message message = Message.fetcher(providerMessageId).fetch();
            return mapTwilioStatus(message.getStatus().toString());
        } catch (Exception e) {
            log.error("Failed to check Twilio message status for SID: {}", providerMessageId, e);
            return Notification.DeliveryStatus.PENDING;
        }
    }
    
    @Override
    public boolean isHealthy() {
        if (!initialized || config == null) {
            return false;
        }
        
        try {
            // Simple health check - verify credentials are set
            String accountSid = config.getCredentials().get("account-sid");
            String authToken = config.getCredentials().get("auth-token");
            return accountSid != null && authToken != null && config.isAvailable();
        } catch (Exception e) {
            log.error("Twilio health check failed", e);
            return false;
        }
    }
    
    @Override
    public int getPriority() {
        return config != null ? config.getPriority() : 100;
    }
    
    /**
     * Get the from number based on channel type
     */
    private String getFromNumber(Notification.NotificationType channel) {
        if (config == null || config.getConfiguration() == null) {
            return null;
        }
        
        if (channel == Notification.NotificationType.WHATSAPP) {
            Object whatsappFrom = config.getConfiguration().get("whatsapp-from");
            if (whatsappFrom != null) {
                String number = whatsappFrom.toString();
                return number.startsWith(WHATSAPP_PREFIX) ? number : WHATSAPP_PREFIX + number;
            }
        } else {
            Object fromNumber = config.getConfiguration().get("from-number");
            return fromNumber != null ? fromNumber.toString() : null;
        }
        
        return null;
    }
    
    /**
     * Format recipient based on channel type
     */
    private String formatRecipient(String recipient, Notification.NotificationType channel) {
        if (channel == Notification.NotificationType.WHATSAPP) {
            return recipient.startsWith(WHATSAPP_PREFIX) ? recipient : WHATSAPP_PREFIX + recipient;
        }
        return recipient;
    }
    
    /**
     * Map Twilio status to our DeliveryStatus enum
     */
    private Notification.DeliveryStatus mapTwilioStatus(String twilioStatus) {
        return switch (twilioStatus.toUpperCase()) {
            case "QUEUED", "ACCEPTED" -> Notification.DeliveryStatus.PENDING;
            case "SENDING", "SENT" -> Notification.DeliveryStatus.SENT;
            case "DELIVERED" -> Notification.DeliveryStatus.DELIVERED;
            case "READ" -> Notification.DeliveryStatus.READ;
            case "FAILED", "UNDELIVERED" -> Notification.DeliveryStatus.FAILED;
            default -> Notification.DeliveryStatus.PENDING;
        };
    }
}
