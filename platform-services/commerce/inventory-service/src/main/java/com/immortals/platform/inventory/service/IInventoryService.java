package com.immortals.platform.inventory.service;

import com.immortals.platform.domain.dto.inventory.ReleaseInventoryRequest;
import com.immortals.platform.domain.dto.inventory.ReserveInventoryRequest;
import com.immortals.platform.domain.dto.inventory.UpdateInventoryRequest;
import com.immortals.platform.domain.entity.Inventory;
import com.immortals.platform.domain.entity.InventoryReservation;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for Inventory operations.
 * Defines the contract for inventory management.
 */
public interface IInventoryService {

    /**
     * Get inventory by product ID
     */
    Inventory getInventoryByProductId(UUID productId);

    /**
     * Get inventory by SKU
     */
    Inventory getInventoryBySku(String sku);

    /**
     * Reserve inventory for an order
     * Uses optimistic locking to prevent overselling
     */
    InventoryReservation reserveInventory(ReserveInventoryRequest request);

    /**
     * Release reserved inventory back to available stock
     */
    void releaseInventory(ReleaseInventoryRequest request);

    /**
     * Confirm reservation (deduct from total stock)
     */
    void confirmReservation(UUID orderId);

    /**
     * Update inventory levels
     */
    Inventory updateInventory(UUID productId, UpdateInventoryRequest request);

    /**
     * Add stock to inventory
     */
    Inventory addStock(UUID productId, Integer quantity, String restockedBy);

    /**
     * Get all low stock products
     */
    List<Inventory> getLowStockProducts();

    /**
     * Get all out of stock products
     */
    List<Inventory> getOutOfStockProducts();

    /**
     * Process expired reservations (scheduled task)
     */
    void processExpiredReservations();
}
