# Platform Infrastructure Services Documentation

Welcome to the Platform Infrastructure Services documentation. This directory contains comprehensive guides for understanding, deploying, and maintaining the infrastructure services.

## 📚 Documentation Index

### Architecture & Design
- **[Architecture Overview](ARCHITECTURE.md)** - System architecture, component interactions, and design decisions
- **[Docker Setup Guide](DOCKER_SETUP.md)** - Docker configuration, containerization, and deployment

### Service Documentation
- **[Config Service](CONFIG_SERVICE.md)** - Centralized configuration management with Git backend
- **[Discovery Service](DISCOVERY_SERVICE.md)** - Service registry and discovery using Netflix Eureka
- **[Gateway Service](GATEWAY_SERVICE.md)** - API Gateway with routing, rate limiting, and circuit breakers

## 🚀 Quick Start

### Prerequisites
- Java 17 or higher
- Maven 3.8+
- Docker & Docker Compose (for containerized deployment)
- Git (for Config Service backend)

### Running Services

```bash
# Run all services with Docker Compose
docker-compose up

# Run individual service
cd config-service
mvn spring-boot:run
```

## 🏗️ Architecture Overview

The platform consists of three core infrastructure services:

```
┌─────────────────┐
│  Gateway Service │  ← Entry point for all requests
└────────┬────────┘
         │
         ├──────────────────────────────┐
         │                              │
┌────────▼────────┐          ┌─────────▼──────────┐
│ Discovery Service│          │  Config Service    │
│   (Eureka)      │          │   (Git-backed)     │
└─────────────────┘          └────────────────────┘
```

### Key Features

#### Config Service
- Git-backed configuration storage
- Symmetric & asymmetric encryption
- Configuration refresh via Kafka
- Audit logging and metrics
- Profile-based configuration

#### Discovery Service
- Service registration and discovery
- Health monitoring
- Self-preservation mode
- Peer replication support
- Custom metrics

#### Gateway Service
- Dynamic routing with Eureka integration
- Rate limiting (Redis-backed)
- Circuit breakers (Resilience4j)
- CORS configuration
- Correlation ID propagation

## 🔧 Development

### Building

```bash
# Build all services
mvn clean install

# Build specific service
cd config-service
mvn clean install

# Skip tests
mvn clean install -DskipTests
```

### Testing

```bash
# Run all tests
./scripts/run-tests.sh

# Run tests for specific service
cd config-service
mvn test

# Run with coverage
mvn test jacoco:report
```

### Code Quality

All services include:
- **JaCoCo** for code coverage reporting
- **Property-based testing** with jqwik
- **Unit tests** with JUnit 5
- **Integration tests** with TestContainers

## 📊 Observability

### Metrics
All services expose Prometheus metrics at `/actuator/prometheus`:
- JVM metrics (memory, threads, GC)
- HTTP request metrics
- Custom business metrics
- Resilience4j metrics (circuit breakers, retries)

### Logging
Structured JSON logging with:
- Correlation IDs for request tracing
- Log levels: ERROR, WARN, INFO, DEBUG
- Logback with Logstash encoder

### Health Checks
Health endpoints at `/actuator/health`:
- Application health
- Dependency health (database, Kafka, Redis)
- Custom health indicators

## 🔒 Security

### Authentication
- Spring Security with HTTP Basic authentication
- Configurable credentials via environment variables
- Protected endpoints require authentication

### Authorization
- Role-based access control (RBAC)
- Admin role for sensitive operations
- Public endpoints: `/actuator/health`, `/actuator/info`

### Input Validation
- Request validation with Jakarta Validation
- Input sanitization for XSS/SQL injection prevention
- Custom validators for complex rules

## 🌐 Configuration

### Environment Variables

#### Config Service
```bash
SPRING_CLOUD_CONFIG_SERVER_GIT_URI=https://github.com/your-org/config-repo
ENCRYPT_KEY=your-encryption-key
SPRING_SECURITY_USER_NAME=admin
SPRING_SECURITY_USER_PASSWORD=admin-password
```

#### Discovery Service
```bash
EUREKA_INSTANCE_HOSTNAME=localhost
EUREKA_CLIENT_REGISTER_WITH_EUREKA=false
SPRING_SECURITY_USER_NAME=admin
SPRING_SECURITY_USER_PASSWORD=admin-password
```

#### Gateway Service
```bash
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://localhost:8761/eureka/
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4200
```

### Profiles
- **dev** - Development environment
- **staging** - Staging environment
- **prod** - Production environment

## 🛠️ Troubleshooting

Common issues and solutions:

### Service Won't Start
1. Check if required ports are available (8888, 8761, 8080)
2. Verify environment variables are set correctly
3. Check logs for specific error messages

### Configuration Not Loading
1. Verify Git repository URL is accessible
2. Check authentication credentials
3. Ensure branch/path configuration is correct

### Service Discovery Issues
1. Verify Eureka server is running
2. Check `eureka.client.service-url.defaultZone` configuration
3. Review network connectivity between services

For more detailed troubleshooting, see individual service documentation.

## 📝 API Documentation

### Config Service
- `GET /{application}/{profile}` - Get configuration
- `POST /encrypt` - Encrypt value
- `POST /decrypt` - Decrypt value
- `POST /actuator/refresh` - Refresh configuration

### Discovery Service
- `GET /eureka/apps` - List registered applications
- `GET /eureka/apps/{app}` - Get application instances
- `POST /eureka/apps/{app}` - Register instance

### Gateway Service
- All routes are dynamically configured based on Eureka registry
- Pattern: `/{service-name}/**` routes to registered service

## 🤝 Contributing

### Code Style
- Follow Java coding conventions
- Use Lombok for boilerplate reduction
- Write comprehensive Javadocs for public APIs
- Include unit and integration tests

### Commit Messages
- Use conventional commits format
- Include issue/ticket references
- Write clear, descriptive messages

### Pull Requests
- Ensure all tests pass
- Update documentation as needed
- Request review from team members

## 📞 Support

For questions or issues:
- Check service-specific documentation
- Review troubleshooting guides
- Contact the Platform Team

## 📄 License

Copyright © 2024 Platform Team. All rights reserved.
