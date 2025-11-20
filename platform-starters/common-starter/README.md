# Common Starter

A Spring Boot starter that provides common utilities, exception handling, and API response models for all microservices in the platform.

## Features

### 1. Exception Hierarchy
- **PlatformException**: Abstract base exception for all platform exceptions
- **BusinessException**: For business logic violations
- **TechnicalException**: For technical/infrastructure failures
- **SecurityException**: For security-related failures
- **ResourceNotFoundException**: When a requested resource is not found
- **DuplicateResourceException**: When attempting to create a resource that already exists
- **InvalidOperationException**: When an operation is invalid in the current context
- **DatabaseException**: When database operations fail
- **CacheException**: When cache operations fail

### 2. Standard API Response Models
- **ApiResponse<T>**: Generic wrapper for API responses with data, message, timestamp, and correlation ID
- **PageableResponse<T>**: Standard response wrapper for paginated data
- **ErrorResponse**: Standard error response structure with timestamp, status, error, message, path, and correlation ID
- **ValidationError**: Represents field-level validation errors

### 3. Global Exception Handler
- Automatically handles all exceptions across REST controllers
- Maps exceptions to appropriate HTTP status codes
- Returns consistent error responses
- Logs exceptions with appropriate severity levels
- Does not expose internal implementation details

### 4. Utility Classes
- **DateTimeUtils**: Date and time operations (formatting, parsing, manipulation)
- **StringUtils**: String manipulation (case conversion, validation, masking)
- **ValidationUtils**: Common validation patterns (email, phone, UUID, URL validation)
- **UUIDGenerator**: Generates UUIDs and correlation IDs

## Usage

### Add Dependency

Add the following dependency to your service's `pom.xml`:

```xml
<dependency>
    <groupId>com.immortals.platform</groupId>
    <artifactId>common-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Auto-Configuration

The starter automatically configures:
- Global exception handler for all REST controllers
- Standard error response format

No additional configuration is required. Simply add the dependency and the features will be available.

### Using API Response Models

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> getUser(@PathVariable String id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
    
    @GetMapping
    public ResponseEntity<PageableResponse<User>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<User> users = userService.findAll(page, size);
        long total = userService.count();
        
        PageableResponse<User> response = PageableResponse.of(users, page, size, total);
        return ResponseEntity.ok(response);
    }
}
```

### Using Exceptions

```java
@Service
public class UserService {
    
    public User findById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
    
    public User create(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("User", user.getEmail());
        }
        return userRepository.save(user);
    }
    
    public void validateOperation(User user) {
        if (!user.isActive()) {
            throw new InvalidOperationException("Cannot perform operation on inactive user");
        }
    }
}
```

### Using Utility Classes

```java
// Date/Time operations
Instant now = DateTimeUtils.now();
String isoString = DateTimeUtils.formatIso(now);
Instant parsed = DateTimeUtils.parseInstant(isoString);
boolean isPast = DateTimeUtils.isPast(someInstant);

// String operations
String email = "user@example.com";
boolean isValid = StringUtils.isValidEmail(email);
String masked = StringUtils.maskEmail(email); // us**@example.com
String camelCase = StringUtils.toCamelCase("hello_world"); // helloWorld

// Validation
ValidationUtils.requireNonNull(user, "user");
ValidationUtils.requireNonBlank(email, "email");
ValidationUtils.requireValidEmail(email, "email");
ValidationUtils.requirePositive(amount, "amount");

// UUID generation
String correlationId = UUIDGenerator.generateCorrelationId();
String shortId = UUIDGenerator.generateShortCorrelationId();
boolean isValid = UUIDGenerator.isValidUUID(someString);
```

## Error Response Format

All errors are returned in a consistent format:

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "User with id '123' not found",
  "path": "/api/v1/users/123",
  "correlationId": "abc-123-def"
}
```

Validation errors include field-level details:

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/users",
  "correlationId": "abc-123-def",
  "errors": [
    {
      "field": "email",
      "rejectedValue": "invalid-email",
      "message": "must be a valid email address",
      "code": "Email"
    }
  ]
}
```

## Exception to HTTP Status Code Mapping

| Exception | HTTP Status |
|-----------|-------------|
| ResourceNotFoundException | 404 Not Found |
| ValidationException | 400 Bad Request |
| BusinessException | 400 Bad Request |
| SecurityException (authentication) | 401 Unauthorized |
| SecurityException (authorization) | 403 Forbidden |
| TechnicalException | 500 Internal Server Error |
| All other exceptions | 500 Internal Server Error |

## Requirements Satisfied

This starter satisfies the following requirements from the requirements document:

- **7.2**: Consistent error response format across all endpoints
- **7.4**: Pagination for all list endpoints
- **7.5**: Common utility functions
- **14.1**: Global exception handler
- **14.2**: Consistent error response format with error code, message, and timestamp
- **14.3**: Appropriate logging of exceptions
- **14.4**: No exposure of internal implementation details
- **14.5**: Mapping of exceptions to appropriate HTTP status codes
