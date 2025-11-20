package com.immortals.notificationservice.api.controller;

import com.immortals.notificationservice.api.dto.WebhookPayload;
import com.immortals.notificationservice.service.DeliveryTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Webhook controller for receiving delivery status updates from providers
 * Supports Twilio, SendGrid, AWS SNS webhooks
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "Provider webhook endpoints")
public class WebhookController {
    
    private final DeliveryTrackingService deliveryTrackingService;
    
    @PostMapping("/twilio")
    @Operation(summary = "Twilio SMS status webhook")
    public ResponseEntity<Void> twilioWebhook(@RequestBody WebhookPayload payload) {
        log.info("Received Twilio webhook: {}", payload);
        deliveryTrackingService.processWebhook("TWILIO", payload);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/sendgrid")
    @Operation(summary = "SendGrid email status webhook")
    public ResponseEntity<Void> sendgridWebhook(@RequestBody WebhookPayload payload) {
        log.info("Received SendGrid webhook: {}", payload);
        deliveryTrackingService.processWebhook("SENDGRID", payload);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/sns")
    @Operation(summary = "AWS SNS status webhook")
    public ResponseEntity<Void> snsWebhook(@RequestBody WebhookPayload payload) {
        log.info("Received AWS SNS webhook: {}", payload);
        deliveryTrackingService.processWebhook("AWS_SNS", payload);
        return ResponseEntity.ok().build();
    }
}
