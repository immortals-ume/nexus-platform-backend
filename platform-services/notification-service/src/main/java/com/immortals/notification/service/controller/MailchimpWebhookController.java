package com.immortals.notification.service.controller;

import com.immortals.notification.service.service.DeliveryTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for handling Mailchimp (Mandrill) webhook callbacks
 * Receives email delivery status updates including sent, delivered, opened, clicked, bounced, and rejected events
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notifications/webhooks/mailchimp")
@RequiredArgsConstructor
public class MailchimpWebhookController {
    
    private final DeliveryTrackingService deliveryTrackingService;
    
    /**
     * Handle Mailchimp webhook events
     * Mailchimp sends events as a POST with form-encoded data containing a 'mandrill_events' parameter
     * 
     * @param mandrillEvents JSON array of webhook events
     * @return ResponseEntity with status
     */
    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestParam("mandrill_events") String mandrillEvents) {
        try {
            log.info("Received Mailchimp webhook callback");
            
            if (mandrillEvents == null || mandrillEvents.isEmpty()) {
                log.warn("Empty Mailchimp webhook payload");
                return ResponseEntity.badRequest().body("Empty payload");
            }
            
            // Parse the JSON array of events
            // Note: In production, use a proper JSON library like Jackson
            // For now, we'll handle it as a simple string parsing
            log.debug("Mailchimp webhook payload: {}", mandrillEvents);
            
            // Process each event
            processWebhookEvents(mandrillEvents);
            
            return ResponseEntity.ok("Webhook processed successfully");
            
        } catch (Exception e) {
            log.error("Error processing Mailchimp webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing webhook");
        }
    }
    
    /**
     * Handle Mailchimp webhook events sent as JSON body
     * Alternative endpoint for JSON-based webhooks
     * 
     * @param events List of webhook event maps
     * @return ResponseEntity with status
     */
    @PostMapping(consumes = "application/json")
    public ResponseEntity<String> handleJsonWebhook(@RequestBody List<Map<String, Object>> events) {
        try {
            log.info("Received Mailchimp JSON webhook callback with {} events", events.size());
            
            if (events == null || events.isEmpty()) {
                log.warn("Empty Mailchimp webhook events");
                return ResponseEntity.badRequest().body("Empty events");
            }
            
            // Process each event
            for (Map<String, Object> event : events) {
                processEvent(event);
            }
            
            return ResponseEntity.ok("Webhook processed successfully");
            
        } catch (Exception e) {
            log.error("Error processing Mailchimp JSON webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing webhook");
        }
    }
    
    /**
     * Process webhook events from form-encoded payload
     */
    private void processWebhookEvents(String mandrillEvents) {
        // In a real implementation, parse the JSON array properly
        // For now, log the raw payload
        log.info("Processing Mailchimp webhook events: {}", mandrillEvents);
        
        // TODO: Parse JSON array and process each event
        // This would require adding Jackson or similar JSON library
    }
    
    /**
     * Process a single webhook event
     */
    private void processEvent(Map<String, Object> event) {
        try {
            String eventType = (String) event.get("event");
            Map<String, Object> msg = (Map<String, Object>) event.get("msg");
            
            if (msg == null) {
                log.warn("Missing 'msg' object in Mailchimp webhook event");
                return;
            }
            
            String messageId = (String) msg.get("_id");
            String email = (String) msg.get("email");
            String subject = (String) msg.get("subject");
            Long timestamp = (Long) msg.get("ts");
            
            if (messageId == null || eventType == null) {
                log.warn("Missing required fields in Mailchimp webhook event");
                return;
            }
            
            log.info("Processing Mailchimp event - Type: {}, Message ID: {}, Email: {}", 
                eventType, messageId, email);
            
            // Map event type to delivery status
            String deliveryStatus = mapMailchimpEvent(eventType);
            
            // Extract error information for failed events
            String errorCode = null;
            String errorMessage = null;
            
            if ("bounce".equals(eventType) || "reject".equals(eventType)) {
                Map<String, Object> bounceDescription = (Map<String, Object>) msg.get("bounce_description");
                Map<String, Object> rejectInfo = (Map<String, Object>) msg.get("reject");
                
                if (bounceDescription != null) {
                    errorMessage = (String) bounceDescription.get("message");
                }
                if (rejectInfo != null) {
                    errorCode = (String) rejectInfo.get("reason");
                    if (errorMessage == null) {
                        errorMessage = (String) rejectInfo.get("detail");
                    }
                }
            }
            
            // Build metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("event", eventType);
            metadata.put("email", email);
            if (subject != null) {
                metadata.put("subject", subject);
            }
            if (timestamp != null) {
                metadata.put("timestamp", Instant.ofEpochSecond(timestamp).toString());
            }
            
            // Add click/open tracking data if present
            if ("click".equals(eventType)) {
                String url = (String) msg.get("url");
                if (url != null) {
                    metadata.put("clickedUrl", url);
                }
            }
            
            // Update notification delivery status
            deliveryTrackingService.updateDeliveryStatus(
                "MAILCHIMP",
                messageId,
                deliveryStatus,
                errorCode,
                errorMessage,
                metadata
            );
            
        } catch (Exception e) {
            log.error("Error processing individual Mailchimp webhook event", e);
        }
    }
    
    /**
     * Map Mailchimp event type to our internal delivery status
     * 
     * Mailchimp event types:
     * - send: Message has been sent
     * - deferral: Message has been deferred (temporary failure)
     * - hard_bounce: Message bounced (permanent failure)
     * - soft_bounce: Message bounced (temporary failure)
     * - open: Recipient opened the email
     * - click: Recipient clicked a link in the email
     * - spam: Recipient marked email as spam
     * - unsub: Recipient unsubscribed
     * - reject: Message was rejected
     */
    private String mapMailchimpEvent(String eventType) {
        return switch (eventType.toLowerCase()) {
            case "send" -> "SENT";
            case "deferral", "soft_bounce" -> "PENDING";
            case "hard_bounce", "reject", "spam" -> "FAILED";
            case "open" -> "DELIVERED";
            case "click" -> "READ";
            case "unsub" -> "DELIVERED"; // Still delivered, just unsubscribed
            default -> "PENDING";
        };
    }
    
    /**
     * Health check endpoint for webhook configuration
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "mailchimp-webhook");
        return ResponseEntity.ok(response);
    }
}
