package com.immortals.usermanagementservice.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.immortals.usermanagementservice.model.audit.Auditable;
import com.immortals.usermanagementservice.model.enums.AddressStatus;
import com.immortals.usermanagementservice.model.enums.AddressType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.envers.Audited;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(
        name = "user_address",
        schema = "user_auth",
        indexes = {
                @Index(name = "idx_user_address_user_id", columnList = "user_id"),
                @Index(name = "idx_user_address_country_id", columnList = "country_id"),
                @Index(name = "idx_user_address_state_id", columnList = "state_id"),
                @Index(name = "idx_user_address_city_id", columnList = "city_id"),
                @Index(name = "idx_user_address_pincode", columnList = "pincode")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_address_uuid", columnNames = "address_uuid")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Audited
@EntityListeners(AuditingEntityListener.class)
public class UserAddress extends Auditable<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_address_sequence")
    @SequenceGenerator(
            name = "user_address_sequence",
            sequenceName = "auth.user_address_sequence",
            allocationSize = 1,
            initialValue = 1
    )
    @Column(name = "user_address_id", nullable = false, updatable = false)
    private Long userAddressId;

    @Column(name = "address_uuid", nullable = false, unique = true, updatable = false, length = 36)
    private String addressUuid;

    @Size(max = 50)
    @Column(name = "label", length = 50)
    private String label;

    @NotBlank
    @Size(max = 255)
    @Column(name = "address_line1", nullable = false, length = 255)
    private String addressLine1;

    @Size(max = 255)
    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Size(max = 255)
    @Column(name = "landmark", length = 255)
    private String landmark;

    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be exactly 6 digits")
    @Column(name = "pincode", nullable = false, length = 6)
    private String pincode;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false, length = 20)
    private AddressType addressType;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AddressStatus status;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified;

    @Column(name = "is_po_box", nullable = false)
    private Boolean isPoBox;

    @Size(max = 50)
    @Column(name = "timezone", length = 50)
    private String timezone;

    @Size(max = 5)
    @Column(name = "language_code", length = 5)
    private String languageCode;

    @Column(name = "formatted_address", columnDefinition = "TEXT")
    private String formattedAddress;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "state_id", nullable = false)
    private States states;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;
}
