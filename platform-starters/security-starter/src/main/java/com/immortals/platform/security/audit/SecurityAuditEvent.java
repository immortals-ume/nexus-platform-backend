package com.immortals.platform.security.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Security audit event for logging security-related activities.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityAuditEvent {
    
    private String eventType;
    private String username;
    private String ipAddress;
    private String resource;
    private String action;
    private boolean success;
    private String failureReason;
    private String correlationId;
    private Instant timestamp;
    
    public enum EventType {
        AUTHENTICATION_SUCCESS,
        AUTHENTICATION_FAILURE,
        AUTHORIZATION_SUCCESS,
        AUTHORIZATION_FAILURE,
        RATE_LIMIT_EXCEEDED,
        TOKEN_VALIDATION_FAILURE,
        ACCESS_DENIED
    }
}
