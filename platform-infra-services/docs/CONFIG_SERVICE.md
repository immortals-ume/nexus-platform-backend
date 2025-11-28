# Config Server

Spring Cloud Config Server for centralized configuration management with Git backend, encryption support, and refresh capabilities.

## Features

### 1. Spring Cloud Config Server
- **Git Backend**: Supports Git repository as configuration source
- **Native Backend**: Supports local filesystem for development
- **Environment-Specific Configs**: Separate profiles for dev, staging, and production
- **Service Registration**: Registers with Eureka for service discovery
- **Basic Authentication**: Secured with username/password

### 2. Configuration Encryption
- **Symmetric Encryption**: AES encryption for passwords and secrets
- **Asymmetric Encryption**: RSA encryption support with keystore
- **Encryption Endpoints**: `/encrypt` and `/decrypt` endpoints
- **Decryption on Delivery**: Automatic decryption when serving configs

### 3. Configuration Refresh
- **Manual Refresh**: `/actuator/refresh` endpoint for single service
- **Broadcast Refresh**: `/actuator/busrefresh` for all services via Spring Cloud Bus
- **Git Webhook Support**: `/monitor` endpoint for automatic refresh on Git push
- **Kafka Integration**: Uses Kafka for broadcasting refresh events

### 4. Observability
- **Metrics**: Configuration request counts, encryption/decryption counts, refresh events
- **Audit Logging**: Logs all configuration access, encryption operations, and refresh events
- **Distributed Tracing**: Integration with observability-starter
- **Prometheus Metrics**: Exposed at `/actuator/prometheus`

## Configuration

### Environment Variables

All environment-specific settings are externalized using environment variables with sensible defaults for development.

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| **Server Configuration** |
| `SERVER_PORT` | Server port | `8888` | No |
| `CONFIG_SERVER_USERNAME` | Basic auth username | `admin` | Yes (staging/prod) |
| `CONFIG_SERVER_PASSWORD` | Basic auth password | `admin123` | Yes (staging/prod) |
| **Git Backend (staging/prod)** |
| `GIT_REPO_URI` | Git repository URI | - | Yes (staging/prod) |
| `GIT_USERNAME` | Git username | - | Yes (staging/prod) |
| `GIT_PASSWORD` | Git password/token | - | Yes (staging/prod) |
| **Encryption** |
| `ENCRYPT_KEY` | Symmetric encryption key | - | Yes (if using encryption) |
| `KEYSTORE_LOCATION` | Keystore file path | - | Yes (prod asymmetric) |
| `KEYSTORE_PASSWORD` | Keystore password | - | Yes (prod asymmetric) |
| `KEYSTORE_ALIAS` | Key alias | - | Yes (prod asymmetric) |
| `KEYSTORE_SECRET` | Key password | - | Yes (prod asymmetric) |
| **Service Discovery** |
| `EUREKA_SERVER_URL` | Eureka server URL | `http://localhost:8761/eureka/` | No |
| **Messaging (staging/prod)** |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers | `localhost:9092` | Yes (staging/prod) |
| **Observability** |
| `ZIPKIN_URL` | Zipkin server URL | `http://localhost:9411` | No |
| `SPRING_PROFILES_ACTIVE` | Active profile (dev/staging/prod) | `native` | No |

**Example Configuration:**

```bash
# Development (uses defaults)
export SPRING_PROFILES_ACTIVE=dev

# Staging
export SPRING_PROFILES_ACTIVE=staging
export CONFIG_SERVER_USERNAME=admin
export CONFIG_SERVER_PASSWORD=secure_password
export GIT_REPO_URI=https://github.com/your-org/config-repo.git
export GIT_USERNAME=config-user
export GIT_PASSWORD=github_token
export EUREKA_SERVER_URL=http://discovery-service:8761/eureka/
export KAFKA_BOOTSTRAP_SERVERS=kafka:9092
export ZIPKIN_URL=http://zipkin:9411

# Production
export SPRING_PROFILES_ACTIVE=prod
export CONFIG_SERVER_USERNAME=admin
export CONFIG_SERVER_PASSWORD=${SECURE_PASSWORD}
export GIT_REPO_URI=${CONFIG_GIT_URI}
export GIT_USERNAME=${CONFIG_GIT_USER}
export GIT_PASSWORD=${CONFIG_GIT_TOKEN}
export KEYSTORE_LOCATION=/etc/config-server/keystore.jks
export KEYSTORE_PASSWORD=${KEYSTORE_PWD}
export KEYSTORE_ALIAS=config-server-key
export KEYSTORE_SECRET=${KEY_SECRET}
export EUREKA_SERVER_URL=${EUREKA_URL}
export KAFKA_BOOTSTRAP_SERVERS=${KAFKA_SERVERS}
export ZIPKIN_URL=${ZIPKIN_URL}
```

### Profiles

- **dev**: Uses native filesystem backend, no Kafka bus
- **staging**: Uses Git backend with Kafka bus enabled
- **prod**: Uses Git backend with asymmetric encryption and Kafka bus

## Endpoints

### Configuration Endpoints
- `GET /{application}/{profile}/{label}` - Get configuration for application
- `GET /{application}-{profile}.yml` - Get configuration as YAML
- `GET /{application}-{profile}.properties` - Get configuration as properties

### Encryption Endpoints
- `POST /encrypt` - Encrypt a value (symmetric)
- `POST /decrypt` - Decrypt a value (symmetric)
- `POST /encrypt/asymmetric` - Encrypt a value (asymmetric)
- `POST /decrypt/asymmetric` - Decrypt a value (asymmetric)
- `GET /encrypt/status` - Get encryption status

### Refresh Endpoints
- `POST /actuator/refresh` - Refresh configuration (local)
- `POST /actuator/busrefresh` - Broadcast refresh to all services
- `POST /actuator/busrefresh/{service}` - Broadcast refresh to specific service

### Webhook Endpoint
- `POST /monitor` - Git webhook endpoint for automatic refresh
- `GET /monitor/health` - Webhook health check

### Actuator Endpoints
- `GET /actuator/health` - Health check
- `GET /actuator/info` - Application info
- `GET /actuator/metrics` - Metrics
- `GET /actuator/prometheus` - Prometheus metrics

## Usage Examples

### Encrypting a Password

```bash
curl -u admin:admin123 -X POST http://localhost:8888/encrypt \
  -H "Content-Type: text/plain" \
  -d "my-secret-password"
```

### Using Encrypted Values in Configuration

```yaml
spring:
  datasource:
    password: '{cipher}AQA...'  # Encrypted value
```

### Triggering Manual Refresh

```bash
# Refresh all services
curl -u admin:admin123 -X POST http://localhost:8888/actuator/busrefresh

# Refresh specific service
curl -u admin:admin123 -X POST http://localhost:8888/actuator/busrefresh/user-service
```

### Setting Up Git Webhook

Configure your Git repository to send webhooks to:
```
POST http://your-config-server:8888/monitor
```

## Git Repository Structure

```
config-repo/
├── application.yml              # Common configuration
├── gateway-service.yml          # Gateway-specific config
├── gateway-service-dev.yml      # Gateway dev config
├── gateway-service-prod.yml     # Gateway prod config
├── auth-service.yml
├── user-service.yml
└── ...
```

## Security

- All endpoints except `/actuator/health` and `/actuator/info` require authentication
- Encryption/decryption endpoints are protected
- Use asymmetric encryption in production for enhanced security
- Store keystore securely and never commit to version control
- Use environment variables for sensitive configuration

## Metrics

Available metrics:
- `config.requests.total` - Total configuration requests
- `config.requests.success` - Successful requests
- `config.requests.failure` - Failed requests
- `config.requests.duration` - Request duration
- `config.encryption.requests` - Encryption requests
- `config.decryption.requests` - Decryption requests
- `config.refresh.events` - Refresh events triggered

## Running

```bash
# Development (native backend)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Staging (Git backend)
mvn spring-boot:run -Dspring-boot.run.profiles=staging

# Production
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## Troubleshooting

### Configuration Not Loading

**Symptom**: Services cannot fetch configuration from Config Server

**Solutions**:
1. Verify Config Server is running: `curl http://localhost:8888/actuator/health`
2. Check authentication credentials in client services
3. Verify Git repository is accessible (if using Git backend)
4. Check Config Server logs for errors: `docker logs config-service`
5. Ensure correct profile is active: check `SPRING_PROFILES_ACTIVE`

### Encryption/Decryption Failures

**Symptom**: Encrypted values not decrypting or encryption endpoint returns errors

**Solutions**:
1. Verify `ENCRYPT_KEY` is set correctly
2. For asymmetric encryption, check keystore location and credentials
3. Ensure encrypted values start with `{cipher}` prefix
4. Test encryption endpoint: `curl -u admin:admin123 -X POST http://localhost:8888/encrypt -d "test"`
5. Check encryption status: `curl http://localhost:8888/encrypt/status`

### Refresh Not Working

**Symptom**: Configuration changes not propagating to services

**Solutions**:
1. Verify Kafka is running and accessible
2. Check Spring Cloud Bus configuration in both Config Server and client services
3. Ensure services have `@RefreshScope` on beans that need refresh
4. Manually trigger refresh: `curl -u admin:admin123 -X POST http://localhost:8888/actuator/busrefresh`
5. Check Kafka topics: `spring-cloud-bus` topic should exist
6. Verify webhook endpoint is accessible from Git repository

### Git Repository Connection Issues

**Symptom**: Config Server cannot connect to Git repository

**Solutions**:
1. Verify Git repository URL is correct
2. Check Git credentials (username/password or token)
3. For private repositories, ensure authentication is configured
4. Test Git connectivity: `git ls-remote <repo-url>`
5. Check firewall rules if using enterprise Git server
6. For SSH, ensure SSH keys are properly configured

### High Memory Usage

**Symptom**: Config Server consuming excessive memory

**Solutions**:
1. Adjust JVM heap settings: `-Xmx512m -Xms256m`
2. Monitor memory with: `curl http://localhost:8888/actuator/metrics/jvm.memory.used`
3. Consider increasing container memory limits
4. Review Git repository size (large repos consume more memory)
5. Enable Git repository caching with appropriate TTL

### Authentication Failures

**Symptom**: 401 Unauthorized errors when accessing Config Server

**Solutions**:
1. Verify `CONFIG_SERVER_USERNAME` and `CONFIG_SERVER_PASSWORD` are set
2. Check client configuration includes credentials in Eureka URL format: `http://user:pass@host:port/eureka/`
3. Ensure Basic Auth header is included in requests
4. Check Spring Security configuration is not blocking requests
5. Review logs for authentication attempts

### Slow Configuration Retrieval

**Symptom**: Configuration requests taking too long

**Solutions**:
1. Enable response caching in Config Server
2. Optimize Git repository structure (avoid large files)
3. Use native backend for development to avoid Git overhead
4. Monitor request duration: `curl http://localhost:8888/actuator/metrics/config.requests.duration`
5. Consider using Config Server clustering for high availability

### Webhook Not Triggering Refresh

**Symptom**: Git push events not triggering automatic refresh

**Solutions**:
1. Verify webhook URL is accessible from Git server
2. Check webhook endpoint: `curl http://localhost:8888/monitor/health`
3. Review Git webhook configuration (should POST to `/monitor`)
4. Check Config Server logs for webhook events
5. Ensure webhook secret matches if configured
6. Test webhook manually: `curl -X POST http://localhost:8888/monitor -H "X-Github-Event: push"`

## Dependencies

- Spring Cloud Config Server
- Spring Cloud Bus (Kafka)
- Spring Cloud Config Monitor
- Spring Cloud Netflix Eureka Client
- Spring Security
- Observability Starter (custom)
