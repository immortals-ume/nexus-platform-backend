package com.immortals.notificationservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tracks health status of notification providers
 * Used for automatic failover decisions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderHealth {
    
    private String providerId;
    private Notification.NotificationType type;
    private HealthStatus status;
    private int successCount;
    private int failureCount;
    private double successRate;
    private LocalDateTime lastSuccessAt;
    private LocalDateTime lastFailureAt;
    private LocalDateTime lastCheckedAt;
    
    public enum HealthStatus {
        HEALTHY,
        DEGRADED,
        UNHEALTHY
    }
    
    public void recordSuccess() {
        this.successCount++;
        this.lastSuccessAt = LocalDateTime.now();
        updateSuccessRate();
        updateHealthStatus();
    }
    
    public void recordFailure() {
        this.failureCount++;
        this.lastFailureAt = LocalDateTime.now();
        updateSuccessRate();
        updateHealthStatus();
    }
    
    private void updateSuccessRate() {
        int total = successCount + failureCount;
        this.successRate = total > 0 ? (double) successCount / total * 100 : 100.0;
    }
    
    private void updateHealthStatus() {
        if (successRate >= 95) {
            this.status = HealthStatus.HEALTHY;
        } else if (successRate >= 80) {
            this.status = HealthStatus.DEGRADED;
        } else {
            this.status = HealthStatus.UNHEALTHY;
        }
    }
    
    public boolean isHealthy() {
        return status == HealthStatus.HEALTHY;
    }
}
