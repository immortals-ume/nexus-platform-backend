# Domain Starter

🏛️ **Domain-driven design utilities and foundational components** for Spring Boot microservices. This starter provides base entities, standard DTOs, domain enums, and common domain patterns that accelerate development while maintaining consistency across services.

## 🌟 Key Features

### 🏗️ Base Entity Classes
- **Audit Fields**: Automatic tracking of creation and modification
- **Optimistic Locking**: Built-in version control for concurrent updates
- **UUID Primary Keys**: Distributed-friendly unique identifiers
- **Soft Delete Support**: Logical deletion with recovery capabilities

### 📋 Standard DTOs
- **Request/Response Models**: Common patterns for API operations
- **Pagination Support**: Complete pagination request/response models
- **Validation Integration**: Built-in validation with Bean Validation
- **Conversion Utilities**: Easy entity-to-DTO mapping

### 🎯 Domain Enums
- **Standardized Enumerations**: Common business status values
- **Order Management**: OrderStatus, PaymentStatus, ShipmentStatus
- **Product Management**: ProductStatus, CategoryType
- **User Management**: UserRole, AccountStatus

### 🔧 Utility Components
- **Address Handling**: Standardized address models and validation
- **Rate Limiting**: Token bucket implementation for API throttling
- **Price History**: Track price changes over time
- **Audit Logging**: Domain event tracking and history

## 📦 Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.immortals.platform</groupId>
    <artifactId>domain-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 🏗️ Architecture

### Package Structure

```
com.immortals.platform.domain
├── entity/                 # Base entity classes
│   ├── BaseEntity         # Base entity with audit fields
│   ├── SoftDeletableEntity # Soft delete support
│   └── VersionedEntity    # Optimistic locking
├── dto/                   # Data Transfer Objects
│   ├── request/          # Request DTOs
│   ├── response/         # Response DTOs
│   └── common/           # Common DTOs
├── enums/                # Domain enumerations
│   ├── OrderStatus       # Order lifecycle states
│   ├── PaymentStatus     # Payment states
│   └── ProductStatus     # Product states
├── util/                 # Domain utilities
│   ├── AddressUtils      # Address handling
│   ├── TokenBucket       # Rate limiting
│   └── PriceCalculator   # Price calculations
└── config/              # Auto-configuration
    └── DomainAutoConfiguration
```

## 💻 Usage Examples

### Base Entity Usage

```java
@Entity
@Table(name = "products")
public class Product extends BaseEntity {
    
    @Column(nullable = false)
    private String name;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal price;
    
    private String description;
    
    @Enumerated(EnumType.STRING)
    private ProductStatus status = ProductStatus.DRAFT;
    
    @Column(name = "category_id")
    private String categoryId;
    
    // Getters, setters, builders...
}
```

### Standard DTOs

```java
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductService productService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponseDTO>> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        
        Product product = productService.create(request);
        ProductResponseDTO response = ProductResponseDTO.from(product);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Product created successfully"));
    }
    
    @GetMapping
    public ResponseEntity<PageResponse<ProductResponseDTO>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) ProductStatus status) {
        
        ProductFilterRequest filter = ProductFilterRequest.builder()
            .categoryId(categoryId)
            .status(status)
            .build();
        
        PageRequest pageRequest = PageRequest.of(page, size);
        PageResponse<ProductResponseDTO> response = productService.findAll(filter, pageRequest);
        
        return ResponseEntity.ok(response);
    }
}
```
### Domain Enums Usage

```java
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {
    
    @Column(unique = true, nullable = false)
    private String orderNumber;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    private ShipmentStatus shipmentStatus = ShipmentStatus.PENDING;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Embedded
    private Address shippingAddress;
    
    // Business methods
    public void confirm() {
        if (status != OrderStatus.PENDING) {
            throw new InvalidOperationException("Can only confirm pending orders");
        }
        this.status = OrderStatus.CONFIRMED;
    }
    
    public void ship() {
        if (status != OrderStatus.PROCESSING) {
            throw new InvalidOperationException("Can only ship processing orders");
        }
        this.status = OrderStatus.SHIPPED;
        this.shipmentStatus = ShipmentStatus.IN_TRANSIT;
    }
    
    public void deliver() {
        if (status != OrderStatus.SHIPPED) {
            throw new InvalidOperationException("Can only deliver shipped orders");
        }
        this.status = OrderStatus.DELIVERED;
        this.shipmentStatus = ShipmentStatus.DELIVERED;
    }
}
```

### Address Handling

```java
@Entity
@Table(name = "users")
public class User extends BaseEntity {
    
    private String email;
    private String firstName;
    private String lastName;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "billing_street")),
        @AttributeOverride(name = "city", column = @Column(name = "billing_city")),
        @AttributeOverride(name = "state", column = @Column(name = "billing_state")),
        @AttributeOverride(name = "country", column = @Column(name = "billing_country")),
        @AttributeOverride(name = "zipCode", column = @Column(name = "billing_zip_code"))
    })
    private Address billingAddress;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "shipping_street")),
        @AttributeOverride(name = "city", column = @Column(name = "shipping_city")),
        @AttributeOverride(name = "state", column = @Column(name = "shipping_state")),
        @AttributeOverride(name = "country", column = @Column(name = "shipping_country")),
        @AttributeOverride(name = "zipCode", column = @Column(name = "shipping_zip_code"))
    })
    private Address shippingAddress;
}

// Usage in service
@Service
public class UserService {
    
    public User createUser(RegisterRequestDTO request) {
        Address address = Address.builder()
            .street(request.getAddress().getStreet())
            .city(request.getAddress().getCity())
            .state(request.getAddress().getState())
            .country(request.getAddress().getCountry())
            .zipCode(request.getAddress().getZipCode())
            .build();
        
        // Validate address
        AddressUtils.validateAddress(address);
        
        User user = User.builder()
            .email(request.getEmail())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .billingAddress(address)
            .shippingAddress(address) // Same as billing initially
            .build();
        
        return userRepository.save(user);
    }
}
```

### Rate Limiting

```java
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicApiController {
    
    private final Map<String, TokenBucket> rateLimiters = new ConcurrentHashMap<>();
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> search(
            @RequestParam String query,
            HttpServletRequest request) {
        
        String clientIp = getClientIp(request);
        TokenBucket bucket = rateLimiters.computeIfAbsent(
            clientIp,
            k -> new TokenBucket(100, 10) // 100 tokens, refill 10 per second
        );
        
        if (!bucket.tryConsume(1)) {
            throw new TechnicalException("Rate limit exceeded. Please try again later.");
        }
        
        List<ProductDTO> results = searchService.search(query);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

### Pagination Implementation

```java
@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository productRepository;
    
    public PageResponse<ProductResponseDTO> findAll(ProductFilterRequest filter, PageRequest pageRequest) {
        // Build specification from filter
        Specification<Product> spec = buildSpecification(filter);
        
        // Create Spring Data pageable
        Sort sort = Sort.by(Sort.Direction.fromString(pageRequest.getSortDirection()), 
                           pageRequest.getSortBy());
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
            pageRequest.getPage(), 
            pageRequest.getSize(), 
            sort
        );
        
        // Execute query
        Page<Product> page = productRepository.findAll(spec, pageable);
        
        // Convert to DTOs
        List<ProductResponseDTO> content = page.getContent().stream()
            .map(ProductResponseDTO::from)
            .collect(Collectors.toList());
        
        // Build response
        return PageResponse.<ProductResponseDTO>builder()
            .content(content)
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .first(page.isFirst())
            .last(page.isLast())
            .numberOfElements(page.getNumberOfElements())
            .empty(page.isEmpty())
            .build();
    }
    
    private Specification<Product> buildSpecification(ProductFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (filter.getCategoryId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("categoryId"), filter.getCategoryId()));
            }
            
            if (filter.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }
            
            if (filter.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), filter.getMinPrice()));
            }
            
            if (filter.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), filter.getMaxPrice()));
            }
            
            if (filter.getSearchTerm() != null) {
                String searchPattern = "%" + filter.getSearchTerm().toLowerCase() + "%";
                Predicate namePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")), searchPattern);
                Predicate descriptionPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("description")), searchPattern);
                predicates.add(criteriaBuilder.or(namePredicate, descriptionPredicate));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
```

## 📋 Available DTOs

### User Management DTOs

```java
// Registration
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDTO {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    @Valid
    private UserAddressDTO address;
}

// Login
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginDto {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Password is required")
    private String password;
}

// Response
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private UserRole role;
    private AccountStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    
    public static UserResponseDTO from(User user) {
        return UserResponseDTO.builder()
            .id(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .role(user.getRole())
            .status(user.getStatus())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }
}
```

### Product Management DTOs

```java
// Create Product
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {
    
    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Product name must not exceed 255 characters")
    private String name;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price format is invalid")
    private BigDecimal price;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    @NotBlank(message = "Category ID is required")
    private String categoryId;
    
    private ProductStatus status = ProductStatus.DRAFT;
}

// Update Product
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRequest {
    
    @Size(max = 255, message = "Product name must not exceed 255 characters")
    private String name;
    
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price format is invalid")
    private BigDecimal price;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    private String categoryId;
    private ProductStatus status;
}

// Filter Products
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilterRequest {
    
    private String categoryId;
    private ProductStatus status;
    
    @DecimalMin(value = "0.0", message = "Minimum price must be non-negative")
    private BigDecimal minPrice;
    
    @DecimalMin(value = "0.0", message = "Maximum price must be non-negative")
    private BigDecimal maxPrice;
    
    @Size(max = 100, message = "Search term must not exceed 100 characters")
    private String searchTerm;
}
```

## 🎯 Domain Enums Reference

### Order Management

```java
public enum OrderStatus {
    PENDING("Order is pending confirmation"),
    CONFIRMED("Order has been confirmed"),
    PROCESSING("Order is being processed"),
    SHIPPED("Order has been shipped"),
    DELIVERED("Order has been delivered"),
    CANCELLED("Order has been cancelled"),
    RETURNED("Order has been returned");
    
    private final String description;
    
    OrderStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean canTransitionTo(OrderStatus newStatus) {
        switch (this) {
            case PENDING:
                return newStatus == CONFIRMED || newStatus == CANCELLED;
            case CONFIRMED:
                return newStatus == PROCESSING || newStatus == CANCELLED;
            case PROCESSING:
                return newStatus == SHIPPED || newStatus == CANCELLED;
            case SHIPPED:
                return newStatus == DELIVERED || newStatus == RETURNED;
            case DELIVERED:
                return newStatus == RETURNED;
            default:
                return false;
        }
    }
}

public enum PaymentStatus {
    PENDING("Payment is pending"),
    AUTHORIZED("Payment has been authorized"),
    CAPTURED("Payment has been captured"),
    FAILED("Payment has failed"),
    REFUNDED("Payment has been refunded"),
    PARTIALLY_REFUNDED("Payment has been partially refunded");
    
    private final String description;
    
    PaymentStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isSuccessful() {
        return this == AUTHORIZED || this == CAPTURED;
    }
    
    public boolean canRefund() {
        return this == CAPTURED;
    }
}

public enum ShipmentStatus {
    PENDING("Shipment is pending"),
    PICKED("Items have been picked"),
    IN_TRANSIT("Shipment is in transit"),
    OUT_FOR_DELIVERY("Shipment is out for delivery"),
    DELIVERED("Shipment has been delivered"),
    FAILED_DELIVERY("Delivery attempt failed"),
    RETURNED_TO_SENDER("Shipment returned to sender");
    
    private final String description;
    
    ShipmentStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isInProgress() {
        return this == PICKED || this == IN_TRANSIT || this == OUT_FOR_DELIVERY;
    }
    
    public boolean isCompleted() {
        return this == DELIVERED;
    }
}
```

### Product Management

```java
public enum ProductStatus {
    DRAFT("Product is in draft state"),
    ACTIVE("Product is active and available"),
    INACTIVE("Product is inactive"),
    OUT_OF_STOCK("Product is out of stock"),
    DISCONTINUED("Product has been discontinued");
    
    private final String description;
    
    ProductStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isAvailable() {
        return this == ACTIVE;
    }
    
    public boolean canPurchase() {
        return this == ACTIVE;
    }
}

public enum CategoryType {
    ELECTRONICS("Electronics and gadgets"),
    CLOTHING("Clothing and accessories"),
    BOOKS("Books and literature"),
    HOME_GARDEN("Home and garden items"),
    SPORTS("Sports and outdoor equipment"),
    TOYS("Toys and games"),
    AUTOMOTIVE("Automotive parts and accessories"),
    HEALTH_BEAUTY("Health and beauty products");
    
    private final String description;
    
    CategoryType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
```

## 🔧 Utility Components

### Address Utilities

```java
@Component
public class AddressUtils {
    
    private static final Set<String> VALID_COUNTRIES = Set.of(
        "US", "CA", "GB", "DE", "FR", "IT", "ES", "AU", "JP", "IN"
    );
    
    public static void validateAddress(Address address) {
        ValidationUtils.requireNonNull(address, "address");
        ValidationUtils.requireNonBlank(address.getStreet(), "street");
        ValidationUtils.requireNonBlank(address.getCity(), "city");
        ValidationUtils.requireNonBlank(address.getState(), "state");
        ValidationUtils.requireNonBlank(address.getCountry(), "country");
        ValidationUtils.requireNonBlank(address.getZipCode(), "zipCode");
        
        if (!VALID_COUNTRIES.contains(address.getCountry().toUpperCase())) {
            throw new ValidationException("Invalid country code: " + address.getCountry());
        }
        
        validateZipCode(address.getZipCode(), address.getCountry());
    }
    
    public static String formatAddress(Address address) {
        if (address == null) {
            return "";
        }
        
        return String.format("%s, %s, %s %s, %s",
            address.getStreet(),
            address.getCity(),
            address.getState(),
            address.getZipCode(),
            address.getCountry());
    }
    
    public static boolean isSameAddress(Address addr1, Address addr2) {
        if (addr1 == null && addr2 == null) {
            return true;
        }
        if (addr1 == null || addr2 == null) {
            return false;
        }
        
        return Objects.equals(normalize(addr1.getStreet()), normalize(addr2.getStreet())) &&
               Objects.equals(normalize(addr1.getCity()), normalize(addr2.getCity())) &&
               Objects.equals(normalize(addr1.getState()), normalize(addr2.getState())) &&
               Objects.equals(normalize(addr1.getCountry()), normalize(addr2.getCountry())) &&
               Objects.equals(normalize(addr1.getZipCode()), normalize(addr2.getZipCode()));
    }
    
    private static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }
    
    private static void validateZipCode(String zipCode, String country) {
        switch (country.toUpperCase()) {
            case "US":
                if (!zipCode.matches("^\\d{5}(-\\d{4})?$")) {
                    throw new ValidationException("Invalid US zip code format");
                }
                break;
            case "CA":
                if (!zipCode.matches("^[A-Za-z]\\d[A-Za-z] \\d[A-Za-z]\\d$")) {
                    throw new ValidationException("Invalid Canadian postal code format");
                }
                break;
            case "GB":
                if (!zipCode.matches("^[A-Za-z]{1,2}\\d[A-Za-z\\d]? \\d[A-Za-z]{2}$")) {
                    throw new ValidationException("Invalid UK postal code format");
                }
                break;
            // Add more country-specific validations as needed
        }
    }
}
```

### Token Bucket Rate Limiter

```java
public class TokenBucket {
    
    private final long capacity;
    private final long refillRate;
    private long tokens;
    private long lastRefillTime;
    
    public TokenBucket(long capacity, long refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.tokens = capacity;
        this.lastRefillTime = System.currentTimeMillis();
    }
    
    public synchronized boolean tryConsume(long tokensRequested) {
        refill();
        
        if (tokens >= tokensRequested) {
            tokens -= tokensRequested;
            return true;
        }
        
        return false;
    }
    
    public synchronized long getAvailableTokens() {
        refill();
        return tokens;
    }
    
    private void refill() {
        long now = System.currentTimeMillis();
        long timePassed = now - lastRefillTime;
        
        if (timePassed > 0) {
            long tokensToAdd = (timePassed / 1000) * refillRate;
            tokens = Math.min(capacity, tokens + tokensToAdd);
            lastRefillTime = now;
        }
    }
}

// Usage in service
@Service
public class ApiRateLimitService {
    
    private final Map<String, TokenBucket> userBuckets = new ConcurrentHashMap<>();
    private final Map<String, TokenBucket> ipBuckets = new ConcurrentHashMap<>();
    
    public boolean allowRequest(String userId, String ipAddress) {
        // Check user-specific rate limit
        if (userId != null) {
            TokenBucket userBucket = userBuckets.computeIfAbsent(
                userId, 
                k -> new TokenBucket(1000, 50) // 1000 tokens, 50 per second
            );
            
            if (!userBucket.tryConsume(1)) {
                return false;
            }
        }
        
        // Check IP-based rate limit
        TokenBucket ipBucket = ipBuckets.computeIfAbsent(
            ipAddress,
            k -> new TokenBucket(100, 10) // 100 tokens, 10 per second
        );
        
        return ipBucket.tryConsume(1);
    }
    
    @Scheduled(fixedRate = 300000) // Clean up every 5 minutes
    public void cleanupBuckets() {
        // Remove buckets that haven't been used recently
        long cutoff = System.currentTimeMillis() - 300000; // 5 minutes ago
        
        userBuckets.entrySet().removeIf(entry -> 
            entry.getValue().lastRefillTime < cutoff);
        ipBuckets.entrySet().removeIf(entry -> 
            entry.getValue().lastRefillTime < cutoff);
    }
}
```

## 🧪 Testing

### Entity Testing

```java
@DataJpaTest
class ProductEntityTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Test
    void shouldPersistProductWithAuditFields() {
        // Given
        Product product = Product.builder()
            .name("Test Product")
            .price(new BigDecimal("99.99"))
            .description("Test description")
            .status(ProductStatus.ACTIVE)
            .categoryId("cat-123")
            .build();
        
        // When
        Product saved = entityManager.persistAndFlush(product);
        
        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getVersion()).isEqualTo(0L);
    }
    
    @Test
    void shouldUpdateVersionOnModification() {
        // Given
        Product product = Product.builder()
            .name("Test Product")
            .price(new BigDecimal("99.99"))
            .build();
        
        Product saved = entityManager.persistAndFlush(product);
        Long originalVersion = saved.getVersion();
        
        // When
        saved.setPrice(new BigDecimal("149.99"));
        Product updated = entityManager.persistAndFlush(saved);
        
        // Then
        assertThat(updated.getVersion()).isEqualTo(originalVersion + 1);
        assertThat(updated.getUpdatedAt()).isAfter(updated.getCreatedAt());
    }
}
```

### DTO Testing

```java
class ProductDTOTest {
    
    @Test
    void shouldConvertEntityToResponseDTO() {
        // Given
        Product product = Product.builder()
            .id("prod-123")
            .name("Test Product")
            .price(new BigDecimal("99.99"))
            .description("Test description")
            .status(ProductStatus.ACTIVE)
            .categoryId("cat-123")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        // When
        ProductResponseDTO dto = ProductResponseDTO.from(product);
        
        // Then
        assertThat(dto.getId()).isEqualTo(product.getId());
        assertThat(dto.getName()).isEqualTo(product.getName());
        assertThat(dto.getPrice()).isEqualTo(product.getPrice());
        assertThat(dto.getDescription()).isEqualTo(product.getDescription());
        assertThat(dto.getStatus()).isEqualTo(product.getStatus());
        assertThat(dto.getCategoryId()).isEqualTo(product.getCategoryId());
    }
    
    @Test
    void shouldValidateCreateProductRequest() {
        // Given
        CreateProductRequest request = CreateProductRequest.builder()
            .name("") // Invalid - blank
            .price(new BigDecimal("-10")) // Invalid - negative
            .categoryId(null) // Invalid - null
            .build();
        
        // When
        Set<ConstraintViolation<CreateProductRequest>> violations = 
            validator.validate(request);
        
        // Then
        assertThat(violations).hasSize(3);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
            .containsExactlyInAnyOrder(
                "Product name is required",
                "Price must be greater than 0",
                "Category ID is required"
            );
    }
}
```

### Enum Testing

```java
class OrderStatusTest {
    
    @Test
    void shouldAllowValidTransitions() {
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.CONFIRMED)).isTrue();
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.PROCESSING)).isTrue();
        assertThat(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.SHIPPED)).isTrue();
        assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED)).isTrue();
    }
    
    @Test
    void shouldRejectInvalidTransitions() {
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.SHIPPED)).isFalse();
        assertThat(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.PENDING)).isFalse();
        assertThat(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.CONFIRMED)).isFalse();
    }
}
```

## 📚 Best Practices

### 1. Entity Design
- **Extend BaseEntity** for all domain entities
- **Use enums** for status fields
- **Implement business methods** on entities
- **Use embedded objects** for value objects like Address

### 2. DTO Usage
- **Separate request/response DTOs** for different operations
- **Use validation annotations** on request DTOs
- **Implement static factory methods** for entity-to-DTO conversion
- **Keep DTOs simple** - no business logic

### 3. Enum Design
- **Add descriptions** to enum values
- **Implement business methods** for state transitions
- **Use meaningful names** that reflect business concepts
- **Document valid transitions** in comments

### 4. Pagination
- **Use consistent pagination parameters** across all endpoints
- **Implement sorting** with sensible defaults
- **Validate page parameters** to prevent errors
- **Include metadata** in pagination responses

### 5. Validation
- **Validate at boundaries** (controller layer)
- **Use Bean Validation** annotations
- **Implement custom validators** for complex rules
- **Provide clear error messages**

## 📄 License

Copyright © 2024 Immortals Platform

Licensed under the Apache License, Version 2.0

## 🆘 Support

- 📖 **Documentation**: [Platform Starters Documentation](../README.md)
- 🐛 **Issues**: [GitHub Issues](https://github.com/YOUR_USERNAME/YOUR_REPO/issues)
- 💬 **Discussions**: [GitHub Discussions](https://github.com/YOUR_USERNAME/YOUR_REPO/discussions)
- 📧 **Email**: kapilsrivastava712@gmail.com

---

**Built with ❤️ by the Immortals Platform Team**