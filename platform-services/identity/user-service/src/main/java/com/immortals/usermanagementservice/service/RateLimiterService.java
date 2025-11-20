package com.immortals.usermanagementservice.service;

import com.immortals.usermanagementservice.model.helper.TokenBucket;
import com.immortals.usermanagementservice.service.cache.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;


import static com.immortals.usermanagementservice.constants.AuthAppConstant.MAX_TOKENS;
import static com.immortals.usermanagementservice.constants.AuthAppConstant.REFILL_TOKENS_PER_SECONDS;
import static com.immortals.usermanagementservice.constants.CacheConstants.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class RateLimiterService {

    private final CacheService<String, String, TokenBucket> hashCacheService;
    private final Duration cacheTtl = Duration.ofHours(1);

    public boolean isAllowed(String ipAddress) {
        long now = System.currentTimeMillis();
        TokenBucket bucket = hashCacheService.get(RATE_LIMITING_HASH_KEY, ipAddress, ipAddress);

        if (bucket == null) {
            bucket = new TokenBucket(MAX_TOKENS, now);
        }

        long elapsedMillis = now - bucket.getLastRefillTimestamp();
        int tokensToAdd = (int) (elapsedMillis / 1000.0 * REFILL_TOKENS_PER_SECONDS);
        int newTokenCount = Math.min(bucket.getTokens() + tokensToAdd, MAX_TOKENS);

        if (newTokenCount > 0) {
            bucket.setTokens(newTokenCount - 1);
            bucket.setLastRefillTimestamp(now);
            hashCacheService.put(RATE_LIMITING_HASH_KEY, ipAddress, bucket, cacheTtl, ipAddress);
            return true;
        } else {
            bucket.setTokens(newTokenCount);
            bucket.setLastRefillTimestamp(now);
            hashCacheService.put(RATE_LIMITING_HASH_KEY, ipAddress, bucket, cacheTtl, ipAddress);
            return false;
        }
    }
}
