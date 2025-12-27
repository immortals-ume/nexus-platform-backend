package com.immortals.platform.inventory.repository;

import com.immortals.platform.domain.entity.InventoryReservation;
import com.immortals.platform.domain.entity.InventoryReservation.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for InventoryReservation entity operations.
 */
@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {

    /**
     * Find reservation by order ID
     */
    Optional<InventoryReservation> findByOrderId(UUID orderId);

    /**
     * Find all reservations for a product
     */
    List<InventoryReservation> findByProductId(UUID productId);

    /**
     * Find all reservations by status
     */
    List<InventoryReservation> findByStatus(ReservationStatus status);

    /**
     * Find expired reservations that are still pending
     */
    @Query("SELECT r FROM InventoryReservation r WHERE r.status = 'PENDING' AND r.expiresAt < :now")
    List<InventoryReservation> findExpiredReservations(@Param("now") Instant now);

    /**
     * Find all pending reservations for a product
     */
    @Query("SELECT r FROM InventoryReservation r WHERE r.productId = :productId AND r.status = 'PENDING'")
    List<InventoryReservation> findPendingReservationsByProduct(@Param("productId") UUID productId);

    /**
     * Calculate total reserved quantity for a product
     */
    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM InventoryReservation r WHERE r.productId = :productId AND r.status = 'PENDING'")
    Integer calculateTotalReservedQuantity(@Param("productId") UUID productId);
}
