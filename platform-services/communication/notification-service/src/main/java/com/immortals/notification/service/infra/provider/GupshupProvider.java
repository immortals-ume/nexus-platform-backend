package com.immortals.notification.service.infra.provider;

import com.immortals.platform.domain.notifications.domain.model.Notification;
import com.immortals.platform.domain.notifications.domain.port.NotificationProvider;
import com.immortals.platform.domain.notifications.dto.ProviderRequest;
import com.immortals.platform.domain.notifications.dto.ProviderResponse;
import com.immortals.platform.domain.notifications.entity.ProviderConfigEntity;
import com.immortals.notification.service.repository.ProviderConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Gupshup provider implementation for SMS and WhatsApp notifications
 * Supports SMS and WhatsApp channels with Gupshup API
 */
@Slf4j
@Component
public class GupshupProvider implements NotificationProvider {
    
    private static final String PROVIDER_ID = "GUPSHUP";
    private static final String SMS_ENDPOINT = "/sm/api/v1/msg";
    private static final String WHATSAPP_ENDPOINT = "/sm/api/v1/app/msg";
    
    private final RestTemplate gupshupRestTemplate;
    private final ProviderConfigRepository providerConfigRepository;
    private ProviderConfigEntity config;
    private boolean initialized = false;
    
    public GupshupProvider(
            @Qualifier("gupshupRestTemplate") RestTemplate gupshupRestTemplate,
            ProviderConfigRepository providerConfigRepository) {
        this.gupshupRestTemplate = gupshupRestTemplate;
        this.providerConfigRepository = providerConfigRepository;
        initializeProvider();
    }
    
    /**
     * Initialize Gupshup provider with configuration from database
     */
    private void initializeProvider() {
        try {
            Optional<ProviderConfigEntity> configOpt = providerConfigRepository.findByProviderId(PROVIDER_ID);
            if (configOpt.isPresent()) {
                this.config = configOpt.get();
                
                // Validate required credentials
                String apiKey = config.getCredentials().get("api-key");
                
                if (apiKey != null && !apiKey.isEmpty()) {
                    initialized = true;
                    log.info("Gupshup provider initialized successfully");
                } else {
                    log.error("Gupshup API key missing in configuration");
                }
            } else {
                log.warn("Gupshup provider configuration not found in database");
            }
        } catch (Exception e) {
            log.error("Failed to initialize Gupshup provider", e);
        }
    }
    
    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }
    
    @Override
    public Notification.NotificationType getChannel() {
        // Gupshup supports both SMS and WhatsApp, return SMS as primary
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
            log.info("Sending {} notification via Gupshup to {}", request.getChannel(), request.getRecipient());
            
            // Route to appropriate endpoint based on channel
            if (request.getChannel() == Notification.NotificationType.WHATSAPP) {
                return sendWhatsAppMessage(request);
            } else {
                return sendSmsMessage(request);
            }
            
        } catch (HttpClientErrorException e) {
            log.error("Gupshup client error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return ProviderResponse.failure(
                PROVIDER_ID, 
                "Client error: " + e.getMessage(), 
                String.valueOf(e.getStatusCode().value())
            );
        } catch (HttpServerErrorException e) {
            log.error("Gupshup server error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return ProviderResponse.failure(
                PROVIDER_ID, 
                "Server error: " + e.getMessage(), 
                String.valueOf(e.getStatusCode().value())
            );
        } catch (Exception e) {
            log.error("Unexpected error sending Gupshup notification", e);
            return ProviderResponse.failure(
                PROVIDER_ID, 
                "Unexpected error: " + e.getMessage(), 
                "INTERNAL_ERROR"
            );
        }
    }
    
    /**
     * Send SMS message via Gupshup
     */
    private ProviderResponse sendSmsMessage(ProviderRequest request) {
        String apiKey = config.getCredentials().get("api-key");
        
        // Build request parameters
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("userid", config.getConfiguration().get("user-id").toString());
        params.add("password", apiKey);
        params.add("send_to", request.getRecipient());
        params.add("msg", request.getMessage());
        params.add("msg_type", "TEXT");
        params.add("method", "sendMessage");
        params.add("format", "json");
        
        // Optional: Add sender ID if configured
        if (config.getConfiguration().containsKey("sender-id")) {
            params.add("mask", config.getConfiguration().get("sender-id").toString());
        }
        
        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
        
        // Make API call
        ResponseEntity<Map> response = gupshupRestTemplate.postForEntity(
            SMS_ENDPOINT,
            entity,
            Map.class
        );
        
        // Parse response
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> responseBody = response.getBody();
            String status = responseBody.get("response").toString();
            
            if ("success".equalsIgnoreCase(status)) {
                String messageId = responseBody.get("id") != null ? 
                    responseBody.get("id").toString() : null;
                
                log.info("Gupshup SMS sent successfully. Message ID: {}", messageId);
                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("gupshupMessageId", messageId);
                metadata.put("gupshupResponse", responseBody);
                
                return ProviderResponse.builder()
                    .success(true)
                    .providerId(PROVIDER_ID)
                    .providerMessageId(messageId)
                    .deliveryStatus(Notification.DeliveryStatus.SENT)
                    .metadata(metadata)
                    .build();
            } else {
                String errorMessage = responseBody.get("message") != null ? 
                    responseBody.get("message").toString() : "Unknown error";
                return ProviderResponse.failure(PROVIDER_ID, errorMessage, "SMS_SEND_FAILED");
            }
        }
        
        return ProviderResponse.failure(PROVIDER_ID, "Invalid response from Gupshup", "INVALID_RESPONSE");
    }
    
    /**
     * Send WhatsApp message via Gupshup
     */
    private ProviderResponse sendWhatsAppMessage(ProviderRequest request) {
        String apiKey = config.getCredentials().get("api-key");
        String appName = config.getConfiguration().get("app-name").toString();
        
        // Build request body for WhatsApp
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("channel", "whatsapp");
        requestBody.put("source", appName);
        requestBody.put("destination", request.getRecipient());
        requestBody.put("message", Map.of(
            "type", "text",
            "text", request.getMessage()
        ));
        
        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", apiKey);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        // Make API call
        ResponseEntity<Map> response = gupshupRestTemplate.postForEntity(
            WHATSAPP_ENDPOINT,
            entity,
            Map.class
        );
        
        // Parse response
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> responseBody = response.getBody();
            String status = responseBody.get("status") != null ? 
                responseBody.get("status").toString() : "";
            
            if ("submitted".equalsIgnoreCase(status) || "success".equalsIgnoreCase(status)) {
                String messageId = responseBody.get("messageId") != null ? 
                    responseBody.get("messageId").toString() : null;
                
                log.info("Gupshup WhatsApp sent successfully. Message ID: {}", messageId);
                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("gupshupMessageId", messageId);
                metadata.put("gupshupResponse", responseBody);
                
                return ProviderResponse.builder()
                    .success(true)
                    .providerId(PROVIDER_ID)
                    .providerMessageId(messageId)
                    .deliveryStatus(Notification.DeliveryStatus.SENT)
                    .metadata(metadata)
                    .build();
            } else {
                String errorMessage = responseBody.get("message") != null ? 
                    responseBody.get("message").toString() : "Unknown error";
                return ProviderResponse.failure(PROVIDER_ID, errorMessage, "WHATSAPP_SEND_FAILED");
            }
        }
        
        return ProviderResponse.failure(PROVIDER_ID, "Invalid response from Gupshup", "INVALID_RESPONSE");
    }
    
    @Override
    public Notification.DeliveryStatus checkStatus(String providerMessageId) {
        if (!initialized) {
            return Notification.DeliveryStatus.PENDING;
        }
        
        // Gupshup status checking would require additional API calls
        // For now, return PENDING as status updates come via webhooks
        log.debug("Status check for Gupshup message ID: {}", providerMessageId);
        return Notification.DeliveryStatus.PENDING;
    }
    
    @Override
    public boolean isHealthy() {
        if (!initialized || config == null) {
            return false;
        }
        
        try {
            // Simple health check - verify credentials are set
            String apiKey = config.getCredentials().get("api-key");
            return apiKey != null && !apiKey.isEmpty() && config.isAvailable();
        } catch (Exception e) {
            log.error("Gupshup health check failed", e);
            return false;
        }
    }
    
    @Override
    public int getPriority() {
        return config != null ? config.getPriority() : 100;
    }
}
