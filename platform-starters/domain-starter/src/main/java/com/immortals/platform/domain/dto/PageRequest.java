package com.immortals.platform.domain.dto;

/**
 * Page request DTO using Java 21 Record.
 * Provides immutable pagination parameters with validation.
 */
public record PageRequest(
    int page,
    int size,
    String sortBy,
    String sortDirection
) {
    /**
     * Compact constructor with validation
     */
    public PageRequest {
        if (page < 0) {
            throw new IllegalArgumentException("Page number must be >= 0");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
        if (sortDirection != null && 
            !sortDirection.equalsIgnoreCase("ASC") && 
            !sortDirection.equalsIgnoreCase("DESC")) {
            throw new IllegalArgumentException("Sort direction must be ASC or DESC");
        }
    }

    /**
     * Default page request (page 0, size 20, no sorting)
     */
    public static PageRequest defaultRequest() {
        return new PageRequest(0, 20, null, null);
    }

    /**
     * Create page request with default sorting
     */
    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size, null, null);
    }

    /**
     * Create page request with sorting
     */
    public static PageRequest of(int page, int size, String sortBy, String sortDirection) {
        return new PageRequest(page, size, sortBy, sortDirection);
    }
}
