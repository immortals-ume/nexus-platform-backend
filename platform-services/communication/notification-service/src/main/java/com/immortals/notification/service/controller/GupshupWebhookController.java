package com.immortals.notification.service.controller;

import com.immortals.notification.service.service.DeliveryTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for handling Gupshup webhook callbacks
 * Receives delivery status updates from Gupshup for SMS and WhatsApp messages
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notifications/webhooks/gupshup")
@RequiredArgsConstructor
public class GupshupWebhookController {
    
    private final DeliveryTrackingService deliveryTrackingService;
    
    /**
     * Handle Gupshup delivery report webhook
     * 
     * Gupshup sends delivery reports with the following parameters:
     * - externalId: Message ID from Gupshup
     * - eventType: Type of event (SENT, DELIVERED, READ, FAILED)
     * - destAddr: Destination phone number
     * - srcAddr: Source phone number
     * - cause: Error cause if failed
     * - errorCode: Error code if failed
     * - channel: Channel type (SMS or WhatsApp)
     * 
     * @param payload Webhook payload from Gupshup
     * @return ResponseEntity with status
     */
    @PostMapping
    public ResponseEntity<String> handleDeliveryReport(@RequestBody Map<String, Object> payload) {
        try {
            log.info("Received Gupshup webhook callback");
            log.debug("Gupshup webhook payload: {}", payload);
            
            // Extract key fields from payload
            String messageId = extractString(payload, "externalId");
            String eventType = extractString(payload, "eventType");
            String channel = extractString(payload, "channel");
            String errorCode = extractString(payload, "errorCode");
            String cause = extractString(payload, "cause");
            
            if (messageId == null || eventType == null) {
                log.warn("Missing required webhook parameters: externalId or eventType");
                return ResponseEntity.badRequest().body("Missing required parameters");
            }
            
            log.info("Processing Gupshup webhook - Message ID: {}, Event: {}, Channel: {}", 
                messageId, eventType, channel);
            
            // Map Gupshup event type to our delivery status
            String deliveryStatus = mapGupshupStatus(eventType);
            
            // Build metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("channel", channel);
            metadata.put("eventType", eventType);
            if (payload.containsKey("destAddr")) {
                metadata.put("destination", extractString(payload, "destAddr"));
            }
            if (payload.containsKey("srcAddr")) {
                metadata.put("source", extractString(payload, "srcAddr"));
            }
            if (payload.containsKey("timestamp")) {
                metadata.put("timestamp", extractString(payload, "timestamp"));
            }
            
            // Update notification delivery status
            deliveryTrackingService.updateDeliveryStatus(
                "GUPSHUP",
                messageId,
                deliveryStatus,
                errorCode,
                cause,
                metadata
            );
            
            return ResponseEntity.ok("Webhook processed successfully");
            
        } catch (Exception e) {
            log.error("Error processing Gupshup webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing webhook");
        }
    }
    
    /**
     * Handle Gupshup SMS delivery report (alternative endpoint)
     * Some Gupshup configurations use query parameters instead of JSON body
     */
    @GetMapping
    public ResponseEntity<String> handleSmsDeliveryReport(
            @RequestParam(required = false) String externalId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String destAddr,
            @RequestParam(required = false) String srcAddr,
            @RequestParam(required = false) String cause,
            @RequestParam(required = false) String errorCode,
            @RequestParam(required = false) String channel) {
        
        try {
            log.info("Received Gupshup SMS webhook callback via GET");
            
            if (externalId == null || eventType == null) {
                log.warn("Missing required webhook parameters: externalId or eventType");
                return ResponseEntity.badRequest().body("Missing required parameters");
            }
            
            log.info("Processing Gupshup SMS webhook - Message ID: {}, Event: {}", 
                externalId, eventType);
            
            // Map Gupshup event type to our delivery status
            String deliveryStatus = mapGupshupStatus(eventType);
            
            // Build metadata
            Map<String, String> metadata = new HashMap<>();
            if (channel != null) metadata.put("channel", channel);
            if (destAddr != null) metadata.put("destination", destAddr);
            if (srcAddr != null) metadata.put("source", srcAddr);
            metadata.put("eventType", eventType);
            
            // Update notification delivery status
            deliveryTrackingService.updateDeliveryStatus(
                "GUPSHUP",
                externalId,
                deliveryStatus,
                errorCode,
                cause,
                metadata
            );
            
            return ResponseEntity.ok("Webhook processed successfully");
            
        } catch (Exception e) {
            log.error("Error processing Gupshup SMS webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing webhook");
        }
    }
    
    /**
     * Extract string value from payload map
     */
    private String extractString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * Map Gupshup event type to our internal delivery status
     * 
     * Gupshup event types:
     * - SENT: Message sent to carrier
     * - DELIVERED: Message delivered to recipient
     * - READ: Message read by recipient (WhatsApp only)
     * - FAILED: Message delivery failed
     * - UNDELIVERED: Message not delivered
     */
    private String mapGupshupStatus(String gupshupEventType) {
        if (gupshupEventType == null) {
            return "PENDING";
        }
        
        return switch (gupshupEventType.toUpperCase()) {
            case "SENT", "SUBMITTED" -> "SENT";
            case "DELIVERED" -> "DELIVERED";
            case "READ" -> "READ";
            case "FAILED", "UNDELIVERED", "REJECTED" -> "FAILED";
            default -> {
                log.warn("Unknown Gupshup event type: {}", gupshupEventType);
                yield "PENDING";
            }
        };
    }
}
