package com.immortals.notification.service.repository;

import com.immortals.platform.domain.notifications.SystemConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemConfigurationRepository extends JpaRepository<SystemConfiguration, Long> {

    Optional<SystemConfiguration> findByConfigKeyAndIsActiveTrue(String configKey);

    List<SystemConfiguration> findByCategoryAndIsActiveTrue(String category);

    List<SystemConfiguration> findByIsActiveTrue();

    @Query("SELECT sc FROM SystemConfiguration sc WHERE sc.configKey LIKE :keyPattern AND sc.isActive = true")
    List<SystemConfiguration> findByConfigKeyPatternAndIsActiveTrue(String keyPattern);

    boolean existsByConfigKey(String configKey);
}
