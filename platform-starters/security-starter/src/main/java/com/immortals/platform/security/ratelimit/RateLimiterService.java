package com.immortals.platform.security.ratelimit;

import com.immortals.cacheservice.service.CacheService;
import com.immortals.platform.security.config.SecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Rate limiter service using cache-starter for distributed rate limiting.
 * Implements sliding window token bucket algorithm.
 * Reuses the cache-starter module instead of duplicating Redis logic.
 */
@Slf4j
@Service
public class RateLimiterService {

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";
    
    private final CacheService<String, Object> cacheService;
    private final SecurityProperties securityProperties;

    public RateLimiterService(CacheService<String, Object> cacheService,
                             SecurityProperties securityProperties) {
        this.cacheService = cacheService;
        this.securityProperties = securityProperties;
        log.info("Rate Limiter Service initialized with cache-starter");
    }

    /**
     * Check if request is allowed based on rate limit.
     * Uses cache-starter for distributed caching.
     * 
     * @param identifier Unique identifier (IP address or user ID)
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean isAllowed(String identifier) {
        SecurityProperties.RateLimit rateLimitConfig = securityProperties.getRateLimit();
        
        if (!rateLimitConfig.getEnabled()) {
            return true;
        }

        int maxTokens = rateLimitConfig.getDefaultLimit();
        int timeWindowSeconds = rateLimitConfig.getTimeWindowSeconds();
        double refillRate = (double) maxTokens / timeWindowSeconds;

        String key = RATE_LIMIT_KEY_PREFIX + identifier;
        long now = System.currentTimeMillis();

        TokenBucket bucket = (TokenBucket) cacheService.get(key);

        if (bucket == null) {
            bucket = new TokenBucket(maxTokens - 1, now);
            cacheService.put(key, bucket);
            log.debug("Rate limit initialized for {}: {} tokens remaining", identifier, bucket.getTokens());
            return true;
        }

        // Calculate tokens to add based on elapsed time
        long elapsedMillis = now - bucket.getLastRefillTimestamp();
        int tokensToAdd = (int) (elapsedMillis / 1000.0 * refillRate);
        int newTokenCount = Math.min(bucket.getTokens() + tokensToAdd, maxTokens);

        if (newTokenCount > 0) {
            bucket.setTokens(newTokenCount - 1);
            bucket.setLastRefillTimestamp(now);
            cacheService.put(key, bucket);
            log.debug("Rate limit check for {}: {} tokens remaining", identifier, bucket.getTokens());
            return true;
        } else {
            bucket.setTokens(0);
            bucket.setLastRefillTimestamp(now);
            cacheService.put(key, bucket);
            log.warn("Rate limit exceeded for {}", identifier);
            return false;
        }
    }

    /**
     * Get remaining tokens for identifier.
     */
    public int getRemainingTokens(String identifier) {
        String key = RATE_LIMIT_KEY_PREFIX + identifier;
        TokenBucket bucket = (TokenBucket) cacheService.get(key);
        
        if (bucket == null) {
            return securityProperties.getRateLimit().getDefaultLimit();
        }
        
        return bucket.getTokens();
    }

    /**
     * Reset rate limit for identifier.
     */
    public void reset(String identifier) {
        String key = RATE_LIMIT_KEY_PREFIX + identifier;
        cacheService.remove(key);
        log.info("Rate limit reset for {}", identifier);
    }
}
