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

```bash
# Server Configuration
CONFIG_SERVER_USERNAME=admin
CONFIG_SERVER_PASSWORD=admin123

# Git Backend (staging/prod)
CONFIG_GIT_URI=https://github.com/your-org/config-repo.git
CONFIG_GIT_USERNAME=your-username
CONFIG_GIT_PASSWORD=your-token

# Encryption (symmetric)
CONFIG_ENCRYPT_KEY=your-secret-key

# Encryption (asymmetric - production)
CONFIG_KEYSTORE_LOCATION=/path/to/config-server.jks
CONFIG_KEYSTORE_PASSWORD=keystore-password
CONFIG_KEYSTORE_ALIAS=config-server-key
CONFIG_KEYSTORE_SECRET=key-password

# Eureka
EUREKA_SERVER_URL=http://localhost:8761/eureka/

# Kafka (for Spring Cloud Bus)
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Observability
ZIPKIN_URL=http://localhost:9411
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

## Dependencies

- Spring Cloud Config Server
- Spring Cloud Bus (Kafka)
- Spring Cloud Config Monitor
- Spring Cloud Netflix Eureka Client
- Spring Security
- Observability Starter (custom)
