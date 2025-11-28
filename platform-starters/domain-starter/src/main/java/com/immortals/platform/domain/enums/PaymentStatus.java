package com.immortals.platform.domain.enums;

/**
 * Payment status enum for e-commerce platform.
 * Represents the lifecycle states of a payment transaction.
 */
public enum PaymentStatus {
    /**
     * Payment has been initiated but not yet processed
     */
    PENDING("Pending", "Payment is pending processing"),

    /**
     * Payment is being processed by payment gateway
     */
    PROCESSING("Processing", "Payment is being processed"),

    /**
     * Payment has been authorized but not yet captured
     */
    AUTHORIZED("Authorized", "Payment has been authorized"),

    /**
     * Payment has been successfully completed
     */
    COMPLETED("Completed", "Payment completed successfully"),

    /**
     * Payment processing failed
     */
    FAILED("Failed", "Payment processing failed"),

    /**
     * Payment has been declined by payment gateway
     */
    DECLINED("Declined", "Payment was declined"),

    /**
     * Payment has been cancelled
     */
    CANCELLED("Cancelled", "Payment has been cancelled"),

    /**
     * Payment has been refunded
     */
    REFUNDED("Refunded", "Payment has been refunded"),

    /**
     * Payment refund is being processed
     */
    REFUND_PENDING("Refund Pending", "Refund is being processed"),

    /**
     * Partial refund has been issued
     */
    PARTIALLY_REFUNDED("Partially Refunded", "Payment has been partially refunded");

    private final String displayName;
    private final String description;

    PaymentStatus(String displayName, String description) {
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
     * Check if payment is in a terminal state
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == DECLINED ||
               this == CANCELLED || this == REFUNDED;
    }

    /**
     * Check if payment is successful
     */
    public boolean isSuccessful() {
        return this == COMPLETED || this == AUTHORIZED;
    }

    /**
     * Check if payment can be refunded
     */
    public boolean isRefundable() {
        return this == COMPLETED || this == PARTIALLY_REFUNDED;
    }

    /**
     * Check if payment is in progress
     */
    public boolean isInProgress() {
        return this == PENDING || this == PROCESSING || this == AUTHORIZED;
    }

    /**
     * Get next valid statuses from current status
     */
    public PaymentStatus[] getNextValidStatuses() {
        return switch (this) {
            case PENDING -> new PaymentStatus[]{PROCESSING, CANCELLED, FAILED};
            case PROCESSING -> new PaymentStatus[]{AUTHORIZED, COMPLETED, FAILED, DECLINED};
            case AUTHORIZED -> new PaymentStatus[]{COMPLETED, CANCELLED, FAILED};
            case COMPLETED -> new PaymentStatus[]{REFUND_PENDING, PARTIALLY_REFUNDED};
            case REFUND_PENDING -> new PaymentStatus[]{REFUNDED, PARTIALLY_REFUNDED, FAILED};
            case PARTIALLY_REFUNDED -> new PaymentStatus[]{REFUND_PENDING, REFUNDED};
            case FAILED, DECLINED, CANCELLED, REFUNDED -> new PaymentStatus[]{};
        };
    }

    /**
     * Check if transition to target status is valid
     */
    public boolean canTransitionTo(PaymentStatus target) {
        for (PaymentStatus validStatus : getNextValidStatuses()) {
            if (validStatus == target) {
                return true;
            }
        }
        return false;
    }
}
