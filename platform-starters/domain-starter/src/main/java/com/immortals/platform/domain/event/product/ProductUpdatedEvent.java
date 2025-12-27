package com.immortals.platform.domain.event.product;

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

    private UUID productId;
    private String sku;
    private String name;
    private String description;
    private String barcode;
    private UUID categoryId;
    private String categoryName;
    private BigDecimal basePrice;
    private BigDecimal currentPrice;
    private BigDecimal oldPrice;
    private String currency;
    private ProductStatus status;
    private ProductStatus oldStatus;
    private String brand;
    private String modelNumber;
    private String[] imageUrls;
    private String metadata;
    private Instant updatedAt;
    private String updatedBy;
    private boolean priceChanged;
    private boolean statusChanged;
}
