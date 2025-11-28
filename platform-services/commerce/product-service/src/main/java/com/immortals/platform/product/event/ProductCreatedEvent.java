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
 * Event published when a new product is created.
 * Contains essential product information for downstream services.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreatedEvent {

    /**
     * Unique identifier of the product
     */
    private UUID productId;

    /**
     * Stock Keeping Unit - unique product identifier
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
     * Currency code (ISO 4217)
     */
    private String currency;

    /**
     * Product status
     */
    private ProductStatus status;

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
     * Timestamp when the product was created
     */
    private Instant createdAt;

    /**
     * User who created the product
     */
    private String createdBy;
}
