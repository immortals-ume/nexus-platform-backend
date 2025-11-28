package com.immortals.platform.domain.entity;

import com.immortals.platform.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(
        name = "cities",
        schema = "user_auth",
        indexes = {
                @Index(name = "idx_cities_name", columnList = "city_name"),
                @Index(name = "idx_cities_state_id", columnList = "state_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cities_name_state", columnNames = {"city_name", "state_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Audited
public class City extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "city_name", nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "state_id", nullable = false)
    private States states;

    @Column(name = "active_ind", nullable = false)
    private Boolean activeInd;
}
