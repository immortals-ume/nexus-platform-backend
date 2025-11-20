package com.immortals.authapp.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event payload for failed authentication events
 */
@Data
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
