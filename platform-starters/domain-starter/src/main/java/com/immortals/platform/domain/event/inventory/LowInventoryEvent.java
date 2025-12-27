package com.immortals.platform.domain.event.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when inventory falls below the reorder threshold.
 * Validates: Requirements 2.5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LowInventoryEvent {

    private UUID productId;
    private String sku;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Integer totalQuantity;
    private Integer reorderThreshold;
    private String warehouseLocation;
    private Instant detectedAt;
    private String severity;

    public static String calculateSeverity(Integer availableQuantity, Integer reorderThreshold) {
        if (availableQuantity == 0) {
            return "CRITICAL";
        } else if (availableQuantity <= reorderThreshold / 2) {
            return "WARNING";
        } else {
            return "INFO";
        }
    }
}
