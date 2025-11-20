# Implementation Plan

- [x] 1. Install Java 21 on the system
  - Download and install Java 21 LTS (OpenJDK or Oracle JDK)
  - macOS: `brew install openjdk@21` or download from https://jdk.java.net/21/
  - Set JAVA_HOME environment variable to Java 21 installation path
  - Verify installation: `java -version` (should show version 21.x.x)
  - Update IDE (IntelliJ/Eclipse/VS Code) to use Java 21 SDK
  - Configure Maven to use Java 21: `mvn -version` (should show Java version 21)
  - _Requirements: Development environment setup_

- [x] 2. Upgrade platform-parent and platform-bom to Spring Boot 3.4.1 and Java 21
  - Update platform-parent POM: Spring Boot 3.4.1 (from 3.5.4)
  - Update platform-bom: Spring Boot 3.4.1, Spring Cloud 2024.0.1
  - Update Java version to 21 (from 17) in parent POM
  - Update all dependency versions in BOM to latest stable versions
  - Update PostgreSQL driver to 42.7.4
  - Update MongoDB driver to 5.2.1
  - Update Kafka to 3.9.0
  - Update Redis Lettuce to 6.4.0
  - Update Elasticsearch to 8.16.1
  - Update Resilience4j to 2.2.0
  - Update Micrometer to 1.14.2
  - Update OpenTelemetry to 1.44.1
  - Update JUnit to 5.11.4
  - Update Mockito to 5.14.2
  - Update Testcontainers to 1.20.4
  - Enable Java 21 compiler settings (--enable-preview for virtual threads)
  - Update Maven compiler plugin to support Java 21
  - _Requirements: Platform foundation upgrade_

- [x] 2. Setup project structure and dependencies for all commerce services
  - Create Maven multi-module project structure for 14 commerce services under platform-services/commerce
  - Each service POM should inherit from platform-parent (com.immortals.platform:platform-parent:1.0.0)
  - Import platform-bom in dependencyManagement section for version management
  - Add platform starters dependencies: cache-starter, messaging-starter, domain-starter, common-starter
  - DO NOT add observability-starter or security-starter (each service handles observability and security independently)
  - Configure Spring Boot 3.4.1 and Spring Cloud 2024.0.1 (from parent POM)
  - Configure Java 21 as source/target version (maven.compiler.source=21, maven.compiler.target=21)
  - Setup database dependencies: PostgreSQL 42.7.4, MongoDB driver 5.2.1 (versions managed by BOM)
  - Configure Kafka client 3.9.0, Redis Lettuce 6.4.0, Elasticsearch 8.16.1 (versions managed by BOM)
  - Add Resilience4j 2.2.0 dependencies for circuit breakers, retries, bulkheads
  - Add Micrometer 1.14.2 and OpenTelemetry 1.44.1 for observability (each service configures independently)
  - Add Spring Security OAuth2 Resource Server for JWT validation (each service configures independently)
  - Enable Spring Boot 3.4 Virtual Threads (spring.threads.virtual.enabled=true)
  - Configure Tomcat to use virtual threads for request handling
  - _Requirements: All services foundation_

- [ ] 2.1 Enable Java 21 features across all services
  - Enable Virtual Threads for all @Async methods and scheduled tasks
  - Use Records for DTOs and immutable data classes
  - Use Pattern Matching for instanceof checks
  - Use Switch Expressions for cleaner conditional logic
  -FOR SQL 
  - Use Sealed Classes for domain hierarchies (OrderStatus, PaymentStatus)
  - Enable preview features if needed (--enable-preview flag)
  - Update code style to leverage Java 21 syntax improvements
  - _Requirements: Modern Java practices_

- [x] 3. Implement domain models in domain-starter with Java 21 features ( THERE WILL BE BUSSINEES MODELS ALSO TO BE HANDLED IN SAME DOMAIN STARTER. EITHER ADD BUSSINESS MODELS WEHEN RELVANT TASKS COMES UP  )
  - Create BaseEntity with audit fields (id, version, createdAt, updatedAt, createdBy, updatedBy)
  - Create common DTOs (PageRequest, PageResponse, ApiResponse, ErrorResponse)
  - Create domain value objects (Money, Address, PhoneNumber)
  - Create enums (OrderStatus, PaymentStatus, ProductStatus, ShipmentStatus)
  - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5_

- [ ]* 2.1 Write property test for BaseEntity audit fields
  - **Property 1: Audit fields are automatically populated**
  - **Validates: Requirements 13.2**

- [ ] 3. Implement exception handling in common-starter (ALREADY EXISTS CHECK WHETHER EUSABLE OTHERWISE YOU CAN UPDATE ONLY RELVANT PARTS REUSING THE EXISINTG )
  - Create BusinessException base class with error codes
  - Create specific exceptions (ResourceNotFoundException, ValidationException, BusinessRuleViolationException)
  - Implement GlobalExceptionHandler with @RestControllerAdvice
  - Create ErrorResponse builder with validation error details
  - Add HTTP status mapping for each exception type
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

- [ ]* 3.1 Write unit tests for exception handling
  - Test GlobalExceptionHandler for each exception type
  - Verify correct HTTP status codes
  - Verify error response format
  - _Requirements: 9.1, 9.2, 9.3, 9.4_

- [ ] 4. Implement Product Service - Core functionality
  - Create Product entity extending BaseEntity
  - Create Category entity with self-referencing parent relationship
  - Create PriceHistory entity for audit trail
  - Implement ProductRepository with JPA
  - Implement CategoryRepository with JPA
  - Create ProductService with CRUD operations
  - Create ProductController with REST endpoints
  - _Requirements: 1.1, 1.2, 1.3_

- [ ]* 4.1 Write property test for product creation
  - **Property 1: Product creation persistence and event publishing**
  - **Validates: Requirements 1.1**

- [ ]* 4.2 Write property test for product updates
  - **Property 2: Product updates are atomic with events**
  - **Validates: Requirements 1.2**

- [ ]* 4.3 Write property test for soft delete
  - **Property 3: Soft delete preserves data**
  - **Validates: Requirements 1.3**

- [ ] 5. Integrate Product Service with Cache Starter
  - Configure CacheService bean with Redis provider
  - Implement cache-aside pattern in ProductService
  - Add cache for product details (5 min TTL)
  - Add cache for category tree (15 min TTL)
  - Implement cache invalidation on updates
  - _Requirements: 1.4, 8.1, 8.2, 8.3_

- [ ]* 5.1 Write property test for caching
  - **Property 4: Cache-aside pattern correctness**
  - **Validates: Requirements 1.4**

- [ ]* 5.2 Write property test for cache invalidation
  - **Property 25: Cache invalidation on updates**
  - **Validates: Requirements 8.3**

- [ ] 6. Implement Product Service - Search and filtering
  - Add search endpoint with filters (category, price range, keyword)
  - Implement pagination with PageRequest/PageResponse
  - Add sorting support (price, name, date)
  - Create database indexes for search performance
  - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_

- [ ]* 6.1 Write property test for search results
  - **Property 5: Search results match criteria**
  - **Validates: Requirements 1.5, 12.1, 12.2, 12.3**

- [ ] 7. Implement Inventory Service - Core functionality
  - Create Inventory entity with optimistic locking
  - Create InventoryReservation entity
  - Implement InventoryRepository with version-based locking
  - Implement InventoryService with reserve/release operations
  - Create InventoryController with REST endpoints
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [ ]* 7.1 Write property test for inventory reservation
  - **Property 6: Inventory reservation prevents overselling**
  - **Validates: Requirements 2.2**

- [ ]* 7.2 Write property test for insufficient stock
  - **Property 7: Insufficient stock returns error without events**
  - **Validates: Requirements 2.3**

- [ ]* 7.3 Write property test for reservation/release
  - **Property 8: Reservation and release are inverse operations**
  - **Validates: Requirements 2.4**

- [ ]* 7.4 Write property test for low inventory alerts
  - **Property 9: Low inventory triggers alerts**
  - **Validates: Requirements 2.5**

- [ ] 8. Integrate Messaging Starter - Outbox pattern
  - Configure Kafka producer and consumer
  - Create OutboxEvent entity
  - Implement OutboxEventPublisher
  - Create OutboxProcessor scheduled job
  - Implement IdempotentEventHandler with Redis
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ]* 8.1 Write property test for outbox atomicity
  - **Property 14: Outbox pattern atomicity**
  - **Validates: Requirements 4.2, 7.1**

- [ ]* 8.2 Write property test for event publishing
  - **Property 20: Outbox events are eventually published**
  - **Validates: Requirements 7.2**

- [ ]* 8.3 Write property test for idempotency
  - **Property 21: Event processing idempotency**
  - **Validates: Requirements 7.3**

- [ ] 9. Implement Product Service - Event publishing
  - Create ProductCreatedEvent, ProductUpdatedEvent, ProductDeletedEvent
  - Integrate OutboxEventPublisher in ProductService
  - Publish events on create, update, delete operations
  - Add correlation ID propagation
  - _Requirements: 1.1, 1.2, 1.3, 7.5_

- [ ]* 9.1 Write property test for event structure
  - **Property 22: Event structure completeness**
  - **Validates: Requirements 7.5**

- [ ] 10. Implement Inventory Service - Event publishing
  - Create InventoryReservedEvent, InventoryReleasedEvent, LowInventoryEvent
  - Integrate OutboxEventPublisher in InventoryService
  - Publish events on reserve, release, low stock
  - _Requirements: 2.2, 2.4, 2.5_

- [ ] 11. Implement Customer Service - Core functionality
  - Create Customer document for MongoDB
  - Create Address embedded document
  - Create Loyalty embedded document
  - Implement CustomerRepository with MongoDB
  - Implement CustomerService with profile management
  - Create CustomerController with REST endpoints
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ]* 11.1 Write property test for default address
  - **Property 11: Only one default address invariant**
  - **Validates: Requirements 3.4**

- [ ]* 11.2 Write property test for profile cache
  - **Property 12: Profile updates invalidate cache**
  - **Validates: Requirements 3.2, 3.5**

- [ ] 12. Implement Customer Service - Event integration
  - Create CustomerCreatedEvent, CustomerUpdatedEvent
  - Implement UserCreatedEvent consumer from Auth Service
  - Create customer profile on UserCreatedEvent
  - Publish CustomerCreatedEvent on profile creation
  - _Requirements: 3.1_

- [ ]* 12.1 Write property test for customer creation from events
  - **Property 10: Customer profile creation from user events**
  - **Validates: Requirements 3.1**

- [ ] 13. Implement Order Service - Core functionality
  - Create Order entity with saga fields
  - Create OrderItem entity
  - Create OrderStatusHistory entity
  - Create SagaState entity
  - Implement OrderRepository, OrderItemRepository
  - Implement OrderService with order creation
  - Create OrderController with REST endpoints
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ]* 13.1 Write property test for order creation
  - **Property 13: Order creation initiates saga**
  - **Validates: Requirements 4.1**

- [ ] 14. Implement Order Service - Saga orchestration
  - Create SagaOrchestrator for order creation saga
  - Implement saga steps (reserve inventory, process payment, create shipment)
  - Implement compensating transactions (release inventory, refund payment)
  - Create SagaStateRepository
  - Implement saga step tracking and recovery
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ]* 14.1 Write property test for saga compensation
  - **Property 15: Saga compensation reverses completed steps**
  - **Validates: Requirements 6.3**

- [ ]* 14.2 Write property test for saga completion
  - **Property 16: Successful saga completes order**
  - **Validates: Requirements 6.4**

- [ ] 15. Implement Payment Service - Core functionality
  - Create Payment entity
  - Create PaymentMethod entity
  - Implement PaymentRepository, PaymentMethodRepository
  - Implement PaymentService with payment processing
  - Implement idempotency key handling with Redis
  - Create PaymentController with REST endpoints
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ]* 15.1 Write property test for idempotency
  - **Property 17: Idempotency prevents duplicate charges**
  - **Validates: Requirements 5.2**

- [ ]* 15.2 Write property test for payment state transitions
  - **Property 18: Payment state transitions are valid**
  - **Validates: Requirements 5.3, 5.4**

- [ ]* 15.3 Write property test for refunds
  - **Property 19: Refund creates inverse transaction**
  - **Validates: Requirements 5.5**

- [ ] 16. Implement Payment Service - Event integration
  - Create PaymentCompletedEvent, PaymentFailedEvent, PaymentRefundedEvent
  - Implement OrderCreatedEvent consumer
  - Process payment on OrderCreatedEvent
  - Publish payment events
  - _Requirements: 5.3, 5.4, 5.5_

- [ ] 17. Implement Order Service - Event consumers
  - Implement PaymentCompletedEvent consumer
  - Implement PaymentFailedEvent consumer
  - Implement InventoryReservedEvent consumer
  - Implement ShipmentDispatchedEvent consumer
  - Update order status based on events
  - _Requirements: 4.3, 4.4, 4.5_

- [ ] 18. Checkpoint - Ensure all tests pass for core services
  - Ensure all tests pass, ask the user if questions arise.


- [ ] 19. Implement Cart Service - Core functionality
  - Configure Redis for cart storage
  - Create CartItem model
  - Create Cart model with items and totals
  - Implement CartService with add/update/remove operations
  - Implement cart total calculation
  - Create CartController with REST endpoints
  - _Requirements: 41.1, 41.2, 41.3, 41.4_

- [ ]* 19.1 Write property test for cart totals
  - **Property 35: Cart total equals sum of items**
  - **Validates: Requirements 41.1**

- [ ] 20. Implement Cart Service - Event integration
  - Create CartItemAddedEvent, CartItemRemovedEvent, CartConvertedEvent
  - Implement OrderCreatedEvent consumer
  - Clear cart on OrderCreatedEvent
  - Publish CartConvertedEvent
  - _Requirements: 41.5_

- [ ]* 20.1 Write property test for cart conversion
  - **Property 36: Cart conversion clears cart**
  - **Validates: Requirements 41.5**

- [ ] 21. Implement Shipping Service - Core functionality
  - Create Shipment entity
  - Implement ShipmentRepository
  - Implement ShippingService with shipment creation
  - Implement tracking number management
  - Create ShippingController with REST endpoints
  - _Requirements: 43.1, 43.2, 43.3, 43.4, 43.5_

- [ ] 22. Implement Shipping Service - Event integration
  - Create ShipmentCreatedEvent, ShipmentDispatchedEvent, ShipmentDeliveredEvent
  - Implement OrderPaidEvent consumer
  - Create shipment on OrderPaidEvent
  - Publish shipment events
  - _Requirements: 43.1, 43.3, 43.5_

- [ ] 23. Implement Review Service - Core functionality
  - Create Review entity
  - Create Rating entity
  - Implement ReviewRepository
  - Implement ReviewService with review creation and moderation
  - Implement rating calculation
  - Create ReviewController with REST endpoints
  - _Requirements: 44.1, 44.2, 44.3, 44.4, 44.5_

- [ ] 24. Implement Review Service - Event integration
  - Create ReviewCreatedEvent, ReviewFlaggedEvent
  - Publish events on review creation and flagging
  - Implement event consumer to update product ratings
  - _Requirements: 44.2, 44.4, 44.5_

- [ ] 25. Implement Promotion Service - Core functionality
  - Create Promotion entity
  - Create Coupon entity
  - Implement PromotionRepository, CouponRepository
  - Implement PromotionService with discount calculation
  - Implement coupon validation and usage tracking
  - Create PromotionController with REST endpoints
  - _Requirements: 45.1, 45.2, 45.3, 45.4, 45.5_

- [ ]* 25.1 Write property test for coupon usage
  - **Property 39: Coupon usage is tracked**
  - **Validates: Requirements 45.3**

- [ ]* 25.2 Write property test for expired promotions
  - **Property 40: Expired promotions are not applied**
  - **Validates: Requirements 45.4**

- [ ] 26. Implement Promotion Service - Event integration
  - Create PromotionCreatedEvent, CouponUsedEvent, PromotionExpiredEvent
  - Publish events on promotion lifecycle
  - Implement scheduled job for promotion expiration
  - _Requirements: 45.1, 45.4_

- [ ] 27. Implement Wishlist Service - Core functionality
  - Create Wishlist entity
  - Create WishlistItem entity
  - Implement WishlistRepository
  - Implement WishlistService with add/remove operations
  - Create WishlistController with REST endpoints
  - _Requirements: 46.1, 46.2, 46.3, 46.4, 46.5_

- [ ] 28. Implement Wishlist Service - Event integration
  - Create PriceDropEvent, BackInStockEvent, WishlistToCartEvent
  - Implement ProductUpdatedEvent consumer for price changes
  - Implement InventoryUpdatedEvent consumer for stock changes
  - Publish wishlist events
  - _Requirements: 46.2, 46.3, 46.4_

- [ ] 29. Implement Search Service - Elasticsearch integration
  - Configure Elasticsearch client
  - Create ProductSearchDocument
  - Implement ProductSearchRepository
  - Implement SearchService with full-text search
  - Implement autocomplete and spell correction
  - Create SearchController with REST endpoints
  - _Requirements: 47.1, 47.2_

- [ ] 30. Implement Search Service - Event consumers
  - Implement ProductCreatedEvent consumer
  - Implement ProductUpdatedEvent consumer
  - Implement ProductDeletedEvent consumer
  - Update Elasticsearch index on product events
  - _Requirements: 47.5_

- [ ] 31. Implement Recommendation Service - Core functionality
  - Create UserBehavior entity for tracking
  - Implement RecommendationRepository
  - Implement RecommendationService with collaborative filtering
  - Implement similar products algorithm
  - Create RecommendationController with REST endpoints
  - _Requirements: 47.3, 47.4_

- [ ] 32. Implement Analytics Service - Core functionality
  - Configure TimeSeries database (InfluxDB or Prometheus)
  - Create SalesMetric entity
  - Create CustomerBehaviorMetric entity
  - Implement AnalyticsRepository
  - Implement AnalyticsService with metric aggregation
  - Create AnalyticsController with REST endpoints
  - _Requirements: 48.1, 48.2, 48.3, 48.4, 48.5_

- [ ] 33. Implement Analytics Service - Event consumers
  - Implement OrderCompletedEvent consumer
  - Implement ProductViewedEvent consumer
  - Aggregate metrics in real-time
  - Generate reports on demand
  - _Requirements: 48.1, 48.2_

- [ ] 34. Implement Fraud Detection Service - Core functionality
  - Create FraudRule entity
  - Create RiskScore entity
  - Implement FraudDetectionRepository
  - Implement FraudDetectionService with risk scoring
  - Implement ML model integration (placeholder)
  - Create FraudDetectionController with REST endpoints
  - _Requirements: 49.1, 49.2, 49.3, 49.4, 49.5_

- [ ] 35. Implement Fraud Detection Service - Event integration
  - Create HighRiskOrderEvent, FraudConfirmedEvent
  - Implement OrderCreatedEvent consumer
  - Analyze orders for fraud on creation
  - Publish fraud events
  - _Requirements: 49.1, 49.2, 49.4_

- [ ] 36. Implement Return Service - Core functionality
  - Create Return entity
  - Create ReturnItem entity
  - Implement ReturnRepository
  - Implement ReturnService with return processing
  - Implement refund initiation
  - Create ReturnController with REST endpoints
  - _Requirements: 50.1, 50.2, 50.3, 50.4, 50.5_

- [ ] 37. Implement Return Service - Event integration
  - Create ReturnApprovedEvent, ReturnReceivedEvent, RefundRequestedEvent
  - Implement return workflow with events
  - Integrate with Payment Service for refunds
  - Integrate with Inventory Service for restocking
  - _Requirements: 50.2, 50.3, 50.4_

- [ ] 38. Implement Customer Service - Loyalty program
  - Add loyalty points calculation logic
  - Implement points earning on order completion
  - Implement points redemption
  - Implement tier management
  - _Requirements: 16.1, 16.2, 16.3, 16.4, 16.5_

- [ ]* 38.1 Write property test for loyalty points
  - **Property 37: Order completion awards points**
  - **Validates: Requirements 16.1**

- [ ]* 38.2 Write property test for points redemption
  - **Property 38: Points redemption decrements balance**
  - **Validates: Requirements 16.3**

- [ ] 39. Implement Customer Service - Loyalty events
  - Create LoyaltyPointsEarnedEvent, LoyaltyPointsRedeemedEvent, LoyaltyTierChangedEvent
  - Implement OrderCompletedEvent consumer
  - Award points on order completion
  - Publish loyalty events
  - _Requirements: 16.1, 16.3, 16.4_

- [ ] 40. Implement Payment Service - Payment methods
  - Implement payment method tokenization
  - Implement default payment method management
  - Implement payment method soft delete
  - Add payment method expiry handling
  - _Requirements: 17.1, 17.2, 17.3, 17.4, 17.5_

- [ ] 41. Implement Order Service - Cancellation and refunds
  - Implement order cancellation logic
  - Implement cancellation saga with compensation
  - Integrate with Payment Service for refunds
  - Integrate with Inventory Service for release
  - _Requirements: 18.1, 18.2, 18.3, 18.4, 18.5_

- [ ] 42. Implement Product Service - Pricing and promotions
  - Add promotional price fields
  - Implement price history tracking
  - Implement promotion period validation
  - Integrate with Promotion Service
  - _Requirements: 19.1, 19.2, 19.3, 19.4, 19.5_

- [ ] 43. Checkpoint - Ensure all tests pass for enhancement services
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 44. Implement Resilience patterns - Circuit breakers
  - Configure Resilience4j circuit breakers
  - Add circuit breakers for external service calls
  - Implement fallback responses
  - Configure circuit breaker thresholds
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

- [ ]* 44.1 Write property test for circuit breaker
  - **Property 31: Circuit breaker opens on threshold**
  - **Validates: Requirements 10.2**

- [ ]* 44.2 Write property test for fallback
  - **Property 32: Open circuit returns fallback**
  - **Validates: Requirements 10.3**

- [ ] 45. Implement Resilience patterns - Retry and timeout
  - Configure retry policies with exponential backoff
  - Configure timeout policies
  - Implement idempotent retry handling
  - Add retry metrics
  - _Requirements: 33.1, 33.2, 33.3, 33.4, 33.5_

- [ ]* 45.1 Write property test for exponential backoff
  - **Property 33: Exponential backoff increases delay**
  - **Validates: Requirements 33.1**

- [ ]* 45.2 Write property test for idempotent retries
  - **Property 34: Idempotent retries are safe**
  - **Validates: Requirements 33.4**

- [ ] 46. Implement Resilience patterns - Bulkhead
  - Configure thread pool bulkheads
  - Separate thread pools for external dependencies
  - Configure bulkhead limits
  - Add bulkhead metrics
  - _Requirements: 26.1, 26.2, 26.3, 26.4, 26.5_

- [ ] 47. Implement Observability in each service
  - Configure Micrometer tracing in each service independently
  - Configure OpenTelemetry exporter (Zipkin/Jaeger)
  - Add trace spans to business operations
  - Add correlation ID propagation across services
  - Configure sampling rates (100% dev, 10% prod)
  - Configure Prometheus metrics endpoint
  - Add business metrics (orders created, payments processed, inventory reserved)
  - Add technical metrics (cache hit rate, event processing time, API latency)
  - Add JVM and system metrics
  - Configure structured logging with JSON format (Logstash encoder)
  - Add correlation IDs to all logs
  - Add contextual information (user ID, order ID, customer ID)
  - Configure log levels per environment
  - _Requirements: 14.3, 28.1, 28.2, 28.3, 28.4, 28.5, 37.1, 37.2, 37.3, 37.4, 37.5_

- [ ] 48. Integrate with existing Auth Service for JWT validation
  - Configure Spring Security OAuth2 Resource Server in each commerce service
  - Configure JWT validation using Auth Service's public key/secret
  - Set JWT issuer to Auth Service URL (e.g., http://auth-service:8080)
  - Configure JWT claims extraction (username, roles, permissions, userId)
  - Implement role-based access control using @PreAuthorize annotations
  - Secure all REST endpoints (except /actuator/health, /actuator/metrics)
  - Create SecurityConfig class in each service with JWT validation
  - Extract user context from JWT claims and store in SecurityContext
  - Add authentication filter to validate JWT tokens on each request
  - Configure CORS to allow requests from API Gateway
  - Add security headers (X-Frame-Options, X-Content-Type-Options, CSP)
  - Implement encryption for payment information (AES-256)
  - Implement encryption for PII fields in Customer Service
  - Configure encryption keys management (externalized in application.yml)
  - Test JWT validation with tokens from Auth Service
  - _Requirements: 29.1, 29.2, 29.3, 29.4, 29.5, 40.2_

- [ ] 48.1 Configure Auth Service integration
  - Ensure Auth Service is running and accessible
  - Get JWT secret or public key from Auth Service configuration
  - Configure commerce services to validate JWT tokens issued by Auth Service
  - Set up service-to-service communication for user validation
  - Configure Feign client to call Auth Service for user details if needed
  - Test end-to-end authentication flow: Login via Auth Service → Get JWT → Call Commerce Service
  - _Requirements: 29.1, 29.2_

- [ ] 52. Implement CQRS - Read models
  - Create read models for order history
  - Create read models for product search
  - Implement event consumers to update read models
  - Configure read replicas for queries
  - _Requirements: 25.1, 25.2, 25.3, 25.4, 25.5_

- [ ]* 52.1 Write property test for CQRS
  - **Property 29: Read model reflects write model**
  - **Validates: Requirements 25.3**

- [ ]* 52.2 Write property test for read/write independence
  - **Property 30: Read and write models are independent**
  - **Validates: Requirements 25.1**

- [ ] 53. Implement Event Sourcing - Order events
  - Create event store for order events
  - Store all order state changes as events
  - Implement event replay for order reconstruction
  - Add event versioning
  - _Requirements: 27.1, 27.2, 27.3, 27.4, 27.5_

- [ ] 54. Implement Health checks
  - Add liveness probes for all services
  - Add readiness probes for all services
  - Implement dependency health checks (database, cache, Kafka)
  - Configure health check endpoints
  - _Requirements: 34.1, 34.2, 34.3, 34.4, 34.5_

- [ ] 55. Implement API versioning
  - Add URL-based versioning (/api/v1/)
  - Implement version routing
  - Add deprecation headers
  - Document version compatibility
  - _Requirements: 38.1, 38.2, 38.3, 38.4, 38.5_

- [ ] 56. Implement Graceful degradation
  - Add fallback for cache failures
  - Add fallback for notification failures
  - Add fallback for non-critical service failures
  - Implement partial response handling
  - _Requirements: 36.1, 36.2, 36.3, 36.4, 36.5_

- [ ] 57. Implement Compliance - Data privacy
  - Implement customer data deletion
  - Implement data anonymization
  - Implement data export functionality
  - Add audit logging for data access
  - Implement data retention policies
  - _Requirements: 40.1, 40.2, 40.3, 40.4, 40.5_

- [ ] 58. Configure API Gateway integration
  - Configure routes for all commerce services
  - Verify rate limiting configuration
  - Verify circuit breaker configuration
  - Add correlation ID propagation
  - Test end-to-end request flow
  - _Requirements: 20.1, 20.2, 20.3, 20.4, 20.5_

- [ ] 59. Setup Docker Compose for development
  - Create docker-compose.yml with all dependencies
  - Configure PostgreSQL containers for each service
  - Configure MongoDB container
  - Configure Redis container
  - Configure Kafka and Zookeeper containers
  - Configure Elasticsearch container
  - Configure Zipkin container
  - Add service containers with environment variables

- [ ] 60. Setup Kubernetes manifests for production
  - Create Deployment manifests for all services
  - Create Service manifests for all services
  - Create ConfigMap for configuration
  - Create Secret for sensitive data
  - Configure resource limits and requests
  - Configure liveness and readiness probes
  - Configure horizontal pod autoscaling

- [ ] 61. Create database migration scripts
  - Create Flyway migrations for Product Service
  - Create Flyway migrations for Order Service
  - Create Flyway migrations for Payment Service
  - Create Flyway migrations for Inventory Service
  - Create Flyway migrations for all other services
  - Add rollback scripts

- [ ] 62. Final Checkpoint - End-to-end testing
  - Ensure all tests pass, ask the user if questions arise.
  - Test complete order flow (product → cart → order → payment → shipment)
  - Test cancellation and refund flow
  - Test event propagation across services
  - Verify observability (traces, metrics, logs)
  - Verify resilience patterns (circuit breakers, retries)
  - Load test critical paths
