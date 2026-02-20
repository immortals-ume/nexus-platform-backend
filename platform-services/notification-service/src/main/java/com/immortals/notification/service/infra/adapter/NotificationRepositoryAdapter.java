package com.immortals.notification.service.infra.adapter;

import com.immortals.notification.service.repository.NotificationLogRepository;
import com.immortals.platform.domain.notifications.domain.Notification;
import com.immortals.notification.service.application.usecase.port.NotificationRepository;
import com.immortals.platform.domain.notifications.entity.NotificationLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter for NotificationRepository port (Infrastructure Layer)
 */
@Component
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final NotificationLogRepository notificationLogRepository;

    @Override
    public Notification save(Notification notification) {
        NotificationLog entity = toEntity(notification);
        NotificationLog saved = notificationLogRepository.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Notification> findByEventId(String eventId) {
        return notificationLogRepository.findByEventId(eventId)
                .map(this::toDomain);
    }

    @Override
    public boolean existsByEventId(String eventId) {
        return notificationLogRepository.existsByEventId(eventId);
    }
    
    /**
     * Find all scheduled notifications that are due for processing
     */
    public List<Notification> findDueScheduledNotifications(LocalDateTime currentTime) {
        return notificationLogRepository.findDueScheduledNotifications(currentTime)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    /**
     * Find all scheduled notifications
     */
    public List<Notification> findAllScheduledNotifications() {
        return notificationLogRepository.findAllScheduledNotifications()
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    /**
     * Find notification by ID
     */
    public Optional<Notification> findById(Long id) {
        return notificationLogRepository.findById(id)
                .map(this::toDomain);
    }
    
    /**
     * Find notifications with pagination and filtering
     */
    public Page<Notification> findWithFilters(
            String status,
            String notificationType,
            String recipient,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {
        
        return notificationLogRepository.findWithFilters(
                status, notificationType, recipient, startDate, endDate, pageable
        ).map(this::toDomain);
    }

    private NotificationLog toEntity(Notification notification) {
        return NotificationLog.builder()
                .eventId(notification.getEventId())
                .notificationType(notification.getType() != null ? notification.getType().name() : null)
                .recipient(notification.getRecipient())
                .countryCode(notification.getCountryCode())
                .locale(notification.getLocale())
                .subject(notification.getSubject())
                .message(notification.getMessage())
                .htmlContent(notification.getHtmlContent())
                .templateCode(notification.getTemplateCode())
                .templateVariables(notification.getTemplateVariables())
                .status(notification.getStatus() != null ? notification.getStatus().name() : null)
                .deliveryStatus(notification.getDeliveryStatus() != null ? notification.getDeliveryStatus().name() : null)
                .errorMessage(notification.getErrorMessage())
                .correlationId(notification.getCorrelationId())
                .providerId(notification.getProviderId())
                .providerMessageId(notification.getProviderMessageId())
                .createdAt(notification.getCreatedAt())
                .processedAt(notification.getProcessedAt())
                .deliveredAt(notification.getDeliveredAt())
                .readAt(notification.getReadAt())
                .scheduledAt(notification.getScheduledAt())
                .retryCount(notification.getRetryCount())
                .maxRetries(notification.getMaxRetries())
                .metadata(notification.getMetadata())
                .build();
    }

    private Notification toDomain(NotificationLog entity) {
        return Notification.builder()
                .id(entity.getId())
                .eventId(entity.getEventId())
                .type(entity.getNotificationType() != null ? Notification.NotificationType.valueOf(entity.getNotificationType()) : null)
                .recipient(entity.getRecipient())
                .countryCode(entity.getCountryCode())
                .locale(entity.getLocale())
                .subject(entity.getSubject())
                .message(entity.getMessage())
                .htmlContent(entity.getHtmlContent())
                .templateCode(entity.getTemplateCode())
                .templateVariables(entity.getTemplateVariables())
                .status(entity.getStatus() != null ? Notification.NotificationStatus.valueOf(entity.getStatus()) : null)
                .deliveryStatus(entity.getDeliveryStatus() != null ? Notification.DeliveryStatus.valueOf(entity.getDeliveryStatus()) : null)
                .errorMessage(entity.getErrorMessage())
                .correlationId(entity.getCorrelationId())
                .providerId(entity.getProviderId())
                .providerMessageId(entity.getProviderMessageId())
                .createdAt(entity.getCreatedAt())
                .processedAt(entity.getProcessedAt())
                .deliveredAt(entity.getDeliveredAt())
                .readAt(entity.getReadAt())
                .scheduledAt(entity.getScheduledAt())
                .retryCount(entity.getRetryCount())
                .maxRetries(entity.getMaxRetries())
                .metadata(entity.getMetadata())
                .build();
    }
}
