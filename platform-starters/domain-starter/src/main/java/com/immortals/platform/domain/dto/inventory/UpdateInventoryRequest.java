package com.immortals.platform.domain.dto.inventory;

import jakarta.validation.constraints.Min;

/**
 * Request DTO for updating inventory.
 */
public record UpdateInventoryRequest(
    @Min(value = 0, message = "Available quantity cannot be negative")
    Integer availableQuantity,

    @Min(value = 0, message = "Total quantity cannot be negative")
    Integer totalQuantity,

    String warehouseLocation,

    @Min(value = 0, message = "Reorder threshold cannot be negative")
    Integer reorderThreshold
) {
}
