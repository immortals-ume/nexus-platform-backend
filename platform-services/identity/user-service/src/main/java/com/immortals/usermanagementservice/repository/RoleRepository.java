package com.immortals.usermanagementservice.repository;


import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Roles, Long> {

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT u FROM Roles u JOIN FETCH u.permissions WHERE u.roleId = :id")
    Optional<Roles> findByIdWithPermissions(@Param("id") Long id);
}
