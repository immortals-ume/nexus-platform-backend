package com.immortals.platform.domain.dto.product;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * DTO for updating an existing category
 */
public record UpdateCategoryRequest(
    @Size(max = 255, message = "Category name must not exceed 255 characters")
    String name,

    String description,

    UUID parentId,

    Integer displayOrder,

    @Size(max = 100, message = "Slug must not exceed 100 characters")
    String slug,

    String imageUrl,

    Boolean isActive,

    String metadata
) {}
