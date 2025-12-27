package com.immortals.platform.inventory.controller;

import com.immortals.platform.domain.dto.ApiResponse;
import com.immortals.platform.domain.dto.inventory.ReleaseInventoryRequest;
import com.immortals.platform.domain.dto.inventory.ReserveInventoryRequest;
import com.immortals.platform.domain.dto.inventory.UpdateInventoryRequest;
import com.immortals.platform.domain.entity.Inventory;
import com.immortals.platform.domain.entity.InventoryReservation;
import com.immortals.platform.inventory.service.IInventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for Inventory operations.
 * Provides endpoints for inventory management, reservations, and stock tracking.
 */
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Inventory", description = "Inventory management API")
public class InventoryController {

    private final IInventoryService inventoryService;

    /**
     * Get inventory by product ID
     */
    @GetMapping("/{productId}")
    @Operation(summary = "Get inventory by product ID")
    public ResponseEntity<ApiResponse<Inventory>> getInventory(@PathVariable UUID productId) {
        log.info("GET /api/v1/inventory/{}", productId);
        
        Inventory inventory = inventoryService.getInventoryByProductId(productId);
        
        return ResponseEntity.ok(ApiResponse.success(inventory, "Inventory retrieved successfully"));
    }

    /**
     * Reserve inventory for an order
     */
    @PostMapping("/reserve")
    @Operation(summary = "Reserve inventory for an order")
    public ResponseEntity<ApiResponse<InventoryReservation>> reserveInventory(
            @Valid @RequestBody ReserveInventoryRequest request) {
        log.info("POST /api/v1/inventory/reserve - Product: {}, Order: {}", 
            request.productId(), request.orderId());
        
        InventoryReservation reservation = inventoryService.reserveInventory(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(reservation, "Inventory reserved successfully"));
    }

    /**
     * Release reserved inventory
     */
    @PostMapping("/release")
    @Operation(summary = "Release reserved inventory")
    public ResponseEntity<ApiResponse<Void>> releaseInventory(
            @Valid @RequestBody ReleaseInventoryRequest request) {
        log.info("POST /api/v1/inventory/release - Product: {}, Order: {}", 
            request.productId(), request.orderId());
        
        inventoryService.releaseInventory(request);
        
        return ResponseEntity.ok(ApiResponse.message("Inventory released successfully"));
    }

    /**
     * Confirm reservation
     */
    @PostMapping("/confirm/{orderId}")
    @Operation(summary = "Confirm inventory reservation")
    public ResponseEntity<ApiResponse<Void>> confirmReservation(@PathVariable UUID orderId) {
        log.info("POST /api/v1/inventory/confirm/{}", orderId);
        
        inventoryService.confirmReservation(orderId);
        
        return ResponseEntity.ok(ApiResponse.message("Reservation confirmed successfully"));
    }

    /**
     * Update inventory
     */
    @PutMapping("/{productId}")
    @Operation(summary = "Update inventory levels")
    public ResponseEntity<ApiResponse<Inventory>> updateInventory(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateInventoryRequest request) {
        log.info("PUT /api/v1/inventory/{}", productId);
        
        Inventory inventory = inventoryService.updateInventory(productId, request);
        
        return ResponseEntity.ok(ApiResponse.success(inventory, "Inventory updated successfully"));
    }

    /**
     * Add stock to inventory
     */
    @PostMapping("/{productId}/add-stock")
    @Operation(summary = "Add stock to inventory")
    public ResponseEntity<ApiResponse<Inventory>> addStock(
            @PathVariable UUID productId,
            @RequestParam Integer quantity,
            @RequestParam(required = false) String restockedBy) {
        log.info("POST /api/v1/inventory/{}/add-stock - Quantity: {}", productId, quantity);
        
        Inventory inventory = inventoryService.addStock(productId, quantity, restockedBy);
        
        return ResponseEntity.ok(ApiResponse.success(inventory, "Stock added successfully"));
    }

    /**
     * Get low stock products
     */
    @GetMapping("/low-stock")
    @Operation(summary = "Get all low stock products")
    public ResponseEntity<ApiResponse<List<Inventory>>> getLowStockProducts() {
        log.info("GET /api/v1/inventory/low-stock");
        
        List<Inventory> lowStockProducts = inventoryService.getLowStockProducts();
        
        return ResponseEntity.ok(ApiResponse.success(lowStockProducts, "Low stock products retrieved successfully"));
    }

    /**
     * Get out of stock products
     */
    @GetMapping("/out-of-stock")
    @Operation(summary = "Get all out of stock products")
    public ResponseEntity<ApiResponse<List<Inventory>>> getOutOfStockProducts() {
        log.info("GET /api/v1/inventory/out-of-stock");
        
        List<Inventory> outOfStockProducts = inventoryService.getOutOfStockProducts();
        
        return ResponseEntity.ok(ApiResponse.success(outOfStockProducts, "Out of stock products retrieved successfully"));
    }
}
