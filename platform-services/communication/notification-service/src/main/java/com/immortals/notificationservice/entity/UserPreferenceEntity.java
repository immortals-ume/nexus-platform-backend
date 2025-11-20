package com.immortals.notificationservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity for user notification preferences
 */
@Entity
@Table(name = "user_preferences", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;
    
    @Column(name = "enabled_channels")
    private String enabledChannels; // Comma-separated: EMAIL,SMS,WHATSAPP
    
    @Column(name = "opted_out_categories")
    private String optedOutCategories; // Comma-separated
    
    @Column(name = "marketing_enabled")
    private boolean marketingEnabled;
    
    @Column(name = "transactional_enabled")
    private boolean transactionalEnabled;
    
    @Column(name = "timezone")
    private String timezone;
    
    @Column(name = "quiet_hours_start")
    private String quietHoursStart;
    
    @Column(name = "quiet_hours_end")
    private String quietHoursEnd;
    
    @Column(name = "quiet_hours_enabled")
    private boolean quietHoursEnabled;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
