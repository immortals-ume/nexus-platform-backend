# Discovery Service

Service registry using Netflix Eureka Server for dynamic service discovery in the microservices platform.

## Overview

The Discovery Service provides a centralized service registry where all microservices register themselves and discover other services. It uses Netflix Eureka Server and includes:

- Service registration and health monitoring
- Client-side load balancing support
- Self-preservation mode for network partition tolerance
- Secure dashboard with basic authentication
- Comprehensive observability (metrics, tracing, logging)
- High availability support with peer awareness

## Features

### Core Functionality
- **Service Registry**: Central registry for all microservices
- **Health Monitoring**: Automatic health checks with configurable intervals
- **Self-Preservation**: Protects against mass de-registration during network issues
- **Peer Awareness**: Support for multiple Eureka instances (HA setup)

### Security
- **Basic Authentication**: Secure dashboard access
- **Admin-only Access**: Restricted access to Eureka dashboard
- **Health Endpoint**: Public health checks for monitoring

### Observability
- **Distributed Tracing**: OpenTelemetry integration via observability-starter
- **Metrics**: Custom Eureka metrics (registered instances, applications, replicas)
- **Prometheus Export**: Metrics available at `/actuator/prometheus`
- **Structured Logging**: JSON logging with correlation IDs

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Server port | `8761` |
| `EUREKA_HOSTNAME` | Eureka instance hostname | `localhost` |
| `EUREKA_USERNAME` | Dashboard username | `admin` |
| `EUREKA_PASSWORD` | Dashboard password | `admin123` |
| `EUREKA_SERVICE_URL` | Eureka service URL for HA | Auto-configured |
| `CONFIG_SERVER_URL` | Config server URL | `http://localhost:8888` |
| `ZIPKIN_URL` | Zipkin server URL | `http://localhost:9411` |
| `SPRING_PROFILES_ACTIVE` | Active profile | `dev` |

### Eureka Server Configuration

Key configuration properties:

```yaml
eureka:
  server:
    enable-self-preservation: true          # Enable self-preservation mode
    renewal-percent-threshold: 0.85         # 85% threshold for self-preservation
    eviction-interval-timer-in-ms: 30000    # Check for expired leases every 30s
    response-cache-update-interval-ms: 30000 # Update response cache every 30s
```

### Profiles

- **dev**: Development profile with verbose logging and disabled self-preservation
- **prod**: Production profile with optimized settings and JSON logging

## Running the Service

### Local Development

```bash
# Run with Maven
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Build and run JAR
mvn clean package
java -jar target/discovery-service-1.0.0.jar
```

### Docker

```bash
# Build Docker image
docker build -t discovery-service:1.0.0 .

# Run container
docker run -d \
  -p 8761:8761 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e EUREKA_USERNAME=admin \
  -e EUREKA_PASSWORD=secure_password \
  --name discovery-service \
  discovery-service:1.0.0
```

### Docker Compose

```yaml
discovery-service:
  image: discovery-service:1.0.0
  ports:
    - "8761:8761"
  environment:
    - SPRING_PROFILES_ACTIVE=prod
    - EUREKA_USERNAME=admin
    - EUREKA_PASSWORD=${EUREKA_PASSWORD}
  healthcheck:
    test: ["CMD", "wget", "--spider", "http://localhost:8761/actuator/health"]
    interval: 30s
    timeout: 3s
    retries: 3
```

## Accessing the Service

### Eureka Dashboard
- **URL**: http://localhost:8761
- **Credentials**: admin / admin123 (default)

### Actuator Endpoints
- **Health**: http://localhost:8761/actuator/health
- **Metrics**: http://localhost:8761/actuator/metrics
- **Prometheus**: http://localhost:8761/actuator/prometheus
- **Info**: http://localhost:8761/actuator/info

## Client Configuration

Services that want to register with Eureka should include:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://admin:admin123@localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true
    lease-renewal-interval-in-seconds: 30
```

## Metrics

Custom metrics exposed:

- `eureka.registered.instances`: Total number of registered service instances
- `eureka.registered.applications`: Total number of registered applications
- `eureka.available.replicas`: Number of available Eureka replicas

## High Availability Setup

For production, run multiple Eureka instances:

```yaml
# Eureka Instance 1
eureka:
  client:
    service-url:
      defaultZone: http://eureka2:8761/eureka/,http://eureka3:8761/eureka/

# Eureka Instance 2
eureka:
  client:
    service-url:
      defaultZone: http://eureka1:8761/eureka/,http://eureka3:8761/eureka/
```

## Troubleshooting

### Self-Preservation Mode

If you see "EMERGENCY! EUREKA MAY BE INCORRECTLY CLAIMING INSTANCES ARE UP WHEN THEY'RE NOT":

- This is self-preservation mode activating
- It prevents mass de-registration during network issues
- In development, you can disable it with `eureka.server.enable-self-preservation=false`
- In production, keep it enabled for resilience

### Services Not Appearing

1. Check service is configured to register with correct Eureka URL
2. Verify network connectivity between service and Eureka
3. Check Eureka logs for registration attempts
4. Verify authentication credentials if security is enabled

### High Memory Usage

- Adjust JVM settings: `-XX:MaxRAMPercentage=75.0`
- Monitor with: `/actuator/metrics/jvm.memory.used`
- Consider increasing container memory limits

## Dependencies

- Spring Boot 3.5.4
- Spring Cloud 2024.0.1
- Netflix Eureka Server
- Platform Observability Starter
- Spring Security

## Reference Documentation

- [Spring Cloud Netflix Eureka](https://docs.spring.io/spring-cloud-netflix/docs/current/reference/html/#spring-cloud-eureka-server)
- [Service Registration and Discovery](https://spring.io/guides/gs/service-registration-and-discovery/)
- [Eureka at Netflix](https://github.com/Netflix/eureka/wiki)

