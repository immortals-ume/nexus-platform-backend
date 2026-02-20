package com.immortals.authapp.service.impl;

import com.immortals.platform.domain.auth.event.*;
import com.immortals.platform.messaging.event.DomainEvent;
import com.immortals.platform.messaging.publisher.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for publishing authentication-related domain events
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthEventPublisher {

    private final EventPublisher eventPublisher;

    @Value("${spring.application.name:auth}")
    private String serviceName;

    @Value("${messaging.topics.user-authenticated:auth-events}")
    private String userAuthenticatedTopic;

    @Value("${messaging.topics.user-authentication-failed:auth-events}")
    private String userAuthenticationFailedTopic;

    @Value("${messaging.topics.user-logged-out:auth-events}")
    private String userLoggedOutTopic;

    @Value("${messaging.topics.token-blacklisted:auth-events}")
    private String tokenBlacklistedTopic;

    @Value("${messaging.topics.user-account-locked:security-events}")
    private String userAccountLockedTopic;

    /**
     * Publish UserAuthenticated event
     */
    public void publishUserAuthenticated(UserAuthenticatedEvent payload, String userId) {
        try {
            DomainEvent<UserAuthenticatedEvent> event = DomainEvent.<UserAuthenticatedEvent>builder()
                    .eventType("UserAuthenticated")
                    .aggregateId(userId)
                    .aggregateType("User")
                    .payload(payload)
                    .correlationId(getCorrelationId())
                    .source(serviceName)
                    .build();

            eventPublisher.publish(userAuthenticatedTopic, userId, event);
            log.info("Published UserAuthenticated event for user: {}", payload.getUsername());
        } catch (Exception e) {
            log.error("Failed to publish UserAuthenticated event for user: {}", payload.getUsername(), e);
        }
    }

    /**
     * Publish UserAuthenticationFailed event
     */
    public void publishUserAuthenticationFailed(UserAuthenticationFailedEvent payload) {
        try {
            DomainEvent<UserAuthenticationFailedEvent> event = DomainEvent.<UserAuthenticationFailedEvent>builder()
                    .eventType("UserAuthenticationFailed")
                    .aggregateId(payload.getUsername())
                    .aggregateType("User")
                    .payload(payload)
                    .correlationId(getCorrelationId())
                    .source(serviceName)
                    .build();

            eventPublisher.publish(userAuthenticationFailedTopic, payload.getUsername(), event);
            log.info("Published UserAuthenticationFailed event for user: {}", payload.getUsername());
        } catch (Exception e) {
            log.error("Failed to publish UserAuthenticationFailed event for user: {}", payload.getUsername(), e);
        }
    }

    /**
     * Publish UserLoggedOut event
     */
    public void publishUserLoggedOut(UserLoggedOutEvent payload) {
        try {
            DomainEvent<UserLoggedOutEvent> event = DomainEvent.<UserLoggedOutEvent>builder()
                    .eventType("UserLoggedOut")
                    .aggregateId(payload.getUsername())
                    .aggregateType("User")
                    .payload(payload)
                    .correlationId(getCorrelationId())
                    .source(serviceName)
                    .build();

            eventPublisher.publish(userLoggedOutTopic, payload.getUsername(), event);
            log.info("Published UserLoggedOut event for user: {}", payload.getUsername());
        } catch (Exception e) {
            log.error("Failed to publish UserLoggedOut event for user: {}", payload.getUsername(), e);
        }
    }

    /**
     * Publish TokenBlacklisted event
     */
    public void publishTokenBlacklisted(TokenBlacklistedEvent payload) {
        try {
            DomainEvent<TokenBlacklistedEvent> event = DomainEvent.<TokenBlacklistedEvent>builder()
                    .eventType("TokenBlacklisted")
                    .aggregateId(payload.getTokenId())
                    .aggregateType("Token")
                    .payload(payload)
                    .correlationId(getCorrelationId())
                    .source(serviceName)
                    .build();

            eventPublisher.publish(tokenBlacklistedTopic, payload.getTokenId(), event);
            log.info("Published TokenBlacklisted event for token: {} (user: {})", 
                    payload.getTokenId(), payload.getUsername());
        } catch (Exception e) {
            log.error("Failed to publish TokenBlacklisted event for token: {} (user: {})", 
                    payload.getTokenId(), payload.getUsername(), e);
        }
    }

    /**
     * Publish UserAccountLocked event
     */
    public void publishUserAccountLocked(UserAccountLockedEvent payload) {
        try {
            DomainEvent<UserAccountLockedEvent> event = DomainEvent.<UserAccountLockedEvent>builder()
                    .eventType("UserAccountLocked")
                    .aggregateId(payload.getUsername())
                    .aggregateType("User")
                    .payload(payload)
                    .correlationId(getCorrelationId())
                    .source(serviceName)
                    .build();

            eventPublisher.publish(userAccountLockedTopic, payload.getUsername(), event);
            log.warn("Published UserAccountLocked event for user: {} after {} failed attempts", 
                    payload.getUsername(), payload.getFailedAttempts());
        } catch (Exception e) {
            log.error("Failed to publish UserAccountLocked event for user: {}", 
                    payload.getUsername(), e);
        }
    }

    /**
     * Get correlation ID from MDC (set by observability-starter)
     */
    private String getCorrelationId() {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isEmpty()) {
            log.debug("No correlation ID found in MDC, event will be published without one");
        }
        return correlationId;
    }
}
