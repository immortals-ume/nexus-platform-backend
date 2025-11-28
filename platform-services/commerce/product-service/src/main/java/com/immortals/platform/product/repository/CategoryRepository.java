package com.immortals.platform.product.repository;

import com.immortals.platform.domain.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Category entity.
 * Provides CRUD operations and custom queries for hierarchical categories.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /**
     * Find category by slug
     */
    Optional<Category> findBySlug(String slug);

    /**
     * Find all root categories (no parent, excluding soft deleted)
     */
    @Query("SELECT c FROM Category c WHERE c.parent IS NULL AND c.deletedAt IS NULL ORDER BY c.displayOrder")
    List<Category> findAllRootCategories();

    /**
     * Find all children of a parent category (excluding soft deleted)
     */
    @Query("SELECT c FROM Category c WHERE c.parent.id = :parentId AND c.deletedAt IS NULL ORDER BY c.displayOrder")
    List<Category> findByParentId(@Param("parentId") UUID parentId);

    /**
     * Find all active categories (excluding soft deleted)
     */
    @Query("SELECT c FROM Category c WHERE c.isActive = true AND c.deletedAt IS NULL ORDER BY c.displayOrder")
    List<Category> findAllActive();

    /**
     * Find all leaf categories (no children, excluding soft deleted)
     */
    @Query("SELECT c FROM Category c WHERE c.deletedAt IS NULL AND " +
           "NOT EXISTS (SELECT 1 FROM Category child WHERE child.parent.id = c.id AND child.deletedAt IS NULL)")
    List<Category> findAllLeafCategories();

    /**
     * Check if slug exists (excluding soft deleted)
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Category c " +
           "WHERE c.slug = :slug AND c.deletedAt IS NULL")
    boolean existsBySlug(@Param("slug") String slug);

    /**
     * Check if category has children (excluding soft deleted)
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Category c " +
           "WHERE c.parent.id = :categoryId AND c.deletedAt IS NULL")
    boolean hasChildren(@Param("categoryId") UUID categoryId);
}
