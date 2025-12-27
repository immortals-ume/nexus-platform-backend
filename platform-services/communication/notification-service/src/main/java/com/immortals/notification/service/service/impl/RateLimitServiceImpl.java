package com.immortals.notification.service.service.impl;

import com.immortals.platform.domain.notifications.domain.model.Notification;
import com.immortals.notification.service.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Implementation of RateLimitService using Redis token bucket algorithm
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    // Default rate limits per channel (per hour)
    private static final long EMAIL_LIMIT_PER_HOUR = 100;
    private static final long SMS_LIMIT_PER_HOUR = 50;
    private static final long WHATSAPP_LIMIT_PER_HOUR = 50;
    private static final long PUSH_LIMIT_PER_HOUR = 200;
    
    private static final long RATE_LIMIT_WINDOW_SECONDS = 3600; // 1 hour
    
    @Override
    public boolean isWithinRateLimit(String userId, Notification.NotificationType channel) {
        String key = generateRateLimitKey(userId, channel);
        String value = redisTemplate.opsForValue().get(key);
        
        if (value == null) {
            return true; // No rate limit record, within limit
        }
        
        long currentCount = Long.parseLong(value);
        long limit = getRateLimitForChannel(channel);
        
        boolean withinLimit = currentCount < limit;
        log.debug("Rate limit check for user: {}, channel: {}, count: {}, limit: {}, withinLimit: {}", 
                userId, channel, currentCount, limit, withinLimit);
        
        return withinLimit;
    }
    
    @Override
    public boolean consumeToken(String userId, Notification.NotificationType channel) {
        if (!isWithinRateLimit(userId, channel)) {
            log.warn("Rate limit exceeded for user: {}, channel: {}", userId, channel);
            return false;
        }
        
        String key = generateRateLimitKey(userId, channel);
        Long newCount = redisTemplate.opsForValue().increment(key);
        
        if (newCount == 1) {
            // First request, set expiration
            redisTemplate.expire(key, RATE_LIMIT_WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        
        log.debug("Token consumed for user: {}, channel: {}, newCount: {}", userId, channel, newCount);
        return true;
    }
    
    @Override
    public long getRemainingTokens(String userId, Notification.NotificationType channel) {
        String key = generateRateLimitKey(userId, channel);
        String value = redisTemplate.opsForValue().get(key);
        
        long currentCount = value != null ? Long.parseLong(value) : 0;
        long limit = getRateLimitForChannel(channel);
        
        return Math.max(0, limit - currentCount);
    }
    
    @Override
    public void resetRateLimit(String userId, Notification.NotificationType channel) {
        String key = generateRateLimitKey(userId, channel);
        redisTemplate.delete(key);
        log.info("Rate limit reset for user: {}, channel: {}", userId, channel);
    }
    
    private String generateRateLimitKey(String userId, Notification.NotificationType channel) {
        return String.format("notification:ratelimit:%s:%s", userId, channel.name());
    }
    
    private long getRateLimitForChannel(Notification.NotificationType channel) {
        return switch (channel) {
            case EMAIL -> EMAIL_LIMIT_PER_HOUR;
            case SMS -> SMS_LIMIT_PER_HOUR;
            case WHATSAPP -> WHATSAPP_LIMIT_PER_HOUR;
            case PUSH_NOTIFICATION -> PUSH_LIMIT_PER_HOUR;
        };
    }
}
