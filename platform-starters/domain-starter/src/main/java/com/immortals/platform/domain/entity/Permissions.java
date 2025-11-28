package com.immortals.platform.domain.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.immortals.platform.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.util.Set;

@Entity
@Table(
        name = "permission",
        schema = "user_auth",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_permission_name", columnNames = "permission_name")
        },
        indexes = {
                @Index(name = "idx_permission_name", columnList = "permission_name")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Audited
public class Permissions extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "permission_name", nullable = false, length = 50)
    @NotNull
    private String permissionName;

    @Column(name = "active_ind", nullable = false)
    @NotNull
    private Boolean activeInd;

    @ManyToMany(mappedBy = "permissions")
    @JsonIgnore
    private transient Set<Roles> roles;
}
