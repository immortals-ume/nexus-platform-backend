package com.immortals.authapp.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.immortals.authapp.model.audit.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.envers.Audited;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(
        name = "users",
        schema = "user_auth",
        indexes = {
                @Index(name = "idx_user_username", columnList = "user_name"),
                @Index(name = "idx_user_email", columnList = "email"),
                @Index(name = "idx_user_contact_number", columnList = "contact_number"),
                @Index(name = "idx_user_active_ind", columnList = "active_ind")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_username", columnNames = "user_name"),
                @UniqueConstraint(name = "uk_user_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_user_contact_number", columnNames = "contact_number")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Audited
@EntityListeners(AuditingEntityListener.class)
@ToString(exclude = {"userAddresses", "roles"})
public class User extends Auditable<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_sequence")
    @SequenceGenerator(
            name = "user_sequence",
            sequenceName = "auth.user_sequence",
            allocationSize = 1,
            initialValue = 1
    )
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @NotBlank
    @Size(max = 50)
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Size(max = 50)
    @Column(name = "middle_name", length = 50)
    private String middleName;

    @NotBlank
    @Size(max = 50)
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @NotBlank
    @Size(min = 3, max = 16)
    @Column(name = "user_name", nullable = false, length = 16, unique = true)
    private String userName;

    @JsonIgnore
    @NotBlank(message = "Password cannot be empty")
    @Size(min = 8, max = 255)
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Email(message = "Email is not in correct format")
    @NotBlank
    @Size(max = 100)
    @Column(name = "email", nullable = false, length = 100, unique = true)
    private String email;

    @Email(message = "Email is not in correct format")
    @Size(max = 100)
    @Column(name = "alternate_email", length = 100)
    private String alternateEmail;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified ;

    @NotBlank
    @Size(max = 5)
    @Column(name = "phone_code", nullable = false, length = 5)
    private String phoneCode;

    @NotBlank
    @Size(min = 10, max = 15)
    @Pattern(regexp = "^(\\+\\d{1,4})?[6-9][0-9]{9}$", message = "Contact number invalid")
    @Column(name = "contact_number", nullable = false, length = 15, unique = true)
    private String contactNumber;

    @Size(min = 10, max = 15)
    @Pattern(regexp = "^(\\+\\d{1,4})?[6-9][0-9]{9}$", message = "Alternate contact invalid")
    @Column(name = "alternate_contact", length = 15)
    private String alternateContact;

    @Column(name = "phone_number_verified", nullable = false)
    private Boolean phoneNumberVerified = false;

    @Column(name = "login_time")
    private Instant login;

    @Column(name = "logout_time")
    private Instant logout;

    @Column(name = "account_non_expired", nullable = false)
    private Boolean accountNonExpired ;

    @Column(name = "account_non_locked", nullable = false)
    private Boolean accountNonLocked ;

    @Column(name = "account_locked", nullable = false)
    private Boolean accountLocked ;

    @Column(name = "credentials_non_expired", nullable = false)
    private Boolean credentialsNonExpired ;

    @Column(name = "active_ind", nullable = false)
    private Boolean activeInd ;

    @JsonIgnore
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAddress> userAddresses;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_role",
            schema = "user_auth",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "role_id")
    )
    private Set<Roles> roles;
}
