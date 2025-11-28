# Product Service - Event Publishing Implementation

## Overview
This document describes the implementation of event publishing in the Product Service as per task 9 of the ecommerce-platform specification.

## Implementation Summary

### 1. Event Payload Classes Created

#### ProductCreatedEvent
- **Location**: `src/main/java/com/immortals/platform/product/event/ProductCreatedEvent.java`
- **Purpose**: Published when a new product is created
- **Key Fields**:
  - productId, sku, name, description, barcode
  - categoryId, categoryName
  - basePrice, currentPrice, currency
  - status, brand, modelNumber
  - imageUrls, metadata
  - createdAt, createdBy

#### ProductUpdatedEvent
- **Location**: `src/main/java/com/immortals/platform/product/event/ProductUpdatedEvent.java`
- **Purpose**: Published when a product is updated
- **Key Fields**:
  - All product fields (same as ProductCreatedEvent)
  - oldPrice, oldStatus (for tracking changes)
  - priceChanged, statusChanged (boolean flags)
  - updatedAt, updatedBy

#### ProductDeletedEvent
- **Location**: `src/main/java/com/immortals/platform/product/event/ProductDeletedEvent.java`
- **Purpose**: Published when a product is soft deleted
- **Key Fields**:
  - productId, sku, name
  - categoryId, categoryName
  - currentPrice, currency
  - statusBeforeDeletion, brand
  - deletedAt, deletedBy, deletionReason

### 2. Kafka Topic Configuration

#### KafkaTopicConfig
- **Location**: `src/main/java/com/immortals/platform/product/config/KafkaTopicConfig.java`
- **Purpose**: Centralizes Kafka topic names for event publishing
- **Topics**:
  - `product.created` - for ProductCreatedEvent
  - `product.updated` - for ProductUpdatedEvent
  - `product.deleted` - for ProductDeletedEvent

#### Application Configuration
- **Location**: `src/main/resources/application.yml`
- **Added**:
  ```yaml
  kafka:
    topics:
      product-created: product.created
      product-updated: product.updated
      product-deleted: product.deleted
  ```

### 3. ProductService Integration

#### Dependencies Added
- `EventPublisher` - from messaging-starter for publishing events
- `KafkaTopicConfig` - for topic name configuration

#### Event Publishing Methods

##### publishProductCreatedEvent(Product product)
- Called after successful product creation
- Wraps ProductCreatedEvent in DomainEvent envelope
- Includes correlation ID for distributed tracing
- Publishes to `product.created` topic
- Error handling: logs errors but doesn't fail transaction

##### publishProductUpdatedEvent(Product product, BigDecimal oldPrice, ProductStatus oldStatus, boolean priceChanged, boolean statusChanged)
- Called after successful product update
- Tracks old values for price and status
- Includes change flags (priceChanged, statusChanged)
- Publishes to `product.updated` topic
- Error handling: logs errors but doesn't fail transaction

##### publishProductDeletedEvent(Product product, ProductStatus statusBeforeDeletion)
- Called after successful soft delete
- Captures status before deletion
- Publishes to `product.deleted` topic
- Error handling: logs errors but doesn't fail transaction

#### Correlation ID Propagation
- **Method**: `getCorrelationId()`
- Retrieves correlation ID from SLF4J MDC (set by observability-starter or API Gateway)
- Falls back to generating new UUID if not present
- Ensures distributed tracing across services

### 4. Integration Points

#### createProduct Method
```java
Product savedProduct = productRepository.save(product);
createPriceHistory(...);
publishProductCreatedEvent(savedProduct);  // NEW
```

#### updateProduct Method
```java
// Track old values
BigDecimal oldPrice = product.getCurrentPrice();
ProductStatus oldStatus = product.getStatus();
boolean priceChanged = false;
boolean statusChanged = false;

// ... update logic ...

Product updatedProduct = productRepository.save(product);
createPriceHistory(...);
publishProductUpdatedEvent(updatedProduct, oldPrice, oldStatus, priceChanged, statusChanged);  // NEW
```

#### deleteProduct Method
```java
ProductStatus statusBeforeDeletion = product.getStatus();
product.markAsDeleted(deletedBy);
product.setStatus(ProductStatus.ARCHIVED);
Product deletedProduct = productRepository.save(product);
publishProductDeletedEvent(deletedProduct, statusBeforeDeletion);  // NEW
```

## Requirements Validation

### Requirement 1.1 (Product Creation)
✅ **Implemented**: ProductCreatedEvent is published after successful product creation

### Requirement 1.2 (Product Updates)
✅ **Implemented**: ProductUpdatedEvent is published after successful product update with old/new value tracking

### Requirement 1.3 (Soft Delete)
✅ **Implemented**: ProductDeletedEvent is published after successful soft delete

### Requirement 7.5 (Event Structure)
✅ **Implemented**: All events include:
- Correlation ID (from MDC or generated)
- Event type (ProductCreated, ProductUpdated, ProductDeleted)
- Timestamp (automatically added by DomainEvent)
- Payload (complete event data)
- Aggregate ID and Type (product ID and "Product")

## Design Patterns Used

### 1. Domain Event Pattern
- Events represent significant business occurrences
- Events are immutable (using Lombok @Builder and @Data)
- Events contain all necessary information for downstream processing

### 2. Event Envelope Pattern
- DomainEvent<T> wraps all events with standard metadata
- Provides consistent structure across all event types
- Includes correlation ID, timestamp, aggregate info

### 3. Publish-Subscribe Pattern
- Product Service publishes events to Kafka topics
- Downstream services (Search, Wishlist, Analytics) can subscribe
- Loose coupling between services

### 4. Correlation ID Propagation
- Enables distributed tracing across microservices
- Propagated through MDC (Mapped Diagnostic Context)
- Falls back to UUID generation if not present

## Error Handling Strategy

### Non-Blocking Event Publishing
- Event publishing failures are logged but don't fail the transaction
- Rationale: Database consistency is more important than immediate event delivery
- Future enhancement: Outbox pattern (task 8) will ensure eventual event delivery

### Transactional Boundaries
- Events are published AFTER database transaction commits
- Ensures events only published for successful operations
- Prevents phantom events for rolled-back transactions

## Dependencies

### Required
- `messaging-starter` - provides EventPublisher and DomainEvent
- `spring-kafka` - Kafka integration
- `slf4j` - for MDC correlation ID retrieval

### Configuration
- Kafka bootstrap servers (from application.yml)
- Topic names (configurable via application.yml)

## Testing Considerations

### Unit Tests
- Mock EventPublisher to verify event publishing calls
- Verify event payload correctness
- Test correlation ID propagation

### Integration Tests
- Use Testcontainers for Kafka
- Verify events are published to correct topics
- Verify event structure and content
- Test event publishing with transaction rollback

### Property-Based Tests (Future)
- Property 1: Product creation persistence and event publishing
- Property 2: Product updates are atomic with events
- Property 3: Soft delete preserves data

## Next Steps

1. **Complete Task 8**: Implement Outbox pattern for guaranteed event delivery
2. **Add Integration Tests**: Test event publishing with real Kafka
3. **Implement Event Consumers**: Other services need to consume these events
4. **Add Monitoring**: Track event publishing metrics (success/failure rates)
5. **Add Dead Letter Queue**: Handle failed event processing

## Notes

- Event publishing is currently synchronous (blocking)
- Future enhancement: Async publishing with CompletableFuture
- Outbox pattern (task 8) will provide transactional guarantees
- Events follow the design document specifications exactly
