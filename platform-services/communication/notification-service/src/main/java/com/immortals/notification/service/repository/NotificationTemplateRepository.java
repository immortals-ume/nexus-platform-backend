package com.immortals.notification.service.repository;

import com.immortals.platform.domain.notifications.entity.NotificationTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for NotificationTemplate entities
 * Provides CRUD operations and custom queries for template management
 */
@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplateEntity, Long> {
    
    /**
     * Find a template by code and locale
     */
    Optional<NotificationTemplateEntity> findByTemplateCodeAndLocale(String templateCode, String locale);
    
    /**
     * Find all templates by code (all locales)
     */
    List<NotificationTemplateEntity> findByTemplateCode(String templateCode);
    
    /**
     * Find all active templates
     */
    @Query("SELECT t FROM NotificationTemplateEntity t WHERE t.active = true AND t.deleted = false")
    List<NotificationTemplateEntity> findAllActive();
    
    /**
     * Find all templates by channel
     */
    List<NotificationTemplateEntity> findByChannel(String channel);
    
    /**
     * Find active template by code and locale
     */
    @Query("SELECT t FROM NotificationTemplateEntity t WHERE t.templateCode = :templateCode " +
           "AND t.locale = :locale AND t.active = true AND t.deleted = false")
    Optional<NotificationTemplateEntity> findActiveByTemplateCodeAndLocale(
        @Param("templateCode") String templateCode, 
        @Param("locale") String locale
    );
    
    /**
     * Check if template exists by code and locale
     */
    boolean existsByTemplateCodeAndLocale(String templateCode, String locale);
}
