package com.immortals.authapp.service;

public interface TokenBlacklistService {
    void blacklistToken(String token, long ttlInMillis);

    boolean isTokenBlacklisted(String token);
}
