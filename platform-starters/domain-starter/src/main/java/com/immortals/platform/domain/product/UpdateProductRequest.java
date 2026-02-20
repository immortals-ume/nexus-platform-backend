package com.immortals.platform.domain.dto.product;

import com.immortals.platform.domain.enums.ProductStatus;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for updating an existing product
 */
public record UpdateProductRequest(
    @Size(max = 255, message = "Product name must not exceed 255 characters")
    String name,

    String description,

    @Size(max = 100, message = "Barcode must not exceed 100 characters")
    String barcode,

    UUID categoryId,

    @Positive(message = "Base price must be positive")
    BigDecimal basePrice,

    @Positive(message = "Current price must be positive")
    BigDecimal currentPrice,

    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    String currency,

    ProductStatus status,

    String[] imageUrls,

    String metadata,

    @Size(max = 100, message = "Brand must not exceed 100 characters")
    String brand,

    @Size(max = 50, message = "Model number must not exceed 50 characters")
    String modelNumber,

    BigDecimal weight,

    @Size(max = 10, message = "Weight unit must not exceed 10 characters")
    String weightUnit,

    BigDecimal length,

    BigDecimal width,

    BigDecimal height,

    @Size(max = 10, message = "Dimension unit must not exceed 10 characters")
    String dimensionUnit
) {}
