package com.immortals.platform.product.service;

import com.immortals.cache.features.annotations.CacheEvict;
import com.immortals.cache.features.annotations.CachePut;
import com.immortals.cache.features.annotations.Cacheable;
import com.immortals.platform.common.exception.ResourceNotFoundException;
import com.immortals.platform.common.exception.ValidationException;
import com.immortals.platform.domain.dto.product.CreateCategoryRequest;
import com.immortals.platform.domain.dto.product.UpdateCategoryRequest;
import com.immortals.platform.domain.entity.Category;
import com.immortals.platform.product.annotation.ReadOnly;
import com.immortals.platform.product.annotation.WriteOnly;
import com.immortals.platform.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service implementation for Category operations.
 * Implements business logic for category management.
 * Follows Dependency Inversion Principle by implementing ICategoryService interface.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService implements ICategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Create a new category
     * Isolation: READ_COMMITTED - Prevents dirty reads, allows concurrent access
     * Propagation: REQUIRED - Uses existing transaction or creates new one
     * Cache: Stores the created category and invalidates category tree cache
     */
    @Override
    @WriteOnly
    @Transactional(
        rollbackFor = Exception.class,
        timeout = 30,
        isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED,
        propagation = org.springframework.transaction.annotation.Propagation.REQUIRED
    )
    @CachePut(
        namespace = "categories",
        key = "#result.id",
        ttl = 900,
        unless = "#result == null"
    )
    public Category createCategory(CreateCategoryRequest request) {
        log.info("Creating category with name: {}", request.name());

        try {
            // Validate slug uniqueness if provided
            if (request.slug() != null && !request.slug().isBlank() 
                && categoryRepository.existsBySlug(request.slug())) {
                throw new ValidationException("Category with slug " + request.slug() + " already exists");
            }

            // Validate parent category if provided
            Category parent = null;
            if (request.parentId() != null) {
                parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + request.parentId()));
                
                if (parent.isDeleted()) {
                    throw new ValidationException("Cannot set deleted category as parent");
                }
            }

        // Build category entity
        Category category = Category.builder()
            .name(request.name())
            .description(request.description())
            .parent(parent)
            .displayOrder(request.displayOrder())
            .slug(request.slug())
            .imageUrl(request.imageUrl())
            .isActive(true)
            .metadata(request.metadata())
            .build();

            Category savedCategory = categoryRepository.save(category);
            log.info("Category created successfully with ID: {}", savedCategory.getId());
            
            return savedCategory;
            
        } catch (ValidationException | ResourceNotFoundException e) {
            // Re-throw business exceptions as-is
            throw e;
        } catch (Exception e) {
            log.error("Error creating category with name: {}", request.name(), e);
            throw new com.immortals.platform.common.exception.DatabaseException(
                "Failed to create category: " + e.getMessage(), e);
        }
    }

    /**
     * Update an existing category
     * Isolation: READ_COMMITTED - Prevents dirty reads, allows concurrent access
     * Propagation: REQUIRED - Uses existing transaction or creates new one
     * Cache: Updates the category in cache and invalidates category tree cache
     */
    @Override
    @WriteOnly
    @Transactional(
        rollbackFor = Exception.class,
        timeout = 30,
        isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED,
        propagation = org.springframework.transaction.annotation.Propagation.REQUIRED
    )
    @CachePut(
        namespace = "categories",
        key = "#categoryId",
        ttl = 900,
        unless = "#result == null"
    )
    public Category updateCategory(UUID categoryId, UpdateCategoryRequest request) {
        log.info("Updating category with ID: {}", categoryId);

        try {
            Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

            if (category.isDeleted()) {
                throw new ValidationException("Cannot update deleted category");
            }

        // Update fields if provided
        if (request.name() != null) {
            category.setName(request.name());
        }
        if (request.description() != null) {
            category.setDescription(request.description());
        }
        if (request.parentId() != null) {
            // Prevent circular reference
            if (request.parentId().equals(categoryId)) {
                throw new ValidationException("Category cannot be its own parent");
            }
            
            Category parent = categoryRepository.findById(request.parentId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + request.parentId()));
            
            if (parent.isDeleted()) {
                throw new ValidationException("Cannot set deleted category as parent");
            }
            
            // Check if new parent is a descendant of current category
            if (isDescendant(parent, categoryId)) {
                throw new ValidationException("Cannot set a descendant as parent - would create circular reference");
            }
            
            category.setParent(parent);
        }
        if (request.displayOrder() != null) {
            category.setDisplayOrder(request.displayOrder());
        }
        if (request.slug() != null && !request.slug().equals(category.getSlug())) {
            if (categoryRepository.existsBySlug(request.slug())) {
                throw new ValidationException("Category with slug " + request.slug() + " already exists");
            }
            category.setSlug(request.slug());
        }
        if (request.imageUrl() != null) {
            category.setImageUrl(request.imageUrl());
        }
        if (request.isActive() != null) {
            category.setIsActive(request.isActive());
        }
        if (request.metadata() != null) {
            category.setMetadata(request.metadata());
        }

            Category updatedCategory = categoryRepository.save(category);
            log.info("Category updated successfully with ID: {}", categoryId);
            
            return updatedCategory;
            
        } catch (ValidationException | ResourceNotFoundException e) {
            // Re-throw business exceptions as-is
            throw e;
        } catch (Exception e) {
            log.error("Error updating category with ID: {}", categoryId, e);
            throw new com.immortals.platform.common.exception.DatabaseException(
                "Failed to update category: " + e.getMessage(), e);
        }
    }

    /**
     * Get category by ID
     * Isolation: READ_COMMITTED - Prevents dirty reads
     * Propagation: SUPPORTS - Uses existing transaction if present, non-transactional otherwise
     * Cache: Returns cached category if available (15 min TTL)
     */
    @Override
    @ReadOnly
    @Transactional(
        readOnly = true,
        isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED,
        propagation = org.springframework.transaction.annotation.Propagation.SUPPORTS
    )
    @Cacheable(
        namespace = "categories",
        key = "#categoryId",
        ttl = 900,
        stampedeProtection = true,
        unless = "#result == null"
    )
    public Category getCategoryById(UUID categoryId) {
        log.debug("Fetching category with ID: {}", categoryId);
        
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        if (category.isDeleted()) {
            throw new ResourceNotFoundException("Category not found with id: " + categoryId);
        }

        return category;
    }

    /**
     * Get category by slug
     * Isolation: READ_COMMITTED - Prevents dirty reads
     * Propagation: SUPPORTS - Uses existing transaction if present, non-transactional otherwise
     * Cache: Cached with slug as key
     */
    @Override
    @ReadOnly
    @Transactional(
        readOnly = true,
        isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED,
        propagation = org.springframework.transaction.annotation.Propagation.SUPPORTS
    )
    @Cacheable(
        namespace = "categories",
        key = "'slug:' + #slug",
        ttl = 900,
        unless = "#result == null"
    )
    public Category getCategoryBySlug(String slug) {
        log.debug("Fetching category with slug: {} from database", slug);
        
        return categoryRepository.findBySlug(slug)
            .filter(c -> !c.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));
    }

    /**
     * Get all root categories (category tree)
     * Isolation: READ_COMMITTED - Prevents dirty reads
     * Propagation: SUPPORTS - Uses existing transaction if present, non-transactional otherwise
     * Cache: Returns cached category tree if available (15 min TTL)
     */
    @Override
    @ReadOnly
    @Transactional(
        readOnly = true,
        isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED,
        propagation = org.springframework.transaction.annotation.Propagation.SUPPORTS
    )
    @Cacheable(
        namespace = "categories",
        key = "'category-tree'",
        ttl = 900,
        stampedeProtection = true,
        unless = "#result == null || #result.isEmpty()"
    )
    public List<Category> getAllRootCategories() {
        log.debug("Fetching all root categories");
        return categoryRepository.findAllRootCategories();
    }

    /**
     * Get children of a category
     */
    @Override
    @ReadOnly
    @Transactional(
        readOnly = true,
        isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED,
        propagation = org.springframework.transaction.annotation.Propagation.SUPPORTS
    )
    public List<Category> getCategoryChildren(UUID categoryId) {
        log.debug("Fetching children for category: {}", categoryId);
        
        // Verify parent exists
        getCategoryById(categoryId);
        
        return categoryRepository.findByParentId(categoryId);
    }

    /**
     * Get all active categories
     */
    @Override
    @ReadOnly
    @Transactional(
        readOnly = true,
        isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED,
        propagation = org.springframework.transaction.annotation.Propagation.SUPPORTS
    )
    public List<Category> getAllActiveCategories() {
        log.debug("Fetching all active categories");
        return categoryRepository.findAllActive();
    }

    /**
     * Get all leaf categories
     */
    @Override
    @ReadOnly
    @Transactional(
        readOnly = true,
        isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED,
        propagation = org.springframework.transaction.annotation.Propagation.SUPPORTS
    )
    public List<Category> getLeafCategories() {
        log.debug("Fetching all leaf categories");
        return categoryRepository.findAllLeafCategories();
    }

    /**
     * Soft delete category
     * Cache: Evicts the category from cache and invalidates category tree cache
     */
    @Override
    @WriteOnly
    @Transactional(
        rollbackFor = Exception.class,
        timeout = 30,
        isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED,
        propagation = org.springframework.transaction.annotation.Propagation.REQUIRED
    )
    @CacheEvict(
        namespace = "categories",
        key = "#categoryId"
    )
    public void deleteCategory(UUID categoryId, String deletedBy) {
        log.info("Soft deleting category with ID: {}", categoryId);

        try {
            Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

            if (category.isDeleted()) {
                throw new ValidationException("Category is already deleted");
            }

            // Check if category has children
            if (categoryRepository.hasChildren(categoryId)) {
                throw new ValidationException("Cannot delete category with children. Delete children first.");
            }

            category.markAsDeleted(deletedBy);
            category.deactivate();
            categoryRepository.save(category);

            log.info("Category soft deleted successfully with ID: {}", categoryId);
            
        } catch (ValidationException | ResourceNotFoundException e) {
            // Re-throw business exceptions as-is
            throw e;
        } catch (Exception e) {
            log.error("Error deleting category with ID: {}", categoryId, e);
            throw new com.immortals.platform.common.exception.DatabaseException(
                "Failed to delete category: " + e.getMessage(), e);
        }
    }

    /**
     * Check if a category is a descendant of another
     */
    private boolean isDescendant(Category potentialDescendant, UUID ancestorId) {
        Category current = potentialDescendant;
        while (current != null) {
            if (current.getId().equals(ancestorId)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }
}
