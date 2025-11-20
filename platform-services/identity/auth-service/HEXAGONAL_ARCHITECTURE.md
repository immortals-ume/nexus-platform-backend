# Hexagonal Architecture - Auth Service

This service follows hexagonal architecture (ports and adapters pattern) with clear separation of concerns.

## Architecture Layers

### 1. Domain Layer (from domain-starter)
**Location:** `com.immortals.platform.domain.*`

Pure business entities and DTOs without any framework dependencies:
- **Entities:** User, Role, Permission, City, Country, States, UserAddress
- **DTOs:** LoginDto, LoginResponse, RegisterRequestDTO, UserDto, etc.
- **Enums:** AddressType, AddressStatus, AuthProvider, UserTypes, etc.
- **Value Objects:** UserPrincipal, ErrorDto, TokenBucket

### 2. Application Layer (Use Cases)
**Location:** `com.immortals.authapp.service.*`

Contains business logic and orchestrates the flow:
- `AuthService` / `AuthServiceImpl` - Authentication use cases (login, logout, refresh token)
- `GuestService` / `GuestServiceImpl` - Guest user registration
- `UserService` / `UserServiceImpl` - User management operations
- `OtpService` - OTP generation and validation
- `LoginAttemptService` - Login attempt tracking
- `TokenBlacklistService` - Token revocation

**Characteristics:**
- Framework-independent business logic
- Orchestrates domain entities and repository operations
- Publishes domain events
- Implements transaction boundaries

### 3. API Layer (Primary/Driving Adapters)
**Location:** `com.immortals.authapp.controller.*`

REST API endpoints that drive the application:
- `AuthController` - Authentication endpoints
- `GuestController` - Guest registration endpoints
- `UserController` - User management endpoints
- `CityController`, `CountryController`, `StateController` - Location endpoints

**Characteristics:**
- Thin adapters that delegate to application services
- Handle HTTP concerns (request/response mapping)
- Input validation
- Exception handling via `GlobalExceptionHandler`

### 4. Infrastructure Layer (Secondary/Driven Adapters)

#### 4.1 Persistence Adapters
**Location:** `com.immortals.authapp.repository.*`

JPA repositories implementing data access:
- `UserRepository` - User persistence
- `RoleRepository` - Role persistence
- `CityRepository`, `CountryRepository`, `StateRepository` - Location persistence

**Note:** Entities in `model/entity/` are JPA-annotated versions that map to domain entities from domain-starter.

#### 4.2 External Service Adapters
**Location:** `com.immortals.authapp.client.*`

Adapters for external services:
- `OtpClient` / `OtpClientImpl` - OTP service integration with circuit breaker

#### 4.3 Security Infrastructure
**Location:** `com.immortals.authapp.security.*`

Security implementations:
- `JwtProvider` - JWT token generation and validation
- `UserDetailsServiceImpl` - Spring Security user details
- `JwtAuthenticationFilter` - JWT authentication filter
- `SecurityConfig` - Security configuration

#### 4.4 Caching Infrastructure
**Location:** `com.immortals.authapp.service.cache.*`

Cache implementations:
- `CacheService` - Cache abstraction
- `RedisHashCacheService` - Redis implementation

#### 4.5 Event Publishing
**Location:** `com.immortals.authapp.event.*`, `com.immortals.authapp.service.AuthEventPublisher`

Domain event publishing:
- `UserRegisteredEvent`
- `UserAuthenticatedEvent`
- `UserAuthenticationFailedEvent`

### 5. Configuration Layer
**Location:** `com.immortals.authapp.config.*`

Infrastructure configuration:
- `DataSourceConfig` - Database configuration (read/write splitting)
- `JpaConfig` - JPA and auditing configuration
- `SecurityConfig` - Security configuration
- `CacheConfiguration` - Cache configuration
- `ResilienceConfig` - Circuit breaker configuration

## Dependency Flow

```
API Layer (Controllers)
    ↓
Application Layer (Services/Use Cases)
    ↓
Domain Layer (Entities/DTOs from domain-starter)
    ↑
Infrastructure Layer (Repositories, Clients, Security)
```

## Key Principles

1. **Dependency Inversion:** Application layer depends on abstractions (interfaces), not implementations
2. **Domain Independence:** Domain entities (from domain-starter) have no framework dependencies
3. **Testability:** Each layer can be tested independently
4. **Reusability:** Domain POJOs are shared via domain-starter across all services
5. **Separation of Concerns:** Clear boundaries between layers

## Benefits

- **Maintainability:** Changes in one layer don't affect others
- **Testability:** Easy to mock dependencies and test in isolation
- **Flexibility:** Easy to swap implementations (e.g., change database, cache provider)
- **Reusability:** Domain models shared across services via domain-starter
- **Technology Independence:** Business logic independent of frameworks
