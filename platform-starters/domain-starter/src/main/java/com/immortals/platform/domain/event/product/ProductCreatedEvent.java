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
 * Event published when a new product is created.
 * Contains essential product information for downstream services.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreatedEvent {

    private UUID productId;
    private String sku;
    private String name;
    private String description;
    private String barcode;
    private UUID categoryId;
    private String categoryName;
    private BigDecimal basePrice;
    private BigDecimal currentPrice;
    private String currency;
    private ProductStatus status;
    private String brand;
    private String modelNumber;
    private String[] imageUrls;
    private String metadata;
    private Instant createdAt;
    private String createdBy;
}
