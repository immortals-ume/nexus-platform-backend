# Domain Starter

A Spring Boot starter providing domain-driven design (DDD) utilities, base entities, DTOs, and common domain models for microservices.

## 🚀 Features

- **Base Entity Classes**: Common entity patterns with audit fields
- **Standard DTOs**: Request/Response models for common operations
- **Domain Enums**: Standardized enumerations (OrderStatus, PaymentStatus, etc.)
- **Pagination Support**: Page request and response models
- **Address Utilities**: Standardized address handling
- **Rate Limiting**: Token bucket implementation
- **Product Domain**: Complete product management DTOs
- **User Domain**: User registration, login, and profile DTOs

## 📦 Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.immortals.platform</groupId>
    <artifactId>domain-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 💻 Usage

### Base Entity

Extend `BaseEntity` for all your domain entities:

```java
@Entity
@Table(name = "products")
public class Product extends BaseEntity {
    
    private String name;
    private BigDecimal price;
    private String description;
    
}
```

`BaseEntity` provides:
- `id` (UUID)
- `createdAt` (Instant)
- `updatedAt` (Instant)
- `createdBy` (String)
- `updatedBy` (String)
- `version` (for optimistic locking)

### Standard DTOs

#### API Response

```java
@RestController
public class ProductController {
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getProduct(@PathVariable String id) {
        Product product = productService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<Product>> createProduct(@RequestBody CreateProductRequest request) {
        Product product = productService.create(request);
        return ResponseEntity.ok(ApiResponse.success(product, "Product created successfully"));
    }
}
```

#### Pagination

```java
@GetMapping
public ResponseEntity<PageResponse<Product>> getProducts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String sortBy) {
    
    PageRequest pageRequest = PageRequest.builder()
        .page(page)
        .size(size)
        .sortBy(sortBy)
        .build();
    
    Page<Product> products = productService.findAll(pageRequest);
    
    PageResponse<Product> response = PageResponse.<Product>builder()
        .content(products.getContent())
        .page(page)
        .size(size)
        .totalElements(products.getTotalElements())
        .totalPages(products.getTotalPages())
        .build();
    
    return ResponseEntity.ok(response);
}
```

### Domain Enums

Standardized enumerations for common business concepts:

```java
@Entity
public class Order extends BaseEntity {
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;
    
    @Enumerated(EnumType.STRING)
    private ShipmentStatus shipmentStatus;
}
```

**Available Enums:**
- `OrderStatus`: PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED
- `PaymentStatus`: PENDING, AUTHORIZED, CAPTURED, FAILED, REFUNDED
- `ShipmentStatus`: PENDING, PICKED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED
- `ProductStatus`: DRAFT, ACTIVE, INACTIVE, OUT_OF_STOCK, DISCONTINUED

### Product Domain

Complete product management DTOs:

```java
@Service
public class ProductService {
    
    public Product createProduct(CreateProductRequest request) {
        Product product = Product.builder()
            .name(request.getName())
            .description(request.getDescription())
            .price(request.getPrice())
            .categoryId(request.getCategoryId())
            .build();
        
        return productRepository.save(product);
    }
    
    public Product updateProduct(String id, UpdateProductRequest request) {
        Product product = findById(id);
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        return productRepository.save(product);
    }
    
    public List<Product> filterProducts(ProductFilterRequest filter) {
        return productRepository.findByFilter(
            filter.getCategoryId(),
            filter.getMinPrice(),
            filter.getMaxPrice(),
            filter.getStatus()
        );
    }
}
```

### User Domain

User authentication and profile DTOs:

```java
@Service
public class UserService {
    
    public UserResponseDTO register(RegisterRequestDTO request) {
        User user = User.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .build();
        
        user = userRepository.save(user);
        
        return UserResponseDTO.from(user);
    }
    
    public LoginResponse login(LoginDto loginDto) {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginDto.getEmail(),
                loginDto.getPassword()
            )
        );
        
        String token = jwtService.generateToken(auth);
        
        return LoginResponse.builder()
            .token(token)
            .user(UserResponseDTO.from(auth.getPrincipal()))
            .build();
    }
}
```

### Address Utilities

Standardized address handling:

```java
@Entity
public class User extends BaseEntity {
    
    @Embedded
    private Address address;
}

Address address = Address.builder()
    .street("123 Main St")
    .city("New York")
    .state("NY")
    .country("USA")
    .zipCode("10001")
    .build();
```

### Rate Limiting

Token bucket implementation for rate limiting:

```java
@Service
public class ApiRateLimiter {
    
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    
    public boolean allowRequest(String userId) {
        TokenBucket bucket = buckets.computeIfAbsent(
            userId,
            k -> new TokenBucket(100, 10)
        );
        
        return bucket.tryConsume(1);
    }
}

@RestController
public class ApiController {
    
    @GetMapping("/api/data")
    public ResponseEntity<?> getData(@RequestHeader("User-Id") String userId) {
        if (!rateLimiter.allowRequest(userId)) {
            return ResponseEntity.status(429)
                .body(ErrorResponse.of("Rate limit exceeded"));
        }
        
        return ResponseEntity.ok(data);
    }
}
```

## 📋 Available DTOs

### User DTOs
- `RegisterRequestDTO`: User registration
- `LoginDto`: User login
- `LoginResponse`: Login response with token
- `UserDto`: User data transfer
- `UserResponseDTO`: User response
- `UserAddressDTO`: User address
- `ResetCredentials`: Password reset

### Product DTOs
- `CreateProductRequest`: Create new product
- `UpdateProductRequest`: Update existing product
- `ProductFilterRequest`: Filter products
- `CreateCategoryRequest`: Create product category
- `UpdateCategoryRequest`: Update product category

### Location DTOs
- `CountryDTO`: Country information
- `StateDTO`: State/province information
- `CityDTO`: City information

### Common DTOs
- `ApiResponse<T>`: Standard API response wrapper
- `PageResponse<T>`: Paginated response
- `PageRequest`: Pagination request
- `ErrorResponse`: Error response

## 🏗️ Domain Entities

### Base Entity

All entities extend `BaseEntity`:

```java
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    private String id;
    
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
    
    @Version
    private Long version;
}
```

### Price History

Track product price changes:

```java
@Entity
public class PriceHistory extends BaseEntity {
    private String productId;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private String reason;
}
```

## 🎯 Best Practices

### 1. Use DTOs for API Boundaries

```java
@PostMapping
public ResponseEntity<ApiResponse<ProductDTO>> createProduct(
        @Valid @RequestBody CreateProductRequest request) {
    
    Product product = productService.create(request);
    ProductDTO dto = ProductDTO.from(product);
    
    return ResponseEntity.ok(ApiResponse.success(dto));
}
```

### 2. Extend Base Entity

```java
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;
    
    private BigDecimal totalAmount;
}
```

### 3. Use Standard Enums

```java
public void processOrder(Order order) {
    order.setStatus(OrderStatus.PROCESSING);
    order.setPaymentStatus(PaymentStatus.AUTHORIZED);
    orderRepository.save(order);
}
```

### 4. Implement Pagination

```java
@GetMapping
public ResponseEntity<PageResponse<Order>> getOrders(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    
    PageRequest pageRequest = PageRequest.of(page, size);
    Page<Order> orders = orderService.findAll(pageRequest);
    
    return ResponseEntity.ok(PageResponse.from(orders));
}
```

### 5. Validate Input

```java
@PostMapping
public ResponseEntity<ApiResponse<User>> register(
        @Valid @RequestBody RegisterRequestDTO request) {
    
    User user = userService.register(request);
    return ResponseEntity.ok(ApiResponse.success(user));
}
```

## 🔍 Error Handling

Use `ErrorResponse` for consistent error responses:

```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
    ErrorResponse error = ErrorResponse.builder()
        .timestamp(Instant.now())
        .status(404)
        .error("Not Found")
        .message(ex.getMessage())
        .path(request.getRequestURI())
        .build();
    
    return ResponseEntity.status(404).body(error);
}
```

## 📊 Pagination Example

Complete pagination implementation:

```java
@Service
public class ProductService {
    
    public PageResponse<ProductDTO> findAll(PageRequest pageRequest) {
        Pageable pageable = PageRequest.of(
            pageRequest.getPage(),
            pageRequest.getSize(),
            Sort.by(pageRequest.getSortBy())
        );
        
        Page<Product> page = productRepository.findAll(pageable);
        
        List<ProductDTO> content = page.getContent().stream()
            .map(ProductDTO::from)
            .collect(Collectors.toList());
        
        return PageResponse.<ProductDTO>builder()
            .content(content)
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .first(page.isFirst())
            .last(page.isLast())
            .build();
    }
}
```

## 🔐 Security Considerations

1. **Never expose entities directly** - Use DTOs
2. **Validate all input** - Use `@Valid` annotation
3. **Sanitize user input** - Prevent injection attacks
4. **Use enums** - Type-safe status values
5. **Audit fields** - Track who created/updated records

## 🧪 Testing

```java
@SpringBootTest
class ProductServiceTest {
    
    @Test
    void shouldCreateProduct() {
        CreateProductRequest request = CreateProductRequest.builder()
            .name("Test Product")
            .price(new BigDecimal("99.99"))
            .categoryId("cat-123")
            .build();
        
        Product product = productService.create(request);
        
        assertThat(product.getId()).isNotNull();
        assertThat(product.getName()).isEqualTo("Test Product");
        assertThat(product.getCreatedAt()).isNotNull();
    }
}
```

## 📚 Dependencies

This starter depends on:
- Spring Boot 3.x
- Spring Data JPA
- Jakarta Validation API
- Lombok

## 🔄 Version History

- **1.0.0**: Initial release with base entities, DTOs, and enums

## 📄 License

Copyright © 2024 Immortals Platform

## 🆘 Support

For issues or questions, contact the platform team.
