package com.immortals.notification.service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * Consolidated webhook controller for receiving delivery status updates from all notification providers.
 * This controller serves as the main entry point for all provider webhooks and delegates to
 * provider-specific controllers for detailed processing.
 * 
 * Supported providers:
 * - Twilio (SMS, WhatsApp)
 * - Gupshup (SMS, WhatsApp)
 * - AWS SNS (SMS, Push)
 * - Mailchimp (Email)
 * 
 * Requirements: 6.1
 */
@RestController
@RequestMapping("/api/v1/notifications/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "Consolidated provider webhook endpoints for notification delivery status updates")
public class WebhookController {
    
    private final TwilioWebhookController twilioWebhookController;
    private final GupshupWebhookController gupshupWebhookController;
    private final AwsSnsWebhookController awsSnsWebhookController;
    private final MailchimpWebhookController mailchimpWebhookController;
    private final ObjectMapper objectMapper;
    
    /**
     * Twilio webhook endpoint for SMS and WhatsApp delivery status updates.
     * Receives status callbacks from Twilio including delivery confirmations, failures, and read receipts.
     * 
     * @param request HTTP request containing Twilio webhook data
     * @return ResponseEntity with processing status
     */
    @PostMapping("/twilio")
    @Operation(
        summary = "Twilio webhook endpoint",
        description = "Receives delivery status updates from Twilio for SMS and WhatsApp messages. " +
                     "Supports signature validation and processes status callbacks including sent, delivered, failed, and read statuses."
    )
    public ResponseEntity<String> twilioWebhook(HttpServletRequest request) {
        log.info("Webhook request received at consolidated endpoint: /api/v1/notifications/webhooks/twilio");
        return twilioWebhookController.handleStatusCallback(request);
    }
    
    /**
     * Gupshup webhook endpoint for SMS and WhatsApp delivery reports.
     * Supports both JSON body and query parameter formats.
     * 
     * @param payload Webhook payload from Gupshup (for POST with JSON body)
     * @param externalId Message ID (for GET with query parameters)
     * @param eventType Event type (for GET with query parameters)
     * @param destAddr Destination address (for GET with query parameters)
     * @param srcAddr Source address (for GET with query parameters)
     * @param cause Error cause (for GET with query parameters)
     * @param errorCode Error code (for GET with query parameters)
     * @param channel Channel type (for GET with query parameters)
     * @param request HTTP request
     * @return ResponseEntity with processing status
     */
    @PostMapping("/gupshup")
    @Operation(
        summary = "Gupshup webhook endpoint (POST)",
        description = "Receives delivery reports from Gupshup for SMS and WhatsApp messages via POST with JSON body. " +
                     "Processes events including sent, delivered, read, and failed statuses."
    )
    public ResponseEntity<String> gupshupWebhookPost(@RequestBody Map<String, Object> payload) {
        log.info("Webhook request received at consolidated endpoint: /api/v1/notifications/webhooks/gupshup (POST)");
        return gupshupWebhookController.handleDeliveryReport(payload);
    }
    
    @GetMapping("/gupshup")
    @Operation(
        summary = "Gupshup webhook endpoint (GET)",
        description = "Receives delivery reports from Gupshup for SMS messages via GET with query parameters. " +
                     "Alternative endpoint for configurations that use query parameters instead of JSON body."
    )
    public ResponseEntity<String> gupshupWebhookGet(
            @RequestParam(required = false) String externalId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String destAddr,
            @RequestParam(required = false) String srcAddr,
            @RequestParam(required = false) String cause,
            @RequestParam(required = false) String errorCode,
            @RequestParam(required = false) String channel) {
        log.info("Webhook request received at consolidated endpoint: /api/v1/notifications/webhooks/gupshup (GET)");
        return gupshupWebhookController.handleSmsDeliveryReport(
            externalId, eventType, destAddr, srcAddr, cause, errorCode, channel
        );
    }
    
    /**
     * AWS SNS webhook endpoint for SMS and Push notification delivery status.
     * Handles subscription confirmations, notifications, and unsubscribe confirmations.
     * Supports signature validation for security.
     * 
     * @param payload Raw JSON payload from SNS
     * @param messageType SNS message type header
     * @return ResponseEntity with processing status
     */
    @PostMapping("/sns")
    @Operation(
        summary = "AWS SNS webhook endpoint",
        description = "Receives delivery status updates and subscription confirmations from AWS SNS for SMS and Push notifications. " +
                     "Validates message signatures and processes subscription confirmations automatically. " +
                     "Handles different message types: SubscriptionConfirmation, Notification, and UnsubscribeConfirmation."
    )
    public ResponseEntity<String> snsWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "x-amz-sns-message-type", required = false) String messageType) {
        log.info("Webhook request received at consolidated endpoint: /api/v1/notifications/webhooks/sns");
        return awsSnsWebhookController.handleSnsNotification(payload, messageType);
    }
    
    /**
     * Mailchimp webhook endpoint for email delivery status updates.
     * Receives events including sent, delivered, opened, clicked, bounced, and rejected.
     * Supports both form-encoded and JSON body formats.
     * 
     * @param mandrillEvents Form-encoded mandrill_events parameter
     * @param events JSON array of webhook events
     * @param request HTTP request
     * @return ResponseEntity with processing status
     */
    @PostMapping(value = "/mailchimp", consumes = "application/x-www-form-urlencoded")
    @Operation(
        summary = "Mailchimp webhook endpoint (form-encoded)",
        description = "Receives email delivery status updates from Mailchimp/Mandrill via form-encoded data. " +
                     "Processes events including send, delivery, open, click, bounce, spam, and unsubscribe events."
    )
    public ResponseEntity<String> mailchimpWebhookForm(
            @RequestParam(value = "mandrill_events", required = false) String mandrillEvents) {
        log.info("Webhook request received at consolidated endpoint: /api/v1/notifications/webhooks/mailchimp (form-encoded)");
        return mailchimpWebhookController.handleWebhook(mandrillEvents);
    }
    
    @PostMapping(value = "/mailchimp", consumes = "application/json")
    @Operation(
        summary = "Mailchimp webhook endpoint (JSON)",
        description = "Receives email delivery status updates from Mailchimp/Mandrill via JSON body. " +
                     "Alternative endpoint for JSON-based webhook configurations."
    )
    public ResponseEntity<String> mailchimpWebhookJson(@RequestBody List<Map<String, Object>> events) {
        log.info("Webhook request received at consolidated endpoint: /api/v1/notifications/webhooks/mailchimp (JSON)");
        return mailchimpWebhookController.handleJsonWebhook(events);
    }
    
    /**
     * Health check endpoint for webhook service
     * 
     * @return ResponseEntity with health status
     */
    @GetMapping("/health")
    @Operation(
        summary = "Webhook service health check",
        description = "Returns the health status of the webhook service"
    )
    public ResponseEntity<Map<String, String>> health() {
        log.debug("Health check request received at /api/v1/notifications/webhooks/health");
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "notification-webhooks",
            "providers", "twilio,gupshup,sns,mailchimp"
        ));
    }
}
