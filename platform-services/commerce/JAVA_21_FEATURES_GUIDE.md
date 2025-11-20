# Java 21 Features Implementation Guide

This document provides guidelines and examples for using Java 21 features across all commerce services.

## Overview

All commerce services are configured to use Java 21 with the following features enabled:
- Virtual Threads for async operations
- Records for DTOs and immutable data
- Pattern Matching for instanceof
- Switch Expressions
- Text Blocks for SQL/JSON
- Sealed Classes for domain hierarchies

## 1. Virtual Threads

### Configuration

Virtual threads are enabled in `application.yml`:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

### Usage in @Async Methods

```java
@Service
public class ProductService {
    
    @Async
    public CompletableFuture<Product> findProductAsync(UUID productId) {
        // This method runs on a virtual thread
        return CompletableFuture.completedFuture(productRepository.findById(productId));
    }
}
```

### Usage in Scheduled Tasks

```java
@Component
public class OutboxProcessor {
    
    @Scheduled(fixedDelay = 5000)
    public void processOutboxEvents() {
        // This scheduled task runs on a virtual thread
        List<OutboxEvent> events = outboxRepository.findPendingEvents();
        events.forEach(this::publishEvent);
    }
}
```

### Usage with ExecutorService

```java
@Configuration
public class AsyncConfig {
    
    @Bean
    public Executor taskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
```

## 2. Records for DTOs

### Request/Response DTOs

```java
// Product DTOs
public record CreateProductRequest(
    String sku,
    String name,
    String description,
    UUID categoryId,
    BigDecimal price,
    String currency
) {
    // Compact constructor for validation
    public CreateProductRequest {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU cannot be blank");
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
    }
}

public record ProductResponse(
    UUID id,
    String sku,
    String name,
    String description,
    UUID categoryId,
    BigDecimal basePrice,
    BigDecimal currentPrice,
    String currency,
    String status,
    BigDecimal averageRating,
    Integer reviewCount,
    List<String> imageUrls,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

// Order DTOs
public record CreateOrderRequest(
    UUID customerId,
    List<OrderItemRequest> items,
    AddressDTO shippingAddress,
    AddressDTO billingAddress,
    String couponCode
) {}

public record OrderItemRequest(
    UUID productId,
    String sku,
    Integer quantity
) {}

public record OrderResponse(
    UUID id,
    String orderNumber,
    UUID customerId,
    String status,
    BigDecimal subtotal,
    BigDecimal tax,
    BigDecimal shippingCost,
    BigDecimal discount,
    BigDecimal total,
    String currency,
    LocalDateTime createdAt
) {}
```

### Pagination DTOs

```java
public record PageRequest(
    int page,
    int size,
    String sortBy,
    String sortDirection
) {
    public PageRequest {
        if (page < 0) throw new IllegalArgumentException("Page must be >= 0");
        if (size <= 0 || size > 100) throw new IllegalArgumentException("Size must be 1-100");
    }
}

public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {}
```

### API Response Wrappers

```java
public record ApiResponse<T>(
    boolean success,
    T data,
    String message,
    LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, LocalDateTime.now());
    }
    
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, LocalDateTime.now());
    }
}

public record ErrorResponse(
    String error,
    String message,
    int status,
    String path,
    LocalDateTime timestamp,
    List<ValidationError> validationErrors
) {}

public record ValidationError(
    String field,
    String message,
    Object rejectedValue
) {}
```

### Value Objects

```java
public record Money(
    BigDecimal amount,
    String currency
) {
    public Money {
        if (amount == null) throw new IllegalArgumentException("Amount cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        if (currency == null || currency.length() != 3) {
            throw new IllegalArgumentException("Currency must be 3-letter code");
        }
    }
    
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
}

public record Address(
    String street,
    String city,
    String state,
    String zipCode,
    String country
) {
    public Address {
        if (street == null || street.isBlank()) {
            throw new IllegalArgumentException("Street cannot be blank");
        }
        // Additional validation...
    }
}

public record PhoneNumber(String value) {
    public PhoneNumber {
        if (value == null || !value.matches("\\+?[0-9]{10,15}")) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
    }
}
```

## 3. Pattern Matching for instanceof

### Before (Java 17)

```java
public BigDecimal calculateDiscount(Promotion promotion, BigDecimal amount) {
    if (promotion instanceof PercentagePromotion) {
        PercentagePromotion p = (PercentagePromotion) promotion;
        return amount.multiply(p.getPercentage()).divide(BigDecimal.valueOf(100));
    } else if (promotion instanceof FixedAmountPromotion) {
        FixedAmountPromotion p = (FixedAmountPromotion) promotion;
        return p.getAmount();
    }
    return BigDecimal.ZERO;
}
```

### After (Java 21)

```java
public BigDecimal calculateDiscount(Promotion promotion, BigDecimal amount) {
    if (promotion instanceof PercentagePromotion p) {
        return amount.multiply(p.percentage()).divide(BigDecimal.valueOf(100));
    } else if (promotion instanceof FixedAmountPromotion p) {
        return p.amount();
    }
    return BigDecimal.ZERO;
}
```

### With Null Checks

```java
public String formatPaymentMethod(Object paymentMethod) {
    if (paymentMethod instanceof CreditCard card && card.isValid()) {
        return "Card ending in " + card.lastFour();
    } else if (paymentMethod instanceof PayPal paypal && paypal.email() != null) {
        return "PayPal: " + paypal.email();
    }
    return "Unknown payment method";
}
```

## 4. Switch Expressions

### Order Status Transitions

```java
public OrderStatus getNextStatus(OrderStatus current) {
    return switch (current) {
        case PENDING -> OrderStatus.CONFIRMED;
        case CONFIRMED -> OrderStatus.PAID;
        case PAID -> OrderStatus.SHIPPED;
        case SHIPPED -> OrderStatus.DELIVERED;
        case DELIVERED, CANCELLED, FAILED -> current; // Terminal states
    };
}
```

### HTTP Status Mapping

```java
public HttpStatus mapToHttpStatus(BusinessException ex) {
    return switch (ex) {
        case ResourceNotFoundException e -> HttpStatus.NOT_FOUND;
        case ValidationException e -> HttpStatus.BAD_REQUEST;
        case BusinessRuleViolationException e -> HttpStatus.UNPROCESSABLE_ENTITY;
        case UnauthorizedException e -> HttpStatus.UNAUTHORIZED;
        case ForbiddenException e -> HttpStatus.FORBIDDEN;
        default -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
}
```

### Payment Processing

```java
public PaymentResult processPayment(PaymentMethod method, BigDecimal amount) {
    return switch (method) {
        case CreditCard card -> processCreditCard(card, amount);
        case DebitCard card -> processDebitCard(card, amount);
        case PayPal paypal -> processPayPal(paypal, amount);
        case BankTransfer transfer -> processBankTransfer(transfer, amount);
        case null -> throw new IllegalArgumentException("Payment method cannot be null");
    };
}
```

### Discount Calculation

```java
public BigDecimal calculateDiscount(Promotion promotion, BigDecimal amount) {
    return switch (promotion) {
        case PercentagePromotion p -> 
            amount.multiply(p.percentage()).divide(BigDecimal.valueOf(100));
        case FixedAmountPromotion p -> 
            p.amount().min(amount);
        case BuyOneGetOnePromotion p -> 
            amount.divide(BigDecimal.valueOf(2));
        case null -> BigDecimal.ZERO;
    };
}
```

## 5. Text Blocks for SQL Queries

### Repository Queries

```java
@Repository
public class ProductRepository {
    
    private static final String SEARCH_PRODUCTS_QUERY = """
        SELECT p.* FROM products p
        WHERE p.status = 'ACTIVE'
          AND (p.name ILIKE :keyword OR p.description ILIKE :keyword)
          AND (:categoryId IS NULL OR p.category_id = :categoryId)
          AND (:minPrice IS NULL OR p.current_price >= :minPrice)
          AND (:maxPrice IS NULL OR p.current_price <= :maxPrice)
        ORDER BY 
          CASE WHEN :sortBy = 'price' THEN p.current_price END ASC,
          CASE WHEN :sortBy = 'name' THEN p.name END ASC,
          CASE WHEN :sortBy = 'date' THEN p.created_at END DESC
        LIMIT :limit OFFSET :offset
        """;
    
    private static final String UPDATE_INVENTORY_QUERY = """
        UPDATE inventory
        SET available_quantity = available_quantity - :quantity,
            reserved_quantity = reserved_quantity + :quantity,
            version = version + 1,
            updated_at = CURRENT_TIMESTAMP
        WHERE product_id = :productId
          AND available_quantity >= :quantity
          AND version = :expectedVersion
        RETURNING *
        """;
    
    private static final String SAGA_COMPENSATION_QUERY = """
        WITH completed_steps AS (
          SELECT step_name, step_data
          FROM saga_steps
          WHERE saga_id = :sagaId
            AND status = 'COMPLETED'
          ORDER BY step_order DESC
        )
        UPDATE saga_steps
        SET status = 'COMPENSATING',
            compensated_at = CURRENT_TIMESTAMP
        WHERE saga_id = :sagaId
          AND step_name IN (SELECT step_name FROM completed_steps)
        RETURNING *
        """;
}
```

### Flyway Migrations

```java
// V1__create_products_table.sql
public class V1__CreateProductsTable {
    public static final String SQL = """
        CREATE TABLE products (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            sku VARCHAR(100) UNIQUE NOT NULL,
            name VARCHAR(255) NOT NULL,
            description TEXT,
            category_id UUID,
            base_price DECIMAL(10,2) NOT NULL,
            current_price DECIMAL(10,2) NOT NULL,
            currency VARCHAR(3) DEFAULT 'USD',
            status VARCHAR(20) NOT NULL,
            average_rating DECIMAL(3,2),
            review_count INTEGER DEFAULT 0,
            image_urls TEXT[],
            metadata JSONB,
            version INTEGER NOT NULL DEFAULT 0,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            created_by VARCHAR(100),
            updated_by VARCHAR(100),
            CONSTRAINT fk_category FOREIGN KEY (category_id) 
                REFERENCES categories(id) ON DELETE SET NULL,
            CONSTRAINT chk_price_positive CHECK (base_price > 0 AND current_price > 0),
            CONSTRAINT chk_rating_range CHECK (average_rating >= 0 AND average_rating <= 5)
        );
        
        CREATE INDEX idx_products_category ON products(category_id);
        CREATE INDEX idx_products_status ON products(status);
        CREATE INDEX idx_products_price ON products(current_price);
        CREATE INDEX idx_products_created_at ON products(created_at DESC);
        CREATE INDEX idx_products_name_trgm ON products USING gin(name gin_trgm_ops);
        """;
}
```

## 6. Text Blocks for JSON Templates

### Event Payloads

```java
@Service
public class EventPublisher {
    
    public String createProductEventPayload(Product product) {
        return """
            {
              "eventId": "%s",
              "eventType": "ProductCreated",
              "occurredAt": "%s",
              "correlationId": "%s",
              "version": 1,
              "payload": {
                "productId": "%s",
                "sku": "%s",
                "name": "%s",
                "categoryId": "%s",
                "price": %s,
                "currency": "%s",
                "status": "%s"
              }
            }
            """.formatted(
                UUID.randomUUID(),
                LocalDateTime.now(),
                MDC.get("correlationId"),
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getCategoryId(),
                product.getCurrentPrice(),
                product.getCurrency(),
                product.getStatus()
            );
    }
}
```

### API Documentation Examples

```java
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    
    @Operation(
        summary = "Create a new product",
        description = """
            Creates a new product in the catalog with the provided details.
            
            Requirements:
            - SKU must be unique
            - Price must be positive
            - Category must exist
            
            Returns:
            - 201 Created with product details
            - 400 Bad Request if validation fails
            - 409 Conflict if SKU already exists
            """
    )
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
        @RequestBody CreateProductRequest request
    ) {
        // Implementation
    }
}
```

## 7. Sealed Classes for Domain Hierarchies

### Order Status Hierarchy

```java
public sealed interface OrderStatus 
    permits PendingOrder, ConfirmedOrder, PaidOrder, ShippedOrder, 
            DeliveredOrder, CancelledOrder, FailedOrder {
    
    String name();
    boolean isTerminal();
    boolean canTransitionTo(OrderStatus next);
}

public record PendingOrder() implements OrderStatus {
    @Override public String name() { return "PENDING"; }
    @Override public boolean isTerminal() { return false; }
    @Override public boolean canTransitionTo(OrderStatus next) {
        return next instanceof ConfirmedOrder || next instanceof CancelledOrder;
    }
}

public record ConfirmedOrder() implements OrderStatus {
    @Override public String name() { return "CONFIRMED"; }
    @Override public boolean isTerminal() { return false; }
    @Override public boolean canTransitionTo(OrderStatus next) {
        return next instanceof PaidOrder || next instanceof CancelledOrder;
    }
}

public record PaidOrder() implements OrderStatus {
    @Override public String name() { return "PAID"; }
    @Override public boolean isTerminal() { return false; }
    @Override public boolean canTransitionTo(OrderStatus next) {
        return next instanceof ShippedOrder || next instanceof CancelledOrder;
    }
}

public record ShippedOrder() implements OrderStatus {
    @Override public String name() { return "SHIPPED"; }
    @Override public boolean isTerminal() { return false; }
    @Override public boolean canTransitionTo(OrderStatus next) {
        return next instanceof DeliveredOrder;
    }
}

public record DeliveredOrder() implements OrderStatus {
    @Override public String name() { return "DELIVERED"; }
    @Override public boolean isTerminal() { return true; }
    @Override public boolean canTransitionTo(OrderStatus next) { return false; }
}

public record CancelledOrder() implements OrderStatus {
    @Override public String name() { return "CANCELLED"; }
    @Override public boolean isTerminal() { return true; }
    @Override public boolean canTransitionTo(OrderStatus next) { return false; }
}

public record FailedOrder() implements OrderStatus {
    @Override public String name() { return "FAILED"; }
    @Override public boolean isTerminal() { return true; }
    @Override public boolean canTransitionTo(OrderStatus next) { return false; }
}
```

### Payment Status Hierarchy

```java
public sealed interface PaymentStatus 
    permits PendingPayment, ProcessingPayment, CompletedPayment, 
            FailedPayment, RefundedPayment {
    
    String name();
    boolean isTerminal();
}

public record PendingPayment() implements PaymentStatus {
    @Override public String name() { return "PENDING"; }
    @Override public boolean isTerminal() { return false; }
}

public record ProcessingPayment() implements PaymentStatus {
    @Override public String name() { return "PROCESSING"; }
    @Override public boolean isTerminal() { return false; }
}

public record CompletedPayment() implements PaymentStatus {
    @Override public String name() { return "COMPLETED"; }
    @Override public boolean isTerminal() { return true; }
}

public record FailedPayment(String reason) implements PaymentStatus {
    @Override public String name() { return "FAILED"; }
    @Override public boolean isTerminal() { return true; }
}

public record RefundedPayment(BigDecimal amount) implements PaymentStatus {
    @Override public String name() { return "REFUNDED"; }
    @Override public boolean isTerminal() { return true; }
}
```

### Product Status Hierarchy

```java
public sealed interface ProductStatus 
    permits ActiveProduct, InactiveProduct, OutOfStockProduct, DiscontinuedProduct {
    
    String name();
    boolean isAvailableForPurchase();
}

public record ActiveProduct() implements ProductStatus {
    @Override public String name() { return "ACTIVE"; }
    @Override public boolean isAvailableForPurchase() { return true; }
}

public record InactiveProduct() implements ProductStatus {
    @Override public String name() { return "INACTIVE"; }
    @Override public boolean isAvailableForPurchase() { return false; }
}

public record OutOfStockProduct() implements ProductStatus {
    @Override public String name() { return "OUT_OF_STOCK"; }
    @Override public boolean isAvailableForPurchase() { return false; }
}

public record DiscontinuedProduct() implements ProductStatus {
    @Override public String name() { return "DISCONTINUED"; }
    @Override public boolean isAvailableForPurchase() { return false; }
}
```

### Shipment Status Hierarchy

```java
public sealed interface ShipmentStatus 
    permits PendingShipment, DispatchedShipment, InTransitShipment, 
            DeliveredShipment, ReturnedShipment {
    
    String name();
    boolean isInProgress();
}

public record PendingShipment() implements ShipmentStatus {
    @Override public String name() { return "PENDING"; }
    @Override public boolean isInProgress() { return false; }
}

public record DispatchedShipment(LocalDateTime dispatchedAt) implements ShipmentStatus {
    @Override public String name() { return "DISPATCHED"; }
    @Override public boolean isInProgress() { return true; }
}

public record InTransitShipment(String currentLocation) implements ShipmentStatus {
    @Override public String name() { return "IN_TRANSIT"; }
    @Override public boolean isInProgress() { return true; }
}

public record DeliveredShipment(LocalDateTime deliveredAt) implements ShipmentStatus {
    @Override public String name() { return "DELIVERED"; }
    @Override public boolean isInProgress() { return false; }
}

public record ReturnedShipment(String reason) implements ShipmentStatus {
    @Override public String name() { return "RETURNED"; }
    @Override public boolean isInProgress() { return false; }
}
```

## 8. Combined Example: Order Service

```java
@Service
public class OrderService {
    
    // Virtual threads for async operations
    @Async
    public CompletableFuture<OrderResponse> createOrderAsync(CreateOrderRequest request) {
        return CompletableFuture.completedFuture(createOrder(request));
    }
    
    // Records for DTOs
    public OrderResponse createOrder(CreateOrderRequest request) {
        // Pattern matching with instanceof
        if (request instanceof CreateOrderRequest req && req.items() != null) {
            Order order = buildOrder(req);
            
            // Switch expression for status handling
            String statusMessage = switch (order.getStatus()) {
                case "PENDING" -> "Order created, awaiting confirmation";
                case "CONFIRMED" -> "Order confirmed, processing payment";
                case "PAID" -> "Payment successful, preparing shipment";
                default -> "Order status: " + order.getStatus();
            };
            
            // Text block for event payload
            String eventPayload = """
                {
                  "orderId": "%s",
                  "customerId": "%s",
                  "status": "%s",
                  "total": %s,
                  "message": "%s"
                }
                """.formatted(
                    order.getId(),
                    order.getCustomerId(),
                    order.getStatus(),
                    order.getTotal(),
                    statusMessage
                );
            
            publishEvent(eventPayload);
            
            return mapToResponse(order);
        }
        
        throw new ValidationException("Invalid order request");
    }
    
    // Sealed class usage
    public boolean canCancelOrder(OrderStatus status) {
        return switch (status) {
            case PendingOrder p, ConfirmedOrder c -> true;
            case PaidOrder p, ShippedOrder s, DeliveredOrder d, 
                 CancelledOrder c, FailedOrder f -> false;
        };
    }
}
```

## 9. Application Configuration

### application.yml

```yaml
spring:
  application:
    name: ${SERVICE_NAME:product-service}
  
  threads:
    virtual:
      enabled: true
  
  task:
    execution:
      pool:
        core-size: 0
        max-size: 100
        queue-capacity: 0
        keep-alive: 60s
    scheduling:
      pool:
        size: 5

server:
  tomcat:
    threads:
      max: 200
      min-spare: 10
```

## 10. Best Practices

### DO:
- ✅ Use Records for all DTOs and immutable data classes
- ✅ Use Pattern Matching to eliminate explicit casts
- ✅ Use Switch Expressions for cleaner conditional logic
- ✅ Use Text Blocks for multi-line strings (SQL, JSON, HTML)
- ✅ Use Sealed Classes for closed type hierarchies
- ✅ Enable Virtual Threads for @Async and @Scheduled methods
- ✅ Use compact constructors in Records for validation
- ✅ Leverage pattern matching in switch expressions

### DON'T:
- ❌ Don't use traditional classes for simple DTOs
- ❌ Don't use explicit casting when pattern matching is available
- ❌ Don't use if-else chains when switch expressions are clearer
- ❌ Don't concatenate strings for SQL queries
- ❌ Don't use enums when sealed classes provide better type safety
- ❌ Don't create platform threads for I/O-bound operations
- ❌ Don't add setters to Records (they're immutable)
- ❌ Don't use instanceof without pattern matching

## 11. Migration Checklist

For each service, ensure:

- [ ] Virtual threads enabled in application.yml
- [ ] @Async methods configured to use virtual threads
- [ ] @Scheduled tasks use virtual thread executor
- [ ] All DTOs converted to Records
- [ ] All instanceof checks use pattern matching
- [ ] All if-else chains converted to switch expressions where appropriate
- [ ] All SQL queries use text blocks
- [ ] All JSON templates use text blocks
- [ ] Status enums converted to sealed classes
- [ ] Compact constructors added for validation in Records
- [ ] Maven compiler configured with --enable-preview
- [ ] Tests updated to work with new Java 21 features

## 12. Testing with Java 21 Features

```java
@Test
void testOrderCreationWithRecords() {
    // Given
    var request = new CreateOrderRequest(
        UUID.randomUUID(),
        List.of(new OrderItemRequest(UUID.randomUUID(), "SKU-001", 2)),
        new Address("123 Main St", "City", "State", "12345", "US"),
        new Address("123 Main St", "City", "State", "12345", "US"),
        null
    );
    
    // When
    var response = orderService.createOrder(request);
    
    // Then
    assertThat(response).isNotNull();
    assertThat(response.status()).isEqualTo("PENDING");
}

@Test
void testPatternMatchingInPaymentProcessing() {
    // Given
    PaymentMethod method = new CreditCard("4111111111111111", "12/25", "123");
    
    // When
    var result = switch (method) {
        case CreditCard card when card.isValid() -> "Valid credit card";
        case PayPal paypal when paypal.email() != null -> "Valid PayPal";
        default -> "Invalid payment method";
    };
    
    // Then
    assertThat(result).isEqualTo("Valid credit card");
}
```

## Summary

This guide provides comprehensive examples of Java 21 features for all commerce services. Each service should follow these patterns to maintain consistency and leverage modern Java capabilities.
