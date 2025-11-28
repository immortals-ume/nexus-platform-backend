package com.immortals.platform.product.repository;

import com.immortals.platform.domain.entity.Product;
import com.immortals.platform.domain.enums.ProductStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Product entity.
 * Provides CRUD operations and custom queries.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    /**
     * Find product by SKU
     */
    Optional<Product> findBySku(String sku);

    /**
     * Find product by barcode
     */
    Optional<Product> findByBarcode(String barcode);

    /**
     * Find products by status (excluding soft deleted)
     */
    @Query("SELECT p FROM Product p WHERE p.status = :status AND p.deletedAt IS NULL")
    Page<Product> findByStatus(@Param("status") ProductStatus status, Pageable pageable);

    /**
     * Find products by category (excluding soft deleted)
     */
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.deletedAt IS NULL")
    Page<Product> findByCategoryId(@Param("categoryId") UUID categoryId, Pageable pageable);

    /**
     * Search products by keyword in name or description (excluding soft deleted)
     */
    @Query("SELECT p FROM Product p WHERE " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "p.deletedAt IS NULL")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Find products by price range (excluding soft deleted)
     */
    @Query("SELECT p FROM Product p WHERE " +
           "p.currentPrice BETWEEN :minPrice AND :maxPrice AND " +
           "p.deletedAt IS NULL")
    Page<Product> findByPriceRange(
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable
    );

    /**
     * Find products by brand (excluding soft deleted)
     */
    @Query("SELECT p FROM Product p WHERE LOWER(p.brand) = LOWER(:brand) AND p.deletedAt IS NULL")
    Page<Product> findByBrand(@Param("brand") String brand, Pageable pageable);

    /**
     * Find products with rating above threshold (excluding soft deleted)
     */
    @Query("SELECT p FROM Product p WHERE p.averageRating >= :minRating AND p.deletedAt IS NULL")
    Page<Product> findByMinRating(@Param("minRating") BigDecimal minRating, Pageable pageable);

    /**
     * Check if SKU exists (excluding soft deleted)
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Product p " +
           "WHERE p.sku = :sku AND p.deletedAt IS NULL")
    boolean existsBySku(@Param("sku") String sku);

    /**
     * Check if barcode exists (excluding soft deleted)
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Product p " +
           "WHERE p.barcode = :barcode AND p.deletedAt IS NULL")
    boolean existsByBarcode(@Param("barcode") String barcode);

    /**
     * Find all active products (excluding soft deleted)
     */
    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' AND p.deletedAt IS NULL")
    Page<Product> findAllActive(Pageable pageable);

    /**
     * Find product by ID with pessimistic write lock for concurrent updates
     * Prevents other transactions from reading, updating, or deleting this row
     */
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.deletedAt IS NULL")
    java.util.Optional<Product> findByIdWithLock(@Param("id") UUID id);

    /**
     * Find product by ID with pessimistic read lock for consistent reads
     * Allows other transactions to read but not modify
     */
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_READ)
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.deletedAt IS NULL")
    java.util.Optional<Product> findByIdWithReadLock(@Param("id") UUID id);
}
