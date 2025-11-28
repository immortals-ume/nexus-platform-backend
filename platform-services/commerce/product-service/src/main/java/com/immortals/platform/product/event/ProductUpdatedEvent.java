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
 * Event published when a product is updated.
 * Contains both old and new values for important fields to enable downstream processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdatedEvent {

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
     * Product description
     */
    private String description;

    /**
     * Product barcode
     */
    private String barcode;

    /**
     * Category ID
     */
    private UUID categoryId;

    /**
     * Category name
     */
    private String categoryName;

    /**
     * Base price of the product
     */
    private BigDecimal basePrice;

    /**
     * Current price of the product
     */
    private BigDecimal currentPrice;

    /**
     * Old price (before update) - useful for price change notifications
     */
    private BigDecimal oldPrice;

    /**
     * Currency code (ISO 4217)
     */
    private String currency;

    /**
     * Product status
     */
    private ProductStatus status;

    /**
     * Old status (before update)
     */
    private ProductStatus oldStatus;

    /**
     * Product brand
     */
    private String brand;

    /**
     * Product model number
     */
    private String modelNumber;

    /**
     * Product image URLs
     */
    private String[] imageUrls;

    /**
     * Product metadata (JSON)
     */
    private String metadata;

    /**
     * Timestamp when the product was updated
     */
    private Instant updatedAt;

    /**
     * User who updated the product
     */
    private String updatedBy;

    /**
     * Flag indicating if price changed
     */
    private boolean priceChanged;

    /**
     * Flag indicating if status changed
     */
    private boolean statusChanged;
}
