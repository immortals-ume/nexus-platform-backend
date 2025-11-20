package com.immortals.authapp.service;

import com.immortals.authapp.service.cache.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.immortals.authapp.constants.AuthAppConstant.BLOCK_DURATION;
import static com.immortals.authapp.constants.AuthAppConstant.MAX_ATTEMPTS;
import static com.immortals.authapp.constants.CacheConstants.ATTEMPT_KEY_PREFIX;
import static com.immortals.authapp.constants.CacheConstants.BLOCK_KEY_PREFIX;

@Service
@RequiredArgsConstructor
public class LoginAttemptService implements LoginAttempt {

    private final CacheService<String, String, String> cacheService;
    String lockingKey = UUID.randomUUID()
            .toString();

    @Override
    public void loginSucceeded(String username) {
        cacheService.remove(ATTEMPT_KEY_PREFIX, username, lockingKey);
        cacheService.remove(BLOCK_KEY_PREFIX, username, lockingKey);
    }

    @Override
    public void loginFailed(String username) {

        Long attempts = cacheService.increment(ATTEMPT_KEY_PREFIX, username, lockingKey, BLOCK_DURATION);

        if (attempts >= MAX_ATTEMPTS) {
            cacheService.put(BLOCK_KEY_PREFIX, username, "BLOCKED", BLOCK_DURATION, lockingKey);
            cacheService.remove(ATTEMPT_KEY_PREFIX, username, lockingKey);
        }
    }

    @Override
    public boolean isBlocked(String username) {
        return cacheService.containsKey(BLOCK_KEY_PREFIX, username, username);
    }
}
