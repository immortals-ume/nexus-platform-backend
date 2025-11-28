# Docker Setup Guide

This guide explains how to run the infrastructure services using Docker and Docker Compose.

## Prerequisites

- Docker 20.10+
- Docker Compose 2.0+
- At least 4GB RAM available for Docker
- Ports 8080, 8761, 8888, 6379, 9092, 9411 available

## Quick Start

### Start All Services

```bash
# From the platform-infra-services directory
docker-compose up -d
```

This will start:
- **Redis** (port 6379) - For Gateway rate limiting
- **Zookeeper** (port 2181) - For Kafka
- **Kafka** (port 9092) - For Config Server refresh events
- **Discovery Service** (port 8761) - Service registry
- **Config Service** (port 8888) - Configuration management
- **Gateway Service** (port 8080) - API Gateway
- **Zipkin** (port 9411) - Distributed tracing (optional)

### Check Service Status

```bash
# View all services
docker-compose ps

# View logs
docker-compose logs -f

# View logs for specific service
docker-compose logs -f gateway-service
```

### Stop All Services

```bash
# Stop services
docker-compose stop

# Stop and remove containers
docker-compose down

# Stop and remove containers + volumes
docker-compose down -v
```

## Service Access

### Discovery Service (Eureka)
- **URL**: http://localhost:8761
- **Credentials**: admin / admin123
- **Health**: http://localhost:8761/actuator/health

### Config Service
- **URL**: http://localhost:8888
- **Credentials**: admin / admin123
- **Health**: http://localhost:8888/actuator/health
- **Example**: http://localhost:8888/gateway-service/dev

### Gateway Service
- **URL**: http://localhost:8080
- **Health**: http://localhost:8080/actuator/health
- **Routes**: http://localhost:8080/actuator/gateway/routes

### Zipkin (Tracing)
- **URL**: http://localhost:9411
- **Health**: http://localhost:9411/health

### Redis
- **Host**: localhost
- **Port**: 6379
- **CLI**: `docker exec -it infra-redis redis-cli`

### Kafka
- **Bootstrap**: localhost:9092
- **Topics**: `docker exec -it infra-kafka kafka-topics --list --bootstrap-server localhost:9092`

## Building Services

### Build All Services

```bash
docker-compose build
```

### Build Specific Service

```bash
docker-compose build config-service
```

### Rebuild Without Cache

```bash
docker-compose build --no-cache
```

## Configuration

### Environment Variables

You can override environment variables in `docker-compose.yml` or create a `.env` file:

```bash
# .env file
EUREKA_USERNAME=admin
EUREKA_PASSWORD=secure_password
CONFIG_SERVER_USERNAME=admin
CONFIG_SERVER_PASSWORD=secure_password
REDIS_PASSWORD=redis_password
```

### Profiles

Change the active profile by modifying the `SPRING_PROFILES_ACTIVE` environment variable:

```yaml
environment:
  - SPRING_PROFILES_ACTIVE=prod
```

## Scaling Services

### Scale Discovery Service (HA)

```bash
docker-compose up -d --scale discovery-service=3
```

### Scale Gateway Service

```bash
docker-compose up -d --scale gateway-service=2
```

## Troubleshooting

### Services Not Starting

1. Check logs: `docker-compose logs <service-name>`
2. Verify ports are available: `netstat -an | grep <port>`
3. Check Docker resources: `docker stats`
4. Ensure dependencies are healthy: `docker-compose ps`

### Health Check Failures

```bash
# Check health status
docker inspect --format='{{json .State.Health}}' <container-name>

# View health check logs
docker inspect <container-name> | jq '.[0].State.Health'
```

### Network Issues

```bash
# Inspect network
docker network inspect platform-infra-services_infra-network

# Test connectivity between containers
docker exec -it gateway-service ping discovery-service
```

### Redis Connection Issues

```bash
# Test Redis
docker exec -it infra-redis redis-cli ping

# Check Redis logs
docker-compose logs redis
```

### Kafka Issues

```bash
# Check Kafka topics
docker exec -it infra-kafka kafka-topics --list --bootstrap-server localhost:9092

# Check consumer groups
docker exec -it infra-kafka kafka-consumer-groups --list --bootstrap-server localhost:9092

# View Kafka logs
docker-compose logs kafka
```

### Memory Issues

```bash
# Check memory usage
docker stats

# Increase Docker memory limit in Docker Desktop settings
# Or adjust JVM settings in docker-compose.yml:
environment:
  - JAVA_OPTS=-Xmx512m -Xms256m
```

## Production Considerations

### Security

1. **Change default passwords** in production
2. **Use secrets management** for sensitive values
3. **Enable TLS/SSL** for all services
4. **Restrict network access** using Docker networks
5. **Run as non-root user** (already configured in Dockerfiles)

### High Availability

1. **Run multiple instances** of Discovery Service (3+ recommended)
2. **Use external Redis cluster** for production
3. **Use managed Kafka** service (AWS MSK, Confluent Cloud)
4. **Deploy across multiple availability zones**

### Monitoring

1. **Enable Prometheus metrics** on all services
2. **Set up Grafana dashboards** for visualization
3. **Configure alerting** for critical metrics
4. **Use centralized logging** (ELK, Splunk)

### Backup

1. **Redis data** is persisted in `redis-data` volume
2. **Config repository** should be backed up (Git)
3. **Eureka registry** is ephemeral (no backup needed)

## Advanced Usage

### Custom Network

```yaml
networks:
  infra-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.28.0.0/16
```

### Volume Mounts

```yaml
volumes:
  - ./config-repo:/config-repo
  - ./logs:/app/logs
```

### Resource Limits

```yaml
deploy:
  resources:
    limits:
      cpus: '1.0'
      memory: 1G
    reservations:
      cpus: '0.5'
      memory: 512M
```

## Cleanup

### Remove All Containers and Volumes

```bash
docker-compose down -v
```

### Remove Images

```bash
docker-compose down --rmi all
```

### Prune Docker System

```bash
docker system prune -a --volumes
```

## Integration with Business Services

Once infrastructure services are running, business services can connect:

```yaml
# In business service docker-compose.yml
services:
  user-service:
    environment:
      - EUREKA_SERVER_URL=http://admin:admin123@discovery-service:8761/eureka/
      - CONFIG_SERVER_URL=http://admin:admin123@config-service:8888
    networks:
      - platform-infra-services_infra-network

networks:
  platform-infra-services_infra-network:
    external: true
```

## References

- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Docker Networking](https://docs.docker.com/network/)
- [Docker Health Checks](https://docs.docker.com/engine/reference/builder/#healthcheck)
