package com.immortals.platform.domain.dto.product;

import com.immortals.platform.domain.enums.ProductStatus;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for filtering and sorting products.
 * Note: Keyword/text search functionality is handled by the Search Service (Elasticsearch).
 * This DTO is for structured filtering only (category, price, status, etc.)
 */
public record ProductFilterRequest(
    UUID categoryId,
    
    BigDecimal minPrice,
    
    BigDecimal maxPrice,
    
    ProductStatus status,
    
    String brand,
    
    BigDecimal minRating,
    
    @Min(value = 0, message = "Page must be non-negative")
    int page,
    
    @Min(value = 1, message = "Size must be at least 1")
    int size,
    
    String sortBy,
    
    String sortDirection
) {
    public ProductFilterRequest {
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (sortBy == null || sortBy.isBlank()) sortBy = "createdAt";
        if (sortDirection == null || sortDirection.isBlank()) sortDirection = "DESC";
    }
}
