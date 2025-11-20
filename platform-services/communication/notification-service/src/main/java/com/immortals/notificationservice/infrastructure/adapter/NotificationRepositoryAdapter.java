package com.immortals.notificationservice.infrastructure.adapter;

import com.immortals.notificationservice.domain.model.Notification;
import com.immortals.notificationservice.domain.port.NotificationRepository;
import com.immortals.notificationservice.entity.NotificationLog;
import com.immortals.notificationservice.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

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
        NotificationLog saved = notificationLogRepository.save(entity);
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
    
    private NotificationLog toEntity(Notification notification) {
        return NotificationLog.builder()
                .id(notification.getId())
                .eventId(notification.getEventId())
                .notificationType(notification.getType().name())
                .recipient(notification.getRecipient())
                .subject(notification.getSubject())
                .message(notification.getMessage())
                .status(notification.getStatus().name())
                .errorMessage(notification.getErrorMessage())
                .correlationId(notification.getCorrelationId())
                .createdAt(notification.getCreatedAt())
                .processedAt(notification.getProcessedAt())
                .retryCount(notification.getRetryCount())
                .build();
    }
    
    private Notification toDomain(NotificationLog entity) {
        return Notification.builder()
                .id(entity.getId())
                .eventId(entity.getEventId())
                .type(Notification.NotificationType.valueOf(entity.getNotificationType()))
                .recipient(entity.getRecipient())
                .subject(entity.getSubject())
                .message(entity.getMessage())
                .status(Notification.NotificationStatus.valueOf(entity.getStatus()))
                .errorMessage(entity.getErrorMessage())
                .correlationId(entity.getCorrelationId())
                .createdAt(entity.getCreatedAt())
                .processedAt(entity.getProcessedAt())
                .retryCount(entity.getRetryCount())
                .build();
    }
}
