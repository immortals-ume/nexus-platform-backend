# Requirements Document: Namespace System Redesign

## Introduction

The current namespace implementation is broken because it attempts to create new cache instances for each namespace using prototype beans and ObjectProvider, which fails silently. This redesign simplifies the architecture by using a single shared cache instance with NamespacedCacheService handling key prefixing for all namespaces. This approach is simpler, more reliable, and follows the principle of composition over inheritance.

## Glossary

- **Namespace**: A logical grouping/isolation of cache entries (e.g., "users", "products")
- **NamespacedCacheService**: Wrapper that prefixes all keys with namespace identifier to prevent collisions
- **CacheServiceFactory**: Factory interface for creating cache service instances
- **UnifiedCacheManager**: Central facade managing multiple cache namespaces
- **Base Cache Service**: The underlying cache implementation (Caffeine, Redis, or Multi-level)
- **Key Prefixing**: Adding namespace prefix to keys (e.g., "users:key1", "products:key2")

## Requirements

### Requirement 1: Single Shared Cache Instance

**User Story:** As a developer, I want the cache system to use a single shared cache instance for all namespaces, so that the architecture is simpler and more reliable.

#### Acceptance Criteria

1. WHEN UnifiedCacheManager is initialized, THE system SHALL create exactly one base cache instance
2. WHEN getCache(namespace) is called, THE system SHALL reuse the same base cache instance for all namespaces
3. WHILE multiple namespaces are used, THE base cache instance SHALL remain unchanged and shared
4. IF a namespace is requested, THEN the system SHALL wrap the base cache with NamespacedCacheService
5. WHERE NamespacedCacheService is used, THE system SHALL prefix all keys with the namespace identifier

### Requirement 2: Simplified Factory Pattern

**User Story:** As a developer, I want the factory pattern to be simple and reliable, so that cache instances are created without errors.

#### Acceptance Criteria

1. WHEN CacheServiceFactory.createCacheService() is called, THE factory SHALL return the same shared instance
2. WHILE the application runs, THE factory SHALL never fail to create instances
3. IF the factory is called multiple times, THEN it SHALL return the same instance each time
4. WHERE ObjectProvider is used, THE system SHALL NOT use prototype beans
5. THEN the system SHALL use singleton beans for all cache services

### Requirement 3: Namespace Isolation via Key Prefixing

**User Story:** As a developer, I want namespaces to be isolated through key prefixing, so that different namespaces don't collide.

#### Acceptance Criteria

1. WHEN a key is stored in namespace "users", THE actual key stored SHALL be "users:key"
2. WHEN a key is retrieved from namespace "users", THE system SHALL look for "users:key"
3. WHILE multiple namespaces share the same cache, THE keys SHALL never collide
4. IF a key exists in namespace "users", THEN the same key in namespace "products" SHALL be separate
5. WHERE NamespacedCacheService wraps the cache, THE namespace prefix SHALL be transparent to callers

### Requirement 4: Proper Cache Instance Lifecycle

**User Story:** As a developer, I want cache instances to be created once and reused, so that resources are managed efficiently.

#### Acceptance Criteria

1. WHEN the application starts, THE base cache instance SHALL be created exactly once
2. WHILE the application runs, THE same cache instance SHALL be reused for all namespaces
3. IF getCache(namespace) is called multiple times, THE same wrapped instance SHALL be returned
4. WHERE caching is used, THE underlying cache SHALL not be recreated for each namespace
5. THEN the system SHALL cache wrapped instances in UnifiedCacheManager

### Requirement 5: CacheAspect Integration

**User Story:** As a developer, I want the @CachePut annotation to work with namespaces, so that caching works transparently.

#### Acceptance Criteria

1. WHEN @CachePut(namespace="users") is used, THE CacheAspect SHALL get the cache for that namespace
2. WHILE the aspect processes the annotation, THE system SHALL use UnifiedCacheManager.getCache(namespace)
3. IF the namespace doesn't exist, THEN the system SHALL create it on-demand
4. WHERE the cache is retrieved, THE aspect SHALL use it to store the result
5. THEN the cached value SHALL be retrievable using the same namespace and key

### Requirement 6: No Prototype Beans

**User Story:** As a developer, I want to avoid prototype beans, so that the system is reliable and doesn't fail silently.

#### Acceptance Criteria

1. WHEN cache services are created, THE system SHALL NOT use @Scope("prototype")
2. WHILE the application runs, THE system SHALL use singleton beans for all cache services
3. IF ObjectProvider is used, THEN it SHALL only be used for optional dependencies
4. WHERE cache instances are created, THE system SHALL use singleton beans
5. THEN the factory SHALL return the same instance every time

## Implementation Notes

- The key insight is that we don't need new cache instances per namespace - we just need key prefixing
- NamespacedCacheService already handles key prefixing correctly
- The factory should return the same instance every time, not create new ones
- UnifiedCacheManager should cache the wrapped instances to avoid recreating them
- This approach is simpler, more reliable, and follows composition over inheritance
