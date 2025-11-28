package com.immortals.platform.product.service;

import com.immortals.platform.domain.dto.PageResponse;
import com.immortals.platform.domain.dto.product.CreateProductRequest;
import com.immortals.platform.domain.dto.product.ProductFilterRequest;
import com.immortals.platform.domain.dto.product.UpdateProductRequest;
import com.immortals.platform.domain.entity.PriceHistory;
import com.immortals.platform.domain.entity.Product;

import java.util.UUID;

/**
 * Service interface for Product operations.
 * Defines the contract for product management business logic.
 * Follows Dependency Inversion Principle - depend on abstraction, not implementation.
 */
public interface IProductService {

    /**
     * Create a new product
     * @param request Product creation request
     * @return Created product
     */
    Product createProduct(CreateProductRequest request);

    /**
     * Update an existing product
     * @param productId Product ID
     * @param request Product update request
     * @return Updated product
     */
    Product updateProduct(UUID productId, UpdateProductRequest request);

    /**
     * Get product by ID
     * @param productId Product ID
     * @return Product entity
     */
    Product getProductById(UUID productId);

    /**
     * Get product by SKU
     * @param sku Product SKU
     * @return Product entity
     */
    Product getProductBySku(String sku);

    /**
     * Get product by barcode
     * @param barcode Product barcode
     * @return Product entity
     */
    Product getProductByBarcode(String barcode);

    /**
     * Soft delete product
     * @param productId Product ID
     * @param deletedBy User performing deletion
     */
    void deleteProduct(UUID productId, String deletedBy);

    /**
     * Filter and sort products (no keyword search - handled by Search Service)
     * @param filterRequest Filter criteria
     * @return Paginated product results
     */
    PageResponse<Product> filterProducts(ProductFilterRequest filterRequest);

    /**
     * Get all products (paginated)
     * @param page Page number
     * @param size Page size
     * @param sortBy Sort field
     * @param sortDirection Sort direction
     * @return Paginated product results
     */
    PageResponse<Product> getAllProducts(int page, int size, String sortBy, String sortDirection);

    /**
     * Get products by category
     * @param categoryId Category ID
     * @param page Page number
     * @param size Page size
     * @return Paginated product results
     */
    PageResponse<Product> getProductsByCategory(UUID categoryId, int page, int size);

    /**
     * Get price history for a product
     * @param productId Product ID
     * @param page Page number
     * @param size Page size
     * @return Paginated price history
     */
    PageResponse<PriceHistory> getPriceHistory(UUID productId, int page, int size);
}
