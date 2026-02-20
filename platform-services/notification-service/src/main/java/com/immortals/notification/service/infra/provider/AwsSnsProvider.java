package com.immortals.notification.service.infra.provider;

import com.immortals.platform.domain.notifications.domain.Notification;
import com.immortals.platform.domain.notifications.domain.NotificationPriority;
import com.immortals.notification.service.application.usecase.port.NotificationProvider;
import com.immortals.platform.domain.notifications.dto.ProviderRequest;
import com.immortals.platform.domain.notifications.dto.ProviderResponse;
import com.immortals.platform.domain.notifications.entity.ProviderConfig;
import com.immortals.notification.service.repository.ProviderConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * AWS SNS provider implementation for SMS and Push notifications
 * Supports SMS and PUSH_NOTIFICATION channels with AWS SNS
 */
@Slf4j
@Component
public class AwsSnsProvider implements NotificationProvider {
    
    private static final String PROVIDER_ID = "AWS_SNS";
    
    private final ProviderConfigRepository providerConfigRepository;
    private ProviderConfig config;
    private SnsClient snsClient;
    private boolean initialized = false;
    
    public AwsSnsProvider(ProviderConfigRepository providerConfigRepository) {
        this.providerConfigRepository = providerConfigRepository;
        initializeProvider();
    }
    
    /**
     * Initialize AWS SNS provider with configuration from database
     */
    private void initializeProvider() {
        try {
            Optional<ProviderConfig> configOpt = providerConfigRepository.findByProviderId(PROVIDER_ID);
            if (configOpt.isPresent()) {
                this.config = configOpt.get();
                
                // Get AWS credentials from configuration
                String accessKeyId = config.getCredentials().get("access-key-id");
                String secretAccessKey = config.getCredentials().get("secret-access-key");
                String regionStr = config.getCredentials().getOrDefault("region", "us-east-1");
                
                if (accessKeyId != null && secretAccessKey != null) {
                    // Create AWS credentials
                    AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
                        accessKeyId, 
                        secretAccessKey
                    );
                    
                    // Initialize SNS client
                    this.snsClient = SnsClient.builder()
                        .region(Region.of(regionStr))
                        .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                        .build();
                    
                    initialized = true;
                    log.info("AWS SNS provider initialized successfully for region: {}", regionStr);
                } else {
                    log.error("AWS SNS credentials missing in configuration");
                }
            } else {
                log.warn("AWS SNS provider configuration not found in database");
            }
        } catch (Exception e) {
            log.error("Failed to initialize AWS SNS provider", e);
        }
    }
    
    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }
    
    @Override
    public Notification.NotificationType getChannel() {
        // AWS SNS supports both SMS and Push, return SMS as primary
        return Notification.NotificationType.SMS;
    }
    
    @Override
    public boolean supports(Notification.NotificationType channel, String countryCode) {
        if (!initialized || config == null || !config.isAvailable()) {
            return false;
        }
        
        // Check if channel is supported (SMS or PUSH_NOTIFICATION)
        if (channel != Notification.NotificationType.SMS && 
            channel != Notification.NotificationType.PUSH_NOTIFICATION) {
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
            log.info("Sending {} notification via AWS SNS to {}", request.getChannel(), request.getRecipient());
            
            // Route to appropriate method based on channel type
            if (request.getChannel() == Notification.NotificationType.PUSH_NOTIFICATION) {
                return sendPushNotification(request);
            } else {
                return sendSmsNotification(request);
            }
            
        } catch (SnsException e) {
            log.error("AWS SNS error: {} - {}", e.awsErrorDetails().errorCode(), 
                e.awsErrorDetails().errorMessage(), e);
            return ProviderResponse.failure(
                PROVIDER_ID, 
                e.awsErrorDetails().errorMessage(), 
                e.awsErrorDetails().errorCode()
            );
        } catch (Exception e) {
            log.error("Unexpected error sending AWS SNS notification", e);
            return ProviderResponse.failure(
                PROVIDER_ID, 
                "Unexpected error: " + e.getMessage(), 
                "INTERNAL_ERROR"
            );
        }
    }
    
    /**
     * Send SMS notification via AWS SNS
     */
    private ProviderResponse sendSmsNotification(ProviderRequest request) {
        //        // Build SMS attributes
        Map<String, MessageAttributeValue> smsAttributes = new HashMap<>();

        new Notification();
        String smsType = request.getPriority() == NotificationPriority.HIGH ?
            "Transactional" : "Promotional";
        smsAttributes.put("AWS.SNS.SMS.SMSType", 
            MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(smsType)
                .build());
        
        // Set sender ID if configured
        if (config.getConfiguration() != null && config.getConfiguration().containsKey("sender-id")) {
            smsAttributes.put("AWS.SNS.SMS.SenderID",
                MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(config.getConfiguration().get("sender-id").toString())
                    .build());
        }
        
        // Build publish request
        PublishRequest publishRequest = PublishRequest.builder()
            .message(request.getMessage())
            .phoneNumber(request.getRecipient())
            .messageAttributes(smsAttributes)
            .build();
        
        // Send SMS
        PublishResponse response = snsClient.publish(publishRequest);
        
        log.info("AWS SNS SMS sent successfully. Message ID: {}", response.messageId());
        
        // Build metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("snsMessageId", response.messageId());
        metadata.put("smsType", smsType);
        
        return ProviderResponse.builder()
            .success(true)
            .providerId(PROVIDER_ID)
            .providerMessageId(response.messageId())
            .deliveryStatus(Notification.DeliveryStatus.SENT)
            .metadata(metadata)
            .build();
    }
    
    /**
     * Send push notification via AWS SNS
     */
    private ProviderResponse sendPushNotification(ProviderRequest request) {
        // For push notifications, recipient should be a topic ARN or endpoint ARN
        String targetArn = request.getRecipient();
        
        // Build message payload based on platform
        String message = buildPushMessage(request);
        
        // Determine if target is a topic or endpoint
        PublishRequest.Builder publishBuilder = PublishRequest.builder()
            .message(message);
        
        if (targetArn.contains(":topic:")) {
            publishBuilder.topicArn(targetArn);
        } else if (targetArn.contains(":endpoint/")) {
            publishBuilder.targetArn(targetArn);
        } else {
            // Assume it's a topic name and construct ARN
            String topicArn = constructTopicArn(targetArn);
            publishBuilder.topicArn(topicArn);
        }
        
        // Add subject if provided
        if (request.getSubject() != null && !request.getSubject().isEmpty()) {
            publishBuilder.subject(request.getSubject());
        }
        
        // Send push notification
        PublishResponse response = snsClient.publish(publishBuilder.build());
        
        log.info("AWS SNS Push notification sent successfully. Message ID: {}", response.messageId());
        
        // Build metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("snsMessageId", response.messageId());
        metadata.put("targetArn", targetArn);
        
        return ProviderResponse.builder()
            .success(true)
            .providerId(PROVIDER_ID)
            .providerMessageId(response.messageId())
            .deliveryStatus(Notification.DeliveryStatus.SENT)
            .metadata(metadata)
            .build();
    }
    
    /**
     * Build push notification message payload
     * For multi-platform support, we can use JSON format
     */
    private String buildPushMessage(ProviderRequest request) {
        // Simple text message for now
        // In production, this should be formatted as JSON for different platforms
        // Example: {"default": "message", "APNS": "{...}", "GCM": "{...}"}
        return request.getMessage();
    }
    
    /**
     * Construct topic ARN from topic name
     */
    private String constructTopicArn(String topicName) {
        // Get AWS account ID and region from configuration
        String region = config.getCredentials().getOrDefault("region", "us-east-1");
        
        // In production, account ID should be in configuration
        // For now, we'll use a placeholder
        String accountId = config.getConfiguration() != null ? 
            config.getConfiguration().getOrDefault("account-id", "").toString() : "";
        
        if (accountId.isEmpty()) {
            log.warn("AWS account ID not configured, using topic name as-is");
            return topicName;
        }
        
        return String.format("arn:aws:sns:%s:%s:%s", region, accountId, topicName);
    }
    
    @Override
    public Notification.DeliveryStatus checkStatus(String providerMessageId) {
        if (!initialized) {
            return Notification.DeliveryStatus.PENDING;
        }
        
        // AWS SNS doesn't provide a direct API to check message status
        // Status updates come via SNS delivery status notifications
        log.debug("Status check for AWS SNS message ID: {}", providerMessageId);
        return Notification.DeliveryStatus.PENDING;
    }
    
    @Override
    public boolean isHealthy() {
        if (!initialized || config == null || snsClient == null) {
            return false;
        }
        
        try {
            // Simple health check - verify credentials are set and client is initialized
            String accessKeyId = config.getCredentials().get("access-key-id");
            String secretAccessKey = config.getCredentials().get("secret-access-key");
            
            // Optionally, we could make a lightweight API call to verify connectivity
            // For now, just check if credentials exist and config is available
            return accessKeyId != null && secretAccessKey != null && config.isAvailable();
        } catch (Exception e) {
            log.error("AWS SNS health check failed", e);
            return false;
        }
    }
    
    @Override
    public int getPriority() {
        return config != null ? config.getPriority() : 100;
    }
    
    /**
     * Cleanup resources when provider is destroyed
     */
    public void destroy() {
        if (snsClient != null) {
            try {
                snsClient.close();
                log.info("AWS SNS client closed successfully");
            } catch (Exception e) {
                log.error("Error closing AWS SNS client", e);
            }
        }
    }
}
