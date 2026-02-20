package com.immortals.platform.domain.auth.event;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;

/**
 * Event published when a user account is locked due to failed login attempts
 */
@Getter
@Builder
public class UserAccountLockedEvent {
    private String username;
    private Instant lockedAt;
    private int failedAttempts;
    private Duration lockDuration;
    private String ipAddress;
    private String userAgent;
    private String reason;
}