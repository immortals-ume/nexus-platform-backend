package com.immortals.platform.inventory.repository;

import com.immortals.platform.domain.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Inventory entity operations.
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    /**
     * Find inventory by product ID
     */
    Optional<Inventory> findByProductId(UUID productId);

    /**
     * Find inventory by product ID with pessimistic write lock
     * Prevents concurrent modifications during reservation
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> findByProductIdWithLock(@Param("productId") UUID productId);

    /**
     * Find inventory by SKU
     */
    Optional<Inventory> findBySku(String sku);

    /**
     * Find inventory by SKU with pessimistic write lock
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.sku = :sku")
    Optional<Inventory> findBySkuWithLock(@Param("sku") String sku);

    /**
     * Find all products with low stock (available quantity <= reorder threshold)
     */
    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity <= i.reorderThreshold")
    List<Inventory> findLowStockProducts();

    /**
     * Find all products with zero available quantity
     */
    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity = 0")
    List<Inventory> findOutOfStockProducts();

    /**
     * Check if product exists in inventory
     */
    boolean existsByProductId(UUID productId);

    /**
     * Check if SKU exists in inventory
     */
    boolean existsBySku(String sku);
}
