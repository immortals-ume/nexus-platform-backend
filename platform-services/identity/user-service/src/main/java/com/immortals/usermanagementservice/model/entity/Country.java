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
        name = "countries",
        schema = "user_auth",
        indexes = {
                @Index(name = "idx_countries_name", columnList = "country_name"),
                @Index(name = "idx_countries_code", columnList = "country_code")
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Audited
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Country extends Auditable<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long countryId;

    @Column(name = "country_name", nullable = false, unique = true, length = 100)
    private String countryName;

    @Column(name = "country_code", nullable = false, unique = true, length = 5)
    private String countryCode;

    @OneToMany(mappedBy = "country")
    private Set<States> states;

    @Column(name = "active_ind", nullable = false)
    private Boolean activeInd;
}
