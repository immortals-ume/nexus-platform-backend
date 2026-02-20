package com.immortals.platform.domain.auth.event;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.time.Instant;

/**
 * Event published when a user logs out
 */
@Getter
@Builder
public class UserLoggedOutEvent {
    private String username;
    private Instant loggedOutAt;
    private String ipAddress;
    private String userAgent;
    private String sessionId;
    private boolean forced; // true if logout was forced (e.g., admin action)
}