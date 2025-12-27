package com.immortals.platform.domain.notifications.entity;

import com.immortals.platform.domain.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Type;

import java.io.Serial;
import java.util.Map;

/**
 * Entity for provider configuration
 * Extends BaseEntity for audit fields and soft delete support
 */
@Entity
@Table(name = "provider_configs", indexes = {
        @Index(name = "idx_provider_id", columnList = "provider_id", unique = true),
        @Index(name = "idx_channel", columnList = "channel"),
        @Index(name = "idx_enabled", columnList = "enabled"),
        @Index(name = "idx_priority", columnList = "priority")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderConfig extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "provider_id", nullable = false, unique = true, length = 50)
    private String providerId;

    @Column(name = "provider_name", nullable = false)
    private String providerName;

    @Column(name = "channel", nullable = false, length = 50)
    private String channel;

    @Type(JsonBinaryType.class)
    @Column(name = "supported_countries", columnDefinition = "jsonb")
    private transient  String[] supportedCountries;

    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 100;

    @Column(name = "enabled")
    @Builder.Default
    private Boolean enabled = true;

    @Type(JsonBinaryType.class)
    @Column(name = "credentials", nullable = false, columnDefinition = "jsonb")
    private transient Map<String, String> credentials;

    @Type(JsonBinaryType.class)
    @Column(name = "configuration", columnDefinition = "jsonb")
    private transient Map<String, Object> configuration;

    @Type(JsonBinaryType.class)
    @Column(name = "rate_limit_config", columnDefinition = "jsonb")
    private transient Map<String, Object> rateLimitConfig;

    public boolean supportsCountry(String countryCode) {
        if (supportedCountries == null) {
            return Boolean.FALSE;
        }
        for (String country : supportedCountries) {
            if ("*".equals(country) || country.equalsIgnoreCase(countryCode)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    public boolean isAvailable() {
        return enabled && !isDeleted();
    }
}
