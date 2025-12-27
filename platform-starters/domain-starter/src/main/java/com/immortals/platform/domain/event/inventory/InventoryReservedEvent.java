package com.immortals.platform.domain.event.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when inventory is successfully reserved for an order.
 * Validates: Requirements 2.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservedEvent {

    private UUID reservationId;
    private UUID productId;
    private String sku;
    private UUID orderId;
    private Integer quantityReserved;
    private Integer remainingQuantity;
    private String warehouseLocation;
    private Instant reservedAt;
    private Instant expiresAt;
    private String reservedBy;
}
