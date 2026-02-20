package com.immortals.platform.domain.auth.event;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.time.Instant;

/**
 * Event published when a token is blacklisted
 */
@Getter
@Builder
public class TokenBlacklistedEvent {
    private String tokenId;
    private String username;
    private Instant blacklistedAt;
    private String reason;
    private Long ttlInMillis;
    private String ipAddress;
}