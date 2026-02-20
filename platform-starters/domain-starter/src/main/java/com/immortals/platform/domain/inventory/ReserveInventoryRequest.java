package com.immortals.platform.domain.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for reserving inventory.
 */
public record ReserveInventoryRequest(
    @NotNull(message = "Product ID is required")
    UUID productId,

    @NotNull(message = "Order ID is required")
    UUID orderId,

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    Integer quantity,

    Integer reservationDurationMinutes
) {
    public ReserveInventoryRequest {
        if (reservationDurationMinutes == null) {
            reservationDurationMinutes = 15;
        }
    }
}
