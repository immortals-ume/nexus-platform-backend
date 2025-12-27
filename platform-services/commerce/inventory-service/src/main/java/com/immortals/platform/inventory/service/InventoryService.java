package com.immortals.platform.inventory.service;

import com.immortals.platform.common.exception.ResourceNotFoundException;
import com.immortals.platform.common.exception.ValidationException;
import com.immortals.platform.domain.dto.inventory.ReleaseInventoryRequest;
import com.immortals.platform.domain.dto.inventory.ReserveInventoryRequest;
import com.immortals.platform.domain.dto.inventory.UpdateInventoryRequest;
import com.immortals.platform.domain.entity.Inventory;
import com.immortals.platform.domain.entity.InventoryReservation;
import com.immortals.platform.domain.entity.InventoryReservation.ReservationStatus;
import com.immortals.platform.inventory.config.KafkaTopicConfig;
import com.immortals.platform.domain.event.inventory.InventoryReleasedEvent;
import com.immortals.platform.domain.event.inventory.InventoryReservedEvent;
import com.immortals.platform.domain.event.inventory.LowInventoryEvent;
import com.immortals.platform.inventory.repository.InventoryRepository;
import com.immortals.platform.inventory.repository.InventoryReservationRepository;
import com.immortals.platform.messaging.event.DomainEvent;
import com.immortals.platform.messaging.publisher.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Service implementation for Inventory operations.
 * Implements business logic for inventory management with event publishing.
 * Follows SOLID principles and microservice design patterns.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService implements IInventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final EventPublisher eventPublisher;
    private final KafkaTopicConfig kafkaTopicConfig;

    /**
     * Get inventory by product ID
     */
    @Override
    @Transactional(
        readOnly = true,
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.SUPPORTS
    )
    @com.immortals.cache.features.annotations.Cacheable(
        namespace = "inventory",
        key = "#productId",
        ttl = 300,
        stampedeProtection = true,
        unless = "#result == null"
    )
    public Inventory getInventoryByProductId(UUID productId) {
        log.debug("Fetching inventory for product ID: {}", productId);
        
        try {
            return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Inventory not found for product ID: " + productId));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching inventory for product ID: {}", productId, e);
            throw new com.immortals.platform.common.exception.DatabaseException(
                "Failed to fetch inventory: " + e.getMessage(), e);
        }
    }

    /**
     * Get inventory by SKU
     */
    @Override
    @Transactional(
        readOnly = true,
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.SUPPORTS
    )
    @com.immortals.cache.features.annotations.Cacheable(
        namespace = "inventory",
        key = "'sku:' + #sku",
        ttl = 300,
        unless = "#result == null"
    )
    public Inventory getInventoryBySku(String sku) {
        log.debug("Fetching inventory for SKU: {}", sku);
        
        try {
            return inventoryRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Inventory not found for SKU: " + sku));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching inventory for SKU: {}", sku, e);
            throw new com.immortals.platform.common.exception.DatabaseException(
                "Failed to fetch inventory: " + e.getMessage(), e);
        }
    }

    /**
     * Reserve inventory for an order
     * Uses pessimistic locking to prevent overselling
     * Publishes InventoryReservedEvent on success
     * Validates: Requirements 2.2
     */
    @Override
    @Transactional(
        rollbackFor = Exception.class,
        timeout = 30,
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.REQUIRED
    )
    public InventoryReservation reserveInventory(ReserveInventoryRequest request) {
        log.info("Reserving inventory - Product: {}, Order: {}, Quantity: {}", 
            request.productId(), request.orderId(), request.quantity());

        try {
            Inventory inventory = inventoryRepository.findByProductIdWithLock(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Inventory not found for product ID: " + request.productId()));

            if (!inventory.hasSufficientStock(request.quantity())) {
                log.warn("Insufficient stock for product: {} - Available: {}, Requested: {}", 
                    request.productId(), inventory.getAvailableQuantity(), request.quantity());
                throw new ValidationException(
                    "Insufficient stock available. Available: " + inventory.getAvailableQuantity() + 
                    ", Requested: " + request.quantity());
            }

            inventory.reserve(request.quantity());
            Inventory updatedInventory = inventoryRepository.save(inventory);

            Instant expiresAt = Instant.now().plus(request.reservationDurationMinutes(), ChronoUnit.MINUTES);
            InventoryReservation reservation = InventoryReservation.builder()
                .productId(request.productId())
                .orderId(request.orderId())
                .quantity(request.quantity())
                .status(ReservationStatus.PENDING)
                .expiresAt(expiresAt)
                .build();

            InventoryReservation savedReservation = reservationRepository.save(reservation);

            if (updatedInventory.isLowStock()) {
                publishLowInventoryEvent(updatedInventory);
            }

            publishInventoryReservedEvent(savedReservation, updatedInventory);

            log.info("Inventory reserved successfully - Reservation ID: {}", savedReservation.getId());
            return savedReservation;

        } catch (ValidationException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error reserving inventory for product: {}", request.productId(), e);
            throw new com.immortals.platform.common.exception.DatabaseException(
                "Failed to reserve inventory: " + e.getMessage(), e);
        }
    }

    /**
     * Release reserved inventory back to available stock
     * Publishes InventoryReleasedEvent
     * Validates: Requirements 2.4
     */
    @Override
    @Transactional(
        rollbackFor = Exception.class,
        timeout = 30,
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.REQUIRED
    )
    public void releaseInventory(ReleaseInventoryRequest request) {
        log.info("Releasing inventory - Product: {}, Order: {}", 
            request.productId(), request.orderId());

        try {
            InventoryReservation reservation = reservationRepository.findByOrderId(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Reservation not found for order ID: " + request.orderId()));

            if (reservation.getStatus() != ReservationStatus.PENDING) {
                throw new ValidationException(
                    "Cannot release reservation with status: " + reservation.getStatus());
            }

            Inventory inventory = inventoryRepository.findByProductIdWithLock(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Inventory not found for product ID: " + request.productId()));

            inventory.release(reservation.getQuantity());
            Inventory updatedInventory = inventoryRepository.save(inventory);

            reservation.release(request.reason());
            reservationRepository.save(reservation);

            publishInventoryReleasedEvent(reservation, updatedInventory, request.reason());

            log.info("Inventory released successfully - Reservation ID: {}", reservation.getId());

        } catch (ValidationException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error releasing inventory for order: {}", request.orderId(), e);
            throw new com.immortals.platform.common.exception.DatabaseException(
                "Failed to release inventory: " + e.getMessage(), e);
        }
    }

    /**
     * Confirm reservation (deduct from total stock)
     */
    @Override
    @Transactional(
        rollbackFor = Exception.class,
        timeout = 30,
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.REQUIRED
    )
    public void confirmReservation(UUID orderId) {
        log.info("Confirming reservation for order: {}", orderId);

        try {
            InventoryReservation reservation = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Reservation not found for order ID: " + orderId));

            if (reservation.getStatus() != ReservationStatus.PENDING) {
                throw new ValidationException(
                    "Cannot confirm reservation with status: " + reservation.getStatus());
            }

            Inventory inventory = inventoryRepository.findByProductIdWithLock(reservation.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Inventory not found for product ID: " + reservation.getProductId()));

            inventory.confirmReservation(reservation.getQuantity());
            inventoryRepository.save(inventory);

            reservation.confirm();
            reservationRepository.save(reservation);

            log.info("Reservation confirmed successfully - Reservation ID: {}", reservation.getId());

        } catch (ValidationException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error confirming reservation for order: {}", orderId, e);
            throw new com.immortals.platform.common.exception.DatabaseException(
                "Failed to confirm reservation: " + e.getMessage(), e);
        }
    }

    /**
     * Update inventory levels
     */
    @Override
    @Transactional(
        rollbackFor = Exception.class,
        timeout = 30,
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.REQUIRED
    )
    @com.immortals.cache.features.annotations.CachePut(
        namespace = "inventory",
        key = "#productId",
        ttl = 300,
        unless = "#result == null"
    )
    public Inventory updateInventory(UUID productId, UpdateInventoryRequest request) {
        log.info("Updating inventory for product: {}", productId);

        try {
            Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Inventory not found for product ID: " + productId));

            boolean wasLowStock = inventory.isLowStock();

            if (request.availableQuantity() != null) {
                inventory.setAvailableQuantity(request.availableQuantity());
            }
            if (request.totalQuantity() != null) {
                inventory.setTotalQuantity(request.totalQuantity());
            }
            if (request.warehouseLocation() != null) {
                inventory.setWarehouseLocation(request.warehouseLocation());
            }
            if (request.reorderThreshold() != null) {
                inventory.setReorderThreshold(request.reorderThreshold());
            }

            Inventory updatedInventory = inventoryRepository.save(inventory);

            if (!wasLowStock && updatedInventory.isLowStock()) {
                publishLowInventoryEvent(updatedInventory);
            }

            log.info("Inventory updated successfully for product: {}", productId);
            return updatedInventory;

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating inventory for product: {}", productId, e);
            throw new com.immortals.platform.common.exception.DatabaseException(
                "Failed to update inventory: " + e.getMessage(), e);
        }
    }

    /**
     * Add stock to inventory
     */
    @Override
    @Transactional(
        rollbackFor = Exception.class,
        timeout = 30,
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.REQUIRED
    )
    @com.immortals.cache.features.annotations.CachePut(
        namespace = "inventory",
        key = "#productId",
        ttl = 300,
        unless = "#result == null"
    )
    public Inventory addStock(UUID productId, Integer quantity, String restockedBy) {
        log.info("Adding stock to product: {} - Quantity: {}", productId, quantity);

        try {
            if (quantity <= 0) {
                throw new ValidationException("Quantity must be positive");
            }

            Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Inventory not found for product ID: " + productId));

            inventory.addStock(quantity, restockedBy);
            Inventory updatedInventory = inventoryRepository.save(inventory);

            log.info("Stock added successfully to product: {}", productId);
            return updatedInventory;

        } catch (ValidationException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error adding stock to product: {}", productId, e);
            throw new com.immortals.platform.common.exception.DatabaseException(
                "Failed to add stock: " + e.getMessage(), e);
        }
    }

    /**
     * Get all low stock products
     */
    @Override
    @Transactional(
        readOnly = true,
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.SUPPORTS
    )
    public List<Inventory> getLowStockProducts() {
        log.debug("Fetching low stock products");
        
        try {
            return inventoryRepository.findLowStockProducts();
        } catch (Exception e) {
            log.error("Error fetching low stock products", e);
            throw new com.immortals.platform.common.exception.DatabaseException(
                "Failed to fetch low stock products: " + e.getMessage(), e);
        }
    }

    /**
     * Get all out of stock products
     */
    @Override
    @Transactional(
        readOnly = true,
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.SUPPORTS
    )
    public List<Inventory> getOutOfStockProducts() {
        log.debug("Fetching out of stock products");
        
        try {
            return inventoryRepository.findOutOfStockProducts();
        } catch (Exception e) {
            log.error("Error fetching out of stock products", e);
            throw new com.immortals.platform.common.exception.DatabaseException(
                "Failed to fetch out of stock products: " + e.getMessage(), e);
        }
    }

    /**
     * Process expired reservations (scheduled task)
     * Runs every 5 minutes
     */
    @Override
    @Scheduled(fixedDelay = 300000) // 5 minutes
    @Transactional(
        rollbackFor = Exception.class,
        timeout = 60,
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.REQUIRED
    )
    public void processExpiredReservations() {
        log.info("Processing expired reservations");

        try {
            List<InventoryReservation> expiredReservations = 
                reservationRepository.findExpiredReservations(Instant.now());

            for (InventoryReservation reservation : expiredReservations) {
                try {
                    Inventory inventory = inventoryRepository.findByProductIdWithLock(reservation.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                            "Inventory not found for product ID: " + reservation.getProductId()));

                    inventory.release(reservation.getQuantity());
                    inventoryRepository.save(inventory);

                    reservation.expire();
                    reservationRepository.save(reservation);

                    publishInventoryReleasedEvent(reservation, inventory, "Reservation expired");

                    log.info("Expired reservation processed - Reservation ID: {}", reservation.getId());

                } catch (Exception e) {
                    log.error("Error processing expired reservation: {}", reservation.getId(), e);
                }
            }

            log.info("Processed {} expired reservations", expiredReservations.size());

        } catch (Exception e) {
            log.error("Error processing expired reservations", e);
        }
    }

    /**
     * Publish InventoryReservedEvent to Kafka
     * Validates: Requirements 2.2
     */
    private void publishInventoryReservedEvent(InventoryReservation reservation, Inventory inventory) {
        try {
            InventoryReservedEvent eventPayload = InventoryReservedEvent.builder()
                .reservationId(reservation.getId())
                .productId(reservation.getProductId())
                .sku(inventory.getSku())
                .orderId(reservation.getOrderId())
                .quantityReserved(reservation.getQuantity())
                .remainingQuantity(inventory.getAvailableQuantity())
                .warehouseLocation(inventory.getWarehouseLocation())
                .reservedAt(reservation.getCreatedAt())
                .expiresAt(reservation.getExpiresAt())
                .reservedBy(reservation.getCreatedBy())
                .build();

            DomainEvent<InventoryReservedEvent> domainEvent = DomainEvent.<InventoryReservedEvent>builder()
                .eventType("InventoryReserved")
                .aggregateId(reservation.getProductId().toString())
                .aggregateType("Inventory")
                .payload(eventPayload)
                .correlationId(getCorrelationId())
                .source("inventory-service")
                .build();

            eventPublisher.publish(kafkaTopicConfig.getInventoryReservedTopic(), domainEvent);
            
            log.info("Published InventoryReservedEvent for reservation ID: {} with correlation ID: {}", 
                reservation.getId(), domainEvent.getCorrelationId());
                
        } catch (Exception e) {
            log.error("Failed to publish InventoryReservedEvent for reservation ID: {}", 
                reservation.getId(), e);
        }
    }

    /**
     * Publish InventoryReleasedEvent to Kafka
     * Validates: Requirements 2.4
     */
    private void publishInventoryReleasedEvent(InventoryReservation reservation, 
                                               Inventory inventory, String reason) {
        try {
            InventoryReleasedEvent eventPayload = InventoryReleasedEvent.builder()
                .reservationId(reservation.getId())
                .productId(reservation.getProductId())
                .sku(inventory.getSku())
                .orderId(reservation.getOrderId())
                .quantityReleased(reservation.getQuantity())
                .availableQuantity(inventory.getAvailableQuantity())
                .reason(reason)
                .releasedAt(Instant.now())
                .releasedBy(reservation.getUpdatedBy())
                .build();

            DomainEvent<InventoryReleasedEvent> domainEvent = DomainEvent.<InventoryReleasedEvent>builder()
                .eventType("InventoryReleased")
                .aggregateId(reservation.getProductId().toString())
                .aggregateType("Inventory")
                .payload(eventPayload)
                .correlationId(getCorrelationId())
                .source("inventory-service")
                .build();

            eventPublisher.publish(kafkaTopicConfig.getInventoryReleasedTopic(), domainEvent);
            
            log.info("Published InventoryReleasedEvent for reservation ID: {} with correlation ID: {}", 
                reservation.getId(), domainEvent.getCorrelationId());
                
        } catch (Exception e) {
            log.error("Failed to publish InventoryReleasedEvent for reservation ID: {}", 
                reservation.getId(), e);
        }
    }

    /**
     * Publish LowInventoryEvent to Kafka
     * Validates: Requirements 2.5
     */
    private void publishLowInventoryEvent(Inventory inventory) {
        try {
            String severity = LowInventoryEvent.calculateSeverity(
                inventory.getAvailableQuantity(), 
                inventory.getReorderThreshold()
            );

            LowInventoryEvent eventPayload = LowInventoryEvent.builder()
                .productId(inventory.getProductId())
                .sku(inventory.getSku())
                .availableQuantity(inventory.getAvailableQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .totalQuantity(inventory.getTotalQuantity())
                .reorderThreshold(inventory.getReorderThreshold())
                .warehouseLocation(inventory.getWarehouseLocation())
                .detectedAt(Instant.now())
                .severity(severity)
                .build();

            DomainEvent<LowInventoryEvent> domainEvent = DomainEvent.<LowInventoryEvent>builder()
                .eventType("LowInventory")
                .aggregateId(inventory.getProductId().toString())
                .aggregateType("Inventory")
                .payload(eventPayload)
                .correlationId(getCorrelationId())
                .source("inventory-service")
                .build();

            eventPublisher.publish(kafkaTopicConfig.getLowInventoryTopic(), domainEvent);
            
            log.info("Published LowInventoryEvent for product ID: {} with severity: {} and correlation ID: {}", 
                inventory.getProductId(), severity, domainEvent.getCorrelationId());
                
        } catch (Exception e) {
            log.error("Failed to publish LowInventoryEvent for product ID: {}", 
                inventory.getProductId(), e);
        }
    }

    /**
     * Get correlation ID from current context or generate new one
     */
    private String getCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
