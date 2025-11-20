# Gateway Service

API Gateway service providing a single entry point for all microservices in the platform.

## Features

- **Service Discovery Integration**: Automatic service discovery using Netflix Eureka
- **Load Balancing**: Client-side load balancing across service instances
- **Rate Limiting**: IP-based rate limiting (1000 requests/min per IP) using Redis
- **Circuit Breaker**: Resilience4j circuit breakers for downstream service protection
- **CORS Support**: Configurable CORS for web clients
- **Request/Response Logging**: Comprehensive audit logging with correlation IDs
- **Distributed Tracing**: Integration with Zipkin/Jaeger for request tracing
- **Metrics**: Prometheus metrics for monitoring gateway operations
- **Fallback Endpoints**: Graceful degradation when services are unavailable

## Architecture

The gateway acts as a reverse proxy, routing requests to appropriate microservices:

```
Client → Gateway → [Auth Service, User Service, Notification Service, etc.]
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Gateway server port | `8080` |
| `EUREKA_SERVER_URL` | Eureka server URL | `http://localhost:8761/eureka/` |
| `REDIS_HOST` | Redis host for rate limiting | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `REDIS_PASSWORD` | Redis password | `` |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins | `http://localhost:3000,http://localhost:4200` |

### Routes

The gateway is configured with routes for the following services:

- **Auth Service**: `/api/v1/auth/**`
- **User Management**: `/api/v1/users/**`
- **Notification Service**: `/api/v1/notifications/**`
- **Storage Service**: `/api/v1/storage/**`, `/api/v1/files/**`
- **OTP Service**: `/api/v1/otp/**`
- **URL Shortener**: `/api/v1/urls/**`, `/s/**`
- **Customer Service**: `/api/v1/customers/**`
- **Order Service**: `/api/v1/orders/**`
- **Payment Service**: `/api/v1/payments/**`
- **Product Service**: `/api/v1/products/**`

## Running Locally

### Prerequisites

- Java 17+
- Maven 3.9+
- Redis (for rate limiting)
- Eureka Server (for service discovery)

### Build

```bash
mvn clean package
```

### Run

```bash
# Development profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Or run the JAR
java -jar target/gateway-service-1.0.0.jar --spring.profiles.active=dev
```

## Docker

### Build Image

```bash
docker build -t gateway-service:1.0.0 .
```

### Run Container

```bash
docker run -p 8080:8080 \
  -e EUREKA_SERVER_URL=http://eureka:8761/eureka/ \
  -e REDIS_HOST=redis \
  -e REDIS_PORT=6379 \
  gateway-service:1.0.0
```

## Monitoring

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

### Metrics

```bash
# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# Gateway routes
curl http://localhost:8080/actuator/gateway/routes
```

### Circuit Breaker Status

```bash
curl http://localhost:8080/actuator/health/circuitBreakers
```

## Rate Limiting

Rate limiting is applied per IP address:
- **Default**: 1000 requests/minute
- **Burst Capacity**: 2000 requests
- **Storage**: Redis

Rate limit headers are included in responses:
- `X-RateLimit-Limit`: Maximum requests allowed
- `X-RateLimit-Remaining`: Remaining requests
- `X-RateLimit-Reset`: Time when limit resets

## Circuit Breaker

Circuit breakers protect downstream services:
- **Sliding Window**: 10 calls
- **Failure Threshold**: 50%
- **Wait Duration**: 10 seconds
- **Half-Open Calls**: 3

When a circuit breaker opens, requests are routed to fallback endpoints.

## Fallback Endpoints

Fallback endpoints provide graceful degradation:
- `/fallback/auth` - Auth service fallback
- `/fallback/users` - User service fallback
- `/fallback/notifications` - Notification service fallback
- `/fallback/storage` - Storage service fallback
- `/fallback/otp` - OTP service fallback
- `/fallback/urls` - URL shortener fallback
- `/fallback/customers` - Customer service fallback
- `/fallback/orders` - Order service fallback
- `/fallback/payments` - Payment service fallback
- `/fallback/products` - Product service fallback

## CORS Configuration

CORS is configured to allow cross-origin requests from web clients. Configure allowed origins using the `CORS_ALLOWED_ORIGINS` environment variable.

## Distributed Tracing

The gateway integrates with Zipkin/Jaeger for distributed tracing:
- Correlation IDs are automatically added to all requests
- Trace context is propagated to downstream services
- Sampling rate: 10% (production), 100% (development)

## Logging

Structured logging with correlation IDs:
- Request/response logging for audit
- Response time logging with warnings for slow requests (>1s)
- Circuit breaker state transitions
- Rate limit violations

## Development

### Project Structure

```
gateway/
├── src/main/java/com/example/gateway/
│   ├── GatewayApplication.java
│   ├── config/
│   │   ├── CorsConfig.java
│   │   ├── GatewayConfig.java
│   │   ├── MetricsConfig.java
│   │   └── ObservabilityConfig.java
│   ├── controller/
│   │   └── FallbackController.java
│   └── filter/
│       ├── CircuitBreakerFilter.java
│       ├── CorrelationIdFilter.java
│       ├── RequestLoggingFilter.java
│       └── ResponseTimeLoggingFilter.java
└── src/main/resources/
    ├── application.yml
    ├── application-dev.yml
    └── application-prod.yml
```

## References

- [Spring Cloud Gateway Documentation](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Netflix Eureka Documentation](https://github.com/Netflix/eureka/wiki)

