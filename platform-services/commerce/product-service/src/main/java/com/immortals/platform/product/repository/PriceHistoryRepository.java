package com.immortals.platform.product.repository;

import com.immortals.platform.domain.entity.PriceHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for PriceHistory entity.
 * Provides CRUD operations and custom queries for price audit trail.
 */
@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, UUID> {

    /**
     * Find all price history for a product ordered by change date descending
     */
    @Query("SELECT ph FROM PriceHistory ph WHERE ph.productId = :productId ORDER BY ph.changedAt DESC")
    Page<PriceHistory> findByProductId(@Param("productId") UUID productId, Pageable pageable);

    /**
     * Find price history for a product within date range
     */
    @Query("SELECT ph FROM PriceHistory ph WHERE ph.productId = :productId AND " +
           "ph.changedAt BETWEEN :startDate AND :endDate ORDER BY ph.changedAt DESC")
    List<PriceHistory> findByProductIdAndDateRange(
        @Param("productId") UUID productId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    /**
     * Find latest price change for a product
     */
    @Query("SELECT ph FROM PriceHistory ph WHERE ph.productId = :productId " +
           "ORDER BY ph.changedAt DESC LIMIT 1")
    PriceHistory findLatestByProductId(@Param("productId") UUID productId);

    /**
     * Count price changes for a product
     */
    @Query("SELECT COUNT(ph) FROM PriceHistory ph WHERE ph.productId = :productId")
    long countByProductId(@Param("productId") UUID productId);
}
