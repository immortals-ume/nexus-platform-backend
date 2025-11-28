package com.immortals.platform.product.service;

import com.immortals.cache.features.annotations.CacheEvict;
import com.immortals.cache.features.annotations.CachePut;
import com.immortals.cache.features.annotations.Cacheable;
import com.immortals.platform.common.exception.ResourceNotFoundException;
import com.immortals.platform.common.exception.ValidationException;
import com.immortals.platform.domain.dto.PageResponse;
import com.immortals.platform.domain.dto.product.CreateProductRequest;
import com.immortals.platform.domain.dto.product.ProductFilterRequest;
import com.immortals.platform.domain.dto.product.UpdateProductRequest;
import com.immortals.platform.domain.entity.Category;
import com.immortals.platform.domain.entity.PriceHistory;
import com.immortals.platform.domain.entity.Product;
import com.immortals.platform.domain.enums.ProductStatus;
import com.immortals.platform.messaging.event.DomainEvent;
import com.immortals.platform.messaging.publisher.EventPublisher;
import com.immortals.platform.product.annotation.ReadOnly;
import com.immortals.platform.product.annotation.WriteOnly;
import com.immortals.platform.product.config.KafkaTopicConfig;
import com.immortals.platform.product.event.ProductCreatedEvent;
import com.immortals.platform.product.event.ProductDeletedEvent;
import com.immortals.platform.product.event.ProductUpdatedEvent;
import com.immortals.platform.product.repository.CategoryRepository;
import com.immortals.platform.product.repository.PriceHistoryRepository;
import com.immortals.platform.product.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service implementation for Product operations.
 * Implements business logic for product management.
 * Follows Dependency Inversion Principle by implementing IProductService interface.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService implements IProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final EventPublisher eventPublisher;
    private final KafkaTopicConfig kafkaTopicConfig;

    /**
     * Create a new product
     * Isolation: READ_COMMITTED - Prevents dirty reads, allows concurrent access
     * Propagation: REQUIRED - Uses existing transaction or creates new one
     * Cache: Stores the created product in cache with 5 min TTL
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
        namespace = "products",
        key = "#result.id",
        ttl = 300,
        unless = "#result == null"
    )
    public Product createProduct(CreateProductRequest request) {
        log.info("Creating product with SKU: {}", request.sku());

        try {
            // Validate SKU uniqueness
            if (productRepository.existsBySku(request.sku())) {
                throw new ValidationException("Product with SKU " + request.sku() + " already exists");
            }

            // Validate barcode uniqueness if provided
            if (request.barcode() != null && !request.barcode().isBlank() 
                && productRepository.existsByBarcode(request.barcode())) {
                throw new ValidationException("Product with barcode " + request.barcode() + " already exists");
            }

            // Validate category if provided
            Category category = null;
            if (request.categoryId() != null) {
                category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));
            }

        // Build product entity
        Product product = Product.builder()
            .sku(request.sku())
            .name(request.name())
            .description(request.description())
            .barcode(request.barcode())
            .category(category)
            .basePrice(request.basePrice())
            .currentPrice(request.basePrice()) // Initially same as base price
            .currency(request.currency())
            .status(request.status())
            .imageUrls(request.imageUrls())
            .metadata(request.metadata())
            .brand(request.brand())
            .modelNumber(request.modelNumber())
            .weight(request.weight())
            .weightUnit(request.weightUnit())
            .length(request.length())
            .width(request.width())
            .height(request.height())
            .dimensionUnit(request.dimensionUnit())
            .reviewCount(0)
            .build();

            Product savedProduct = productRepository.save(product);

            // Create initial price history
            createPriceHistory(savedProduct.getId(), null, savedProduct.getBasePrice(), 
                "Initial price", "CREATION");

            // Publish ProductCreatedEvent
            publishProductCreatedEvent(savedProduct);

            log.info("Product created successfully with ID: {}", savedProduct.getId());
            return savedProduct;
            
        } catch (ValidationException | ResourceNotFoundException e) {
            // Re-throw business exceptions as-is
            throw e;
        } catch (Exception e) {
            log.error("Error creating product with SKU: {}", request.sku(), e);
            throw new com.immortals.platform.common.exception.DatabaseException(
                "Failed to create product: " + e.getMessage(), e);
        }
    }

    /**
     * Update an existing product
     * Isolation: READ_COMMITTED - Prevents dirty reads, allows concurrent access
     * Propagation: REQUIRED - Uses existing transaction or creates new one
     * Uses pessimistic write lock to prevent concurrent modifications
     * Cache: Updates the product in cache with 5 min TTL
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
        namespace = "products",
        key = "#productId",
        ttl = 300,
        unless = "#result == null"
    )
    public Product updateProduct(UUID productId, UpdateProductRequest request) {
        log.info("Updating product with ID: {} [Thread: {}]", productId, Thread.currentThread().getName());

        try {
            // Use pessimistic write lock for concurrent update protection
            Product product = productRepository.findByIdWithLock(productId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

            if (product.isDeleted()) {
                throw new ValidationException("Cannot update deleted product");
            }

        // Track price changes and status changes
        BigDecimal oldPrice = product.getCurrentPrice();
        ProductStatus oldStatus = product.getStatus();
        boolean priceChanged = false;
        boolean statusChanged = false;

        // Update fields if provided
        if (request.name() != null) {
            product.setName(request.name());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.barcode() != null && !request.barcode().equals(product.getBarcode())) {
            if (productRepository.existsByBarcode(request.barcode())) {
                throw new ValidationException("Product with barcode " + request.barcode() + " already exists");
            }
            product.setBarcode(request.barcode());
        }
        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));
            product.setCategory(category);
        }
        if (request.basePrice() != null) {
            product.setBasePrice(request.basePrice());
        }
        if (request.currentPrice() != null && !request.currentPrice().equals(oldPrice)) {
            product.setCurrentPrice(request.currentPrice());
            priceChanged = true;
        }
        if (request.currency() != null) {
            product.setCurrency(request.currency());
        }
        if (request.status() != null && !request.status().equals(oldStatus)) {
            product.setStatus(request.status());
            statusChanged = true;
        }
        if (request.imageUrls() != null) {
            product.setImageUrls(request.imageUrls());
        }
        if (request.metadata() != null) {
            product.setMetadata(request.metadata());
        }
        if (request.brand() != null) {
            product.setBrand(request.brand());
        }
        if (request.modelNumber() != null) {
            product.setModelNumber(request.modelNumber());
        }
        if (request.weight() != null) {
            product.setWeight(request.weight());
        }
        if (request.weightUnit() != null) {
            product.setWeightUnit(request.weightUnit());
        }
        if (request.length() != null) {
            product.setLength(request.length());
        }
        if (request.width() != null) {
            product.setWidth(request.width());
        }
        if (request.height() != null) {
            product.setHeight(request.height());
        }
        if (request.dimensionUnit() != null) {
            product.setDimensionUnit(request.dimensionUnit());
        }

            Product updatedProduct = productRepository.save(product);

            // Create price history if price changed
            if (priceChanged) {
                createPriceHistory(productId, oldPrice, request.currentPrice(), 
                    "Price updated", "UPDATE");
            }

            // Publish ProductUpdatedEvent
            publishProductUpdatedEvent(updatedProduct, oldPrice, oldStatus, priceChanged, statusChanged);

            log.info("Product updated successfully with ID: {}", productId);
            return updatedProduct;
            
        } catch (ValidationException | ResourceNotFoundException e) {
            // Re-throw business exceptions as-is
            throw e;
        } catch (Exception e) {
            log.error("Error updating product with ID: {}", productId, e);
            throw new com.immortals.platform.common.exception.DatabaseException(
                "Failed to update product: " + e.getMessage(), e);
        }
    }

    /**
     * Get product by ID
     * Isolation: READ_COMMITTED - Prevents dirty reads
     * Propagation: SUPPORTS - Uses existing transaction if present, non-transactional otherwise
     * Cache: Cache-aside pattern - checks L1 (Caffeine) then L2 (Redis) before DB
     */
    @Override
    @ReadOnly
    @Transactional(
        readOnly = true,
        isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED,
        propagation = org.springframework.transaction.annotation.Propagation.SUPPORTS
    )
    @Cacheable(
        namespace = "products",
        key = "#productId",
        ttl = 300,
        stampedeProtection = true,
        unless = "#result == null"
    )
    public Product getProductById(UUID productId) {
        log.debug("Fetching product with ID: {} from database", productId);
        
        try {
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

            if (product.isDeleted()) {
                throw new ResourceNotFoundException("Product not found with id: " + productId);
            }

            return product;
            
        } catch (ResourceNotFoundException e) {
            // Re-throw business exception as-is
            throw e;
        } catch (Exception e) {
            log.error("Error fetching product with ID: {}", productId, e);
            throw new com.immortals.platform.common.exception.DatabaseException(
                "Failed to fetch product: " + e.getMessage(), e);
        }
    }

    /**
     * Get product by SKU
     * Isolation: READ_COMMITTED - Prevents dirty reads
     * Propagation: SUPPORTS - Uses existing transaction if present, non-transactional otherwise
     * Cache: Cached with SKU as key
     */
    @Override
    @ReadOnly
    @Transactional(
        readOnly = true,
        isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED,
        propagation = org.springframework.transaction.annotation.Propagation.SUPPORTS
    )
    @Cacheable(
        namespace = "products",
        key = "'sku:' + #sku",
        ttl = 300,
        unless = "#result == null"
    )
    public Product getProductBySku(String sku) {
        log.debug("Fetching product with SKU: {} from database", sku);
        
        return productRepository.findBySku(sku)
            .filter(p -> !p.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));
    }

    /**
     * Get product by barcode
     * Isolation: READ_COMMITTED - Prevents dirty reads
     * Propagation: SUPPORTS - Uses existing transaction if present, non-transactional otherwise
     * Cache: Cached with barcode as key
     */
    @Override
    @ReadOnly
    @Transactional(
        readOnly = true,
        isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED,
        propagation = org.springframework.transaction.annotation.Propagation.SUPPORTS
    )
    @Cacheable(
        namespace = "products",
        key = "'barcode:' + #barcode",
        ttl = 300,
        unless = "#result == null"
    )
    public Product getProductByBarcode(String barcode) {
        log.debug("Fetching product with barcode: {} from database", barcode);
        
        return productRepository.findByBarcode(barcode)
            .filter(p -> !p.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with barcode: " + barcode));
    }

    /**
     * Soft delete product
     * Isolation: READ_COMMITTED - Prevents dirty reads, allows concurrent access
     * Propagation: REQUIRED - Uses existing transaction or creates new one
     * Cache: Evicts the product from cache on deletion
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
        namespace = "products",
        key = "#productId"
    )
    public void deleteProduct(UUID productId, String deletedBy) {
        log.info("Soft deleting product with ID: {}", productId);

        try {
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

            if (product.isDeleted()) {
                throw new ValidationException("Product is already deleted");
            }

            ProductStatus statusBeforeDeletion = product.getStatus();
            
            product.markAsDeleted(deletedBy);
            product.setStatus(ProductStatus.ARCHIVED);
            Product deletedProduct = productRepository.save(product);

            // Publish ProductDeletedEvent
            publishProductDeletedEvent(deletedProduct, statusBeforeDeletion);

            log.info("Product soft deleted successfully with ID: {}", productId);
            
        } catch (ValidationException | ResourceNotFoundException e) {
            // Re-throw business exceptions as-is
            throw e;
        } catch (Exception e) {
            log.error("Error deleting product with ID: {}", productId, e);
            throw new com.immortals.platform.common.exception.DatabaseException(
                "Failed to delete product: " + e.getMessage(), e);
        }
    }

    /**
     * Filter and sort products (no keyword search - that's handled by Search Service)
     * Isolation: READ_COMMITTED - Prevents dirty reads
     * Propagation: SUPPORTS - Uses existing transaction if present, non-transactional otherwise
     * Cache: Cached filter results with 2 min TTL
     */
    @Override
    @ReadOnly
    @Transactional(
        readOnly = true,
        isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED,
        propagation = org.springframework.transaction.annotation.Propagation.SUPPORTS
    )
    @Cacheable(
        namespace = "product-list",
        key = "'filter:' + #filterRequest.hashCode()",
        ttl = 120,
        unless = "#result == null || #result.content.isEmpty()"
    )
    public PageResponse<Product> filterProducts(ProductFilterRequest filterRequest) {
        log.debug("Filtering products with filters: {} from database", filterRequest);

        Specification<Product> spec = buildProductFilterSpecification(filterRequest);
        
        Sort sort = Sort.by(
            "DESC".equalsIgnoreCase(filterRequest.sortDirection()) 
                ? Sort.Direction.DESC 
                : Sort.Direction.ASC,
            filterRequest.sortBy()
        );

        Pageable pageable = PageRequest.of(filterRequest.page(), filterRequest.size(), sort);
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        return PageResponse.from(productPage);
    }

    /**
     * Get all products (paginated)
     * Cache: Cached product list with 2 min TTL
     */
    @Override
    @ReadOnly
    @Transactional(
        readOnly = true,
        isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED,
        propagation = org.springframework.transaction.annotation.Propagation.SUPPORTS
    )
    @Cacheable(
        namespace = "product-list",
        key = "'all:' + #page + ':' + #size + ':' + #sortBy + ':' + #sortDirection",
        ttl = 120,
        unless = "#result == null || #result.content.isEmpty()"
    )
    public PageResponse<Product> getAllProducts(int page, int size, String sortBy, String sortDirection) {
        log.debug("Fetching all products from database - page: {}, size: {}", page, size);

        Sort sort = Sort.by(
            "DESC".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC,
            sortBy
        );

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> productPage = productRepository.findAll(
            (root, query, cb) -> cb.isNull(root.get("deletedAt")),
            pageable
        );

        return PageResponse.from(productPage);
    }

    /**
     * Get products by category
     * Cache: Cached category products with 2 min TTL
     */
    @ReadOnly
    @Transactional(
        readOnly = true,
        isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED,
        propagation = org.springframework.transaction.annotation.Propagation.SUPPORTS
    )
    @Cacheable(
        namespace = "product-list",
        key = "'category:' + #categoryId + ':' + #page + ':' + #size",
        ttl = 120,
        unless = "#result == null || #result.content.isEmpty()"
    )
    public PageResponse<Product> getProductsByCategory(UUID categoryId, int page, int size) {
        log.debug("Fetching products for category: {} from database", categoryId);

        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productRepository.findByCategoryId(categoryId, pageable);

        return PageResponse.from(productPage);
    }

    /**
     * Get price history for a product
     */
    @ReadOnly
    @Transactional(
        readOnly = true,
        isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED,
        propagation = org.springframework.transaction.annotation.Propagation.SUPPORTS
    )
    public PageResponse<PriceHistory> getPriceHistory(UUID productId, int page, int size) {
        log.debug("Fetching price history for product: {}", productId);

        // Verify product exists
        getProductById(productId);

        Pageable pageable = PageRequest.of(page, size);
        Page<PriceHistory> historyPage = priceHistoryRepository.findByProductId(productId, pageable);

        return PageResponse.from(historyPage);
    }

    /**
     * Build JPA Specification for product filtering (no keyword search)
     * Note: Keyword/text search is handled by Search Service with Elasticsearch
     */
    private Specification<Product> buildProductFilterSpecification(ProductFilterRequest filterRequest) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Exclude soft deleted
            predicates.add(cb.isNull(root.get("deletedAt")));

            // Category filter
            if (filterRequest.categoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), filterRequest.categoryId()));
            }

            // Price range filter
            if (filterRequest.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("currentPrice"), filterRequest.minPrice()));
            }
            if (filterRequest.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("currentPrice"), filterRequest.maxPrice()));
            }

            // Status filter
            if (filterRequest.status() != null) {
                predicates.add(cb.equal(root.get("status"), filterRequest.status()));
            }

            // Brand filter
            if (filterRequest.brand() != null && !filterRequest.brand().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("brand")), filterRequest.brand().toLowerCase()));
            }

            // Rating filter
            if (filterRequest.minRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("averageRating"), filterRequest.minRating()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Create price history record
     */
    private void createPriceHistory(UUID productId, BigDecimal oldPrice, BigDecimal newPrice, 
                                    String reason, String changeType) {
        PriceHistory priceHistory = PriceHistory.builder()
            .productId(productId)
            .oldPrice(oldPrice)
            .newPrice(newPrice)
            .changedAt(Instant.now())
            .reason(reason)
            .changeType(changeType)
            .build();

        priceHistoryRepository.save(priceHistory);
    }

    /**
     * Publish ProductCreatedEvent to Kafka
     * Includes correlation ID propagation for distributed tracing
     */
    private void publishProductCreatedEvent(Product product) {
        try {
            ProductCreatedEvent eventPayload = ProductCreatedEvent.builder()
                .productId(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .barcode(product.getBarcode())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .basePrice(product.getBasePrice())
                .currentPrice(product.getCurrentPrice())
                .currency(product.getCurrency())
                .status(product.getStatus())
                .brand(product.getBrand())
                .modelNumber(product.getModelNumber())
                .imageUrls(product.getImageUrls())
                .metadata(product.getMetadata())
                .createdAt(product.getCreatedAt())
                .createdBy(product.getCreatedBy())
                .build();

            DomainEvent<ProductCreatedEvent> domainEvent = DomainEvent.<ProductCreatedEvent>builder()
                .eventType("ProductCreated")
                .aggregateId(product.getId().toString())
                .aggregateType("Product")
                .payload(eventPayload)
                .correlationId(getCorrelationId())
                .build();

            eventPublisher.publish(kafkaTopicConfig.getProductCreatedTopic(), domainEvent);
            
            log.info("Published ProductCreatedEvent for product ID: {} with correlation ID: {}", 
                product.getId(), domainEvent.getCorrelationId());
                
        } catch (Exception e) {
            log.error("Failed to publish ProductCreatedEvent for product ID: {}", product.getId(), e);
            // Don't throw exception - event publishing failure should not fail the transaction
            // Events will be retried via outbox pattern if implemented
        }
    }

    /**
     * Publish ProductUpdatedEvent to Kafka
     * Includes correlation ID propagation for distributed tracing
     */
    private void publishProductUpdatedEvent(Product product, BigDecimal oldPrice, 
                                           ProductStatus oldStatus, boolean priceChanged, 
                                           boolean statusChanged) {
        try {
            ProductUpdatedEvent eventPayload = ProductUpdatedEvent.builder()
                .productId(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .barcode(product.getBarcode())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .basePrice(product.getBasePrice())
                .currentPrice(product.getCurrentPrice())
                .oldPrice(oldPrice)
                .currency(product.getCurrency())
                .status(product.getStatus())
                .oldStatus(oldStatus)
                .brand(product.getBrand())
                .modelNumber(product.getModelNumber())
                .imageUrls(product.getImageUrls())
                .metadata(product.getMetadata())
                .updatedAt(product.getUpdatedAt())
                .updatedBy(product.getUpdatedBy())
                .priceChanged(priceChanged)
                .statusChanged(statusChanged)
                .build();

            DomainEvent<ProductUpdatedEvent> domainEvent = DomainEvent.<ProductUpdatedEvent>builder()
                .eventType("ProductUpdated")
                .aggregateId(product.getId().toString())
                .aggregateType("Product")
                .payload(eventPayload)
                .correlationId(getCorrelationId())
                .build();

            eventPublisher.publish(kafkaTopicConfig.getProductUpdatedTopic(), domainEvent);
            
            log.info("Published ProductUpdatedEvent for product ID: {} with correlation ID: {}", 
                product.getId(), domainEvent.getCorrelationId());
                
        } catch (Exception e) {
            log.error("Failed to publish ProductUpdatedEvent for product ID: {}", product.getId(), e);
            // Don't throw exception - event publishing failure should not fail the transaction
        }
    }

    /**
     * Publish ProductDeletedEvent to Kafka
     * Includes correlation ID propagation for distributed tracing
     */
    private void publishProductDeletedEvent(Product product, ProductStatus statusBeforeDeletion) {
        try {
            ProductDeletedEvent eventPayload = ProductDeletedEvent.builder()
                .productId(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .currentPrice(product.getCurrentPrice())
                .currency(product.getCurrency())
                .statusBeforeDeletion(statusBeforeDeletion)
                .brand(product.getBrand())
                .deletedAt(product.getDeletedAt())
                .deletedBy(product.getDeletedBy())
                .deletionReason("Soft delete via Product Service")
                .build();

            DomainEvent<ProductDeletedEvent> domainEvent = DomainEvent.<ProductDeletedEvent>builder()
                .eventType("ProductDeleted")
                .aggregateId(product.getId().toString())
                .aggregateType("Product")
                .payload(eventPayload)
                .correlationId(getCorrelationId())
                .build();

            eventPublisher.publish(kafkaTopicConfig.getProductDeletedTopic(), domainEvent);
            
            log.info("Published ProductDeletedEvent for product ID: {} with correlation ID: {}", 
                product.getId(), domainEvent.getCorrelationId());
                
        } catch (Exception e) {
            log.error("Failed to publish ProductDeletedEvent for product ID: {}", product.getId(), e);
            // Don't throw exception - event publishing failure should not fail the transaction
        }
    }

    /**
     * Get correlation ID from MDC (set by observability-starter or API Gateway)
     * Falls back to generating a new UUID if not present
     */
    private String getCorrelationId() {
        try {
            String correlationId = org.slf4j.MDC.get("correlationId");
            if (correlationId != null && !correlationId.isBlank()) {
                return correlationId;
            }
        } catch (Exception e) {
            log.debug("Could not retrieve correlation ID from MDC", e);
        }
        // Generate new correlation ID if not present
        return UUID.randomUUID().toString();
    }
}
