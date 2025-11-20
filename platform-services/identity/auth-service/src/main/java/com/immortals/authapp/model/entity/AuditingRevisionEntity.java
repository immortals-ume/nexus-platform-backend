package com.immortals.authapp.model.entity;

import com.immortals.authapp.audit.AuditingRevisionListener;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

@Entity
@Table(
        name = "revinfo",
        schema = "user_audit",
        indexes = {
                @Index(name = "idx_revinfo_timestamp", columnList = "timestamp")
        }
)
@Getter
@Setter
@RevisionEntity(AuditingRevisionListener.class)
public class AuditingRevisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "revinfo_seq_gen")
    @SequenceGenerator(
            name = "revinfo_seq_gen",
            sequenceName = "user_audit.revinfo_seq"
    )
    @RevisionNumber
    private Long id;

    @RevisionTimestamp
    @Column(nullable = false)
    private Long timestamp;

    @Column(name = "username", length = 100)
    private String username;
}
