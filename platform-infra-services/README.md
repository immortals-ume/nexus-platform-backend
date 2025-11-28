# Platform Infrastructure Services

Production-ready infrastructure services that provide core platform capabilities for microservices architecture.

## Services

- **config-service**: Centralized configuration management with Git backend, encryption, and refresh capabilities
- **discovery-service**: Service discovery and registry using Netflix Eureka
- **gateway-service**: API Gateway with routing, rate limiting, circuit breakers, and security

## Quick Start

```bash
# Run all services with Docker Compose
docker-compose up

# Run individual service
cd config-service
mvn spring-boot:run
```

## Documentation

Comprehensive documentation is available in the `docs/` folder:

- [Architecture Overview](docs/ARCHITECTURE.md) - System architecture and component interactions
- [Docker Setup Guide](docs/DOCKER_SETUP.md) - Docker configuration and deployment
- [Config Service](docs/CONFIG_SERVICE.md) - Configuration management service details
- [Discovery Service](docs/DISCOVERY_SERVICE.md) - Service registry and discovery details
- [Gateway Service](docs/GATEWAY_SERVICE.md) - API Gateway configuration and features

## Scripts

Utility scripts are available in the `scripts/` folder:

- `build-docker-images.sh` - Build Docker images for all services
- `run-tests.sh` - Run tests across all services

## Development

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose (for containerized deployment)
- Git (for Config Service backend)

### Building
```bash
# Build all services
mvn clean install

# Build specific service
cd config-service
mvn clean install
```

### Testing
```bash
# Run all tests
./scripts/run-tests.sh

# Run tests for specific service
cd config-service
mvn test
```
