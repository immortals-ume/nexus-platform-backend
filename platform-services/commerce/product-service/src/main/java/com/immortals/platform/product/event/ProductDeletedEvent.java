package com.immortals.platform.product.event;

import com.immortals.platform.domain.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a product is soft deleted.
 * Contains product information for cleanup and archival in downstream services.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDeletedEvent {

    /**
     * Unique identifier of the product
     */
    private UUID productId;

    /**
     * Stock Keeping Unit
     */
    private String sku;

    /**
     * Product name
     */
    private String name;

    /**
     * Category ID
     */
    private UUID categoryId;

    /**
     * Category name
     */
    private String categoryName;

    /**
     * Current price at time of deletion
     */
    private BigDecimal currentPrice;

    /**
     * Currency code (ISO 4217)
     */
    private String currency;

    /**
     * Status before deletion
     */
    private ProductStatus statusBeforeDeletion;

    /**
     * Product brand
     */
    private String brand;

    /**
     * Timestamp when the product was deleted
     */
    private Instant deletedAt;

    /**
     * User who deleted the product
     */
    private String deletedBy;

    /**
     * Reason for deletion (optional)
     */
    private String deletionReason;
}
