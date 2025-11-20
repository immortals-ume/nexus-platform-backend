package com.immortals.notificationservice.api.controller;

import com.immortals.notificationservice.api.dto.NotificationRequest;
import com.immortals.notificationservice.api.dto.NotificationResponse;
import com.immortals.notificationservice.application.usecase.SendBulkNotificationUseCase;
import com.immortals.notificationservice.application.usecase.SendNotificationUseCase;
import com.immortals.notificationservice.domain.model.Notification;
import com.immortals.platform.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * REST API controller for notifications
 * Uses common-starter's ApiResponse for consistent responses
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "Notification management APIs")
public class NotificationController {
    
    private final SendNotificationUseCase sendNotificationUseCase;
    private final SendBulkNotificationUseCase sendBulkNotificationUseCase;
    
    @PostMapping("/send")
    @Operation(summary = "Send a notification", description = "Send email, SMS, or WhatsApp notification")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendNotification(
            @Valid @RequestBody NotificationRequest request) {
        
        log.info("Received notification request: type={}, recipient={}", 
                 request.getType(), request.getRecipient());
        
        Notification notification = Notification.builder()
                .eventId(UUID.randomUUID().toString())
                .type(Notification.NotificationType.valueOf(request.getType().toUpperCase()))
                .recipient(request.getRecipient())
                .subject(request.getSubject())
                .message(request.getMessage())
                .htmlContent(request.getHtmlContent())
                .correlationId(request.getCorrelationId())
                .build();
        
        Notification result = sendNotificationUseCase.execute(notification);
        
        NotificationResponse response = NotificationResponse.builder()
                .id(result.getId())
                .eventId(result.getEventId())
                .status(result.getStatus().name())
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(
                response,
                result.isSent() ? "Notification sent successfully" : "Failed to send notification"
        ));
    }
    
    @PostMapping("/send/bulk")
    @Operation(summary = "Send bulk notifications", 
               description = "Send multiple notifications in parallel for high throughput")
    public ResponseEntity<ApiResponse<String>> sendBulkNotifications(
            @Valid @RequestBody List<NotificationRequest> requests) {
        
        log.info("Received bulk notification request: count={}", requests.size());
        
        List<Notification> notifications = requests.stream()
                .map(request -> Notification.builder()
                        .eventId(UUID.randomUUID().toString())
                        .type(Notification.NotificationType.valueOf(request.getType().toUpperCase()))
                        .recipient(request.getRecipient())
                        .subject(request.getSubject())
                        .message(request.getMessage())
                        .htmlContent(request.getHtmlContent())
                        .correlationId(request.getCorrelationId())
                        .build())
                .toList(); // Java 17 feature
        
        // Process asynchronously
        sendBulkNotificationUseCase.execute(notifications);
        
        return ResponseEntity.accepted().body(
                ApiResponse.success(
                        "Bulk notification processing initiated",
                        "Processing " + requests.size() + " notifications"
                )
        );
    }
}
