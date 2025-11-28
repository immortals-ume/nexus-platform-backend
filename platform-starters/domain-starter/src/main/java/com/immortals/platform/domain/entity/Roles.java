package com.immortals.platform.domain.entity;

import com.immortals.platform.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.util.Set;

@Getter
@Setter
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
@Audited
public class Roles extends BaseEntity {

    private static final long serialVersionUID = 1L;

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
}
