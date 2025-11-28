package com.immortals.platform.product.service;

import com.immortals.platform.domain.dto.product.CreateCategoryRequest;
import com.immortals.platform.domain.dto.product.UpdateCategoryRequest;
import com.immortals.platform.domain.entity.Category;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for Category operations.
 * Defines the contract for category management business logic.
 * Follows Dependency Inversion Principle - depend on abstraction, not implementation.
 */
public interface ICategoryService {

    /**
     * Create a new category
     * @param request Category creation request
     * @return Created category
     */
    Category createCategory(CreateCategoryRequest request);

    /**
     * Update an existing category
     * @param categoryId Category ID
     * @param request Category update request
     * @return Updated category
     */
    Category updateCategory(UUID categoryId, UpdateCategoryRequest request);

    /**
     * Get category by ID
     * @param categoryId Category ID
     * @return Category entity
     */
    Category getCategoryById(UUID categoryId);

    /**
     * Get category by slug
     * @param slug Category slug
     * @return Category entity
     */
    Category getCategoryBySlug(String slug);

    /**
     * Get all root categories (categories without parent)
     * @return List of root categories
     */
    List<Category> getAllRootCategories();

    /**
     * Get children of a category
     * @param categoryId Parent category ID
     * @return List of child categories
     */
    List<Category> getCategoryChildren(UUID categoryId);

    /**
     * Get all active categories
     * @return List of active categories
     */
    List<Category> getAllActiveCategories();

    /**
     * Get all leaf categories (categories without children)
     * @return List of leaf categories
     */
    List<Category> getLeafCategories();

    /**
     * Soft delete category
     * @param categoryId Category ID
     * @param deletedBy User performing deletion
     */
    void deleteCategory(UUID categoryId, String deletedBy);
}
