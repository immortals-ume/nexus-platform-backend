package com.immortals.platform.domain.entity;

import com.immortals.platform.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * PriceHistory entity for tracking product price changes over time.
 * Provides audit trail for pricing decisions and analysis.
 */
@Entity
@Table(name = "price_history", indexes = {
    @Index(name = "idx_price_history_product", columnList = "product_id"),
    @Index(name = "idx_price_history_changed_at", columnList = "changed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceHistory extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "Product ID is required")
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "old_price", precision = 10, scale = 2)
    private BigDecimal oldPrice;

    @NotNull(message = "New price is required")
    @Positive(message = "New price must be positive")
    @Column(name = "new_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal newPrice;

    @NotNull(message = "Changed at timestamp is required")
    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Size(max = 100, message = "Changed by must not exceed 100 characters")
    @Column(name = "changed_by", length = 100)
    private String changedBy;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    @Column(name = "reason", length = 500)
    private String reason;

    @Size(max = 50, message = "Change type must not exceed 50 characters")
    @Column(name = "change_type", length = 50)
    private String changeType;

    @Column(name = "price_difference", precision = 10, scale = 2)
    private BigDecimal priceDifference;

    @Column(name = "percentage_change", precision = 5, scale = 2)
    private BigDecimal percentageChange;

    /**
     * Calculate price difference and percentage change
     */
    @PrePersist
    @PreUpdate
    public void calculateChanges() {
        if (oldPrice != null && newPrice != null) {
            this.priceDifference = newPrice.subtract(oldPrice);

            if (oldPrice.compareTo(BigDecimal.ZERO) > 0) {
                this.percentageChange = priceDifference
                    .divide(oldPrice, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            }
        }

        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }

    /**
     * Check if price increased
     */
    public boolean isPriceIncrease() {
        return priceDifference != null && priceDifference.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Check if price decreased
     */
    public boolean isPriceDecrease() {
        return priceDifference != null && priceDifference.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Get absolute price change
     */
    public BigDecimal getAbsolutePriceChange() {
        return priceDifference != null ? priceDifference.abs() : BigDecimal.ZERO;
    }
}
