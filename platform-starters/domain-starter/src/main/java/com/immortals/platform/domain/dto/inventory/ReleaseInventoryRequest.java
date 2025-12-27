package com.immortals.platform.domain.dto.inventory;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for releasing reserved inventory.
 */
public record ReleaseInventoryRequest(
    @NotNull(message = "Product ID is required")
    UUID productId,

    @NotNull(message = "Order ID is required")
    UUID orderId,

    String reason
) {
    public ReleaseInventoryRequest {
        if (reason == null || reason.isBlank()) {
            reason = "Order cancelled or expired";
        }
    }
}
