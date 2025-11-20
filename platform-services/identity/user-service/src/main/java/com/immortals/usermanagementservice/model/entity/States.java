package com.immortals.usermanagementservice.model.entity;


import com.immortals.usermanagementservice.model.audit.Auditable;
import jakarta.persistence.*;
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
        name = "states",
        schema = "user_auth",
        indexes = {
                @Index(name = "idx_states_country_id", columnList = "country_id"),
                @Index(name = "idx_states_name", columnList = "state_name")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_states_name_country", columnNames = {"state_name", "country_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Audited
@EntityListeners(AuditingEntityListener.class)
public class States extends Auditable<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "state_name", nullable = false, length = 100)
    private String name;

    @Column(name = "state_code", nullable = false, length = 10)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    @OneToMany(mappedBy = "states")
    private Set<City> cities;

    @Column(name = "active_ind", nullable = false)
    private Boolean activeInd;
}

