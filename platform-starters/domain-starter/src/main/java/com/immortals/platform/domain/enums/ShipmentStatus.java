package com.immortals.platform.domain.enums;

/**
 * Shipment status enum for e-commerce platform.
 * Represents the lifecycle states of a shipment.
 */
public enum ShipmentStatus {
    /**
     * Shipment has been created but not yet dispatched
     */
    PENDING("Pending", "Shipment is being prepared"),

    /**
     * Shipment is being prepared for dispatch
     */
    PROCESSING("Processing", "Shipment is being processed"),

    /**
     * Shipment has been dispatched from warehouse
     */
    DISPATCHED("Dispatched", "Shipment has been dispatched"),

    /**
     * Shipment is in transit to destination
     */
    IN_TRANSIT("In Transit", "Shipment is on the way"),

    /**
     * Shipment is out for delivery
     */
    OUT_FOR_DELIVERY("Out for Delivery", "Shipment is out for delivery"),

    /**
     * Shipment has been delivered successfully
     */
    DELIVERED("Delivered", "Shipment has been delivered"),

    /**
     * Delivery attempt failed
     */
    DELIVERY_FAILED("Delivery Failed", "Delivery attempt failed"),

    /**
     * Shipment has been cancelled
     */
    CANCELLED("Cancelled", "Shipment has been cancelled"),

    /**
     * Shipment is being returned
     */
    RETURNING("Returning", "Shipment is being returned"),

    /**
     * Shipment has been returned
     */
    RETURNED("Returned", "Shipment has been returned"),

    /**
     * Shipment is lost
     */
    LOST("Lost", "Shipment is lost");

    private final String displayName;
    private final String description;

    ShipmentStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if shipment is in a terminal state
     */
    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED || this == RETURNED || this == LOST;
    }

    /**
     * Check if shipment is in transit
     */
    public boolean isInTransit() {
        return this == DISPATCHED || this == IN_TRANSIT || this == OUT_FOR_DELIVERY;
    }

    /**
     * Check if shipment can be cancelled
     */
    public boolean isCancellable() {
        return this == PENDING || this == PROCESSING;
    }

    /**
     * Check if shipment is active
     */
    public boolean isActive() {
        return !isTerminal();
    }

    /**
     * Check if shipment was successful
     */
    public boolean isSuccessful() {
        return this == DELIVERED;
    }

    /**
     * Get next valid statuses from current status
     */
    public ShipmentStatus[] getNextValidStatuses() {
        return switch (this) {
            case PENDING -> new ShipmentStatus[]{PROCESSING, CANCELLED};
            case PROCESSING -> new ShipmentStatus[]{DISPATCHED, CANCELLED};
            case DISPATCHED -> new ShipmentStatus[]{IN_TRANSIT, RETURNING, LOST};
            case IN_TRANSIT -> new ShipmentStatus[]{OUT_FOR_DELIVERY, DELIVERY_FAILED, RETURNING, LOST};
            case OUT_FOR_DELIVERY -> new ShipmentStatus[]{DELIVERED, DELIVERY_FAILED, RETURNING};
            case DELIVERY_FAILED -> new ShipmentStatus[]{OUT_FOR_DELIVERY, RETURNING, LOST};
            case RETURNING -> new ShipmentStatus[]{RETURNED};
            case DELIVERED, CANCELLED, RETURNED, LOST -> new ShipmentStatus[]{};
        };
    }

    /**
     * Check if transition to target status is valid
     */
    public boolean canTransitionTo(ShipmentStatus target) {
        for (ShipmentStatus validStatus : getNextValidStatuses()) {
            if (validStatus == target) {
                return true;
            }
        }
        return false;
    }
}
