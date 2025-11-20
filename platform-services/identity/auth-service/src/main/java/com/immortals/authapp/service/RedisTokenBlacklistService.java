package com.immortals.authapp.service;

import com.immortals.authapp.service.cache.CacheService;
import com.immortals.platform.common.exception.AuthenticationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

import static com.immortals.authapp.constants.CacheConstants.BLACKLIST_HASH_KEY;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisTokenBlacklistService implements TokenBlacklistService {

    private final CacheService<String, String, Object> hashCacheService;

    @Override
    public void blacklistToken(String token, long ttlInMillis) {
        try {
            hashCacheService.put(
                    BLACKLIST_HASH_KEY,
                    token,
                    "blacklisted",
                    Duration.ofMillis(ttlInMillis),
                    UUID.randomUUID().toString()
            );
            log.info("Token blacklisted: {} (TTL: {} ms)", token, ttlInMillis);
        } catch (Exception e) {
            log.error("Failed to blacklist token: {} (TTL: {} ms). Error: {}", token, ttlInMillis, e.getMessage(), e);
            throw new AuthenticationException(e.getMessage());
        }
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        try {
            boolean result = hashCacheService.containsKey(
                    BLACKLIST_HASH_KEY,
                    token,
                    UUID.randomUUID().toString()
            );
            log.debug("Checked blacklist for token {}: {}", token, result);
            return result;
        } catch (Exception e) {
            log.error("Failed to check if token is blacklisted: {}. Error: {}", token, e.getMessage(), e);
            throw new AuthenticationException(e.getMessage());
        }
    }
}
