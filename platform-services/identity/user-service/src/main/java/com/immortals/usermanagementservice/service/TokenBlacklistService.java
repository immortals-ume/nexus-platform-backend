package com.immortals.usermanagementservice.service;

public interface TokenBlacklistService {
    void blacklistToken(String token, long ttlInMillis);

    boolean isTokenBlacklisted(String token);
}
