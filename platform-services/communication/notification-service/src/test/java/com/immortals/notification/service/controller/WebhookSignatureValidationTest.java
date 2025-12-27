package com.immortals.notification.service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.immortals.notification.service.service.DeliveryTrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for webhook signature validation
 * Tests that signature validation is properly implemented for Twilio and AWS SNS webhooks
 */
@ExtendWith(MockitoExtension.class)
class WebhookSignatureValidationTest {
    
    @Mock
    private DeliveryTrackingService deliveryTrackingService;
    
    private TwilioWebhookController twilioController;
    private AwsSnsWebhookController snsController;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        twilioController = new TwilioWebhookController(deliveryTrackingService);
        snsController = new AwsSnsWebhookController(deliveryTrackingService, objectMapper);
    }
    
    @Test
    void testTwilioWebhookWithoutSignatureWhenValidationDisabled() {
        // Given
        ReflectionTestUtils.setField(twilioController, "signatureValidationEnabled", false);
        ReflectionTestUtils.setField(twilioController, "twilioAuthToken", "test-token");
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("MessageSid", "SM123456");
        request.setParameter("MessageStatus", "delivered");
        
        // When
        ResponseEntity<String> response = twilioController.handleStatusCallback(request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(deliveryTrackingService, times(1)).updateDeliveryStatus(
            eq("TWILIO"),
            eq("SM123456"),
            eq("DELIVERED"),
            any(),
            any(),
            any()
        );
    }
    
    @Test
    void testTwilioWebhookWithMissingSignatureWhenValidationEnabled() {
        // Given
        ReflectionTestUtils.setField(twilioController, "signatureValidationEnabled", true);
        ReflectionTestUtils.setField(twilioController, "twilioAuthToken", "test-token");
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("MessageSid", "SM123456");
        request.setParameter("MessageStatus", "delivered");
        // No X-Twilio-Signature header
        
        // When
        ResponseEntity<String> response = twilioController.handleStatusCallback(request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("Invalid signature");
        verify(deliveryTrackingService, never()).updateDeliveryStatus(any(), any(), any(), any(), any(), any());
    }
    
    @Test
    void testTwilioWebhookWithMissingRequiredParameters() {
        // Given
        ReflectionTestUtils.setField(twilioController, "signatureValidationEnabled", false);
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        // Missing MessageSid and MessageStatus
        
        // When
        ResponseEntity<String> response = twilioController.handleStatusCallback(request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Missing required parameters");
        verify(deliveryTrackingService, never()).updateDeliveryStatus(any(), any(), any(), any(), any(), any());
    }
    
    @Test
    void testAwsSnsWebhookWithoutSignatureWhenValidationDisabled() throws Exception {
        // Given
        ReflectionTestUtils.setField(snsController, "signatureValidationEnabled", false);
        
        String payload = """
            {
                "Type": "Notification",
                "MessageId": "test-message-id",
                "TopicArn": "arn:aws:sns:us-east-1:123456789:test-topic",
                "Message": "{\\"notification\\":{\\"messageId\\":\\"msg-123\\",\\"status\\":\\"SUCCESS\\"}}",
                "Timestamp": "2024-01-15T10:00:00.000Z",
                "SignatureVersion": "1",
                "Signature": "test-signature",
                "SigningCertURL": "https://sns.us-east-1.amazonaws.com/cert.pem"
            }
            """;
        
        // When
        ResponseEntity<String> response = snsController.handleSnsNotification(payload, "Notification");
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
    
    @Test
    void testAwsSnsWebhookWithInvalidCertificateUrl() throws Exception {
        // Given
        ReflectionTestUtils.setField(snsController, "signatureValidationEnabled", true);
        
        String payload = """
            {
                "Type": "Notification",
                "MessageId": "test-message-id",
                "TopicArn": "arn:aws:sns:us-east-1:123456789:test-topic",
                "Message": "{\\"notification\\":{\\"messageId\\":\\"msg-123\\",\\"status\\":\\"SUCCESS\\"}}",
                "Timestamp": "2024-01-15T10:00:00.000Z",
                "SignatureVersion": "1",
                "Signature": "test-signature",
                "SigningCertURL": "https://malicious-site.com/cert.pem"
            }
            """;
        
        // When
        ResponseEntity<String> response = snsController.handleSnsNotification(payload, "Notification");
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("Invalid signature");
    }
    
    @Test
    void testAwsSnsWebhookWithMissingMessageType() throws Exception {
        // Given
        ReflectionTestUtils.setField(snsController, "signatureValidationEnabled", false);
        
        String payload = """
            {
                "MessageId": "test-message-id",
                "TopicArn": "arn:aws:sns:us-east-1:123456789:test-topic",
                "Message": "test message"
            }
            """;
        
        // When
        ResponseEntity<String> response = snsController.handleSnsNotification(payload, null);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Missing message type");
    }
    
    @Test
    void testTwilioWebhookSkipsValidationWhenAuthTokenNotConfigured() {
        // Given
        ReflectionTestUtils.setField(twilioController, "signatureValidationEnabled", true);
        ReflectionTestUtils.setField(twilioController, "twilioAuthToken", "");
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("MessageSid", "SM123456");
        request.setParameter("MessageStatus", "delivered");
        // No signature header
        
        // When
        ResponseEntity<String> response = twilioController.handleStatusCallback(request);
        
        // Then - Should process successfully since auth token is not configured
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(deliveryTrackingService, times(1)).updateDeliveryStatus(
            eq("TWILIO"),
            eq("SM123456"),
            eq("DELIVERED"),
            any(),
            any(),
            any()
        );
    }
}
