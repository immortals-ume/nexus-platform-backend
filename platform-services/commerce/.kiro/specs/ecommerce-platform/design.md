# E-Commerce Platform Design Document

## Overview

The E-Commerce Platform is a distributed, event-driven microservices architecture designed for high availability, horizontal scalability, and eventual consistency. The platform consists of 14 commerce microservices that follow hexagonal architecture principles, leverage platform starters for cross-cutting concerns, and integrate with existing infrastructure services (Auth, Notification, Storage, API Gateway).

### Design Principles

1. **Hexagonal Architecture**: Each service separates business logic from infrastructure concerns through ports and adapters
2. **Database Per Service**: Each service owns its database schema with no shared tables
3. **Event-Driven Communication**: Services communicate asynchronously via Kafka events using the outbox pattern
4. **CAP Theorem - AP System**: Prioritizes Availability and Partition tolerance with eventual consistency
5. **Microservices Patterns**: Saga, Outbox, CQRS, Event Sourcing, Circuit Breaker, Bulkhead
6. **Platform Starters**: Leverage cache-starter, messaging-starter, domain-starter, common-starter, observability-starter, security-starter
7. **Resilience First**: Circuit breakers, retries, timeouts, bulkheads, graceful degradation
8. **Observability**: Distributed tracing, metrics, structured logging with correlation IDs

### Technology Stack

- **Language**: Java 17 (LTS)
- **Framework**: Spring Boot 3.4.1, Spring Cloud 2024.0.1
- **Databases**: PostgreSQL 16 (transactional), MongoDB 7 (customer profiles), Redis 7 (cache, rate limiting)
- **Messaging**: Apache Kafka 3.9 with Schema Registry
- **Search**: Elasticsearch 8.16
- **Cache**: Redis 7 (distributed), Caffeine 3.1 (local)
- **Service Discovery**: Netflix Eureka
- **API Gateway**: Spring Cloud Gateway
- **Observability**: Micrometer 1.14, OpenTelemetry 1.44, Zipkin, Prometheus
- **Resilience**: Resilience4j 2.2
- **Testing**: JUnit 5.11, Mockito 5.14, Testcontainers 1.20, JUnit-QuickCheck 1.0 (property-based testing)
- **Build**: Maven 3.9+, Java 17

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           API Gateway                                    │
│  (Rate Limiting, Circuit Breaking, Routing, CORS, Auth Validation)     │
└────────────────────────────┬────────────────────────────────────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        │                    │                    │
┌───────▼────────┐  ┌────────▼────────┐  ┌────────▼────────┐
│ Product Service│  │Customer Service │  │  Order Service  │
│   (Postgres)   │  │   (MongoDB)     │  │   (Postgres)    │
└───────┬────────┘  └────────┬────────┘  └────────┬────────┘
        │                    │                    │
        └────────────────────┼────────────────────┘
                             │
                    ┌────────▼────────┐
                    │  Kafka Cluster  │
                    │  (Event Bus)    │
                    └────────┬────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
┌───────▼────────┐  ┌────────▼────────┐  ┌────────▼────────┐
│ Payment Service│  │  Cart Service   │  │Inventory Service│
│   (Postgres)   │  │    (Redis)      │  │   (Postgres)    │
└────────────────┘  └─────────────────┘  └─────────────────┘

┌────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│Shipping Service│  │ Review Service  │  │Promotion Service│
│   (Postgres)   │  │   (Postgres)    │  │   (Postgres)    │
└────────────────┘  └─────────────────┘  └─────────────────┘

┌────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│Wishlist Service│  │  Search Service │  │Analytics Service│
│   (Postgres)   │  │ (Elasticsearch) │  │  (TimeSeries)   │
└────────────────┘  └─────────────────┘  └─────────────────┘

┌────────────────┐  ┌─────────────────┐
│  Fraud Service │  │  Return Service │
│   (Postgres)   │  │   (Postgres)    │
└────────────────┘  └─────────────────┘

Infrastructure Services (Existing):
┌────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│  Auth Service  │  │Notification Svc │  │ Storage Service │
│   (Postgres)   │  │   (Postgres)    │  │   (S3/Minio)    │
└────────────────┘  └─────────────────┘  └─────────────────┘
```

### Service Communication Patterns

1. **Synchronous (REST/OpenFeign)**:
   - API Gateway → Services (request routing)
   - Order Service → Inventory Service (inventory check)
   - Order Service → Payment Service (payment initiation)
   - Services → Auth Service (token validation)

2. **Asynchronous (Kafka Events)**:
   - All inter-service state changes
   - Event-driven saga orchestration
   - CQRS read model updates
   - Analytics data collection

3. **Cache-Aside Pattern**:
   - Check Redis cache first
   - On miss, fetch from database
   - Update cache with TTL

## Components and Interfaces

### 1. Product Service

**Responsibility**: Product catalog, categories, pricing, metadata

**Technology**: Spring Boot, PostgreSQL, Redis Cache, Kafka

**Hexagonal Architecture**:
```
┌─────────────────────────────────────────────────────────┐
│                    Adapters (Input)                      │
│  REST Controllers, Kafka Consumers, Scheduled Jobs      │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│                   Application Layer                      │
│  ProductService, CategoryService, PricingService        │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│                    Domain Layer                          │
│  Product, Category, Price (Entities & Value Objects)    │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│                  Adapters (Output)                       │
│  JPA Repositories, Kafka Producers, Cache, Storage API  │
└─────────────────────────────────────────────────────────┘
```

**REST API Endpoints**:
```
POST   /api/v1/products                    - Create product
GET    /api/v1/products/{id}               - Get product by ID
PUT    /api/v1/products/{id}               - Update product
DELETE /api/v1/products/{id}               - Soft delete product
GET    /api/v1/products                    - List products (paginated)
GET    /api/v1/products/search             - Search products
GET    /api/v1/products/category/{id}      - Get products by category
POST   /api/v1/products/{id}/images        - Upload product images
GET    /api/v1/categories                  - List categories
POST   /api/v1/categories                  - Create category
```

**Events Published**:
- `ProductCreatedEvent`
- `ProductUpdatedEvent`
- `ProductDeletedEvent`
- `ProductPriceChangedEvent`
- `LowInventoryEvent`

**Events Consumed**:
- `InventoryUpdatedEvent` (from Inventory Service)
- `ReviewCreatedEvent` (from Review Service - update ratings)

**Database Schema**:
```sql
CREATE TABLE products (
    id UUID PRIMARY KEY,
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
    version INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    parent_id UUID,
    description TEXT,
    display_order INTEGER,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE price_history (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    old_price DECIMAL(10,2),
    new_price DECIMAL(10,2) NOT NULL,
    changed_at TIMESTAMP NOT NULL,
    changed_by VARCHAR(100)
);
```

**Caching Strategy**:
- Product details: 5 minutes TTL
- Category tree: 15 minutes TTL
- Product list: 2 minutes TTL
- Cache invalidation on updates

### 2. Customer Service

**Responsibility**: Customer profiles, addresses, loyalty, preferences

**Technology**: Spring Boot, MongoDB, Redis Cache, Kafka

**REST API Endpoints**:
```
GET    /api/v1/customers/{id}              - Get customer profile
PUT    /api/v1/customers/{id}              - Update customer profile
POST   /api/v1/customers/{id}/addresses    - Add address
PUT    /api/v1/customers/{id}/addresses/{addressId} - Update address
DELETE /api/v1/customers/{id}/addresses/{addressId} - Delete address
PUT    /api/v1/customers/{id}/addresses/{addressId}/default - Set default
GET    /api/v1/customers/{id}/loyalty      - Get loyalty points
POST   /api/v1/customers/{id}/loyalty/redeem - Redeem points
```

**Events Published**:
- `CustomerCreatedEvent`
- `CustomerUpdatedEvent`
- `AddressAddedEvent`
- `LoyaltyPointsEarnedEvent`
- `LoyaltyPointsRedeemedEvent`
- `LoyaltyTierChangedEvent`

**Events Consumed**:
- `UserCreatedEvent` (from Auth Service - create customer profile)
- `OrderCompletedEvent` (from Order Service - award loyalty points)

**MongoDB Schema**:
```javascript
{
  _id: ObjectId,
  userId: String,  // Reference to Auth Service user
  email: String,
  firstName: String,
  lastName: String,
  phoneNumber: String,
  addresses: [
    {
      id: String,
      type: String,  // SHIPPING, BILLING
      street: String,
      city: String,
      state: String,
      zipCode: String,
      country: String,
      isDefault: Boolean
    }
  ],
  loyalty: {
    points: Number,
    tier: String,  // BRONZE, SILVER, GOLD, PLATINUM
    tierSince: Date
  },
  preferences: {
    newsletter: Boolean,
    smsNotifications: Boolean,
    language: String,
    currency: String
  },
  createdAt: Date,
  updatedAt: Date,
  version: Number
}
```

**Caching Strategy**:
- Customer profile: 5 minutes TTL
- Cache invalidation on updates

### 3. Order Service

**Responsibility**: Order processing, saga orchestration, order history

**Technology**: Spring Boot, PostgreSQL, Kafka, Resilience4j

**REST API Endpoints**:
```
POST   /api/v1/orders                      - Create order
GET    /api/v1/orders/{id}                 - Get order by ID
GET    /api/v1/orders                      - List customer orders
PUT    /api/v1/orders/{id}/cancel          - Cancel order
GET    /api/v1/orders/{id}/status          - Get order status
GET    /api/v1/orders/{id}/history         - Get order status history
```

**Events Published**:
- `OrderCreatedEvent`
- `OrderConfirmedEvent`
- `OrderPaidEvent`
- `OrderCancelledEvent`
- `OrderCompletedEvent`
- `OrderStatusChangedEvent`

**Events Consumed**:
- `PaymentCompletedEvent` (from Payment Service)
- `PaymentFailedEvent` (from Payment Service)
- `ShipmentDispatchedEvent` (from Shipping Service)
- `ShipmentDeliveredEvent` (from Shipping Service)
- `InventoryReservedEvent` (from Inventory Service)
- `InventoryReservationFailedEvent` (from Inventory Service)

**Database Schema**:
```sql
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    customer_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    tax DECIMAL(10,2) NOT NULL,
    shipping_cost DECIMAL(10,2) NOT NULL,
    discount DECIMAL(10,2) DEFAULT 0,
    total DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    shipping_address JSONB NOT NULL,
    billing_address JSONB NOT NULL,
    saga_id UUID,
    saga_status VARCHAR(50),
    version INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    sku VARCHAR(100) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE order_status_history (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    reason TEXT,
    changed_at TIMESTAMP NOT NULL,
    changed_by VARCHAR(100)
);

CREATE TABLE saga_state (
    saga_id UUID PRIMARY KEY,
    saga_type VARCHAR(100) NOT NULL,
    current_step VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    completed_steps TEXT[],
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

**Saga Orchestration - Order Creation Saga**:
```
1. Create Order (PENDING)
2. Reserve Inventory → InventoryReservedEvent
3. Process Payment → PaymentCompletedEvent
4. Confirm Order (CONFIRMED)
5. Create Shipment → ShipmentCreatedEvent
6. Complete Order (COMPLETED)

Compensating Transactions:
- Payment Failed → Release Inventory
- Inventory Failed → Cancel Order
- Shipment Failed → Refund Payment, Release Inventory
```


### 4. Payment Service

**Responsibility**: Payment processing, refunds, payment methods

**Technology**: Spring Boot, PostgreSQL, Kafka, Resilience4j

**REST API Endpoints**:
```
POST   /api/v1/payments                    - Process payment
GET    /api/v1/payments/{id}               - Get payment by ID
POST   /api/v1/payments/{id}/refund        - Process refund
POST   /api/v1/payment-methods             - Add payment method
GET    /api/v1/payment-methods             - List payment methods
DELETE /api/v1/payment-methods/{id}        - Delete payment method
PUT    /api/v1/payment-methods/{id}/default - Set default
```

**Events Published**:
- `PaymentCompletedEvent`
- `PaymentFailedEvent`
- `PaymentRefundedEvent`
- `PaymentMethodAddedEvent`

**Events Consumed**:
- `OrderCreatedEvent` (from Order Service - initiate payment)
- `RefundRequestedEvent` (from Return Service)

**Database Schema**:
```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    status VARCHAR(50) NOT NULL,
    payment_method_id UUID,
    payment_gateway VARCHAR(50),
    transaction_id VARCHAR(255),
    idempotency_key VARCHAR(255) UNIQUE,
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE payment_methods (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    token VARCHAR(255) NOT NULL,
    last_four VARCHAR(4),
    expiry_month INTEGER,
    expiry_year INTEGER,
    is_default BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL
);
```

**Idempotency**: Uses Redis to store idempotency keys with 24-hour TTL

### 5. Cart Service

**Responsibility**: Shopping cart management

**Technology**: Spring Boot, Redis, Kafka

**REST API Endpoints**:
```
POST   /api/v1/cart/items                  - Add item to cart
PUT    /api/v1/cart/items/{itemId}         - Update cart item
DELETE /api/v1/cart/items/{itemId}         - Remove cart item
GET    /api/v1/cart                        - Get cart
DELETE /api/v1/cart                        - Clear cart
```

**Events Published**:
- `CartItemAddedEvent`
- `CartItemRemovedEvent`
- `CartAbandonedEvent`
- `CartConvertedEvent`

**Events Consumed**:
- `OrderCreatedEvent` (from Order Service - clear cart)

**Redis Data Structure**:
```json
{
  "cart:{customerId}": {
    "items": [
      {
        "productId": "uuid",
        "sku": "string",
        "name": "string",
        "quantity": 2,
        "unitPrice": 29.99,
        "totalPrice": 59.98
      }
    ],
    "subtotal": 59.98,
    "lastUpdated": "2024-01-01T10:00:00Z"
  }
}
```

**TTL**: 7 days for cart data

### 6. Inventory Service

**Responsibility**: Stock tracking, reservations

**Technology**: Spring Boot, PostgreSQL, Kafka

**REST API Endpoints**:
```
GET    /api/v1/inventory/{productId}       - Get inventory
POST   /api/v1/inventory/{productId}/reserve - Reserve inventory
POST   /api/v1/inventory/{productId}/release - Release inventory
PUT    /api/v1/inventory/{productId}       - Update inventory
GET    /api/v1/inventory/low-stock         - Get low stock products
```

**Events Published**:
- `InventoryReservedEvent`
- `InventoryReservationFailedEvent`
- `InventoryReleasedEvent`
- `InventoryUpdatedEvent`
- `LowInventoryEvent`

**Events Consumed**:
- `OrderCreatedEvent` (from Order Service - reserve inventory)
- `OrderCancelledEvent` (from Order Service - release inventory)
- `ShipmentDispatchedEvent` (from Shipping Service - deduct inventory)

**Database Schema**:
```sql
CREATE TABLE inventory (
    id UUID PRIMARY KEY,
    product_id UUID UNIQUE NOT NULL,
    sku VARCHAR(100) NOT NULL,
    available_quantity INTEGER NOT NULL,
    reserved_quantity INTEGER NOT NULL,
    total_quantity INTEGER NOT NULL,
    warehouse_location VARCHAR(100),
    reorder_threshold INTEGER DEFAULT 10,
    version INTEGER NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE inventory_reservations (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    order_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```

**Optimistic Locking**: Uses version field to prevent overselling


### 7. Shipping Service

**Responsibility**: Fulfillment, tracking, delivery

**Technology**: Spring Boot, PostgreSQL, Kafka

**REST API Endpoints**:
```
POST   /api/v1/shipments                   - Create shipment
GET    /api/v1/shipments/{id}              - Get shipment
PUT    /api/v1/shipments/{id}/dispatch     - Dispatch shipment
PUT    /api/v1/shipments/{id}/tracking     - Update tracking
GET    /api/v1/shipments/order/{orderId}   - Get shipments by order
```

**Events Published**:
- `ShipmentCreatedEvent`
- `ShipmentDispatchedEvent`
- `ShipmentInTransitEvent`
- `ShipmentDeliveredEvent`

**Events Consumed**:
- `OrderPaidEvent` (from Order Service - create shipment)

**Database Schema**:
```sql
CREATE TABLE shipments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    tracking_number VARCHAR(100),
    carrier VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    shipping_address JSONB NOT NULL,
    weight DECIMAL(10,2),
    shipping_cost DECIMAL(10,2),
    estimated_delivery DATE,
    actual_delivery TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### 8-14. Additional Services (Summary)

**Review Service**: Customer reviews, ratings, moderation (PostgreSQL)
**Promotion Service**: Coupons, discounts, campaigns (PostgreSQL)
**Wishlist Service**: Save for later, price alerts (PostgreSQL)
**Search Service**: Elasticsearch, autocomplete, faceted search
**Analytics Service**: Metrics, reports, dashboards (TimeSeries DB)
**Fraud Service**: Risk scoring, ML-based detection (PostgreSQL)
**Return Service**: Return management, refund processing (PostgreSQL)

## Data Models

### Domain Starter - Base Entities

All entities extend from `BaseEntity` in domain-starter:

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Version
    private Integer version;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @CreatedBy
    private String createdBy;
    
    @LastModifiedBy
    private String updatedBy;
}
```

### Common DTOs (domain-starter)

```java
public record PageRequest(int page, int size, String sortBy, String sortDirection) {}

public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}

public record ApiResponse<T>(
    boolean success,
    T data,
    String message,
    LocalDateTime timestamp
) {}

public record ErrorResponse(
    String error,
    String message,
    int status,
    String path,
    LocalDateTime timestamp,
    List<ValidationError> validationErrors
) {}
```

### Event Base Class (messaging-starter)

```java
public abstract class DomainEvent {
    private String eventId = UUID.randomUUID().toString();
    private String eventType;
    private LocalDateTime occurredAt = LocalDateTime.now();
    private String correlationId;
    private String causationId;
    private Integer version = 1;
}
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Product Service Properties

**Property 1: Product creation persistence and event publishing**
*For any* valid product data, when a product is created, the product should exist in the database and a ProductCreatedEvent should be published to the outbox
**Validates: Requirements 1.1**

**Property 2: Product updates are atomic with events**
*For any* existing product and valid update data, updating the product should result in both the database update and ProductUpdatedEvent publication succeeding or both failing
**Validates: Requirements 1.2**

**Property 3: Soft delete preserves data**
*For any* product, when deleted, the product should remain in the database with status INACTIVE and a ProductDeletedEvent should be published
**Validates: Requirements 1.3**

**Property 4: Cache-aside pattern correctness**
*For any* product ID, retrieving the product twice in succession should return the same data, with the second retrieval being served from cache
**Validates: Requirements 1.4**

**Property 5: Search results match criteria**
*For any* search criteria, all returned products should match the specified filters (category, price range, etc.)
**Validates: Requirements 1.5**

### Inventory Service Properties

**Property 6: Inventory reservation prevents overselling**
*For any* product with quantity Q, the sum of all successful concurrent reservation requests should never exceed Q
**Validates: Requirements 2.2**

**Property 7: Insufficient stock returns error without events**
*For any* product, when attempting to reserve more than available quantity, the operation should fail and no InventoryReservedEvent should be published
**Validates: Requirements 2.3**

**Property 8: Reservation and release are inverse operations**
*For any* product, reserving quantity Q then releasing quantity Q should result in the same available quantity as before
**Validates: Requirements 2.4**

**Property 9: Low inventory triggers alerts**
*For any* product, when available quantity falls below the reorder threshold, a LowInventoryEvent should be published
**Validates: Requirements 2.5**

### Customer Service Properties

**Property 10: Customer profile creation from user events**
*For any* UserCreatedEvent, a corresponding customer profile should be created with matching user ID
**Validates: Requirements 3.1**

**Property 11: Only one default address invariant**
*For any* customer with multiple addresses, setting an address as default should result in exactly one address having isDefault=true
**Validates: Requirements 3.4**

**Property 12: Profile updates invalidate cache**
*For any* customer profile update, the cached profile should be invalidated and subsequent reads should fetch fresh data
**Validates: Requirements 3.2**

### Order Service Properties

**Property 13: Order creation initiates saga**
*For any* valid order request, creating an order should result in a saga instance being created with status INITIATED
**Validates: Requirements 4.1**

**Property 14: Outbox pattern atomicity**
*For any* order creation, either both the order record and OrderCreatedEvent in outbox are persisted, or neither are
**Validates: Requirements 4.2**

**Property 15: Saga compensation reverses completed steps**
*For any* saga with N completed steps that fails at step N+1, all N steps should have compensating transactions executed in reverse order
**Validates: Requirements 6.3**

**Property 16: Successful saga completes order**
*For any* order saga where all steps succeed, the saga status should be COMPLETED and order status should be CONFIRMED or PAID
**Validates: Requirements 6.4**

### Payment Service Properties

**Property 17: Idempotency prevents duplicate charges**
*For any* payment request with idempotency key K, submitting the same request multiple times should result in exactly one payment being processed
**Validates: Requirements 5.2**

**Property 18: Payment state transitions are valid**
*For any* payment, the status transitions should follow the valid state machine: PENDING → COMPLETED or PENDING → FAILED
**Validates: Requirements 5.3, 5.4**

**Property 19: Refund creates inverse transaction**
*For any* completed payment of amount A, processing a refund should create a refund transaction of amount A and publish PaymentRefundedEvent
**Validates: Requirements 5.5**

### Event-Driven Communication Properties

**Property 20: Outbox events are eventually published**
*For any* event stored in the outbox, the event should be published to Kafka within 5 seconds under normal conditions
**Validates: Requirements 7.2**

**Property 21: Event processing idempotency**
*For any* event with event ID E, processing the same event multiple times should have the same effect as processing it once
**Validates: Requirements 7.3**

**Property 22: Event structure completeness**
*For any* published event, the event should contain correlation ID, event type, timestamp, and payload
**Validates: Requirements 7.5**

**Property 23: Failed event processing moves to DLQ**
*For any* event that fails processing 3 times, the event should be moved to the dead letter queue
**Validates: Requirements 7.4**

### Caching Properties

**Property 24: Cache miss falls back to database**
*For any* cache key, when cache is unavailable, the service should successfully retrieve data from the database
**Validates: Requirements 8.4**

**Property 25: Cache invalidation on updates**
*For any* entity update, the corresponding cache entry should be invalidated immediately
**Validates: Requirements 8.3**

### Transaction and Consistency Properties

**Property 26: Transactional atomicity**
*For any* operation that modifies multiple database records, either all changes are committed or all are rolled back
**Validates: Requirements 15.1**

**Property 27: Optimistic locking detects conflicts**
*For any* entity with version V, concurrent updates should result in at most one success and others receiving OptimisticLockException
**Validates: Requirements 15.4**

**Property 28: Eventual consistency time bound**
*For any* event published, all read models should reflect the change within 5 seconds
**Validates: Requirements 22.2**

### CQRS Properties

**Property 29: Read model reflects write model**
*For any* write operation, the read model should eventually contain the same data as the write model
**Validates: Requirements 25.3**

**Property 30: Read and write models are independent**
*For any* read model query failure, write operations should continue to succeed
**Validates: Requirements 25.1**

### Circuit Breaker Properties

**Property 31: Circuit breaker opens on threshold**
*For any* external service call, when failure rate exceeds 50% over 10 calls, the circuit breaker should open
**Validates: Requirements 10.2**

**Property 32: Open circuit returns fallback**
*For any* call when circuit breaker is open, the call should fail fast and return a fallback response if available
**Validates: Requirements 10.3**

### Retry Properties

**Property 33: Exponential backoff increases delay**
*For any* failed operation with retry, each retry attempt should wait longer than the previous (100ms, 200ms, 400ms)
**Validates: Requirements 33.1**

**Property 34: Idempotent retries are safe**
*For any* idempotent operation, retrying N times should produce the same result as executing once
**Validates: Requirements 33.4**

### Cart Service Properties

**Property 35: Cart total equals sum of items**
*For any* cart, the cart subtotal should equal the sum of (quantity × unitPrice) for all items
**Validates: Requirements 41.1**

**Property 36: Cart conversion clears cart**
*For any* cart that is converted to an order, the cart should be empty after conversion
**Validates: Requirements 41.5**

### Loyalty Properties

**Property 37: Order completion awards points**
*For any* completed order of amount A, the customer should receive loyalty points proportional to A
**Validates: Requirements 16.1**

**Property 38: Points redemption decrements balance**
*For any* loyalty points redemption of P points, the customer's point balance should decrease by P
**Validates: Requirements 16.3**

### Promotion Properties

**Property 39: Coupon usage is tracked**
*For any* coupon code used in an order, the coupon usage count should increment by 1
**Validates: Requirements 45.3**

**Property 40: Expired promotions are not applied**
*For any* promotion with end date in the past, the promotion should not be applied to new orders
**Validates: Requirements 45.4**


## Error Handling

### Exception Hierarchy (common-starter)

```java
// Base exception
public class BusinessException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;
}

// Specific exceptions
public class ResourceNotFoundException extends BusinessException {}
public class ValidationException extends BusinessException {}
public class BusinessRuleViolationException extends BusinessException {}
public class ExternalServiceException extends BusinessException {}
public class ConcurrencyException extends BusinessException {}
```

### Global Exception Handler (common-starter)

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        return ResponseEntity.status(400).body(createErrorResponse(ex));
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(404).body(createErrorResponse(ex));
    }
    
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleViolationException ex) {
        return ResponseEntity.status(422).body(createErrorResponse(ex));
    }
    
    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockException ex) {
        return ResponseEntity.status(409).body(createErrorResponse(ex));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(500).body(createErrorResponse(ex));
    }
}
```

### Error Response Format

```json
{
  "error": "VALIDATION_ERROR",
  "message": "Invalid input data",
  "status": 400,
  "path": "/api/v1/products",
  "timestamp": "2024-01-01T10:00:00Z",
  "validationErrors": [
    {
      "field": "price",
      "message": "Price must be greater than 0",
      "rejectedValue": -10
    }
  ]
}
```

## Testing Strategy

### Unit Testing

**Framework**: JUnit 5, Mockito, AssertJ

**Scope**:
- Business logic in service layer
- Domain model validation
- Utility functions
- Exception handling

**Example**:
```java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    
    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private EventPublisher eventPublisher;
    
    @InjectMocks
    private ProductService productService;
    
    @Test
    void shouldCreateProductAndPublishEvent() {
        // Given
        ProductRequest request = createValidProductRequest();
        Product product = Product.from(request);
        when(productRepository.save(any())).thenReturn(product);
        
        // When
        ProductResponse response = productService.createProduct(request);
        
        // Then
        assertThat(response.id()).isNotNull();
        verify(productRepository).save(any(Product.class));
        verify(eventPublisher).publish(any(ProductCreatedEvent.class));
    }
}
```

### Property-Based Testing

**Framework**: JUnit-QuickCheck (already in messaging-starter)

**Configuration**: Minimum 100 iterations per property test

**Tagging Format**: `// Feature: ecommerce-platform, Property {number}: {property_text}`

**Example**:
```java
@RunWith(JUnitQuickcheck.class)
public class InventoryServicePropertyTest {
    
    @Property(trials = 100)
    // Feature: ecommerce-platform, Property 6: Inventory reservation prevents overselling
    public void reservationsShouldNotExceedAvailableQuantity(
        @InRange(min = "1", max = "100") int initialQuantity,
        @InRange(min = "1", max = "10") int numReservations,
        @InRange(min = "1", max = "20") int quantityPerReservation
    ) {
        // Given
        Product product = createProductWithInventory(initialQuantity);
        
        // When
        List<ReservationResult> results = IntStream.range(0, numReservations)
            .parallel()
            .mapToObj(i -> inventoryService.reserve(product.getId(), quantityPerReservation))
            .collect(Collectors.toList());
        
        // Then
        long successfulReservations = results.stream().filter(ReservationResult::isSuccess).count();
        long totalReserved = successfulReservations * quantityPerReservation;
        
        assertThat(totalReserved).isLessThanOrEqualTo(initialQuantity);
    }
    
    @Property(trials = 100)
    // Feature: ecommerce-platform, Property 8: Reservation and release are inverse operations
    public void reserveAndReleaseShouldRestoreQuantity(
        @InRange(min = "10", max = "100") int initialQuantity,
        @InRange(min = "1", max = "10") int reserveQuantity
    ) {
        // Given
        Product product = createProductWithInventory(initialQuantity);
        
        // When
        inventoryService.reserve(product.getId(), reserveQuantity);
        inventoryService.release(product.getId(), reserveQuantity);
        
        // Then
        int finalQuantity = inventoryService.getAvailableQuantity(product.getId());
        assertThat(finalQuantity).isEqualTo(initialQuantity);
    }
}
```

### Integration Testing

**Framework**: Spring Boot Test, Testcontainers

**Scope**:
- API endpoints
- Database operations
- Kafka event publishing/consuming
- Cache operations
- Service-to-service communication

**Example**:
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class ProductServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Test
    void shouldCreateProductEndToEnd() {
        // Given
        ProductRequest request = createValidProductRequest();
        
        // When
        ResponseEntity<ProductResponse> response = restTemplate.postForEntity(
            "/api/v1/products",
            request,
            ProductResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().id()).isNotNull();
        
        // Verify database
        Optional<Product> savedProduct = productRepository.findById(response.getBody().id());
        assertThat(savedProduct).isPresent();
    }
}
```

### Contract Testing

**Framework**: Spring Cloud Contract

**Scope**: API contracts between services

### Performance Testing

**Framework**: JMeter, Gatling

**Scope**:
- Load testing (1000 req/s)
- Stress testing
- Endurance testing
- Response time validation (p95 < 200ms for reads)

### Chaos Testing

**Framework**: Chaos Monkey for Spring Boot

**Scope**:
- Random service instance termination
- Network latency injection
- Database connection failures
- Kafka unavailability


## Platform Starters Integration

### 1. Cache Starter Integration

**Dependency**:
```xml
<dependency>
    <groupId>com.immortals.platform</groupId>
    <artifactId>cache-core</artifactId>
    <version>2.0.0</version>
</dependency>
```

**Usage**:
```java
import com.immortals.cache.core.CacheService;
import java.time.Duration;

@Service
public class ProductService {
    
    @Autowired
    private CacheService<String, Product> cacheService;
    
    @Autowired
    private ProductRepository productRepository;
    
    public Product getProduct(UUID id) {
        String cacheKey = "product:" + id;
        return cacheService.get(cacheKey)
            .orElseGet(() -> {
                Product product = productRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
                cacheService.put(cacheKey, product, Duration.ofMinutes(5));
                return product;
            });
    }
    
    public void updateProduct(UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.update(request);
        productRepository.save(product);
        cacheService.remove("product:" + id);  // Invalidate cache
    }
}
```

### 2. Messaging Starter Integration

**Dependency**:
```xml
<dependency>
    <groupId>com.immortals.platform</groupId>
    <artifactId>messaging-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Event Publishing with Outbox**:
```java
@Service
public class OrderService {
    
    @Autowired
    private OutboxEventPublisher eventPublisher;
    
    @Transactional
    public Order createOrder(OrderRequest request) {
        // Create order
        Order order = Order.from(request);
        orderRepository.save(order);
        
        // Publish event using outbox pattern
        OrderCreatedEvent event = new OrderCreatedEvent(order);
        eventPublisher.publish(event);  // Stored in outbox table in same transaction
        
        return order;
    }
}
```

**Event Consuming with Idempotency**:
```java
@Component
public class PaymentEventConsumer {
    
    @Autowired
    private IdempotentEventHandler eventHandler;
    
    @KafkaListener(topics = "payment-events")
    public void handlePaymentEvent(PaymentCompletedEvent event) {
        eventHandler.handle(event.getEventId(), () -> {
            // Process event - will only execute once even if event is received multiple times
            Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
            order.markAsPaid();
            orderRepository.save(order);
        });
    }
}
```

### 3. Domain Starter Integration

**Dependency**:
```xml
<dependency>
    <groupId>com.immortals.platform</groupId>
    <artifactId>domain-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Entity Definition (Java 21)**:
```java
@Entity
@Table(name = "products")
public class Product extends BaseEntity {  // Inherits id, version, audit fields
    
    @Column(nullable = false, unique = true)
    private String sku;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private BigDecimal price;
    
    @Enumerated(EnumType.STRING)
    private ProductStatus status;
    
    // Business methods using Java 21 features
    public void updatePrice(BigDecimal newPrice) {
        // Pattern matching for null check
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Price must be positive");
        }
        this.price = newPrice;
    }
    
    // Switch expression (Java 21)
    public String getStatusDescription() {
        return switch (status) {
            case ACTIVE -> "Product is available for purchase";
            case INACTIVE -> "Product is temporarily unavailable";
            case DISCONTINUED -> "Product is no longer available";
            case OUT_OF_STOCK -> "Product is out of stock";
        };
    }
}

// Sealed class for ProductStatus (Java 21)
public sealed interface ProductStatus permits Active, Inactive, Discontinued, OutOfStock {
    record Active() implements ProductStatus {}
    record Inactive() implements ProductStatus {}
    record Discontinued() implements ProductStatus {}
    record OutOfStock() implements ProductStatus {}
}

// DTOs as Records (Java 21)
public record ProductRequest(
    @NotBlank String sku,
    @NotBlank String name,
    String description,
    @NotNull @Positive BigDecimal price,
    @NotNull String categoryId
) {}

public record ProductResponse(
    String id,
    String sku,
    String name,
    String description,
    BigDecimal price,
    String status,
    LocalDateTime createdAt
) {}
```

### 4. Common Starter Integration

**Dependency**:
```xml
<dependency>
    <groupId>com.immortals.platform</groupId>
    <artifactId>common-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

**API Response Wrapper**:
```java
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.createProduct(request);
        ApiResponse<ProductResponse> response = ApiResponse.success(product, "Product created successfully");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> listProducts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<ProductResponse> products = productService.listProducts(page, size);
        return ResponseEntity.ok(products);
    }
}
```

**Constants Management**:
```java
public class OrderConstants {
    public static final String ORDER_CREATED_TOPIC = "order-events";
    public static final int MAX_ORDER_ITEMS = 50;
    public static final BigDecimal MIN_ORDER_AMOUNT = new BigDecimal("1.00");
    public static final Duration ORDER_TIMEOUT = Duration.ofMinutes(30);
}
```

### 5. Observability Starter Integration

**Dependency**:
```xml
<dependency>
    <groupId>com.immortals.platform</groupId>
    <artifactId>observability-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Distributed Tracing with Virtual Threads (Java 21 + Spring Boot 3.4)**:
```java
@Service
public class OrderService {
    
    @Autowired
    private Tracer tracer;
    
    // Async method using Virtual Threads (Spring Boot 3.4)
    @Async
    public CompletableFuture<Order> createOrderAsync(OrderRequest request) {
        Span span = tracer.nextSpan().name("createOrder").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            span.tag("customer.id", request.customerId().toString());
            span.tag("order.items.count", String.valueOf(request.items().size()));
            
            Order order = processOrder(request);
            
            span.tag("order.id", order.getId().toString());
            span.tag("order.total", order.getTotal().toString());
            
            return CompletableFuture.completedFuture(order);
        } finally {
            span.end();
        }
    }
    
    // Parallel processing with Virtual Threads
    public List<OrderItem> validateOrderItems(List<OrderItemRequest> items) {
        // Virtual threads make this highly efficient
        return items.parallelStream()
            .map(this::validateAndCreateOrderItem)
            .toList();
    }
    
    private OrderItem validateAndCreateOrderItem(OrderItemRequest request) {
        // Each validation runs on a virtual thread
        Product product = productService.getProduct(request.productId());
        inventoryService.checkAvailability(request.productId(), request.quantity());
        return new OrderItem(product, request.quantity());
    }
}

// Configuration for Virtual Threads
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    @Override
    public Executor getAsyncExecutor() {
        // Spring Boot 3.4 automatically uses virtual threads when enabled
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
```

**Metrics**:
```java
@Service
public class ProductService {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    public Product createProduct(ProductRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            Product product = productRepository.save(Product.from(request));
            meterRegistry.counter("products.created", "status", "success").increment();
            return product;
        } catch (Exception e) {
            meterRegistry.counter("products.created", "status", "failure").increment();
            throw e;
        } finally {
            sample.stop(Timer.builder("products.create.duration")
                .description("Time to create product")
                .register(meterRegistry));
        }
    }
}
```

### 6. Security Starter Integration

**Dependency**:
```xml
<dependency>
    <groupId>com.immortals.platform</groupId>
    <artifactId>security-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

**JWT Validation with Auth Service Integration**:
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Value("${auth.service.jwt.secret}")
    private String jwtSecret;
    
    @Value("${auth.service.issuer}")
    private String jwtIssuer;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/actuator/health", "/actuator/metrics", "/actuator/prometheus").permitAll()
                .requestMatchers("/api/v1/products/**").permitAll()  // Public product browsing
                
                // Authenticated endpoints
                .requestMatchers("/api/v1/orders/**").authenticated()
                .requestMatchers("/api/v1/cart/**").authenticated()
                .requestMatchers("/api/v1/customers/**").authenticated()
                
                // Admin endpoints
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/inventory/**").hasAnyRole("ADMIN", "INVENTORY_MANAGER")
                
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );
        
        return http.build();
    }
    
    @Bean
    public JwtDecoder jwtDecoder() {
        // Validate JWT tokens issued by Auth Service
        SecretKeySpec secretKey = new SecretKeySpec(
            jwtSecret.getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
        );
        
        return NimbusJwtDecoder.withSecretKey(secretKey)
            .build();
    }
    
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:8080"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

// Extract user context from JWT
@Component
public class UserContextHolder {
    
    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("userId");
        }
        return null;
    }
    
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("username");
        }
        return null;
    }
    
    public static List<String> getCurrentUserRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        }
        return List.of();
    }
}

// Usage in service layer
@Service
public class OrderService {
    
    public Order createOrder(OrderRequest request) {
        String userId = UserContextHolder.getCurrentUserId();
        String username = UserContextHolder.getCurrentUsername();
        
        // Create order with user context
        Order order = Order.builder()
            .customerId(UUID.fromString(userId))
            .createdBy(username)
            .build();
        
        return orderRepository.save(order);
    }
}

// Configuration in application.yml
auth:
  service:
    url: http://auth-service:8080
    jwt:
      secret: ${AUTH_JWT_SECRET}  # Same secret as Auth Service
    issuer: http://auth-service:8080
```

## Deployment Architecture

### Service Configuration

**application.yml** (Product Service example - Spring Boot 3.4.1 with Java 21):
```yaml
spring:
  application:
    name: product-service
  
  # Java 21 Virtual Threads (Spring Boot 3.4+)
  threads:
    virtual:
      enabled: true
  
  datasource:
    url: jdbc:postgresql://localhost:5432/product_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          time_zone: UTC
    open-in-view: false
  
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        enable.idempotence: true
        max.in.flight.requests.per.connection: 5
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
      auto-offset-reset: earliest
      enable-auto-commit: false
  
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
      password: ${REDIS_PASSWORD}
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_SERVER_URL}
  instance:
    prefer-ip-address: true
    lease-renewal-interval-in-seconds: 10
    lease-expiration-duration-in-seconds: 30

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,info
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
  tracing:
    sampling:
      probability: 0.1
  observations:
    key-values:
      application: ${spring.application.name}

# Resilience4j 2.2.0
resilience4j:
  circuitbreaker:
    instances:
      inventoryService:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 3
        automatic-transition-from-open-to-half-open-enabled: true
        record-exceptions:
          - java.net.ConnectException
          - java.util.concurrent.TimeoutException
  retry:
    instances:
      default:
        max-attempts: 3
        wait-duration: 100ms
        exponential-backoff-multiplier: 2
        retry-exceptions:
          - java.net.ConnectException
          - java.util.concurrent.TimeoutException
  bulkhead:
    instances:
      default:
        max-concurrent-calls: 10
        max-wait-duration: 100ms
  timelimiter:
    instances:
      default:
        timeout-duration: 5s

# Server configuration with Virtual Threads
server:
  port: 8080
  tomcat:
    threads:
      max: 200
      min-spare: 10
  shutdown: graceful

# Logging
logging:
  level:
    root: INFO
    com.immortals: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### Docker Compose (Development)

```yaml
version: '3.8'

services:
  postgres-product:
    image: postgres:15
    environment:
      POSTGRES_DB: product_db
      POSTGRES_USER: product_user
      POSTGRES_PASSWORD: product_pass
    ports:
      - "5432:5432"
  
  postgres-order:
    image: postgres:15
    environment:
      POSTGRES_DB: order_db
      POSTGRES_USER: order_user
      POSTGRES_PASSWORD: order_pass
    ports:
      - "5433:5432"
  
  mongodb:
    image: mongo:6
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: admin
    ports:
      - "27017:27017"
  
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
  
  kafka:
    image: confluentinc/cp-kafka:7.4.0
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
    ports:
      - "9092:9092"
  
  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"
  
  elasticsearch:
    image: elasticsearch:8.8.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    ports:
      - "9200:9200"
  
  zipkin:
    image: openzipkin/zipkin
    ports:
      - "9411:9411"
```

### Kubernetes Deployment (Production)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: product-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: product-service
  template:
    metadata:
      labels:
        app: product-service
    spec:
      containers:
      - name: product-service
        image: product-service:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: product-db-secret
              key: username
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: product-db-secret
              key: password
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 20
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: product-service
spec:
  selector:
    app: product-service
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: ClusterIP
```

## Migration Strategy

### Phase 1: Core Services (Weeks 1-4)
1. Product Service
2. Customer Service
3. Inventory Service
4. Cart Service

### Phase 2: Transaction Services (Weeks 5-8)
5. Order Service
6. Payment Service
7. Shipping Service

### Phase 3: Enhancement Services (Weeks 9-12)
8. Review Service
9. Promotion Service
10. Wishlist Service

### Phase 4: Intelligence Services (Weeks 13-16)
11. Search Service
12. Analytics Service
13. Fraud Service
14. Return Service

