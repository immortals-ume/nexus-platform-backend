package com.immortals.platform.domain.enums;

/**
 * Order status enum for e-commerce platform.
 * Represents the lifecycle states of an order.
 */
public enum OrderStatus {
    /**
     * Order has been created but not yet confirmed
     */
    PENDING("Pending", "Order is awaiting confirmation"),

    /**
     * Order has been confirmed and inventory reserved
     */
    CONFIRMED("Confirmed", "Order has been confirmed"),

    /**
     * Payment has been successfully processed
     */
    PAID("Paid", "Payment completed successfully"),

    /**
     * Order is being prepared for shipment
     */
    PROCESSING("Processing", "Order is being processed"),

    /**
     * Order has been shipped
     */
    SHIPPED("Shipped", "Order has been shipped"),

    /**
     * Order has been delivered to customer
     */
    DELIVERED("Delivered", "Order has been delivered"),

    /**
     * Order has been cancelled
     */
    CANCELLED("Cancelled", "Order has been cancelled"),

    /**
     * Order processing failed
     */
    FAILED("Failed", "Order processing failed"),

    /**
     * Order has been completed successfully
     */
    COMPLETED("Completed", "Order has been completed");

    private final String displayName;
    private final String description;

    OrderStatus(String displayName, String description) {
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
     * Check if order is in a terminal state
     */
    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED || this == FAILED || this == COMPLETED;
    }

    /**
     * Check if order can be cancelled
     */
    public boolean isCancellable() {
        return this == PENDING || this == CONFIRMED;
    }

    /**
     * Check if order is active
     */
    public boolean isActive() {
        return !isTerminal();
    }

    /**
     * Get next valid statuses from current status
     */
    public OrderStatus[] getNextValidStatuses() {
        return switch (this) {
            case PENDING -> new OrderStatus[]{CONFIRMED, CANCELLED, FAILED};
            case CONFIRMED -> new OrderStatus[]{PAID, CANCELLED, FAILED};
            case PAID -> new OrderStatus[]{PROCESSING, CANCELLED, FAILED};
            case PROCESSING -> new OrderStatus[]{SHIPPED, FAILED};
            case SHIPPED -> new OrderStatus[]{DELIVERED, FAILED};
            case DELIVERED -> new OrderStatus[]{COMPLETED};
            case CANCELLED, FAILED, COMPLETED -> new OrderStatus[]{};
        };
    }

    /**
     * Check if transition to target status is valid
     */
    public boolean canTransitionTo(OrderStatus target) {
        for (OrderStatus validStatus : getNextValidStatuses()) {
            if (validStatus == target) {
                return true;
            }
        }
        return false;
    }
}
