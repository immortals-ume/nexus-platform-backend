package com.immortals.notificationservice.application.usecase;

import com.immortals.notificationservice.domain.model.Notification;
import com.immortals.notificationservice.domain.port.NotificationProvider;
import com.immortals.notificationservice.domain.port.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;


/**
 * Use case for sending bulk notifications efficiently
 * Optimized for high-throughput scenarios
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SendBulkNotificationUseCase {
    
    private final NotificationRepository notificationRepository;
    private final List<NotificationProvider> notificationProviders;
    
    /**
     * Send multiple notifications in parallel with batch processing
     */
    @Async("bulkNotificationExecutor")
    @Transactional
    public CompletableFuture<List<Notification>> execute(List<Notification> notifications) {
        log.info("Processing bulk notification request: count={}", notifications.size());
        
        // Initialize notifications
        notifications.forEach(notification -> {
            if (notification.getCreatedAt() == null) {
                notification.setCreatedAt(LocalDateTime.now());
            }
            if (notification.getStatus() == null) {
                notification.setStatus(Notification.NotificationStatus.PENDING);
            }
            if (notification.getRetryCount() == null) {
                notification.setRetryCount(0);
            }
        });
        
        // Filter out duplicates (idempotency check) - Java 17 toList()
        var newNotifications = notifications.stream()
                .filter(n -> !notificationRepository.existsByEventId(n.getEventId()))
                .toList();
        
        log.info("Filtered notifications: total={}, new={}, duplicates={}", 
                 notifications.size(), newNotifications.size(), 
                 notifications.size() - newNotifications.size());
        
        // Batch save pending notifications - Java 17 toList()
        var savedNotifications = newNotifications.stream()
                .map(notificationRepository::save)
                .toList();
        
        // Process notifications in parallel
        var futures = savedNotifications.stream()
                .map(this::processNotificationAsync)
                .toList();
        
        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        
        // Collect results - Java 17 toList()
        var results = futures.stream()
                .map(CompletableFuture::join)
                .toList();
        
        var successCount = results.stream().filter(Notification::isSent).count();
        var failureCount = results.stream().filter(Notification::isFailed).count();
        
        log.info("Bulk notification processing completed: total={}, success={}, failed={}", 
                 results.size(), successCount, failureCount);
        
        return CompletableFuture.completedFuture(results);
    }
    
    private CompletableFuture<Notification> processNotificationAsync(Notification notification) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Find appropriate provider
                NotificationProvider provider = notificationProviders.stream()
                        .filter(p -> p.supports(notification.getType()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No provider found for type: " + notification.getType()));
                
                // Send notification
                boolean success = provider.send(notification);
                
                // Update status
                if (success) {
                    notification.markAsSent();
                } else {
                    notification.markAsFailed("Provider failed to send notification");
                }
                
                // Save updated status
                return notificationRepository.save(notification);
                
            } catch (Exception e) {
                log.error("Error processing notification: eventId={}, error={}", 
                         notification.getEventId(), e.getMessage());
                notification.markAsFailed(e.getMessage());
                return notificationRepository.save(notification);
            }
        });
    }
}
