package com.immortals.platform.domain.auth.entity;

import com.immortals.platform.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import org.hibernate.envers.Audited;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

@Getter
@Entity
@Table(
        name = "role",
        schema = "user_auth",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_role_name", columnNames = "role_name")
        },
        indexes = {
                @Index(name = "idx_role_name", columnList = "role_name")
        }
)
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Audited
@Setter
public class Roles extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "role_id", nullable = false, unique = true, updatable = false)
    private Long roleId;

    @Column(name = "role_name", nullable = false, length = 20)
    @NotNull
    private String roleName;

    @Column(name = "description", length = 200)
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permission",
            schema = "user_auth",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private transient Set<Permissions> permissions;

    @ManyToMany(mappedBy = "roles")
    private transient Set<User> users;

    @Column(name = "active_ind", nullable = false)
    @NotNull
    private Boolean activeInd;

    @PrePersist
    public void generateRoleId() {
        if (this.roleId == null) {
            this.roleId = this.getId();
        }
    }
}
