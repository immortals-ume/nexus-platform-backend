package com.immortals.authapp.service.impl;

import com.immortals.authapp.service.LoginAttempt;
import com.immortals.platform.cache.providers.redis.RedisHashCacheService;
import com.immortals.platform.common.db.annotation.ReadOnly;
import com.immortals.platform.common.db.annotation.WriteOnly;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService implements LoginAttempt {

    private static final String ATTEMPT_KEY_PREFIX = "LOGIN_ATTEMPT:";
    private static final String BLOCK_KEY_PREFIX = "LOGIN_BLOCK:";

    private final RedisHashCacheService<String, String, Object> hashCacheService;

    @Value("${auth.max-login-attempts:5}")
    private int maxAttempts;
    @Value("${auth.login-block-duration-minutes:15}")
    private int blockDurationMinutes;

    @Override
    @WriteOnly
    public void loginSucceeded(String username) {
        try {
            resetAttempts(username);
            log.debug("Login succeeded for user: {}, cleared failed attempts", username);
        } catch (Exception e) {
            log.error("Error clearing login attempts for user: {}", username, e);
        }
    }

    @Override
    @WriteOnly
    public void loginFailed(String username) {
        try {
            String attemptKey = ATTEMPT_KEY_PREFIX + username;
            Object currentAttempts = hashCacheService.get("login_attempts", attemptKey);

            int attempts = currentAttempts != null ? Integer.parseInt(currentAttempts.toString()) : 0;
            attempts++;

            hashCacheService.put("login_attempts", attemptKey, String.valueOf(attempts),
                    Duration.ofMinutes(blockDurationMinutes));

            log.warn("Login failed for user: {}, attempt count: {}", username, attempts);

            if (attempts >= maxAttempts) {
                blockUser(username);
                log.warn("User {} blocked after {} failed login attempts", username, attempts);
            }
        } catch (Exception e) {
            log.error("Error tracking login failure for user: {}", username, e);
        }
    }

    @Override
    @Cacheable(value = "login-blocks", key = "#username", condition = "#username != null")
    @ReadOnly
    public boolean isBlocked(String username) {
        try {
            String blockKey = BLOCK_KEY_PREFIX + username;
            Object blockTime = hashCacheService.get("login_blocks", blockKey);

            if (blockTime != null) {
                Instant blockedAt = Instant.parse(blockTime.toString());
                Instant unblockTime = blockedAt.plus(Duration.ofMinutes(blockDurationMinutes));

                if (Instant.now()
                        .isBefore(unblockTime)) {
                    log.debug("User {} is still blocked until {}", username, unblockTime);
                    return true;
                } else {
                    unblockUser(username);
                    log.info("Block expired for user: {}", username);
                    return false;
                }
            }

            return false;
        } catch (Exception e) {
            log.error("Error checking if user is blocked: {}", username, e);
            return false;
        }
    }

    @Override
    @CacheEvict(value = {"login-attempts", "login-blocks"}, key = "#username")
    @WriteOnly
    public void resetAttempts(String username) {
        try {
            String attemptKey = ATTEMPT_KEY_PREFIX + username;
            hashCacheService.remove("login_attempts", attemptKey);
            log.debug("Reset login attempts for user: {}", username);
        } catch (Exception e) {
            log.error("Error resetting login attempts for user: {}", username, e);
        }
    }

    @Override
    @ReadOnly
    public int getFailedAttempts(String username) {
        try {
            String attemptKey = ATTEMPT_KEY_PREFIX + username;
            Object attempts = hashCacheService.get("login_attempts", attemptKey);
            return attempts != null ? Integer.parseInt(attempts.toString()) : 0;
        } catch (Exception e) {
            log.error("Error getting failed attempts for user: {}", username, e);
            return 0;
        }
    }

    /**
     * Block user for the configured duration
     */
    private void blockUser(String username) {
        try {
            String blockKey = BLOCK_KEY_PREFIX + username;
            String blockTime = Instant.now()
                    .toString();

            hashCacheService.put("login_blocks", blockKey, blockTime,
                    Duration.ofMinutes(blockDurationMinutes));

            log.warn("User {} blocked for {} minutes due to failed login attempts",
                    username, blockDurationMinutes);
        } catch (Exception e) {
            log.error("Error blocking user: {}", username, e);
        }
    }

    /**
     * Unblock user and clear attempts
     */
    @CacheEvict(value = {"login-attempts", "login-blocks"}, key = "#username")
    @WriteOnly
    private void unblockUser(String username) {
        try {
            String blockKey = BLOCK_KEY_PREFIX + username;
            String attemptKey = ATTEMPT_KEY_PREFIX + username;

            hashCacheService.remove("login_blocks", blockKey);
            hashCacheService.remove("login_attempts", attemptKey);

            log.info("User {} unblocked and attempts cleared", username);
        } catch (Exception e) {
            log.error("Error unblocking user: {}", username, e);
        }
    }

    /**
     * Get remaining block time for user
     */
    @ReadOnly
    public Duration getRemainingBlockTime(String username) {
        try {
            String blockKey = BLOCK_KEY_PREFIX + username;
            Object blockTime = hashCacheService.get("login_blocks", blockKey);

            if (blockTime != null) {
                Instant blockedAt = Instant.parse(blockTime.toString());
                Instant unblockTime = blockedAt.plus(Duration.ofMinutes(blockDurationMinutes));
                Instant now = Instant.now();

                if (now.isBefore(unblockTime)) {
                    return Duration.between(now, unblockTime);
                }
            }

            return Duration.ZERO;
        } catch (Exception e) {
            log.error("Error getting remaining block time for user: {}", username, e);
            return Duration.ZERO;
        }
    }
}