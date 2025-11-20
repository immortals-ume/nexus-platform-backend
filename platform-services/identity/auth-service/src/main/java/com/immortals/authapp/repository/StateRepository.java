package com.immortals.authapp.repository;

import com.immortals.platform.domain.entity.States;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StateRepository extends JpaRepository<States, Long> {

    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<States> findByNameAndActiveIndTrue(String stateName);
}
