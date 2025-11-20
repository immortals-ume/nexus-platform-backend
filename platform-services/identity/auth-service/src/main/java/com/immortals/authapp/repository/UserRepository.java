package com.immortals.authapp.repository;

import com.immortals.platform.domain.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = {"userAddresses"},type = EntityGraph.EntityGraphType.LOAD)
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select user from User user where user.activeInd = true and user.userName= :userNameOrEmailOrPhone or user.email = : userNameOrEmailOrPhone or user.contactNumber = :userNameOrEmailOrPhone")
    Optional<User> findUser(@Param("userNameOrEmailOrPhone") String userNameOrEmailOrPhone);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.userId = :id")
    Optional<User> findByIdWithRoles(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    Boolean existsByUserName(String username);

    @Lock(LockModeType.PESSIMISTIC_READ)
    Boolean existsByEmail(String email);
}