package com.immortals.platform.domain.auth.event;

import lombok.*;

import java.time.Instant;

/**
 * Event payload for successful authentication events
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthenticatedEvent {
    private String username;
    private Instant authenticatedAt;
    private String ipAddress;
    private String userAgent;
    private boolean rememberMe;
}
