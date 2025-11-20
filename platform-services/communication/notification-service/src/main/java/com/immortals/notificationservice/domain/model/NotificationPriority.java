package com.immortals.notificationservice.domain.model;

/**
 * Priority levels for notification processing
 * HIGH - Processed immediately (OTP, alerts)
 * MEDIUM - Processed within minutes (transactional emails)
 * LOW - Processed in batches (marketing emails)
 */
public enum NotificationPriority {
    HIGH(1),
    MEDIUM(2),
    LOW(3);
    
    private final int level;
    
    NotificationPriority(int level) {
        this.level = level;
    }
    
    public int getLevel() {
        return level;
    }
    
    public boolean isHigherThan(NotificationPriority other) {
        return this.level < other.level;
    }
}
