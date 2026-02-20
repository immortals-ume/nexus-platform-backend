package com.immortals.notification.service.infra.provider;

import com.immortals.platform.domain.notifications.domain.Notification;
import com.immortals.notification.service.application.usecase.port.NotificationProvider;
import com.immortals.platform.domain.notifications.dto.ProviderRequest;
import com.immortals.platform.domain.notifications.dto.ProviderResponse;
import com.immortals.platform.domain.notifications.entity.ProviderConfig;
import com.immortals.notification.service.repository.ProviderConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Mailchimp Transactional API (Mandrill) provider implementation for email notifications
 * Supports EMAIL channel with HTML and plain text emails
 */
@Slf4j
@Component
public class MailchimpProvider implements NotificationProvider {
    
    private static final String PROVIDER_ID = "MAILCHIMP";
    private static final String SEND_ENDPOINT = "/messages/send.json";
    
    private final RestTemplate mailchimpRestTemplate;
    private final ProviderConfigRepository providerConfigRepository;
    private ProviderConfig config;
    private boolean initialized = false;
    
    public MailchimpProvider(
            @Qualifier("mailchimpRestTemplate") RestTemplate mailchimpRestTemplate,
            ProviderConfigRepository providerConfigRepository) {
        this.mailchimpRestTemplate = mailchimpRestTemplate;
        this.providerConfigRepository = providerConfigRepository;
        initializeProvider();
    }
    
    /**
     * Initialize Mailchimp provider with configuration from database
     */
    private void initializeProvider() {
        try {
            Optional<ProviderConfig> configOpt = providerConfigRepository.findByProviderId(PROVIDER_ID);
            if (configOpt.isPresent()) {
                this.config = configOpt.get();
                
                // Verify API key is present
                String apiKey = config.getCredentials().get("api-key");
                
                if (apiKey != null && !apiKey.isEmpty()) {
                    initialized = true;
                    log.info("Mailchimp provider initialized successfully");
                } else {
                    log.error("Mailchimp API key missing in configuration");
                }
            } else {
                log.warn("Mailchimp provider configuration not found in database");
            }
        } catch (Exception e) {
            log.error("Failed to initialize Mailchimp provider", e);
        }
    }
    
    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }
    
    @Override
    public Notification.NotificationType getChannel() {
        return Notification.NotificationType.EMAIL;
    }
    
    @Override
    public boolean supports(Notification.NotificationType channel, String countryCode) {
        if (!initialized || config == null || !config.isAvailable()) {
            return false;
        }
        
        // Only support EMAIL channel
        if (channel != Notification.NotificationType.EMAIL) {
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
            log.info("Sending email notification via Mailchimp to {}", request.getRecipient());
            
            // Build Mailchimp API request payload
            Map<String, Object> payload = buildMailchimpPayload(request);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(payload, headers);
            
            // Send request to Mailchimp API
            ResponseEntity<List> response = mailchimpRestTemplate.postForEntity(
                SEND_ENDPOINT,
                httpEntity,
                List.class
            );
            
            // Parse response
            if (response.getBody() != null && !response.getBody().isEmpty()) {
                Map<String, Object> result = (Map<String, Object>) response.getBody().get(0);
                String status = (String) result.get("status");
                String messageId = (String) result.get("_id");
                String email = (String) result.get("email");
                String rejectReason = (String) result.get("reject_reason");
                
                log.info("Mailchimp email sent. Status: {}, Message ID: {}, Email: {}", 
                    status, messageId, email);
                
                // Check if email was rejected
                if ("rejected".equals(status) || "invalid".equals(status)) {
                    return ProviderResponse.failure(
                        PROVIDER_ID,
                        "Email rejected: " + (rejectReason != null ? rejectReason : status),
                        status.toUpperCase()
                    );
                }
                
                // Build metadata
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("mailchimpMessageId", messageId);
                metadata.put("mailchimpStatus", status);
                metadata.put("email", email);
                if (rejectReason != null) {
                    metadata.put("rejectReason", rejectReason);
                }
                
                return ProviderResponse.builder()
                    .success(true)
                    .providerId(PROVIDER_ID)
                    .providerMessageId(messageId)
                    .deliveryStatus(mapMailchimpStatus(status))
                    .metadata(metadata)
                    .build();
            } else {
                return ProviderResponse.failure(
                    PROVIDER_ID,
                    "Empty response from Mailchimp API",
                    "EMPTY_RESPONSE"
                );
            }
            
        } catch (HttpClientErrorException e) {
            log.error("Mailchimp API client error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return ProviderResponse.failure(
                PROVIDER_ID,
                "Client error: " + e.getMessage(),
                e.getStatusCode().toString()
            );
        } catch (HttpServerErrorException e) {
            log.error("Mailchimp API server error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return ProviderResponse.failure(
                PROVIDER_ID,
                "Server error: " + e.getMessage(),
                e.getStatusCode().toString()
            );
        } catch (Exception e) {
            log.error("Unexpected error sending Mailchimp notification", e);
            return ProviderResponse.failure(
                PROVIDER_ID,
                "Unexpected error: " + e.getMessage(),
                "INTERNAL_ERROR"
            );
        }
    }
    
    /**
     * Build Mailchimp API request payload
     */
    private Map<String, Object> buildMailchimpPayload(ProviderRequest request) {
        Map<String, Object> payload = new HashMap<>();
        
        // Add API key
        payload.put("key", config.getCredentials().get("api-key"));
        
        // Build message object
        Map<String, Object> message = new HashMap<>();
        
        // Set subject
        if (request.getSubject() != null && !request.getSubject().isEmpty()) {
            message.put("subject", request.getSubject());
        } else {
            message.put("subject", "Notification");
        }
        
        // Set from email and name
        String fromEmail = getFromEmail();
        String fromName = getFromName();
        message.put("from_email", fromEmail);
        if (fromName != null && !fromName.isEmpty()) {
            message.put("from_name", fromName);
        }
        
        // Set recipient
        List<Map<String, String>> to = new ArrayList<>();
        Map<String, String> recipient = new HashMap<>();
        recipient.put("email", request.getRecipient());
        recipient.put("type", "to");
        to.add(recipient);
        message.put("to", to);
        
        // Set content - support both HTML and plain text
        if (request.getHtmlContent() != null && !request.getHtmlContent().isEmpty()) {
            message.put("html", request.getHtmlContent());
        } else {
            message.put("text", request.getMessage());
        }
        
        // Set additional options
        message.put("auto_text", true); // Auto-generate text version from HTML
        message.put("auto_html", false); // Don't auto-generate HTML from text
        message.put("inline_css", true); // Inline CSS for better email client support
        message.put("important", request.getPriority() == Notification.NotificationPriority.HIGH);
        
        // Add tracking options
        message.put("track_opens", true);
        message.put("track_clicks", true);
        
        // Add metadata if present
        if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
            List<Map<String, String>> metadata = new ArrayList<>();
            request.getMetadata().forEach((key, value) -> {
                Map<String, String> metaItem = new HashMap<>();
                metaItem.put("name", key);
                metaItem.put("content", value);
                metadata.add(metaItem);
            });
            message.put("metadata", metadata);
        }
        
        payload.put("message", message);
        
        // Set async to false for immediate response
        payload.put("async", false);
        
        return payload;
    }
    
    /**
     * Get from email from configuration
     */
    private String getFromEmail() {
        if (config.getConfiguration() != null && config.getConfiguration().containsKey("from-email")) {
            return config.getConfiguration().get("from-email").toString();
        }
        return "noreply@example.com"; // Default fallback
    }
    
    /**
     * Get from name from configuration
     */
    private String getFromName() {
        if (config.getConfiguration() != null && config.getConfiguration().containsKey("from-name")) {
            return config.getConfiguration().get("from-name").toString();
        }
        return null;
    }
    
    /**
     * Map Mailchimp status to our DeliveryStatus enum
     */
    private Notification.DeliveryStatus mapMailchimpStatus(String mailchimpStatus) {
        return switch (mailchimpStatus.toLowerCase()) {
            case "sent" -> Notification.DeliveryStatus.SENT;
            case "queued", "scheduled" -> Notification.DeliveryStatus.PENDING;
            case "rejected", "invalid" -> Notification.DeliveryStatus.FAILED;
            default -> Notification.DeliveryStatus.PENDING;
        };
    }
    
    @Override
    public Notification.DeliveryStatus checkStatus(String providerMessageId) {
        if (!initialized) {
            return Notification.DeliveryStatus.PENDING;
        }
        
        // Mailchimp status updates come via webhooks
        // This method would require additional API calls to check message info
        log.debug("Status check for Mailchimp message ID: {}", providerMessageId);
        return Notification.DeliveryStatus.PENDING;
    }
    
    @Override
    public boolean isHealthy() {
        if (!initialized || config == null) {
            return false;
        }
        
        try {
            // Simple health check - verify API key is set
            String apiKey = config.getCredentials().get("api-key");
            return apiKey != null && !apiKey.isEmpty() && config.isAvailable();
        } catch (Exception e) {
            log.error("Mailchimp health check failed", e);
            return false;
        }
    }
    
    @Override
    public int getPriority() {
        return config != null ? config.getPriority() : 100;
    }
}
