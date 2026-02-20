package com.immortals.notification.service.controller;

import com.immortals.notification.service.service.DeliveryTrackingService;
import com.immortals.notification.service.service.impl.NotificationMetricsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for handling Twilio webhook callbacks
 * Receives delivery status updates from Twilio for SMS and WhatsApp messages
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notifications/webhooks/twilio")
@RequiredArgsConstructor
public class TwilioWebhookController {
    
    private final DeliveryTrackingService deliveryTrackingService;
    private final NotificationMetricsService metricsService;
    
    @Value("${twilio.auth-token:}")
    private String twilioAuthToken;
    
    @Value("${twilio.webhook.signature-validation.enabled:true}")
    private boolean signatureValidationEnabled;
    
    /**
     * Handle Twilio status callback webhook
     * 
     * @param request HTTP request containing Twilio webhook data
     * @return ResponseEntity with status
     */
    @PostMapping
    public ResponseEntity<String> handleStatusCallback(HttpServletRequest request) {
        try {
            log.info("Received Twilio webhook callback");
            
            // Extract webhook parameters
            Map<String, String> params = extractParameters(request);
            
            // Validate webhook signature if enabled
            if (signatureValidationEnabled && !validateSignature(request, params)) {
                log.warn("Invalid Twilio webhook signature");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid signature");
            }
            
            // Extract key fields
            String messageSid = params.get("MessageSid");
            String messageStatus = params.get("MessageStatus");
            String errorCode = params.get("ErrorCode");
            String errorMessage = params.get("ErrorMessage");
            
            if (messageSid == null || messageStatus == null) {
                log.warn("Missing required webhook parameters: MessageSid or MessageStatus");
                return ResponseEntity.badRequest().body("Missing required parameters");
            }
            
            log.info("Processing Twilio webhook - SID: {}, Status: {}", messageSid, messageStatus);
            
            // Record webhook metrics
            metricsService.recordWebhookReceived("TWILIO", messageStatus);
            
            // Update notification delivery status
            deliveryTrackingService.updateDeliveryStatus(
                "TWILIO",
                messageSid,
                mapTwilioStatus(messageStatus),
                errorCode,
                errorMessage,
                params
            );
            
            return ResponseEntity.ok("Webhook processed successfully");
            
        } catch (Exception e) {
            log.error("Error processing Twilio webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing webhook");
        }
    }
    
    /**
     * Extract all parameters from the request
     */
    private Map<String, String> extractParameters(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            params.put(paramName, request.getParameter(paramName));
        }
        
        return params;
    }
    
    /**
     * Validate Twilio webhook signature
     * 
     * @param request HTTP request
     * @param params Request parameters
     * @return true if signature is valid, false otherwise
     */
    private boolean validateSignature(HttpServletRequest request, Map<String, String> params) {
        try {
            if (twilioAuthToken == null || twilioAuthToken.isEmpty()) {
                log.warn("Twilio auth token not configured, skipping signature validation");
                return true;
            }
            
            String signature = request.getHeader("X-Twilio-Signature");
            if (signature == null) {
                log.warn("Missing X-Twilio-Signature header");
                return false;
            }
            
            String url = getFullURL(request);
            
//            RequestValidator validator = new RequestValidator(twilioAuthToken);
//            return validator.validate(url, params, signature);
            
        } catch (Exception e) {
            log.error("Error validating Twilio signature", e);
            return false;
        }
    }
    
    /**
     * Get full URL from request
     */
    private String getFullURL(HttpServletRequest request) {
        StringBuffer requestURL = request.getRequestURL();
        String queryString = request.getQueryString();
        
        if (queryString == null) {
            return requestURL.toString();
        } else {
            return requestURL.append('?').append(queryString).toString();
        }
    }
    
    /**
     * Map Twilio status to our internal delivery status
     */
    private String mapTwilioStatus(String twilioStatus) {
        return switch (twilioStatus.toUpperCase()) {
            case "QUEUED", "ACCEPTED" -> "PENDING";
            case "SENDING", "SENT" -> "SENT";
            case "DELIVERED" -> "DELIVERED";
            case "READ" -> "READ";
            case "FAILED", "UNDELIVERED" -> "FAILED";
            default -> "PENDING";
        };
    }
}
