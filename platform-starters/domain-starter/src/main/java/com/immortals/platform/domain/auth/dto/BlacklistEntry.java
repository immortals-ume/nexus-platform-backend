package com.immortals.platform.domain.auth.dto;

import java.time.Instant;

/**
 * Blacklist entry data class
 */
public record BlacklistEntry(String token, Instant blacklistedAt, long ttlInMillis) {
    public boolean isExpired() {
        return Instant.now()
                .isAfter(blacklistedAt.plusMillis(ttlInMillis));
    }
}