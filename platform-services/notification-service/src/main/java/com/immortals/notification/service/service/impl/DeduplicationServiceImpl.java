package com.immortals.notification.service.service.impl;

import com.immortals.platform.domain.notifications.domain.Notification;
import com.immortals.notification.service.service.DeduplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;

/**
 * Redis-based deduplication service with cache-starter integration
 * Prevents duplicate notifications within configurable time windows
 * 
 * Requirements:
 * - 14.1: Check for duplicate requests using event ID or correlation ID
 * - 14.2: Skip processing duplicate requests within deduplication window
 * - 14.4: Use cache with TTL for performance
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeduplicationServiceImpl implements DeduplicationService {
    
    private final StringRedisTemplate redisTemplate;
    private static final String DEDUP_KEY_PREFIX = "notification:dedup:";

    @Value("${platform.notification.deduplication.window-seconds:300}")
    private long defaultDeduplicationWindow;
    
    @Value("${platform.notification.deduplication.email-window-seconds:300}")
    private long emailWindow;
    
    @Value("${platform.notification.deduplication.sms-window-seconds:60}")
    private long smsWindow;
    
    @Value("${platform.notification.deduplication.whatsapp-window-seconds:60}")
    private long whatsappWindow;
    
    @Value("${platform.notification.deduplication.push-window-seconds:60}")
    private long pushWindow;
    
    @Value("${platform.notification.deduplication.enabled:true}")
    private boolean deduplicationEnabled;
    
    /**
     * Check if notification is duplicate within time window
     * Uses event ID as primary deduplication key for idempotency
     * Requirement 14.1: Check for duplicate requests using event ID
     * Requirement 14.2: Skip processing duplicate requests within deduplication window
     */
    @Override
    public boolean isDuplicate(Notification notification) {
        if (!deduplicationEnabled) {
            log.debug("Deduplication is disabled");
            return false;
        }
        
        try {
            if (notification.getEventId() != null && !notification.getEventId().isBlank()) {
                String eventIdKey = generateEventIdKey(notification.getEventId());
                Boolean exists = redisTemplate.hasKey(eventIdKey);
                
                if (exists) {
                    log.warn("Duplicate notification detected by event ID: eventId={}", 
                            notification.getEventId());
                    return Boolean.TRUE;
                }
            }
            String contentKey = generateDeduplicationKey(notification);
            Boolean exists = redisTemplate.hasKey(contentKey);
            
            if (exists) {
                log.warn("Duplicate notification detected by content: key={}", contentKey);
                return Boolean.TRUE;
            }
            
            return Boolean.FALSE;
            
        } catch (Exception e) {
            log.error("Error checking for duplicate notification, allowing processing: eventId={}", 
                    notification.getEventId(), e);
            // Fallback: Allow processing if cache check fails (Requirement 14.5)
            return Boolean.FALSE;
        }
    }
    
    /**
     * Mark notification as sent for deduplication
     * Stores both event ID and content-based keys with TTL
     * Requirement 14.4: Use cache with TTL for performance
     */
    @Override
    public void markAsSent(Notification notification) {
        if (!deduplicationEnabled) {
            log.debug("Deduplication is disabled, skipping mark as sent");
            return;
        }
        
        try {
            long window = getDeduplicationWindow(notification.getType());
            Duration ttl = Duration.ofSeconds(window);
            if (notification.getEventId() != null && !notification.getEventId().isBlank()) {
                String eventIdKey = generateEventIdKey(notification.getEventId());
                redisTemplate.opsForValue().set(eventIdKey, "sent", ttl);
                log.debug("Marked notification as sent by event ID: eventId={}, window={}s", 
                        notification.getEventId(), window);
            }
            String contentKey = generateDeduplicationKey(notification);
            redisTemplate.opsForValue().set(contentKey, "sent", ttl);
            log.debug("Marked notification as sent by content: key={}, window={}s", 
                    contentKey, window);
            
        } catch (Exception e) {
            log.error("Error marking notification as sent in deduplication cache: eventId={}", 
                    notification.getEventId(), e);
            // Continue processing even if cache write fails (Requirement 14.5)
        }
    }
    
    /**
     * Generate event ID based deduplication key
     * Format: notification:dedup:event:{eventId}
     */
    private String generateEventIdKey(String eventId) {
        return String.format("%sevent:%s", DEDUP_KEY_PREFIX, eventId);
    }
    
    /**
     * Generate content-based deduplication key
     * Format: notification:dedup:{type}:{recipient}:{hash}
     * Hash includes subject, message, and template code
     */
    @Override
    public String generateDeduplicationKey(Notification notification) {
        int contentHash = Objects.hash(
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
    
    /**
     * Get deduplication window in seconds based on notification type
     */
    @Override
    public long getDeduplicationWindow(Notification.NotificationType type) {
        return switch (type) {
            case EMAIL -> emailWindow;
            case SMS -> smsWindow;
            case WHATSAPP -> whatsappWindow;
            case PUSH_NOTIFICATION -> pushWindow;
            default -> defaultDeduplicationWindow;
        };
    }
}
