package com.immortals.platform.domain.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * DTO for creating a new category
 */
public record CreateCategoryRequest(
    @NotBlank(message = "Category name is required")
    @Size(max = 255, message = "Category name must not exceed 255 characters")
    String name,

    String description,

    UUID parentId,

    Integer displayOrder,

    @Size(max = 100, message = "Slug must not exceed 100 characters")
    String slug,

    String imageUrl,

    String metadata
) {}
