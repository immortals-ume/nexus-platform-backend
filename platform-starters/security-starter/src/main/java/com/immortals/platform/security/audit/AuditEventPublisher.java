package com.immortals.platform.security.audit;

import com.immortals.platform.security.config.SecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Publisher for security audit events.
 * Logs security events with correlation IDs for tracing.
 */
@Slf4j
@Component
public class AuditEventPublisher {

    private final SecurityProperties securityProperties;

    public AuditEventPublisher(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    /**
     * Publish authentication success event.
     */
    public void publishAuthenticationSuccess(String username, String ipAddress, String correlationId) {
        if (!securityProperties.getAudit().getLogAuthenticationAttempts()) {
            return;
        }

        SecurityAuditEvent event = SecurityAuditEvent.builder()
                .eventType(SecurityAuditEvent.EventType.AUTHENTICATION_SUCCESS.name())
                .username(username)
                .ipAddress(ipAddress)
                .success(true)
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .build();

        logAuditEvent(event);
    }

    /**
     * Publish authentication failure event.
     */
    public void publishAuthenticationFailure(String username, String ipAddress, String reason, String correlationId) {
        if (!securityProperties.getAudit().getLogAuthenticationAttempts()) {
            return;
        }

        SecurityAuditEvent event = SecurityAuditEvent.builder()
                .eventType(SecurityAuditEvent.EventType.AUTHENTICATION_FAILURE.name())
                .username(username)
                .ipAddress(ipAddress)
                .success(false)
                .failureReason(reason)
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .build();

        logAuditEvent(event);
    }

    /**
     * Publish authorization failure event.
     */
    public void publishAuthorizationFailure(String username, String resource, String action, String reason, String correlationId) {
        if (!securityProperties.getAudit().getLogAuthorizationFailures()) {
            return;
        }

        SecurityAuditEvent event = SecurityAuditEvent.builder()
                .eventType(SecurityAuditEvent.EventType.AUTHORIZATION_FAILURE.name())
                .username(username)
                .resource(resource)
                .action(action)
                .success(false)
                .failureReason(reason)
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .build();

        logAuditEvent(event);
    }

    /**
     * Publish rate limit violation event.
     */
    public void publishRateLimitViolation(String identifier, String ipAddress, String correlationId) {
        if (!securityProperties.getAudit().getLogRateLimitViolations()) {
            return;
        }

        SecurityAuditEvent event = SecurityAuditEvent.builder()
                .eventType(SecurityAuditEvent.EventType.RATE_LIMIT_EXCEEDED.name())
                .username(identifier)
                .ipAddress(ipAddress)
                .success(false)
                .failureReason("Rate limit exceeded")
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .build();

        logAuditEvent(event);
    }

    /**
     * Publish token validation failure event.
     */
    public void publishTokenValidationFailure(String token, String reason, String correlationId) {
        SecurityAuditEvent event = SecurityAuditEvent.builder()
                .eventType(SecurityAuditEvent.EventType.TOKEN_VALIDATION_FAILURE.name())
                .success(false)
                .failureReason(reason)
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .build();

        logAuditEvent(event);
    }

    /**
     * Log audit event in structured format.
     */
    private void logAuditEvent(SecurityAuditEvent event) {
        log.info("SECURITY_AUDIT: eventType={}, username={}, ipAddress={}, resource={}, action={}, success={}, reason={}, correlationId={}, timestamp={}",
                event.getEventType(),
                event.getUsername(),
                event.getIpAddress(),
                event.getResource(),
                event.getAction(),
                event.isSuccess(),
                event.getFailureReason(),
                event.getCorrelationId(),
                event.getTimestamp());
    }
}
