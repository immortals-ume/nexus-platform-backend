package com.immortals.platform.domain.entity;

import com.immortals.platform.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Inventory reservation entity for tracking temporary stock holds.
 */
@Entity
@Table(name = "inventory_reservations", indexes = {
    @Index(name = "idx_reservation_product_id", columnList = "product_id"),
    @Index(name = "idx_reservation_order_id", columnList = "order_id"),
    @Index(name = "idx_reservation_status", columnList = "status"),
    @Index(name = "idx_reservation_expires_at", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReservation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ReservationStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "reason", length = 500)
    private String reason;

    public enum ReservationStatus {
        PENDING,
        CONFIRMED,
        RELEASED,
        EXPIRED
    }

    /**
     * Check if reservation has expired
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt) && status == ReservationStatus.PENDING;
    }

    /**
     * Confirm the reservation
     */
    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
        this.confirmedAt = Instant.now();
    }

    /**
     * Release the reservation
     */
    public void release(String reason) {
        this.status = ReservationStatus.RELEASED;
        this.releasedAt = Instant.now();
        this.reason = reason;
    }

    /**
     * Mark as expired
     */
    public void expire() {
        this.status = ReservationStatus.EXPIRED;
    }
}
