package com.immortals.notification.service.controller;

import com.immortals.notification.service.application.usecase.ScheduleNotificationUseCase;
import com.immortals.notification.service.application.usecase.impl.SendBulkNotificationUseCase;
import com.immortals.notification.service.application.usecase.impl.SendNotificationUseCase;
import com.immortals.notification.service.infra.adapter.NotificationRepositoryAdapter;
import com.immortals.platform.domain.dto.ApiResponse;
import com.immortals.platform.domain.dto.PageResponse;
import com.immortals.platform.domain.notifications.domain.Notification;
import com.immortals.platform.domain.notifications.domain.NotificationPriority;
import com.immortals.platform.domain.notifications.dto.NotificationRequest;
import com.immortals.platform.domain.notifications.dto.NotificationResponse;
import com.immortals.platform.domain.notifications.dto.SendNotificationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "Notification management APIs")
public class NotificationController {

    private final SendNotificationUseCase sendNotificationUseCase;
    private final SendBulkNotificationUseCase sendBulkNotificationUseCase;
    private final ScheduleNotificationUseCase scheduleNotificationUseCase;
    private final NotificationRepositoryAdapter notificationRepository;
    private final NotificationRequestValidator requestValidator;

    /**
     * POST /api/v1/notifications - Send notification
     * Requirements: 1.1
     */
    @PostMapping
    @Operation(summary = "Send or schedule a notification",
            description = "Send a notification immediately or schedule it for future delivery")
    public ResponseEntity<ApiResponse<NotificationResponse>> createNotification(
            @Valid @RequestBody SendNotificationRequest request) {

        log.info("Received notification request: type={}, recipient={}, scheduledAt={}",
                request.getType(), request.getRecipient(), request.getScheduledAt());

        requestValidator.validate(request);

        Notification notification = Notification.builder()
                .eventId(UUID.randomUUID()
                        .toString())
                .type(Notification.NotificationType.valueOf(request.getType()
                        .toUpperCase()))
                .priority(request.getPriority() != null ?
                        NotificationPriority.valueOf(request.getPriority()
                                .toUpperCase()) :
                        NotificationPriority.NORMAL)
                .recipient(request.getRecipient())
                .subject(request.getSubject())
                .message(request.getMessage())
                .htmlContent(request.getHtmlContent())
                .correlationId(request.getCorrelationId())
                .templateCode(request.getTemplateCode())
                .templateVariables(request.getTemplateVariables())
                .countryCode(request.getCountryCode())
                .locale(request.getLocale())
                .metadata(request.getMetadata())
                .build();

        Notification result;
        if (request.getScheduledAt() != null) {
            result = scheduleNotificationUseCase.schedule(notification, request.getScheduledAt());
        } else {
            result = sendNotificationUseCase.execute(notification);
        }

        NotificationResponse response = mapToResponse(result);

        return ResponseEntity.ok(ApiResponse.success(
                response,
                result.getStatus() == Notification.NotificationStatus.SCHEDULED
                        ? "Notification scheduled successfully"
                        : result.isSent() ? "Notification sent successfully" : "Failed to send notification"
        ));
    }

    /**
     * GET /api/v1/notifications - List notifications with pagination, search, and filter
     * Requirements: 11.3
     */
    @GetMapping
    @Operation(summary = "List notifications with pagination and filtering",
            description = "Get notifications with optional filters for status, type, recipient, and date range")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> listNotifications(
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction (ASC or DESC)")
            @RequestParam(defaultValue = "DESC") String sortDirection,

            @Parameter(description = "Filter by status")
            @RequestParam(required = false) String status,

            @Parameter(description = "Filter by notification type")
            @RequestParam(required = false) String type,

            @Parameter(description = "Search by recipient")
            @RequestParam(required = false) String recipient,

            @Parameter(description = "Filter by start date (ISO format)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,

            @Parameter(description = "Filter by end date (ISO format)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Listing notifications: page={}, size={}, status={}, type={}, recipient={}",
                page, size, status, type, recipient);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Notification> notificationPage = notificationRepository.findWithFilters(
                status, type, recipient, startDate, endDate, pageable
        );

        List<NotificationResponse> responses = notificationPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        PageResponse<NotificationResponse> pageResponse = PageResponse.of(
                responses,
                notificationPage.getNumber(),
                notificationPage.getSize(),
                notificationPage.getTotalElements()
        );

        return ResponseEntity.ok(ApiResponse.success(
                pageResponse,
                "Found " + notificationPage.getTotalElements() + " notifications"
        ));
    }

    /**
     * GET /api/v1/notifications/{id} - Get notification details
     * Requirements: 11.3
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get notification details",
            description = "Get detailed information about a specific notification")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotification(
            @Parameter(description = "Notification ID")
            @PathVariable Long id) {

        log.info("Fetching notification details: id={}", id);

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + id));

        NotificationResponse response = mapToDetailedResponse(notification);

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Notification retrieved successfully"
        ));
    }

    /**
     * POST /api/v1/notifications/{id}/retry - Retry failed notification
     * Requirements: 11.3
     */
    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry a failed notification",
            description = "Retry sending a notification that previously failed")
    public ResponseEntity<ApiResponse<NotificationResponse>> retryNotification(
            @Parameter(description = "Notification ID")
            @PathVariable Long id) {

        log.info("Retrying notification: id={}", id);

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + id));

        if (notification.getStatus() != Notification.NotificationStatus.FAILED) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(
                            "Cannot retry notification",
                            "Only failed notifications can be retried. Current status: " + notification.getStatus()
                    ));
        }

        // Reset status and retry
        notification.setStatus(Notification.NotificationStatus.PENDING);
        notification.setErrorMessage(null);
        Notification result = sendNotificationUseCase.execute(notification);

        NotificationResponse response = mapToResponse(result);

        return ResponseEntity.ok(ApiResponse.success(
                response,
                result.isSent() ? "Notification retried successfully" : "Retry failed"
        ));
    }

    /**
     * DELETE /api/v1/notifications/{id} - Cancel scheduled notification
     * Requirements: 13.4
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a scheduled notification",
            description = "Cancel a notification that is scheduled for future delivery")
    public ResponseEntity<ApiResponse<String>> cancelScheduledNotification(
            @Parameter(description = "Notification ID")
            @PathVariable Long id) {

        log.info("Cancelling scheduled notification: id={}", id);

        scheduleNotificationUseCase.cancelScheduled(id);

        return ResponseEntity.ok(ApiResponse.success(
                "Notification cancelled successfully",
                "Scheduled notification with ID " + id + " has been cancelled"
        ));
    }

    /**
     * GET /api/v1/notifications/scheduled - List scheduled notifications
     * Requirements: 13.5
     */
    @GetMapping("/scheduled")
    @Operation(summary = "List scheduled notifications",
            description = "Get all notifications that are scheduled for future delivery")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> listScheduledNotifications() {

        log.info("Fetching all scheduled notifications");

        List<Notification> scheduledNotifications = notificationRepository.findAllScheduledNotifications();

        List<NotificationResponse> responses = scheduledNotifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(
                responses,
                "Found " + responses.size() + " scheduled notifications"
        ));
    }

    /**
     * Legacy endpoint for backward compatibility
     */
    @PostMapping("/send")
    @Operation(summary = "Send a notification (legacy)", description = "Send email, SMS, or WhatsApp notification")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendNotification(
            @Valid @RequestBody NotificationRequest request) {

        log.info("Received notification request: type={}, recipient={}",
                request.getType(), request.getRecipient());

        Notification notification = Notification.builder()
                .eventId(UUID.randomUUID()
                        .toString())
                .type(Notification.NotificationType.valueOf(request.getType()
                        .toUpperCase()))
                .recipient(request.getRecipient())
                .subject(request.getSubject())
                .message(request.getMessage())
                .htmlContent(request.getHtmlContent())
                .correlationId(request.getCorrelationId())
                .build();

        Notification result = sendNotificationUseCase.execute(notification);

        NotificationResponse response = mapToResponse(result);

        return ResponseEntity.ok(ApiResponse.success(
                response,
                result.isSent() ? "Notification sent successfully" : "Failed to send notification"
        ));
    }

    /**
     * Legacy endpoint for bulk notifications
     */
    @PostMapping("/send/bulk")
    @Operation(summary = "Send bulk notifications",
            description = "Send multiple notifications in parallel for high throughput")
    public ResponseEntity<ApiResponse<String>> sendBulkNotifications(
            @Valid @RequestBody List<NotificationRequest> requests) {

        log.info("Received bulk notification request: count={}", requests.size());

        List<Notification> notifications = requests.stream()
                .map(request -> Notification.builder()
                        .eventId(UUID.randomUUID()
                                .toString())
                        .type(Notification.NotificationType.valueOf(request.getType()
                                .toUpperCase()))
                        .recipient(request.getRecipient())
                        .subject(request.getSubject())
                        .message(request.getMessage())
                        .htmlContent(request.getHtmlContent())
                        .correlationId(request.getCorrelationId())
                        .build())
                .toList();

        sendBulkNotificationUseCase.execute(notifications);

        return ResponseEntity.accepted()
                .body(
                        ApiResponse.success(
                                "Bulk notification processing initiated",
                                "Processing " + requests.size() + " notifications"
                        )
                );
    }

    /**
     * Map Notification domain model to NotificationResponse DTO (basic fields)
     */
    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .eventId(notification.getEventId())
                .type(notification.getType() != null ? notification.getType()
                        .name() : null)
                .recipient(notification.getRecipient())
                .subject(notification.getSubject())
                .status(notification.getStatus() != null ? notification.getStatus()
                        .name() : null)
                .deliveryStatus(notification.getDeliveryStatus() != null ? notification.getDeliveryStatus()
                        .name() : null)
                .scheduledAt(notification.getScheduledAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    /**
     * Map Notification domain model to detailed NotificationResponse DTO (all fields)
     */
    private NotificationResponse mapToDetailedResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .eventId(notification.getEventId())
                .type(notification.getType() != null ? notification.getType()
                        .name() : null)
                .recipient(notification.getRecipient())
                .countryCode(notification.getCountryCode())
                .locale(notification.getLocale())
                .subject(notification.getSubject())
                .message(notification.getMessage())
                .templateCode(notification.getTemplateCode())
                .status(notification.getStatus() != null ? notification.getStatus()
                        .name() : null)
                .deliveryStatus(notification.getDeliveryStatus() != null ? notification.getDeliveryStatus()
                        .name() : null)
                .errorMessage(notification.getErrorMessage())
                .providerId(notification.getProviderId())
                .providerMessageId(notification.getProviderMessageId())
                .correlationId(notification.getCorrelationId())
                .scheduledAt(notification.getScheduledAt())
                .createdAt(notification.getCreatedAt())
                .processedAt(notification.getProcessedAt())
                .deliveredAt(notification.getDeliveredAt())
                .readAt(notification.getReadAt())
                .retryCount(notification.getRetryCount())
                .metadata(notification.getMetadata())
                .build();
    }
}
