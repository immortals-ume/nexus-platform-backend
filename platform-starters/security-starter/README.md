# Security Starter

A reusable Spring Boot starter module that provides comprehensive security features for microservices, extracted from the auth-app implementation.

## Overview

The security-starter module provides OAuth2 resource server capabilities, JWT validation, role-based access control (RBAC), rate limiting, audit logging, and input sanitization. It is designed to be used by all microservices that need to validate JWT tokens and enforce security policies.

## Architecture

### Integration with Other Starters

The security-starter **does NOT duplicate functionality**. It integrates with:

1. **cache-starter**: For distributed rate limiting and token caching
2. **observability-starter**: For metrics, tracing, and structured logging
3. **common-starter**: For common utilities and exception handling

```
┌─────────────────────────────────────────────────────────────┐
│                     Microservice                             │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              security-starter                          │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐ │ │
│  │  │ JWT          │  │ Rate         │  │ RBAC        │ │ │
│  │  │ Validation   │  │ Limiting     │  │ & Audit     │ │ │
│  │  └──────────────┘  └──────────────┘  └─────────────┘ │ │
│  │         │                  │                 │        │ │
│  └─────────┼──────────────────┼─────────────────┼────────┘ │
│            │                  │                 │          │
│  ┌─────────▼──────────┐ ┌────▼─────────┐ ┌────▼─────────┐ │
│  │ cache-starter      │ │ observability│ │ common       │ │
│  │ (Redis caching)    │ │ -starter     │ │ -starter     │ │
│  └────────────────────┘ └──────────────┘ └──────────────┘ │
└─────────────────────────────────────────────────────────────┘


## Key Differences from auth-app

### What Stays in auth-app (Authentication Service)
- **JWT Token Generation** (private key operations)
- **User Authentication** (login, password validation)
- **User Management** (CRUD operations on users)
- **Database Layer** (User, Role, Permission entities)
- **OAuth2 Client** (social login integration)
- **Password Management** (reset, change)
- **Token Blacklisting** (logout, revocation)

### What Goes in security-starter (Resource Servers)
- **JWT Token Validation** (public key verification)
- **Rate Limiting** (distributed via cache-starter)
- **RBAC Enforcement** (role/permission checking)
- **Security Audit Logging** (authentication/authorization events)
- **Input Sanitization** (XSS, SQL injection prevention)
- **Security Filters** (rate limit, audit)

## Features

### 1. OAuth2 Resource Server with JWT Validation

Validates JWT tokens issued by auth-app using RSA public key verification.

**Key Components:**
- `JwtTokenValidator`: Validates token signature, expiration, and issuer
- `SecurityAutoConfiguration`: Configures OAuth2 resource server
- Token validation results are cached (via cache-starter) to reduce overhead

**Usage:**
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public User getUser(@PathVariable Long id) {
        // JWT token automatically validated
        // User principal available in SecurityContext
        return userService.findById(id);
    }
}
```

### 2. Role-Based Access Control (RBAC)

Fine-grained authorization using roles and permissions from JWT claims.

**Key Components:**
- `CustomPermissionEvaluator`: Evaluates permissions from JWT
- `RoleHierarchyConfig`: Defines role inheritance (ADMIN > MANAGER > USER > GUEST)
- Custom annotations: `@RequiresPermission`, `@RequiresAnyRole`

**Role Hierarchy:**
```
SUPER_ADMIN > ADMIN > MANAGER > USER > GUEST
```

**Usage:**
```java
@PreAuthorize("hasRole('ADMIN')")
public void adminOnlyMethod() { }

@PreAuthorize("hasPermission(null, 'user:write')")
public void requiresPermission() { }

@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public void managerOrAdmin() { }
```

### 3. Distributed Rate Limiting

Token bucket algorithm using cache-starter for distributed rate limiting across instances.

**Key Components:**
- `RateLimiterService`: Implements token bucket algorithm
- `RateLimitFilter`: Applies rate limiting to HTTP requests
- Uses cache-starter (Redis) for distributed state

**Features:**
- IP-based rate limiting
- User-based rate limiting (authenticated users)
- Configurable limits per endpoint
- Rate limit headers in responses (X-RateLimit-Limit, X-RateLimit-Remaining)

**Configuration:**
```yaml
platform:
  security:
    rate-limit:
      enabled: true
      default-limit: 100  # requests per minute
      time-window-seconds: 60
      ip-based: true
      user-based: true
      excluded-paths:
        - /actuator/health
        - /actuator/info
```

### 4. Security Audit Logging

Comprehensive audit logging for security events with correlation IDs.

**Key Components:**
- `AuditEventPublisher`: Publishes security audit events
- `SecurityAuditEvent`: Event model for audit logs

**Events Logged:**
- Authentication success/failure
- Authorization failures
- Rate limit violations
- Token validation failures

**Integration with observability-starter:**
All audit logs include correlation IDs for distributed tracing.

### 5. Input Sanitization

Utilities to prevent XSS, SQL injection, and path traversal attacks.

**Key Components:**
- `InputSanitizer`: Sanitizes user input
- `@ValidEmail`: Custom email validator

**Usage:**
```java
@Autowired
private InputSanitizer sanitizer;

public void processInput(String userInput) {
    String safe = sanitizer.sanitizeXSS(userInput);
    
    if (sanitizer.containsSQLInjection(userInput)) {
        throw new SecurityException("SQL injection detected");
    }
}
```

## Configuration

### Required Configuration

```yaml
platform:
  security:
    jwt:
      enabled: true
      public-key: |
        -----BEGIN PUBLIC KEY-----
        MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
        -----END PUBLIC KEY-----
      issuer: "auth-service"
      cache-enabled: true
      cache-ttl-seconds: 300
```

### Full Configuration Example

```yaml
platform:
  security:
    jwt:
      enabled: true
      public-key: ${JWT_PUBLIC_KEY}
      issuer: "auth-service"
      cache-enabled: true
      cache-ttl-seconds: 300
    
    rate-limit:
      enabled: true
      default-limit: 100
      time-window-seconds: 60
      ip-based: true
      user-based: true
      excluded-paths:
        - /actuator/health
        - /actuator/info
        - /swagger-ui/**
    
    cors:
      enabled: true
      allowed-origins:
        - "https://app.example.com"
        - "https://admin.example.com"
      allowed-methods:
        - GET
        - POST
        - PUT
        - DELETE
        - OPTIONS
      allowed-headers:
        - "*"
      allow-credentials: true
      max-age: 3600
    
    audit:
      enabled: true
      log-authentication-attempts: true
      log-authorization-failures: true
      log-rate-limit-violations: true
```

## Dependencies

Add to your service's `pom.xml`:

```xml
<dependency>
    <groupId>com.immortals.platform</groupId>
    <artifactId>security-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

The security-starter automatically brings in:
- `cache-starter` (for rate limiting)
- `observability-starter` (for metrics and tracing)
- `common-starter` (for utilities)

## How It Works with auth-app

### Token Flow

```
1. User logs in to auth-app
   ↓
2. auth-app generates JWT with private key
   ↓
3. JWT contains: userId, email, roles, permissions
   ↓
4. User makes request to microservice with JWT
   ↓
5. security-starter validates JWT with public key
   ↓
6. Validation result cached (via cache-starter)
   ↓
7. Roles/permissions extracted from JWT
   ↓
8. RBAC enforced via @PreAuthorize
   ↓
9. Request processed if authorized
```

### Database Layer

**security-starter does NOT have a database layer** because:
- It only validates tokens (stateless)
- User data comes from JWT claims
- No need to query database for every request

**auth-app has the database layer** because:
- Stores users, roles, permissions
- Manages authentication state
- Handles user CRUD operations

### Cache Usage

**security-starter uses cache-starter for:**
- JWT validation result caching (reduce CPU overhead)
- Rate limiting state (distributed across instances)

**auth-app uses cache for:**
- Token blacklist (logout/revocation)
- Login attempt tracking
- Session management
- Rate limiting (same as security-starter)

## Integration Example

### Service Using security-starter

```java
@SpringBootApplication
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public List<Order> getOrders() {
        // JWT automatically validated by security-starter
        // User info available from SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();
        return orderService.findByUserId(userId);
    }
    
    @PostMapping
    @PreAuthorize("hasPermission(null, 'order:create')")
    public Order createOrder(@RequestBody OrderRequest request) {
        // Permission checked from JWT claims
        return orderService.create(request);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteOrder(@PathVariable Long id) {
        // Only admins can delete
        orderService.delete(id);
    }
}
```

### Configuration in Service

```yaml
spring:
  application:
    name: order-service

platform:
  security:
    jwt:
      public-key: ${JWT_PUBLIC_KEY}  # Same public key as auth-app
      issuer: "auth-service"
    rate-limit:
      default-limit: 200  # Higher limit for order service
```

## Metrics

The security-starter exposes metrics via observability-starter:

- `security.jwt.validation.success` - Successful JWT validations
- `security.jwt.validation.failure` - Failed JWT validations
- `security.rate.limit.exceeded` - Rate limit violations
- `security.authorization.denied` - Authorization failures

## Testing

### Unit Testing with Security

```java
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @WithMockUser(roles = "USER")
    void testGetOrders() throws Exception {
        mockMvc.perform(get("/api/orders"))
               .andExpect(status().isOk());
    }
    
    @Test
    @WithMockUser(roles = "GUEST")
    void testGetOrders_Forbidden() throws Exception {
        mockMvc.perform(get("/api/orders"))
               .andExpect(status().isForbidden());
    }
}
```

## Migration from auth-app

If you have an existing service using auth-app's security code:

1. **Remove duplicate security code** from your service
2. **Add security-starter dependency**
3. **Update configuration** to use `platform.security` prefix
4. **Replace custom filters** with security-starter's filters
5. **Use @PreAuthorize** instead of custom authorization logic

## Best Practices

1. **Always use HTTPS** in production
2. **Rotate JWT keys** periodically
3. **Set appropriate rate limits** per service
4. **Monitor audit logs** for security events
5. **Use role hierarchy** to simplify authorization
6. **Cache JWT validation** to reduce overhead
7. **Sanitize all user input** before processing

## Troubleshooting

### JWT Validation Fails

- Check public key matches auth-app's private key
- Verify issuer matches configuration
- Check token expiration
- Review audit logs for details

### Rate Limiting Not Working

- Verify cache-starter is configured
- Check Redis connectivity
- Review rate limit configuration
- Check excluded paths

### Authorization Denied

- Verify JWT contains required roles/permissions
- Check role hierarchy configuration
- Review @PreAuthorize expressions
- Check audit logs for details

## Requirements Satisfied

- **Requirement 6.1**: OAuth2 resource server configuration ✓
- **Requirement 6.2**: JWT token validation ✓
- **Requirement 6.3**: Role-based access control ✓
- **Requirement 6.5**: Rate limiting (100 req/min default) ✓
- **Requirement 6.6**: Input sanitization ✓
- **Requirement 4.3**: Metrics integration ✓

## License

Apache License 2.0
