package com.immortals.platform.domain.entity;

import com.immortals.platform.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Inventory entity representing stock levels for products.
 * Uses optimistic locking to prevent overselling.
 */
@Entity
@Table(name = "inventory", indexes = {
    @Index(name = "idx_inventory_product_id", columnList = "product_id"),
    @Index(name = "idx_inventory_sku", columnList = "sku")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "product_id", nullable = false, unique = true)
    private UUID productId;

    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "warehouse_location", length = 100)
    private String warehouseLocation;

    @Column(name = "reorder_threshold")
    @Builder.Default
    private Integer reorderThreshold = 10;

    @Column(name = "last_restocked_at")
    private Instant lastRestockedAt;

    @Column(name = "last_restocked_by", length = 100)
    private String lastRestockedBy;

    /**
     * Check if inventory is below reorder threshold
     */
    public boolean isLowStock() {
        return availableQuantity <= reorderThreshold;
    }

    /**
     * Check if sufficient quantity is available for reservation
     */
    public boolean hasSufficientStock(Integer quantity) {
        return availableQuantity >= quantity;
    }

    /**
     * Reserve inventory (decrements available, increments reserved)
     */
    public void reserve(Integer quantity) {
        if (!hasSufficientStock(quantity)) {
            throw new IllegalStateException("Insufficient stock available");
        }
        this.availableQuantity -= quantity;
        this.reservedQuantity += quantity;
    }

    /**
     * Release reserved inventory (increments available, decrements reserved)
     */
    public void release(Integer quantity) {
        if (this.reservedQuantity < quantity) {
            throw new IllegalStateException("Cannot release more than reserved quantity");
        }
        this.availableQuantity += quantity;
        this.reservedQuantity -= quantity;
    }

    /**
     * Confirm reservation (decrements reserved, decrements total)
     */
    public void confirmReservation(Integer quantity) {
        if (this.reservedQuantity < quantity) {
            throw new IllegalStateException("Cannot confirm more than reserved quantity");
        }
        this.reservedQuantity -= quantity;
        this.totalQuantity -= quantity;
    }

    /**
     * Add stock (increments both available and total)
     */
    public void addStock(Integer quantity, String restockedBy) {
        this.availableQuantity += quantity;
        this.totalQuantity += quantity;
        this.lastRestockedAt = Instant.now();
        this.lastRestockedBy = restockedBy;
    }
}
