package com.immortals.platform.domain.enums;

/**
 * Product status enum for e-commerce platform.
 * Represents the lifecycle states of a product in the catalog.
 */
public enum ProductStatus {
    /**
     * Product is in draft state, not yet published
     */
    DRAFT("Draft", "Product is being prepared"),
    
    /**
     * Product is active and available for purchase
     */
    ACTIVE("Active", "Product is available for purchase"),
    
    /**
     * Product is temporarily out of stock
     */
    OUT_OF_STOCK("Out of Stock", "Product is temporarily unavailable"),
    
    /**
     * Product is inactive and not visible to customers
     */
    INACTIVE("Inactive", "Product is not available"),
    
    /**
     * Product has been discontinued
     */
    DISCONTINUED("Discontinued", "Product has been discontinued"),
    
    /**
     * Product is archived (soft deleted)
     */
    ARCHIVED("Archived", "Product has been archived");

    private final String displayName;
    private final String description;

    ProductStatus(String displayName, String description) {
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
     * Check if product is available for purchase
     */
    public boolean isAvailableForPurchase() {
        return this == ACTIVE;
    }

    /**
     * Check if product is visible to customers
     */
    public boolean isVisible() {
        return this == ACTIVE || this == OUT_OF_STOCK;
    }

    /**
     * Check if product can be edited
     */
    public boolean isEditable() {
        return this != ARCHIVED;
    }

    /**
     * Check if product is in a terminal state
     */
    public boolean isTerminal() {
        return this == ARCHIVED;
    }

    /**
     * Get next valid statuses from current status
     */
    public ProductStatus[] getNextValidStatuses() {
        return switch (this) {
            case DRAFT -> new ProductStatus[]{ACTIVE, INACTIVE, ARCHIVED};
            case ACTIVE -> new ProductStatus[]{OUT_OF_STOCK, INACTIVE, DISCONTINUED, ARCHIVED};
            case OUT_OF_STOCK -> new ProductStatus[]{ACTIVE, INACTIVE, DISCONTINUED, ARCHIVED};
            case INACTIVE -> new ProductStatus[]{ACTIVE, DISCONTINUED, ARCHIVED};
            case DISCONTINUED -> new ProductStatus[]{ARCHIVED};
            case ARCHIVED -> new ProductStatus[]{};
        };
    }

    /**
     * Check if transition to target status is valid
     */
    public boolean canTransitionTo(ProductStatus target) {
        for (ProductStatus validStatus : getNextValidStatuses()) {
            if (validStatus == target) {
                return true;
            }
        }
        return false;
    }
}
