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
 * Event published when a product is soft deleted.
 * Contains product information for cleanup and archival in downstream services.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDeletedEvent {

    private UUID productId;
    private String sku;
    private String name;
    private UUID categoryId;
    private String categoryName;
    private BigDecimal currentPrice;
    private String currency;
    private ProductStatus statusBeforeDeletion;
    private String brand;
    private Instant deletedAt;
    private String deletedBy;
    private String deletionReason;
}
