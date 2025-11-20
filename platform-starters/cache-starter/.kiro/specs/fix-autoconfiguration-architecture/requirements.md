# Requirements Document: Fix AutoConfiguration Architecture

## Introduction

The cache service has a broken separation of concerns between AutoConfiguration and Configuration classes. AutoConfiguration classes are attempting to implement logic that should be delegated to Configuration classes. This violates the Spring Boot autoconfiguration pattern and leaves implementations incomplete (with TODO comments). The fix requires:

1. **Complete all Configuration class implementations** in cache-providers module
2. **AutoConfiguration classes should ONLY orchestrate** - load and wire pre-built beans from Configuration classes
3. **All logic must be moved from AutoConfiguration to Configuration** classes
4. **Decorator chain must be fully implemented** with actual decorator classes
5. **Properties must properly flow** from YAML → CacheProperties → Provider-specific Properties → Configuration → Beans

## Glossary

- **AutoConfiguration**: Spring Boot class that orchestrates bean creation based on conditions and properties (cache-features module)
- **Configuration**: Spring class that creates actual infrastructure beans (cache-providers module)
- **CacheProperties**: Main properties holder for all cache configuration from application.yml
- **Provider-specific Properties**: Caffeine/Redis properties extracted from CacheProperties
- **Decorator**: Wrapper around CacheService that adds cross-cutting concerns (compression, encryption, etc.)
- **Namespace**: Logical grouping/isolation of cache instances
- **L1 Cache**: Local in-memory cache (Caffeine)
- **L2 Cache**: Distributed cache (Redis)

## Requirements

### Requirement 1: Complete Caffeine Configuration Implementation

**User Story:** As a developer, I want Caffeine cache to be fully configured and instantiated by CaffeineConfiguration class, so that AutoConfiguration only needs to reference it.

#### Acceptance Criteria

1. WHEN CacheProperties specifies `type: caffeine`, THE CaffeineConfiguration SHALL create a fully configured Caffeine Cache bean
2. WHEN CaffeineProperties are provided, THE CaffeineConfiguration SHALL apply maximum size, TTL, and eviction policies
3. WHEN L1CacheService is instantiated, THE CaffeineConfiguration SHALL provide all required dependencies (Cache, MeterRegistry, namespace)
4. WHILE CaffeineAutoConfiguration runs, THE CaffeineConfiguration SHALL already have created the base cache infrastructure
5. IF CaffeineConfiguration is missing dependencies, THEN it SHALL throw CacheConfigurationException with clear error message

### Requirement 2: Complete Redis Configuration Implementation

**User Story:** As a developer, I want Redis cache to be fully configured with connection pooling, pipelining, and resilience by RedisConfiguration class, so that AutoConfiguration only orchestrates it.

#### Acceptance Criteria

1. WHEN CacheProperties specifies `type: redis`, THE RedisConfiguration SHALL create RedisTemplate with proper serialization
2. WHEN RedisProperties specify pipelining settings, THE RedisConfiguration SHALL configure batch size and enable/disable pipelining
3. WHEN RedisProperties specify connection pool settings, THE RedisConfiguration SHALL apply pool size, timeout, and retry settings
4. WHILE RedisCacheService is instantiated, THE RedisConfiguration SHALL provide RedisTemplate, MeterRegistry, and namespace
5. IF Redis connection fails during configuration, THEN it SHALL throw CacheConnectionException with connection details

### Requirement 3: Complete Multi-Level Cache Configuration Implementation

**User Story:** As a developer, I want multi-level cache to be fully configured with L1 and L2 coordination by MultiLevelCacheConfiguration class, so that AutoConfiguration only wires the pieces together.

#### Acceptance Criteria

1. WHEN CacheProperties specifies `type: multi-level`, THE MultiLevelCacheConfiguration SHALL create both L1 and L2 cache services
2. WHEN MultiLevelCacheProperties specify eviction publisher settings, THE MultiLevelCacheConfiguration SHALL configure Redis pub/sub for distributed invalidation
3. WHILE MultiLevelCacheService is instantiated, THE MultiLevelCacheConfiguration SHALL provide L1 cache, L2 cache, and EvictionPublisher
4. IF L1 or L2 configuration fails, THEN MultiLevelCacheConfiguration SHALL throw CacheConfigurationException with details of which level failed
5. WHERE multi-level cache is enabled, THE MultiLevelCacheConfiguration SHALL ensure both L1 and L2 are properly initialized before creating service

### Requirement 4: Implement All Decorator Classes

**User Story:** As a developer, I want all decorators (compression, encryption, metrics, stampede protection) to be fully implemented, so that DefaultDecoratorChainBuilder can apply them without TODO comments.

#### Acceptance Criteria

1. WHEN CompressionDecorator is applied, THE cache values SHALL be compressed using GzipCompressionStrategy before storage
2. WHEN EncryptionDecorator is applied, THE cache values SHALL be encrypted using AesGcmEncryptionStrategy before storage
3. WHEN MetricsDecorator is applied, THE cache operations SHALL record latency, hit/miss rates, and throughput metrics
4. WHEN StampedeProtectionDecorator is applied, THE concurrent requests for same key SHALL use distributed lock to prevent thundering herd
5. IF decorator application fails, THEN DefaultDecoratorChainBuilder SHALL throw CacheConfigurationException with decorator name and error

### Requirement 5: AutoConfiguration Classes Should Only Orchestrate

**User Story:** As a developer, I want AutoConfiguration classes to only load and wire pre-built Configuration beans, so that the architecture is clean and maintainable.

#### Acceptance Criteria

1. WHEN CacheAutoConfiguration runs, THE CacheAutoConfiguration SHALL only import and wire Configuration classes
2. WHEN CaffeineAutoConfiguration runs, THE CaffeineAutoConfiguration SHALL only create Supplier<CacheService> that delegates to CaffeineConfiguration beans
3. WHEN RedisAutoConfiguration runs, THE RedisAutoConfiguration SHALL only create Supplier<CacheService> that delegates to RedisConfiguration beans
4. WHEN MultiLevelAutoConfiguration runs, THE MultiLevelAutoConfiguration SHALL only create Supplier<CacheService> that delegates to MultiLevelCacheConfiguration beans
5. IF any Configuration class is missing, THEN AutoConfiguration SHALL throw CacheConfigurationException with clear message about missing configuration

### Requirement 6: Properties Flow Correctly Through Layers

**User Story:** As a developer, I want properties to flow cleanly from YAML → CacheProperties → Provider Properties → Configuration → Beans, so that configuration is traceable and maintainable.

#### Acceptance Criteria

1. WHEN application.yml specifies `immortals.cache.caffeine.maximum-size`, THE CacheProperties SHALL extract it into CaffeineProperties
2. WHEN CaffeineProperties are injected into CaffeineConfiguration, THE configuration SHALL use these properties to build Caffeine cache
3. WHEN CaffeineAutoConfiguration creates Supplier, THE supplier SHALL pass CaffeineProperties to CaffeineConfiguration
4. WHILE DefaultUnifiedCacheManager creates cache instances, THE namespace-specific configuration SHALL override default configuration
5. IF properties are invalid (e.g., negative size), THEN CacheProperties validation SHALL throw IllegalArgumentException with property name and constraint

### Requirement 7: Decorator Chain Builder Fully Implemented

**User Story:** As a developer, I want DefaultDecoratorChainBuilder to apply all configured decorators without TODO comments, so that features like encryption and compression actually work.

#### Acceptance Criteria

1. WHEN config.isCompressionEnabled() is true, THE DefaultDecoratorChainBuilder SHALL wrap cache with CompressionDecorator
2. WHEN config.isEncryptionEnabled() is true, THE DefaultDecoratorChainBuilder SHALL wrap cache with EncryptionDecorator
3. WHEN config.isStampedeProtectionEnabled() is true, THE DefaultDecoratorChainBuilder SHALL wrap cache with StampedeProtectionDecorator
4. WHEN meterRegistry is available, THE DefaultDecoratorChainBuilder SHALL wrap cache with MetricsDecorator
5. WHERE multiple decorators are enabled, THE DefaultDecoratorChainBuilder SHALL apply them in correct order: Metrics → Compression → Encryption → StampedeProtection

### Requirement 8: Configuration Classes Validate and Fail Fast

**User Story:** As a developer, I want Configuration classes to validate all settings during bean creation, so that configuration errors are caught at startup, not at runtime.

#### Acceptance Criteria

1. WHEN CaffeineConfiguration creates cache, THE configuration SHALL validate maximum size is positive
2. WHEN RedisConfiguration creates RedisTemplate, THE configuration SHALL validate host and port are valid
3. WHEN MultiLevelCacheConfiguration creates services, THE configuration SHALL validate both L1 and L2 are properly configured
4. IF validation fails, THEN Configuration class SHALL throw CacheConfigurationException with specific validation error
5. WHERE optional settings are provided, THE Configuration class SHALL apply sensible defaults if not specified

