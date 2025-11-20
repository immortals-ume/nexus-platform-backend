package com.immortals.authapp.service;

import com.immortals.platform.domain.entity.User;
import com.immortals.authapp.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service wrapper for database operations with resilience patterns.
 * Implements retry logic and circuit breaker for database operations.
 * 
 * Requirements:
 * - 5.2: Add retry logic for database operations
 * - 5.1: Circuit breaker for database operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResilientDatabaseService {

    private final UserRepository userRepository;

    /**
     * Find user with retry logic for transient database failures.
     * 
     * @param userNameOrEmailOrPhone username, email, or phone number
     * @return Optional containing the user if found
     */
    @Retry(name = "databaseOperations")
    @CircuitBreaker(name = "databaseOperations", fallbackMethod = "findUserFallback")
    @Transactional(readOnly = true)
    public Optional<User> findUserWithResilience(String userNameOrEmailOrPhone) {
        log.debug("Finding user: {}", userNameOrEmailOrPhone);
        try {
            return userRepository.findUser(userNameOrEmailOrPhone);
        } catch (Exception e) {
            log.error("Database error while finding user: {}", userNameOrEmailOrPhone, e);
            throw e;
        }
    }

    /**
     * Find user by ID with roles, with retry logic.
     * 
     * @param userId the user ID
     * @return Optional containing the user with roles if found
     */
    @Retry(name = "databaseOperations")
    @CircuitBreaker(name = "databaseOperations", fallbackMethod = "findByIdWithRolesFallback")
    @Transactional(readOnly = true)
    public Optional<User> findByIdWithRolesResilient(Long userId) {
        log.debug("Finding user by ID with roles: {}", userId);
        try {
            return userRepository.findByIdWithRoles(userId);
        } catch (Exception e) {
            log.error("Database error while finding user by ID: {}", userId, e);
            throw e;
        }
    }

    /**
     * Check if username exists with retry logic.
     * 
     * @param username the username to check
     * @return true if username exists
     */
    @Retry(name = "databaseOperations")
    @CircuitBreaker(name = "databaseOperations", fallbackMethod = "existsByUsernameFallback")
    @Transactional(readOnly = true)
    public Boolean existsByUsernameResilient(String username) {
        log.debug("Checking if username exists: {}", username);
        try {
            return userRepository.existsByUserName(username);
        } catch (Exception e) {
            log.error("Database error while checking username existence: {}", username, e);
            throw e;
        }
    }

    /**
     * Check if email exists with retry logic.
     * 
     * @param email the email to check
     * @return true if email exists
     */
    @Retry(name = "databaseOperations")
    @CircuitBreaker(name = "databaseOperations", fallbackMethod = "existsByEmailFallback")
    @Transactional(readOnly = true)
    public Boolean existsByEmailResilient(String email) {
        log.debug("Checking if email exists: {}", email);
        try {
            return userRepository.existsByEmail(email);
        } catch (Exception e) {
            log.error("Database error while checking email existence: {}", email, e);
            throw e;
        }
    }

    /**
     * Save user with retry logic.
     * 
     * @param user the user to save
     * @return the saved user
     */
    @Retry(name = "databaseOperations")
    @CircuitBreaker(name = "databaseOperations", fallbackMethod = "saveUserFallback")
    @Transactional
    public User saveUserResilient(User user) {
        log.debug("Saving user: {}", user.getUserName());
        try {
            return userRepository.save(user);
        } catch (Exception e) {
            log.error("Database error while saving user: {}", user.getUserName(), e);
            throw e;
        }
    }

    // Fallback methods

    private Optional<User> findUserFallback(String userNameOrEmailOrPhone, Exception e) {
        log.error("Circuit breaker activated for findUser. Returning empty. Input: {}, Error: {}", 
                userNameOrEmailOrPhone, e.getMessage());
        return Optional.empty();
    }

    private Optional<User> findByIdWithRolesFallback(Long userId, Exception e) {
        log.error("Circuit breaker activated for findByIdWithRoles. Returning empty. UserId: {}, Error: {}", 
                userId, e.getMessage());
        return Optional.empty();
    }

    private Boolean existsByUsernameFallback(String username, Exception e) {
        log.error("Circuit breaker activated for existsByUsername. Returning false. Username: {}, Error: {}", 
                username, e.getMessage());
        // Conservative fallback: assume username doesn't exist to allow operation to proceed
        return false;
    }

    private Boolean existsByEmailFallback(String email, Exception e) {
        log.error("Circuit breaker activated for existsByEmail. Returning false. Email: {}, Error: {}", 
                email, e.getMessage());
        // Conservative fallback: assume email doesn't exist to allow operation to proceed
        return false;
    }

    private User saveUserFallback(User user, Exception e) {
        log.error("Circuit breaker activated for saveUser. Cannot save user: {}, Error: {}", 
                user.getUserName(), e.getMessage());
        throw new RuntimeException("Unable to save user due to database unavailability", e);
    }
}
