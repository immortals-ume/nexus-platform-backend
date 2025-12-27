package com.immortals.notification.service.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.immortals.notification.service.service.DeliveryTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for handling AWS SNS webhook callbacks
 * Receives delivery status updates and subscription confirmations from AWS SNS
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notifications/webhooks/sns")
@RequiredArgsConstructor
public class AwsSnsWebhookController {
    
    private final DeliveryTrackingService deliveryTrackingService;
    private final ObjectMapper objectMapper;
    
    @Value("${aws.sns.webhook.signature-validation.enabled:true}")
    private boolean signatureValidationEnabled;
    
    /**
     * Handle AWS SNS webhook notifications
     * 
     * AWS SNS sends different types of messages:
     * 1. SubscriptionConfirmation - Initial subscription setup
     * 2. Notification - Actual delivery status updates
     * 3. UnsubscribeConfirmation - Unsubscribe confirmation
     * 
     * @param payload Raw JSON payload from SNS
     * @param messageType SNS message type header
     * @return ResponseEntity with status
     */
    @PostMapping
    public ResponseEntity<String> handleSnsNotification(
            @RequestBody String payload,
            @RequestHeader(value = "x-amz-sns-message-type", required = false) String messageType) {
        
        try {
            log.info("Received AWS SNS webhook callback, message type: {}", messageType);
            log.debug("SNS webhook payload: {}", payload);
            
            // Parse JSON payload
            JsonNode message = objectMapper.readTree(payload);
            
            // Validate message signature if enabled
            if (signatureValidationEnabled && !validateSignature(message)) {
                log.warn("Invalid AWS SNS message signature");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid signature");
            }
            
            // Handle different message types
            String type = message.has("Type") ? message.get("Type").asText() : messageType;
            
            if (type == null) {
                log.warn("Missing message type in SNS webhook");
                return ResponseEntity.badRequest().body("Missing message type");
            }
            
            return switch (type) {
                case "SubscriptionConfirmation" -> handleSubscriptionConfirmation(message);
                case "Notification" -> handleNotification(message);
                case "UnsubscribeConfirmation" -> handleUnsubscribeConfirmation(message);
                default -> {
                    log.warn("Unknown SNS message type: {}", type);
                    yield ResponseEntity.badRequest().body("Unknown message type");
                }
            };
            
        } catch (Exception e) {
            log.error("Error processing AWS SNS webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing webhook");
        }
    }
    
    /**
     * Handle subscription confirmation
     * AWS SNS requires confirming the subscription by visiting the SubscribeURL
     */
    private ResponseEntity<String> handleSubscriptionConfirmation(JsonNode message) {
        try {
            String subscribeUrl = message.get("SubscribeURL").asText();
            String topicArn = message.get("TopicArn").asText();
            
            log.info("Confirming SNS subscription for topic: {}", topicArn);
            
            // Visit the subscribe URL to confirm subscription
            URL url = new URL(subscribeUrl);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(url.openStream()))) {
                String response = reader.lines().reduce("", String::concat);
                log.info("Subscription confirmed successfully: {}", response);
            }
            
            return ResponseEntity.ok("Subscription confirmed");
            
        } catch (Exception e) {
            log.error("Error confirming SNS subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error confirming subscription");
        }
    }
    
    /**
     * Handle notification message containing delivery status
     */
    private ResponseEntity<String> handleNotification(JsonNode message) {
        try {
            // Extract the actual notification message
            String messageContent = message.get("Message").asText();
            JsonNode notificationData = objectMapper.readTree(messageContent);
            
            log.debug("SNS notification data: {}", notificationData);
            
            // AWS SNS delivery status notifications have different formats
            // depending on whether it's SMS or Push notification
            
            // Check if this is an SMS delivery status
            if (notificationData.has("notification")) {
                return handleSmsDeliveryStatus(notificationData);
            }
            
            // Check if this is a push notification delivery status
            if (notificationData.has("delivery")) {
                return handlePushDeliveryStatus(notificationData);
            }
            
            // Generic notification handling
            log.info("Received generic SNS notification");
            return ResponseEntity.ok("Notification received");
            
        } catch (Exception e) {
            log.error("Error handling SNS notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error handling notification");
        }
    }
    
    /**
     * Handle SMS delivery status notification
     */
    private ResponseEntity<String> handleSmsDeliveryStatus(JsonNode notificationData) {
        try {
            JsonNode notification = notificationData.get("notification");
            
            // Extract delivery status information
            String messageId = notification.has("messageId") ? 
                notification.get("messageId").asText() : null;
            String status = notification.has("status") ? 
                notification.get("status").asText() : null;
            String providerResponse = notification.has("providerResponse") ? 
                notification.get("providerResponse").asText() : null;
            
            if (messageId == null || status == null) {
                log.warn("Missing required fields in SMS delivery status");
                return ResponseEntity.badRequest().body("Missing required fields");
            }
            
            log.info("Processing SNS SMS delivery status - Message ID: {}, Status: {}", 
                messageId, status);
            
            // Map SNS status to our delivery status
            String deliveryStatus = mapSnsStatus(status);
            
            // Build metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("snsStatus", status);
            if (providerResponse != null) {
                metadata.put("providerResponse", providerResponse);
            }
            if (notification.has("dwellTimeMs")) {
                metadata.put("dwellTimeMs", notification.get("dwellTimeMs").asText());
            }
            if (notification.has("priceInUSD")) {
                metadata.put("priceInUSD", notification.get("priceInUSD").asText());
            }
            
            // Extract error information if failed
            String errorCode = null;
            String errorMessage = null;
            if ("FAILURE".equalsIgnoreCase(status) && providerResponse != null) {
                errorCode = "SMS_DELIVERY_FAILED";
                errorMessage = providerResponse;
            }
            
            // Update notification delivery status
            deliveryTrackingService.updateDeliveryStatus(
                "AWS_SNS",
                messageId,
                deliveryStatus,
                errorCode,
                errorMessage,
                metadata
            );
            
            return ResponseEntity.ok("SMS delivery status processed");
            
        } catch (Exception e) {
            log.error("Error processing SMS delivery status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing SMS delivery status");
        }
    }
    
    /**
     * Handle push notification delivery status
     */
    private ResponseEntity<String> handlePushDeliveryStatus(JsonNode notificationData) {
        try {
            JsonNode delivery = notificationData.get("delivery");
            
            // Extract delivery status information
            String messageId = delivery.has("messageId") ? 
                delivery.get("messageId").asText() : null;
            String status = delivery.has("deliveryStatus") ? 
                delivery.get("deliveryStatus").asText() : "UNKNOWN";
            
            if (messageId == null) {
                log.warn("Missing messageId in push delivery status");
                return ResponseEntity.badRequest().body("Missing messageId");
            }
            
            log.info("Processing SNS push delivery status - Message ID: {}, Status: {}", 
                messageId, status);
            
            // Map SNS status to our delivery status
            String deliveryStatus = mapSnsStatus(status);
            
            // Build metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("snsStatus", status);
            if (delivery.has("statusCode")) {
                metadata.put("statusCode", delivery.get("statusCode").asText());
            }
            if (delivery.has("token")) {
                metadata.put("token", delivery.get("token").asText());
            }
            
            // Extract error information if failed
            String errorCode = null;
            String errorMessage = null;
            if (delivery.has("statusMessage")) {
                String statusMessage = delivery.get("statusMessage").asText();
                if (!"SUCCESS".equalsIgnoreCase(status)) {
                    errorCode = "PUSH_DELIVERY_FAILED";
                    errorMessage = statusMessage;
                }
                metadata.put("statusMessage", statusMessage);
            }
            
            // Update notification delivery status
            deliveryTrackingService.updateDeliveryStatus(
                "AWS_SNS",
                messageId,
                deliveryStatus,
                errorCode,
                errorMessage,
                metadata
            );
            
            return ResponseEntity.ok("Push delivery status processed");
            
        } catch (Exception e) {
            log.error("Error processing push delivery status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing push delivery status");
        }
    }
    
    /**
     * Handle unsubscribe confirmation
     */
    private ResponseEntity<String> handleUnsubscribeConfirmation(JsonNode message) {
        try {
            String topicArn = message.get("TopicArn").asText();
            log.info("Received unsubscribe confirmation for topic: {}", topicArn);
            return ResponseEntity.ok("Unsubscribe confirmed");
        } catch (Exception e) {
            log.error("Error handling unsubscribe confirmation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error handling unsubscribe confirmation");
        }
    }
    
    /**
     * Validate SNS message signature
     * AWS SNS signs all messages with a certificate
     * 
     * @param message SNS message JSON
     * @return true if signature is valid, false otherwise
     */
    private boolean validateSignature(JsonNode message) {
        try {
            // Extract signature fields
            String signatureVersion = message.get("SignatureVersion").asText();
            String signature = message.get("Signature").asText();
            String signingCertUrl = message.get("SigningCertURL").asText();
            
            // Verify certificate URL is from AWS
            if (!signingCertUrl.startsWith("https://sns.") || 
                !signingCertUrl.contains(".amazonaws.com/")) {
                log.warn("Invalid signing certificate URL: {}", signingCertUrl);
                return false;
            }
            
            // Only support signature version 1
            if (!"1".equals(signatureVersion)) {
                log.warn("Unsupported signature version: {}", signatureVersion);
                return false;
            }
            
            // Download and parse certificate
            URL certUrl = new URL(signingCertUrl);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(certUrl.openStream());
            PublicKey publicKey = cert.getPublicKey();
            
            // Build string to sign based on message type
            String stringToSign = buildStringToSign(message);
            
            // Verify signature
            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initVerify(publicKey);
            sig.update(stringToSign.getBytes("UTF-8"));
            
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            boolean isValid = sig.verify(signatureBytes);
            
            if (!isValid) {
                log.warn("SNS message signature validation failed");
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Error validating SNS signature", e);
            return false;
        }
    }
    
    /**
     * Build the string to sign for signature verification
     * The fields and order depend on the message type
     */
    private String buildStringToSign(JsonNode message) {
        StringBuilder stringToSign = new StringBuilder();
        String messageType = message.get("Type").asText();
        
        if ("Notification".equals(messageType)) {
            appendField(stringToSign, "Message", message);
            appendField(stringToSign, "MessageId", message);
            if (message.has("Subject")) {
                appendField(stringToSign, "Subject", message);
            }
            appendField(stringToSign, "Timestamp", message);
            appendField(stringToSign, "TopicArn", message);
            appendField(stringToSign, "Type", message);
        } else if ("SubscriptionConfirmation".equals(messageType) || 
                   "UnsubscribeConfirmation".equals(messageType)) {
            appendField(stringToSign, "Message", message);
            appendField(stringToSign, "MessageId", message);
            appendField(stringToSign, "SubscribeURL", message);
            appendField(stringToSign, "Timestamp", message);
            appendField(stringToSign, "Token", message);
            appendField(stringToSign, "TopicArn", message);
            appendField(stringToSign, "Type", message);
        }
        
        return stringToSign.toString();
    }
    
    /**
     * Append a field to the string to sign
     */
    private void appendField(StringBuilder sb, String fieldName, JsonNode message) {
        if (message.has(fieldName)) {
            sb.append(fieldName).append("\n");
            sb.append(message.get(fieldName).asText()).append("\n");
        }
    }
    
    /**
     * Map AWS SNS status to our internal delivery status
     */
    private String mapSnsStatus(String snsStatus) {
        if (snsStatus == null) {
            return "PENDING";
        }
        
        return switch (snsStatus.toUpperCase()) {
            case "SUCCESS", "DELIVERED" -> "DELIVERED";
            case "FAILURE", "FAILED" -> "FAILED";
            case "PENDING" -> "PENDING";
            default -> {
                log.warn("Unknown SNS status: {}", snsStatus);
                yield "PENDING";
            }
        };
    }
}
