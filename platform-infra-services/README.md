# Platform Services

Infrastructure services that provide core platform capabilities.

## Services

- **gateway-service**: API Gateway with routing, rate limiting, and security
- **discovery-service**: Service discovery using Netflix Eureka
- **config-service**: Centralized configuration management
- **admin-service**: Spring Boot Admin for monitoring (planned)

## Running Services

Each service can be run independently or via Docker Compose.

```bash
# Run individual service
cd gateway-service
mvn spring-boot:run

# Run all services
docker-compose up platform-services
```
