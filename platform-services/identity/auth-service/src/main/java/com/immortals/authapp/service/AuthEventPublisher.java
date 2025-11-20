package com.immortals.authapp.service;

import com.immortals.authapp.event.UserAuthenticatedEvent;
import com.immortals.authapp.event.UserAuthenticationFailedEvent;
import com.immortals.authapp.event.UserRegisteredEvent;
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

    @Value("${spring.application.name:auth-service}")
    private String serviceName;

    @Value("${messaging.topics.user-registered:user-events}")
    private String userRegisteredTopic;

    @Value("${messaging.topics.user-authenticated:auth-events}")
    private String userAuthenticatedTopic;

    @Value("${messaging.topics.user-authentication-failed:auth-events}")
    private String userAuthenticationFailedTopic;

    /**
     * Publish UserRegistered event
     */
    public void publishUserRegistered(UserRegisteredEvent payload) {
        try {
            DomainEvent<UserRegisteredEvent> event = DomainEvent.<UserRegisteredEvent>builder()
                    .eventType("UserRegistered")
                    .aggregateId(payload.getUserId())
                    .aggregateType("User")
                    .payload(payload)
                    .correlationId(getCorrelationId())
                    .source(serviceName)
                    .build();

            eventPublisher.publish(userRegisteredTopic, payload.getUserId(), event);
            log.info("Published UserRegistered event for user: {}", payload.getUsername());
        } catch (Exception e) {
            log.error("Failed to publish UserRegistered event for user: {}", payload.getUsername(), e);
        }
    }

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
