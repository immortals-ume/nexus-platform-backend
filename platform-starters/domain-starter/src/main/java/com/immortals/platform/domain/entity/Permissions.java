package com.immortals.platform.domain.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.immortals.platform.domain.audit.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.envers.Audited;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
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
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Audited
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Permissions extends Auditable<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id", nullable = false, updatable = false)
    private Long permissionId;

    @Column(name = "permission_name", nullable = false, length = 50)
    @NotNull
    private String permissionName;

    @Column(name = "active_ind", nullable = false)
    @NotNull
    private Boolean activeInd;

    @ManyToMany(mappedBy = "permissions")
    @JsonIgnore
    private Set<Roles> roles;
}
