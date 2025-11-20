# Platform Starters

Shared Spring Boot starters providing common functionality across all services.

## Starters

- **common-starter**: Common utilities, exceptions, and base classes
- **security-starter**: Security configuration, JWT, OAuth2, RBAC
- **observability-starter**: Distributed tracing, metrics, logging
- **cache-starter**: Redis caching with resilience patterns
- **messaging-starter**: Kafka messaging with event-driven patterns

## Usage

Add the required starter as a dependency in your service:

```xml
<dependency>
    <groupId>com.immortals.platform</groupId>
    <artifactId>security-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Building

```bash
mvn clean install
```
