package com.immortals.usermanagementservice.model.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable<U> implements java.io.Serializable{
    @CreatedBy
    @Column(name = "created_by", updatable = false, nullable = false)
    protected U createdBy;

    @CreatedDate
    @Column(name = "created_date", updatable = false, nullable = false)
    protected LocalDateTime createdDate;

    @Column(name = "updated_by")
    protected U updatedBy;

    @Column(name = "updated_date")
    protected LocalDateTime updatedDate;

    @Column(name = "deleted_by")
    protected U deletedBy;

    @Column(name = "deleted_date")
    protected LocalDateTime deletedDate;
}
