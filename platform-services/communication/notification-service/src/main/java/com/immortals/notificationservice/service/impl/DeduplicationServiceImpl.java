package com.immortals.notificationservice.service.impl;

import com.immortals.notificationservice.domain.model.Notification;
import com.immortals.notificationservice.service.DeduplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;

/**
 * Redis-based deduplication service
 * Prevents duplicate notifications within configurable time windows
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeduplicationServiceImpl implements DeduplicationService {
    
    private final StringRedisTemplate redisTemplate;
    
    private static final String DEDUP_KEY_PREFIX = "notification:dedup:";
    
    // Deduplication windows by type
    private static final long EMAIL_WINDOW = 300; // 5 minutes
    private static final long SMS_WINDOW = 60;    // 1 minute
    private static final long WHATSAPP_WINDOW = 60; // 1 minute
    
    @Override
    public boolean isDuplicate(Notification notification) {
        var key = generateDeduplicationKey(notification);
        var exists = redisTemplate.hasKey(key);
        
        if (Boolean.TRUE.equals(exists)) {
            log.warn("Duplicate notification detected: {}", key);
            return true;
        }
        
        return false;
    }
    
    @Override
    public void markAsSent(Notification notification) {
        var key = generateDeduplicationKey(notification);
        var window = getDeduplicationWindow(notification.getType());
        
        redisTemplate.opsForValue().set(key, "sent", Duration.ofSeconds(window));
        log.debug("Marked notification as sent: {}, window: {}s", key, window);
    }
    
    @Override
    public String generateDeduplicationKey(Notification notification) {
        // Key format: notification:dedup:{type}:{recipient}:{hash}
        var contentHash = Objects.hash(
            notification.getSubject(),
            notification.getMessage(),
            notification.getTemplateCode()
        );
        
        return String.format("%s%s:%s:%d",
            DEDUP_KEY_PREFIX,
            notification.getType(),
            notification.getRecipient(),
            contentHash
        );
    }
    
    @Override
    public long getDeduplicationWindow(Notification.NotificationType type) {
        return switch (type) {
            case EMAIL -> EMAIL_WINDOW;
            case SMS -> SMS_WINDOW;
            case WHATSAPP -> WHATSAPP_WINDOW;
        };
    }
}
