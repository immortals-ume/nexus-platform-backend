package com.immortals.platform.domain.auth.event;

import lombok.*;

import java.time.Instant;

/**
 * Event payload for failed authentication events
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthenticationFailedEvent {
    private String username;
    private Instant failedAt;
    private String reason;
    private String ipAddress;
    private String userAgent;
}
