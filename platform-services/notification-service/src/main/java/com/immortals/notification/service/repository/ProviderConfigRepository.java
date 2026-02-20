package com.immortals.notification.service.repository;

import com.immortals.platform.domain.notifications.entity.ProviderConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ProviderConfig entity
 */
@Repository
public interface ProviderConfigRepository extends JpaRepository<ProviderConfig, Long> {
    
    /**
     * Find provider configuration by provider ID
     */
    Optional<ProviderConfig> findByProviderId(String providerId);
    
    /**
     * Find all enabled providers for a specific channel
     */
    @Query("SELECT p FROM ProviderConfig p WHERE p.channel = :channel AND p.enabled = true AND p.deleted = false")
    List<ProviderConfig> findEnabledByChannel(String channel);
    
    /**
     * Find all enabled providers
     */
    @Query("SELECT p FROM ProviderConfig p WHERE p.enabled = true AND p.deleted = false ORDER BY p.priority ASC")
    List<ProviderConfig> findAllEnabled();
    
    /**
     * Check if provider exists and is enabled
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM ProviderConfig p WHERE p.providerId = :providerId AND p.enabled = true AND p.deleted = false")
    boolean existsByProviderIdAndEnabled(String providerId);
}
