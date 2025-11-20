# Platform BOM (Bill of Materials)

## Overview

The Platform BOM provides centralized dependency version management for all microservices in the platform. It ensures consistent versions across all services and simplifies dependency management.

## Purpose

- **Centralized Version Management**: All dependency versions are defined in one place
- **Consistency**: Ensures all services use compatible versions of libraries
- **Simplified POMs**: Service POMs don't need to specify versions for managed dependencies
- **Easy Updates**: Update a dependency version once, and all services inherit the change

## Usage

### In Service POMs

To use the Platform BOM in your service, add it to the `dependencyManagement` section:

```xml
<dependencyManagement>
    <dependencies>
        <!-- Platform BOM -->
        <dependency>
            <groupId>com.immortals.platform</groupId>
            <artifactId>platform-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Then, add dependencies without specifying versions:

```xml
<dependencies>
    <!-- PostgreSQL - version managed by BOM -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    
    <!-- Resilience4j - version managed by BOM -->
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-spring-boot3</artifactId>
    </dependency>
    
    <!-- Lombok - version managed by BOM -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
</dependencies>
```

## Managed Dependencies

### Spring Framework
- Spring Boot 3.5.4
- Spring Cloud 2024.0.1

### Database Drivers
- PostgreSQL 42.7.4
- MongoDB 5.2.1
- H2 2.3.232 (for testing)

### Caching
- Lettuce 6.4.0 (Redis client)
- Jedis 5.2.0 (Alternative Redis client)

### Messaging
- Apache Kafka 3.9.0

### Resilience
- Resilience4j 2.2.0
  - Circuit Breaker
  - Retry
  - Rate Limiter
  - Time Limiter
  - Bulkhead

### Observability
- Micrometer 1.14.2
- Micrometer Tracing 1.4.2
- OpenTelemetry 1.44.1
- Zipkin Reporter 3.4.2

### Testing
- JUnit Jupiter 5.11.4
- Mockito 5.14.2
- AssertJ 3.27.3
- Testcontainers 1.20.4
- REST Assured 5.5.0
- Awaitility 4.2.2

### Utilities
- Lombok 1.18.36
- MapStruct 1.6.3
- Jackson 2.18.2
- Apache Commons Lang3 3.17.0
- Apache Commons Collections4 4.5.0-M2
- Apache Commons IO 2.18.0
- Google Guava 33.3.1

### API Documentation
- SpringDoc OpenAPI 2.7.0

### Database Migration
- Flyway 10.21.0
- Liquibase 4.30.0

### Security
- JJWT 0.12.6
- Bouncy Castle 1.79

### HTTP Clients
- OkHttp 4.12.0
- Feign 13.5

## Version Updates

To update a dependency version:

1. Update the version property in `platform-bom/pom.xml`
2. Test the change with affected services
3. Commit and release a new BOM version
4. Update services to use the new BOM version

## Best Practices

1. **Always use the BOM**: All services should import the Platform BOM
2. **Don't override versions**: Avoid specifying versions in service POMs unless absolutely necessary
3. **Test before updating**: Test dependency updates thoroughly before releasing a new BOM version
4. **Document breaking changes**: If a version update introduces breaking changes, document them in release notes
5. **Keep versions aligned**: Ensure all related dependencies (e.g., Jackson modules) use the same version

## Compatibility

This BOM is compatible with:
- Java 17+
- Spring Boot 3.x
- Spring Cloud 2024.x

## Related Modules

- [platform-parent](../platform-parent/README.md) - Parent POM with build configuration
- [platform-starters](../platform-starters/) - Reusable Spring Boot starters
