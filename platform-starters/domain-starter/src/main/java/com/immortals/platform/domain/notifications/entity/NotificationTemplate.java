package com.immortals.platform.domain.notifications.entity;

import com.immortals.platform.domain.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.io.Serial;
import java.util.Map;

/**
 * Entity for notification templates with multi-language support
 * Extends BaseEntity for audit fields and soft delete support
 */
@Entity
@Table(name = "notification_templates", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_template_code_locale", columnNames = {"template_code", "locale"})
    },
    indexes = {
        @Index(name = "idx_template_code", columnList = "template_code"),
        @Index(name = "idx_active", columnList = "active"),
        @Index(name = "idx_channel", columnList = "channel"),
        @Index(name = "idx_locale", columnList = "locale")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate extends BaseEntity {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    @Column(name = "template_code", nullable = false, length = 100)
    private String templateCode;
    
    @Column(name = "template_name", nullable = false)
    private String templateName;
    
    @Column(name = "channel", nullable = false, length = 50)
    private String channel;
    
    @Column(name = "locale", nullable = false, length = 10)
    @Builder.Default
    private String locale = "en_US";
    
    @Column(name = "subject", length = 500)
    private String subject;
    
    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;
    
    @Column(name = "html_template", columnDefinition = "TEXT")
    private String htmlTemplate;
    
    @Column(name = "engine", nullable = false, length = 20)
    @Builder.Default
    private String engine = "PLAIN_TEXT";
    
    @Column(name = "active")
    private Boolean active;


    @Column(name = "deleted")
    private Boolean deleted;


    @Type(JsonBinaryType.class)
    @Column(name = "default_variables", columnDefinition = "jsonb")
    private transient Map<String, String> defaultVariables;

    /**
     * Check if template is active and usable
     */
    public boolean isUsable() {
        return Boolean.TRUE.equals(active) && !isDeleted();
    }
}
