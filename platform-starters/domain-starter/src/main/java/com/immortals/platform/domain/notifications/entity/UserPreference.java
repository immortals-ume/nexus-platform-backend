
package com.immortals.platform.domain.notifications.entity;

import com.immortals.platform.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;


/**
 * JPA entity for user notification preferences
 */
@Entity
@Table(name = "user_preferences", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;
    
    @Column(name = "enabled_channels")
    private String enabledChannels;
    
    @Column(name = "opted_out_categories")
    private String optedOutCategories;
    
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
}
