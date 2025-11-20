package com.immortals.notificationservice.application.usecase;

import com.immortals.notificationservice.domain.model.Notification;
import com.immortals.notificationservice.domain.port.NotificationProvider;
import com.immortals.notificationservice.domain.port.NotificationRepository;
import com.immortals.platform.common.exception.BusinessException;
import com.immortals.platform.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Use case for sending notifications
 * Implements hexagonal architecture application layer
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SendNotificationUseCase {
    
    private final NotificationRepository notificationRepository;
    private final List<NotificationProvider> notificationProviders;
    
    @Transactional
    public Notification execute(Notification notification) {
        log.info("Processing notification: eventId={}, type={}, recipient={}", 
                 notification.getEventId(), notification.getType(), notification.getRecipient());
        
        // Idempotency check
        if (notificationRepository.existsByEventId(notification.getEventId())) {
            log.info("Notification already processed: eventId={}", notification.getEventId());
            return notificationRepository.findByEventId(notification.getEventId())
                    .orElseThrow(() -> new ResourceNotFoundException("Notification", notification.getEventId()));
        }
        
        // Initialize notification (Java 17 - no null checks needed with proper defaults)
        initializeNotification(notification);
        
        // Save as pending
        var savedNotification = notificationRepository.save(notification);
        
        // Find appropriate provider using Java 17 pattern matching
        var provider = findProvider(notification.getType());
        
        // Send notification
        var success = provider.send(notification);
        
        // Update status
        updateNotificationStatus(savedNotification, success);
        
        return notificationRepository.save(savedNotification);
    }
    
    private void initializeNotification(Notification notification) {
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(LocalDateTime.now());
        }
        if (notification.getStatus() == null) {
            notification.setStatus(Notification.NotificationStatus.PENDING);
        }
        if (notification.getRetryCount() == null) {
            notification.setRetryCount(0);
        }
    }
    
    private NotificationProvider findProvider(Notification.NotificationType type) {
        return notificationProviders.stream()
                .filter(p -> p.supports(type))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "No notification provider found for type: " + type));
    }
    
    private void updateNotificationStatus(Notification notification, boolean success) {
        if (success) {
            notification.markAsSent();
            log.info("Notification sent successfully: eventId={}", notification.getEventId());
        } else {
            notification.markAsFailed("Provider failed to send notification");
            log.error("Failed to send notification: eventId={}", notification.getEventId());
        }
    }
}
