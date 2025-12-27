package com.immortals.platform.domain.notifications.entity;

import com.immortals.platform.domain.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity to track notification processing for idempotency
 */
@Entity
@Table(
        name = "notification_logs",
        indexes = {
                @Index(name = "idx_event_id", columnList = "event_id", unique = true),
                @Index(name = "idx_correlation_id", columnList = "correlation_id"),
                @Index(name = "idx_country_code", columnList = "country_code"),
                @Index(name = "idx_delivery_status", columnList = "delivery_status"),
                @Index(name = "idx_provider_id", columnList = "provider_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class NotificationLog extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "notification_log_id", nullable = false, unique = true)
    private Long notificationLogId;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "notification_type", nullable = false)
    private String notificationType;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "locale")
    private String locale;

    @Column(name = "subject")
    private String subject;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "html_content", columnDefinition = "TEXT")
    private String htmlContent;

    @Column(name = "template_code")
    private String templateCode;

    @Type(JsonBinaryType.class)
    @Column(name = "template_variables", columnDefinition = "jsonb")
    private transient Map<String, Object> templateVariables;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "delivery_status")
    private String deliveryStatus;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "retry_count", nullable = false, columnDefinition = "int default 0")
    private Integer retryCount;

    @Column(name = "max_retries", nullable = false, columnDefinition = "int default 3")
    private Integer maxRetries;

    @Type(JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private transient Map<String, String> metadata;

}
