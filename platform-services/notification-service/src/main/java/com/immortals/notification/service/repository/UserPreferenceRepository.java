package com.immortals.notification.service.repository;

import com.immortals.platform.domain.notifications.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for UserPreference entities
 */
@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
    
    /**
     * Find user preference by user ID
     */
    Optional<UserPreference> findByUserId(String userId);
    
    /**
     * Check if user preference exists
     */
    boolean existsByUserId(String userId);
}
