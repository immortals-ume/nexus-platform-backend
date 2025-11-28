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

All environment-specific settings are externalized using environment variables with sensible defaults for development.

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| **Server Configuration** |
| `SERVER_PORT` | Server port | `8761` | No |
| **Eureka Configuration** |
| `EUREKA_HOSTNAME` | Eureka instance hostname | `localhost` | No |
| `EUREKA_USERNAME` | Dashboard username | `admin` | Yes (staging/prod) |
| `EUREKA_PASSWORD` | Dashboard password | `admin123` | Yes (staging/prod) |
| `EUREKA_SERVICE_URL` | Eureka service URL for HA | Auto-configured | No |
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
export EUREKA_HOSTNAME=discovery-service
export EUREKA_USERNAME=admin
export EUREKA_PASSWORD=secure_password
export CONFIG_SERVER_URL=http://config-server:8888
export ZIPKIN_URL=http://zipkin:9411

# Production
export SPRING_PROFILES_ACTIVE=prod
export EUREKA_HOSTNAME=discovery-service
export EUREKA_USERNAME=admin
export EUREKA_PASSWORD=${SECURE_PASSWORD}
export EUREKA_SERVICE_URL=${EUREKA_CLUSTER_URL}
export CONFIG_SERVER_URL=${CONFIG_SERVER_URL}
export ZIPKIN_URL=${ZIPKIN_URL}
```

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

**Symptom**: Dashboard shows "EMERGENCY! EUREKA MAY BE INCORRECTLY CLAIMING INSTANCES ARE UP WHEN THEY'RE NOT"

**Explanation**: This is self-preservation mode activating to prevent mass de-registration during network issues

**Solutions**:
1. In development, disable with `eureka.server.enable-self-preservation=false`
2. In production, keep it enabled for resilience
3. Check network connectivity between services and Eureka
4. Verify services are sending heartbeats correctly
5. Review renewal threshold: default is 85% of expected renewals

### Services Not Appearing in Registry

**Symptom**: Registered services not showing up in Eureka dashboard

**Solutions**:
1. Verify service is configured to register: `eureka.client.register-with-eureka=true`
2. Check Eureka URL in service configuration
3. Verify network connectivity: `curl http://eureka-host:8761/actuator/health`
4. Check authentication credentials if security is enabled
5. Review service logs for registration errors
6. Ensure service has `@EnableDiscoveryClient` or `@EnableEurekaClient`
7. Check firewall rules between service and Eureka

### Service Instances Stuck in Registry

**Symptom**: Stopped services still appear as UP in registry

**Solutions**:
1. Wait for lease expiration (default 90 seconds)
2. Check eviction interval: `eureka.server.eviction-interval-timer-in-ms`
3. Verify services are sending proper shutdown signals
4. Manually remove instance via Eureka REST API if needed
5. Review self-preservation mode status

### High Memory Usage

**Symptom**: Discovery Service consuming excessive memory

**Solutions**:
1. Adjust JVM settings: `-XX:MaxRAMPercentage=75.0`
2. Monitor with: `curl http://localhost:8761/actuator/metrics/jvm.memory.used`
3. Consider increasing container memory limits
4. Review number of registered instances (large registries need more memory)
5. Optimize response cache settings

### Dashboard Authentication Failures

**Symptom**: Cannot access Eureka dashboard with credentials

**Solutions**:
1. Verify `EUREKA_USERNAME` and `EUREKA_PASSWORD` environment variables
2. Check Spring Security configuration
3. Clear browser cache and cookies
4. Try accessing health endpoint without auth: `curl http://localhost:8761/actuator/health`
5. Review security logs for authentication attempts

### Peer Replication Issues

**Symptom**: Eureka instances not replicating registry data

**Solutions**:
1. Verify peer URLs are configured correctly
2. Check network connectivity between Eureka instances
3. Ensure all instances have same cluster configuration
4. Review logs for replication errors
5. Check authentication credentials for peer communication
6. Verify `eureka.client.service-url.defaultZone` includes all peers

### Slow Service Discovery

**Symptom**: Services taking too long to discover each other

**Solutions**:
1. Reduce cache refresh interval: `eureka.client.registry-fetch-interval-seconds`
2. Optimize response cache: `eureka.server.response-cache-update-interval-ms`
3. Enable delta updates: `eureka.client.disable-delta=false`
4. Check network latency between services and Eureka
5. Consider running Eureka closer to services (same region/datacenter)

### Health Check Failures

**Symptom**: Services marked as DOWN despite being healthy

**Solutions**:
1. Verify service health endpoint is accessible
2. Check health check configuration in service
3. Review health check timeout settings
4. Ensure service is fully started before health checks begin
5. Check for resource constraints (CPU, memory) affecting health
6. Review custom health indicators for failures

### Registry Size Growing Unbounded

**Symptom**: Registry contains many stale or duplicate entries

**Solutions**:
1. Enable and verify eviction is working
2. Check lease expiration settings
3. Ensure services properly de-register on shutdown
4. Review self-preservation threshold
5. Manually clean registry if needed via REST API
6. Restart Eureka server as last resort (registry is ephemeral)

### Connection Refused Errors

**Symptom**: Services cannot connect to Eureka server

**Solutions**:
1. Verify Eureka server is running: `docker ps` or `systemctl status eureka`
2. Check port 8761 is accessible: `telnet eureka-host 8761`
3. Review firewall rules and security groups
4. Verify DNS resolution for Eureka hostname
5. Check Eureka server logs for startup errors
6. Ensure correct protocol (http vs https)

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

