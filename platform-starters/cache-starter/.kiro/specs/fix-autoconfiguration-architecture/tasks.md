# Implementation Plan: Fix AutoConfiguration Architecture

## Strategy: Reuse Existing Configuration Classes Directly

The project already has complete Configuration classes in cache-providers module:
- `CaffeineConfiguration` - Creates Caffeine cache beans
- `RedisConfiguration` - Creates Redis connection and template beans
- `MultiLevelCacheAutoConfiguration` - Creates L1, L2, and MultiLevelCacheService beans

AutoConfiguration classes in cache-features should only orchestrate by:
1. Importing Configuration classes
2. Creating Supplier<CacheService> that delegates to Configuration beans
3. Wiring everything into UnifiedCacheManager

---

- [x] 1. Fix CaffeineConfiguration - Add L1CacheService Bean
  - Add l1CacheService() bean that wraps caffeineCache() in L1CacheService
  - Inject CaffeineProperties and MeterRegistry
  - Add validation for maximum size > 0 and TTL is valid
  - Add proper error handling with CacheConfigurationException
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 8.1, 8.2_

- [x] 2. Fix CacheStandaloneConfiguration - Add RedisCacheService Bean
  - Add redisCacheService() bean that wraps redisTemplate() in RedisCacheService
  - Inject RedisTemplate, RedisProperties, and MeterRegistry
  - Add validation for host/port are not empty
  - Add proper error handling with CacheConnectionException
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 8.1, 8.2_

- [ ] 3. Implement MetricsDecorator
  - Create MetricsDecorator class in cache-features/metrics module
  - Implement put() to record operation latency and counter
  - Implement get() to record hit/miss and latency
  - Implement remove() to record eviction and latency
  - Use MeterRegistry to record all metrics
  - _Requirements: 4.3, 4.5_


- [ ] 4. Implement StampedeProtectionDecorator
  - Create StampedeProtectionDecorator class in cache-features/resilience module
  - Implement get() with distributed lock using Redisson
  - Implement lock timeout and retry logic
  - Handle lock acquisition failures gracefully
  - _Requirements: 4.4, 4.5_

- [ ] 5. Complete DefaultDecoratorChainBuilder Implementation
  - Remove TODO comments from DefaultDecoratorChainBuilder
  - Implement metrics decorator wrapping
  - Implement compression decorator wrapping (already has CompressionDecorator)
  - Implement encryption decorator wrapping (already has EncryptionDecorator)
  - Implement stampede protection decorator wrapping
  - Apply decorators in correct order: Metrics → Compression → Encryption → StampedeProtection
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 6. Fix CaffeineAutoConfiguration - Only Orchestrate
  - Remove @ConditionalOnProperty matchIfMissing=true (should be false or removed)
  - Remove direct L1CacheService creation from baseCacheProvider()
  - Add @Import(CaffeineConfiguration.class) to import Configuration class
  - Create baseCacheProvider() Supplier that injects and delegates to CaffeineConfiguration.l1CacheService()
  - Ensure caffeineProperties() only extracts from CacheProperties
  - _Requirements: 5.2, 6.1, 6.3_

- [x] 7. Fix RedisAutoConfiguration - Only Orchestrate
  - Remove direct RedisConnectionFactory and RedisTemplate creation
  - Add @Import(RedisConfiguration.class) to import Configuration class
  - Create baseCacheProvider() Supplier that injects and delegates to RedisConfiguration.redisCacheService() here diffentiate abaed on properites which configuration to choose
  - Ensure redisProperties() only extracts from CacheProperties
  - _Requirements: 5.3, 6.1, 6.4_

- [x] 8. Fix MultiLevelAutoConfiguration - Only Orchestrate
  - Remove direct L1CacheService, RedisCacheService, and MultiLevelCacheService creation
  - Add @Import(MultiLevelCacheAutoConfiguration.class) to import existing Configuration class keep single autoconfiguration clas remove duplicate 
  - Create baseCacheProvider() Supplier that injects and delegates to MultiLevelCacheAutoConfiguration.multiLevelCacheService()
  - Ensure only orchestration, no direct bean creation
  - _Requirements: 5.4, 6.1_

- [x] 9. Fix CacheAutoConfiguration - Wire Everything Together  first check for redundancy and use exisitnig code
  - Inject DefaultDecoratorChainBuilder (from DecoratorAutoConfiguration)
  - Pass DecoratorChainBuilder to DefaultUnifiedCacheManager (not null)
  - Verify CacheAutoConfiguration only imports other AutoConfiguration classes
  - Verify UnifiedCacheManager bean uses baseCacheProvider Supplier
  - Add proper logging for initialization
  - _Requirements: 5.1, 5.5, 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 10. Create DefaultDecoratorChainBuilder Bean in DecoratorAutoConfiguration
  - Add decoratorChainBuilder() bean that creates DefaultDecoratorChainBuilder
  - Inject MeterRegistry, RedissonClient, encryption key, compression threshold, stampede timeout
  - Pass all dependencies to DefaultDecoratorChainBuilder constructor
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 11. Add CacheProperties Validation
  - Add validation annotations to CacheProperties class
  - Validate cache type is one of: caffeine, redis, multi-level
  - Validate default TTL is positive duration
  - Validate encryption key is provided if encryption enabled
  - Throw IllegalArgumentException with clear message on validation failure
  - _Requirements: 6.5, 8.3, 8.4_

- [x] 12. Add Provider Properties Validation
  - Add validation to CaffeineProperties for maximum size > 0
  - Add validation to RedisProperties for host not empty and port > 0
  - Add validation to MultiLevelCacheProperties for eviction publisher settings
  - Throw CacheConfigurationException on validation failure
  - _Requirements: 8.3, 8.4_

- [x] 13. Add Configuration Error Handling
  - Update CaffeineConfiguration to catch and wrap exceptions with CacheConfigurationException
  - Update RedisConfiguration to catch and wrap exceptions with CacheConnectionException
  - Update MultiLevelCacheAutoConfiguration to catch and wrap exceptions with CacheConfigurationException
  - Include property names and values in error messages
  - _Requirements: 1.5, 2.5, 3.5, 8.4_

- [ ]* 14. Write Unit Tests for CaffeineConfiguration
  - Test caffeineCache() bean creation with valid properties
  - Test l1CacheService() bean creation
  - Test validation of maximum size
  - Test validation of TTL
  - Test exception handling for invalid properties
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [ ]* 15. Write Unit Tests for RedisConfiguration
  - Test redisTemplate() bean creation with valid properties
  - Test redisCacheService() bean creation
  - Test validation of host/port
  - Test validation of connection pool settings
  - Test exception handling for invalid properties
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [ ]* 16. Write Unit Tests for MultiLevelCacheConfiguration
  - Test l1Cache() bean creation
  - Test l2Cache() bean creation
  - Test evictionPublisher() bean creation
  - Test multiLevelCacheService() bean creation
  - Test validation that both L1 and L2 are initialized
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ]* 17. Write Unit Tests for Decorators
  - Test CompressionDecorator compresses and decompresses values
  - Test EncryptionDecorator encrypts and decrypts values
  - Test MetricsDecorator records metrics
  - Test StampedeProtectionDecorator prevents thundering herd
  - Test decorator exception handling
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ]* 18. Write Integration Tests for AutoConfiguration
  - Test CacheAutoConfiguration with caffeine type
  - Test CacheAutoConfiguration with redis type
  - Test CacheAutoConfiguration with multi-level type
  - Test property extraction and flow through layers
  - Test UnifiedCacheManager creation
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 6.2, 6.3, 6.4_

- [ ]* 19. Write Integration Tests for Decorator Chain
  - Test DefaultDecoratorChainBuilder applies all decorators
  - Test decorator order is correct
  - Test decorators work together without conflicts
  - Test decorator configuration from properties
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ]* 20. Write End-to-End Tests
  - Test full flow from YAML properties to cache operations
  - Test cache operations with all decorator combinations
  - Test fallback behavior when L2 fails
  - Test statistics collection across all components
  - _Requirements: All_

