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

All environment-specific settings are externalized using environment variables with sensible defaults for development.

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| **Server Configuration** |
| `SERVER_PORT` | Gateway server port | `8080` | No |
| **Service Discovery** |
| `EUREKA_SERVER_URL` | Eureka server URL | `http://localhost:8761/eureka/` | No |
| **Redis (Rate Limiting)** |
| `REDIS_HOST` | Redis host | `localhost` | Yes (staging/prod) |
| `REDIS_PORT` | Redis port | `6379` | No |
| `REDIS_PASSWORD` | Redis password | `` | Yes (staging/prod) |
| **CORS Configuration** |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins | `http://localhost:3000,http://localhost:4200` | Yes (staging/prod) |
| **Config Server** |
| `CONFIG_SERVER_URL` | Config server URL | `http://localhost:8888` | Yes (staging/prod) |
| **Observability** |
| `ZIPKIN_URL` | Zipkin server URL | `http://localhost:9411` | No |
| `SPRING_PROFILES_ACTIVE` | Active profile (dev/staging/prod) | `dev` | No |

**Example Configuration:**

```bash
# Development (uses defaults)
export SPRING_PROFILES_ACTIVE=dev

# Staging
export SPRING_PROFILES_ACTIVE=staging
export REDIS_HOST=redis
export REDIS_PORT=6379
export REDIS_PASSWORD=redis_password
export EUREKA_SERVER_URL=http://discovery-service:8761/eureka/
export CORS_ALLOWED_ORIGINS=https://staging.example.com
export CONFIG_SERVER_URL=http://config-server:8888
export ZIPKIN_URL=http://zipkin:9411

# Production
export SPRING_PROFILES_ACTIVE=prod
export REDIS_HOST=${REDIS_HOST}
export REDIS_PORT=6379
export REDIS_PASSWORD=${REDIS_PASSWORD}
export EUREKA_SERVER_URL=${EUREKA_URL}
export CORS_ALLOWED_ORIGINS=${ALLOWED_ORIGINS}
export CONFIG_SERVER_URL=${CONFIG_SERVER_URL}
export ZIPKIN_URL=${ZIPKIN_URL}
```

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

## Troubleshooting

### 404 Not Found Errors

**Symptom**: Requests to gateway return 404 errors

**Solutions**:
1. Verify target service is registered in Eureka: check dashboard at http://localhost:8761
2. Check route configuration: `curl http://localhost:8080/actuator/gateway/routes`
3. Verify request path matches configured route predicates
4. Check service discovery is working: `eureka.client.fetch-registry=true`
5. Review gateway logs for routing decisions
6. Ensure service name in route matches Eureka registration

### Rate Limiting Not Working

**Symptom**: Rate limits not being enforced

**Solutions**:
1. Verify Redis is running and accessible: `redis-cli ping`
2. Check Redis connection: `curl http://localhost:8080/actuator/health`
3. Review rate limit configuration in application.yml
4. Check rate limit headers in responses: `X-RateLimit-Remaining`
5. Verify rate limit filter is enabled
6. Test with multiple requests: `for i in {1..100}; do curl http://localhost:8080/api/v1/users; done`

### Circuit Breaker Not Opening

**Symptom**: Circuit breaker not activating despite service failures

**Solutions**:
1. Check circuit breaker configuration: sliding window size and failure threshold
2. Verify enough calls have been made to trigger evaluation (minimum 10 by default)
3. Review circuit breaker metrics: `curl http://localhost:8080/actuator/health/circuitBreakers`
4. Check logs for circuit breaker state transitions
5. Ensure Resilience4j is properly configured
6. Test with simulated failures to verify behavior

### CORS Errors

**Symptom**: Browser shows CORS policy errors

**Solutions**:
1. Verify `CORS_ALLOWED_ORIGINS` includes the requesting origin
2. Check CORS configuration in CorsConfig.java
3. Ensure preflight OPTIONS requests are allowed
4. Review browser console for specific CORS error
5. Test with curl to isolate browser-specific issues
6. Verify credentials support if using cookies

### Slow Response Times

**Symptom**: Gateway responses are slow

**Solutions**:
1. Check downstream service response times
2. Review gateway metrics: `curl http://localhost:8080/actuator/metrics/gateway.requests`
3. Monitor response time logs (warnings for >1s)
4. Check for circuit breakers in half-open state
5. Verify Redis performance for rate limiting
6. Review connection pool settings
7. Check for network latency between gateway and services

### Service Discovery Failures

**Symptom**: Gateway cannot discover backend services

**Solutions**:
1. Verify Eureka server is accessible: `curl http://localhost:8761/actuator/health`
2. Check Eureka client configuration in gateway
3. Ensure gateway is registered with Eureka
4. Review service registry: `curl http://localhost:8761/eureka/apps`
5. Check authentication credentials for Eureka
6. Verify network connectivity to Eureka server

### Fallback Endpoints Not Working

**Symptom**: Circuit breaker opens but fallback not triggered

**Solutions**:
1. Verify FallbackController is properly configured
2. Check circuit breaker filter configuration
3. Review fallback route mappings
4. Test fallback endpoint directly: `curl http://localhost:8080/fallback/users`
5. Check logs for fallback routing decisions
6. Ensure fallback paths match circuit breaker configuration

### Redis Connection Errors

**Symptom**: Gateway fails to connect to Redis

**Solutions**:
1. Verify Redis is running: `docker ps | grep redis`
2. Check Redis host and port configuration
3. Test Redis connection: `redis-cli -h <host> -p <port> ping`
4. Verify Redis password if authentication is enabled
5. Check firewall rules between gateway and Redis
6. Review Redis logs for connection attempts

### High Memory Usage

**Symptom**: Gateway consuming excessive memory

**Solutions**:
1. Adjust JVM heap settings: `-XX:MaxRAMPercentage=75.0`
2. Monitor memory metrics: `curl http://localhost:8080/actuator/metrics/jvm.memory.used`
3. Review connection pool sizes
4. Check for memory leaks in custom filters
5. Consider increasing container memory limits
6. Profile application with JVM tools

### Correlation ID Not Propagating

**Symptom**: Correlation IDs missing in downstream services

**Solutions**:
1. Verify CorrelationIdFilter is enabled
2. Check filter order in configuration
3. Review logs to confirm correlation ID generation
4. Ensure downstream services extract correlation ID from headers
5. Check header name matches: `X-Correlation-ID`
6. Test with explicit correlation ID: `curl -H "X-Correlation-ID: test-123" http://localhost:8080/api/v1/users`

### Timeout Errors

**Symptom**: Gateway returns timeout errors

**Solutions**:
1. Check timeout configuration: `spring.cloud.gateway.httpclient.connect-timeout`
2. Verify downstream service is responding
3. Review response time metrics
4. Adjust timeout values if legitimate requests are timing out
5. Check for network issues between gateway and services
6. Consider implementing retry logic for transient failures

### Authentication/Authorization Issues

**Symptom**: Authentication failures at gateway

**Solutions**:
1. Verify JWT token validation configuration
2. Check token expiration and refresh logic
3. Review security filter chain configuration
4. Test with valid tokens: `curl -H "Authorization: Bearer <token>" http://localhost:8080/api/v1/users`
5. Check logs for authentication errors
6. Verify auth service is accessible and healthy

### Load Balancing Not Working

**Symptom**: Requests always go to same service instance

**Solutions**:
1. Verify multiple instances are registered in Eureka
2. Check load balancer configuration
3. Review service instance metadata
4. Test with multiple requests to observe distribution
5. Check for sticky session configuration
6. Verify load balancer algorithm (round-robin, random, etc.)

## References

- [Spring Cloud Gateway Documentation](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Netflix Eureka Documentation](https://github.com/Netflix/eureka/wiki)

