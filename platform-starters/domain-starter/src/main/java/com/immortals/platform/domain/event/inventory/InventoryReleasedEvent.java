package com.immortals.platform.domain.event.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when reserved inventory is released back to available stock.
 * Validates: Requirements 2.4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReleasedEvent {

    private UUID reservationId;
    private UUID productId;
    private String sku;
    private UUID orderId;
    private Integer quantityReleased;
    private Integer availableQuantity;
    private String reason;
    private Instant releasedAt;
    private String releasedBy;
}
