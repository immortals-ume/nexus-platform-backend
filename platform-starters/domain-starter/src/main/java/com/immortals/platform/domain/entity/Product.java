package com.immortals.platform.domain.entity;

import com.immortals.platform.domain.BaseEntity;
import com.immortals.platform.domain.enums.ProductStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * Product entity representing a product in the e-commerce catalog.
 * Extends BaseEntity for audit fields and soft delete support.
 */
@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_sku", columnList = "sku"),
    @Index(name = "idx_product_barcode", columnList = "barcode"),
    @Index(name = "idx_product_status", columnList = "status"),
    @Index(name = "idx_product_category", columnList = "category_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "SKU is required")
    @Size(max = 100, message = "SKU must not exceed 100 characters")
    @Column(name = "sku", nullable = false, unique = true, length = 100)
    private String sku;

    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Product name must not exceed 255 characters")
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Size(max = 100, message = "Barcode must not exceed 100 characters")
    @Column(name = "barcode", unique = true, length = 100)
    private String barcode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @NotNull(message = "Base price is required")
    @Positive(message = "Base price must be positive")
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @NotNull(message = "Current price is required")
    @Positive(message = "Current price must be positive")
    @Column(name = "current_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal currentPrice;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @NotNull(message = "Product status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status;

    @DecimalMin(value = "0.0", message = "Average rating must be between 0 and 5")
    @DecimalMax(value = "5.0", message = "Average rating must be between 0 and 5")
    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Min(value = 0, message = "Review count cannot be negative")
    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "image_urls", columnDefinition = "TEXT[]")
    private String[] imageUrls;

    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;

    @Size(max = 100, message = "Brand must not exceed 100 characters")
    @Column(name = "brand", length = 100)
    private String brand;

    @Size(max = 50, message = "Model number must not exceed 50 characters")
    @Column(name = "model_number", length = 50)
    private String modelNumber;

    @Column(name = "weight", precision = 10, scale = 2)
    private BigDecimal weight;

    @Size(max = 10, message = "Weight unit must not exceed 10 characters")
    @Column(name = "weight_unit", length = 10)
    private String weightUnit;

    @Column(name = "length", precision = 10, scale = 2)
    private BigDecimal length;

    @Column(name = "width", precision = 10, scale = 2)
    private BigDecimal width;

    @Column(name = "height", precision = 10, scale = 2)
    private BigDecimal height;

    @Size(max = 10, message = "Dimension unit must not exceed 10 characters")
    @Column(name = "dimension_unit", length = 10)
    private String dimensionUnit;

    /**
     * Check if product is available for purchase
     */
    public boolean isAvailableForPurchase() {
        return status == ProductStatus.ACTIVE && !isDeleted();
    }

    /**
     * Check if product is visible to customers
     */
    public boolean isVisible() {
        return status.isVisible() && !isDeleted();
    }

    /**
     * Apply promotional price
     */
    public void applyPromotionalPrice(BigDecimal promotionalPrice) {
        if (promotionalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Promotional price must be positive");
        }
        this.currentPrice = promotionalPrice;
    }

    /**
     * Reset to base price
     */
    public void resetToBasePrice() {
        this.currentPrice = this.basePrice;
    }

    /**
     * Update average rating
     */
    public void updateRating(BigDecimal newAverageRating, Integer newReviewCount) {
        this.averageRating = newAverageRating;
        this.reviewCount = newReviewCount;
    }

    /**
     * Activate product
     */
    public void activate() {
        if (!status.canTransitionTo(ProductStatus.ACTIVE)) {
            throw new IllegalStateException("Cannot activate product from status: " + status);
        }
        this.status = ProductStatus.ACTIVE;
    }

    /**
     * Deactivate product
     */
    public void deactivate() {
        if (!status.canTransitionTo(ProductStatus.INACTIVE)) {
            throw new IllegalStateException("Cannot deactivate product from status: " + status);
        }
        this.status = ProductStatus.INACTIVE;
    }

    /**
     * Mark as out of stock
     */
    public void markOutOfStock() {
        if (!status.canTransitionTo(ProductStatus.OUT_OF_STOCK)) {
            throw new IllegalStateException("Cannot mark product as out of stock from status: " + status);
        }
        this.status = ProductStatus.OUT_OF_STOCK;
    }
}
