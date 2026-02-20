package com.immortals.authapp.service.impl;

import com.immortals.authapp.service.TokenBlacklistService;
import com.immortals.platform.cache.providers.redis.RedisHashCacheService;
import com.immortals.platform.common.db.annotation.ReadOnly;
import com.immortals.platform.common.db.annotation.WriteOnly;
import com.immortals.platform.common.exception.AuthenticationException;
import com.immortals.platform.domain.auth.dto.BlacklistEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

import static com.immortals.platform.domain.auth.constants.CacheConstants.BLACKLIST_HASH_KEY;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisTokenBlacklistService implements TokenBlacklistService {

    private final RedisHashCacheService<String, String, Object> hashCacheService;

    @Override
    @CachePut(value = "token-blacklist", key = "#token")
    @WriteOnly
    public void blacklistToken(String token, long ttlInMillis) {
        try {
            BlacklistEntry entry = new BlacklistEntry(token, Instant.now(), ttlInMillis);

            hashCacheService.put(
                    BLACKLIST_HASH_KEY,
                    token,
                    entry,
                    Duration.ofMillis(ttlInMillis)
            );

            log.info("Token blacklisted: {} (TTL: {} ms)", token, ttlInMillis);
        } catch (Exception e) {
            log.error("Failed to blacklist token: {} (TTL: {} ms). Error: {}", token, ttlInMillis, e.getMessage(), e);
            throw new AuthenticationException("Failed to blacklist token: " + e.getMessage());
        }
    }

    @Override
    @Cacheable(value = "token-blacklist", key = "#token", condition = "#token != null")
    @ReadOnly
    public boolean isTokenBlacklisted(String token) {
        try {
            boolean result = hashCacheService.containsKey(BLACKLIST_HASH_KEY, token);

            log.debug("Checked blacklist for token {}: {}", token, result);
            return result;
        } catch (Exception e) {
            log.error("Failed to check if token is blacklisted: {}. Error: {}", token, e.getMessage(), e);
            throw new AuthenticationException("Failed to check if token is blacklisted", e.getMessage());
        }
    }

    /**
     * Get blacklist entry details
     */
    @Cacheable(value = "token-blacklist-details", key = "#token", condition = "#token != null")
    @ReadOnly
    public BlacklistEntry getBlacklistEntry(String token) {
        try {
            Object entry = hashCacheService.get(BLACKLIST_HASH_KEY, token);
            if (entry instanceof BlacklistEntry) {
                return (BlacklistEntry) entry;
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to get blacklist entry for token: {}. Error: {}", token, e.getMessage(), e);
            return null;
        }
    }

}
