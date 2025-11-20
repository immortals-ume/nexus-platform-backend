# Auth App

A robust authentication and authorization service designed to provide secure access control for modern applications.
Auth App supports multiple authentication mechanisms, advanced token management, and seamless integration with
third-party services.

## 📋 Table of Contents

- [Overview](#-overview)
- [Architecture](#-architecture)
- [Features](#-features)
- [Prerequisites](#-prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Data Models](#data-models)
- [Security](#-security-best-practices)
- [Development](#-development)
- [Testing](#-testing)
- [Deployment](#-deployment)
- [Monitoring](#-monitoring)
- [Troubleshooting](#-troubleshooting)
- [Contributing](#-contributing)
- [License](#-license)
- [Contact](#-contact)

## Overview

Auth App is a comprehensive authentication and authorization microservice built with Spring Boot. It provides a secure,
scalable, and flexible solution for managing user identities, authentication, and access control in distributed systems.
The service is designed to be integrated with other microservices in the ecosystem, providing centralized authentication
and authorization capabilities.

## Architecture

Auth App follows a layered architecture pattern with clear separation of concerns:

### Layers

1. **API Layer (Controllers)**: Handles HTTP requests and responses
2. **Service Layer**: Contains business logic and orchestrates operations
3. **Repository Layer**: Manages data access and persistence
4. **Security Layer**: Implements authentication and authorization mechanisms

### Key Components

- **JWT Authentication**: Stateless authentication using JSON Web Tokens
- **Role-Based Access Control (RBAC)**: Fine-grained access control based on user roles and permissions
- **Rate Limiting**: Protection against brute force attacks and DoS
- **Auditing**: Comprehensive tracking of data changes and user actions

### Integration Points

- **Redis**: For caching and rate limiting
- **Database**: Postgres for persistent storage
- **OTP Service**: For two-factor authentication (Refer here for implementation: [otp-service-implementation](https://github.com/immortals-ume/otp-service))

## 🚀 Features

- **Username/Password Authentication**
    - Secure login using email/username and password
    - Password hashing with industry best practices
    - Role-based access control (RBAC) for user privileges

- **Role-Based Access Control (RBAC)**
    - Fine-grained access based on user roles and permissions
    - Custom role definitions for flexibility
    - Permission-based authorization for API endpoints

- **Two-Factor Authentication (2FA) / OTP Service (Mobile/Email)**
    - Additional security via app-based authenticators (TOTP) (Will be Available in Further Versions)
    - Optional enforcement for critical actions (Will be Available in Further Versions)
    - One-time password (OTP) generation and validation
    - SMS and email OTP delivery support 

- **Remember Me**
    - Persistent login functionality
    - Secure storage and expiration control for authentication tokens

- **Refresh Token**
    - Secure refresh token mechanism for session extension
    - Token expiration and revocation support

- **Anonymous Authentication**
    - Guest/anonymous user support with limited access
    - Seamless upgrade from guest to registered user without losing session

- **Logout**
    - Secure logout with token invalidation
    - Global logout for all active sessions

- **Security Measures**
    - JWT-based authentication
    - Secure HTTPS communication
    - Rate limiting and brute force protection
    - IP-based and username-based rate limiting

- **User Management**
    - User registration and profile management
    - Password reset and account recovery
    - Email and phone verification

- **Location Management**
    - Country, state, and city data management
    - User address management with geocoding support

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.8.x or higher
- PostgreSQL 14.x or higher
- Redis 6.x or higher
- Docker and Docker Compose (optional, for containerized deployment)

## 🛠️ Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/immortals-ume/auth-app.git
   cd auth-app
   ```

2. **Install dependencies**
   ```bash
   mvn clean install
   ```

3. **Configure environment variables**
     - edit `src/main/resources/application.yml` as needed

4. **Set up the database**
   ```text
   To Setup Db and cache we need to refer to [devops module](ttps://github.com/immortals-ume/infra-devops)
   In this module run cache from redis folder  you should run single Node based redis since the module is configured using the same , and 
   postgres Db  from db folder  first run postgresql db primary then replica this will create read write replica
   ```

5. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

### Docker Installation

1. **Build the Docker image**
   ```bash
   docker build -t auth-app .
   ```

2. **Run with Docker Compose**
   ```bash
   docker-compose up -d
   ```

## ⚙️ Configuration

### Application Properties

Key configuration properties in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/auth_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  redis:
    host: localhost
    port: 6379
    
jwt:
  secret: ${JWT_SECRET}
  expiration: 3600000  # 1 hour in milliseconds
  refresh-expiration: 86400000  # 24 hours in milliseconds

security:
  rate-limit:
    enabled: true
    max-requests: 10
    time-window: 60  # seconds
```

### Environment Variables

| Variable      | Description                | Default   |
|---------------|----------------------------|-----------|
| `DB_USERNAME` | Database username          | postgres  |
| `DB_PASSWORD` | Database password          | -         |
| `JWT_SECRET`  | Secret key for JWT signing | -         |
| `REDIS_HOST`  | Redis host                 | localhost |
| `REDIS_PORT`  | Redis port                 | 6379      |

## 📚 API Documentation

The Auth App provides comprehensive API documentation using OpenAPI/Swagger.

### Accessing API Documentation

- **Swagger UI**: Available at `/swagger-ui.html` when the application is running
- **OpenAPI Specification**: Available at `/v3/api-docs`

### API Endpoints

The API is organized into the following categories:

1. **Authentication API**
    - Login, logout, token refresh
    - Guest authentication
    - Password management

2. **User Management API**
    - User registration and profile management
    - Role and permission management

3. **Location API**
    - Country, state, and city management
    - User address management

For detailed API documentation, refer to the OpenAPI specifications:

- [Authentication API Specification](/specs/auth-api.yaml)
- [Location API Specification](/specs/users-location-api.yaml)

## 📊 Data Models

### Core Entities

1. **User**
    - Basic user information (username, email, name)
    - Authentication details (password, account status)
    - Contact information (phone, email)
    - Relationships with roles and addresses

2. **UserAddress**
    - Address information (street, city, state, country)
    - Address type and status
    - Geolocation data (latitude, longitude)

3. **Roles**
    - Role name and description
    - Associated permissions
    - Active status

4. **Permissions**
    - Permission name
    - Active status

### Entity Relationships

- **User to Roles**: Many-to-Many (a user can have multiple roles, a role can be assigned to multiple users)
- **Roles to Permissions**: Many-to-Many (a role can have multiple permissions, a permission can be part of multiple
  roles)
- **User to UserAddress**: One-to-Many (a user can have multiple addresses)
- **UserAddress to Location Entities**: Many-to-One (an address belongs to one city, state, and country)

## 🔒 Security Best Practices

- **Authentication**: JWT-based with proper signature validation and expiration
- **Password Storage**: BCrypt hashing with appropriate work factor
- **Communication**: All API endpoints require HTTPS
- **Rate Limiting**: IP-based and username-based rate limiting to prevent brute force attacks
- **Input Validation**: Comprehensive validation for all user inputs
- **CORS**: Properly configured Cross-Origin Resource Sharing
- **Content Security Policy**: Implemented to prevent XSS attacks
- **Security Headers**: HTTP security headers to enhance browser security

## 👨‍💻 Development

### Development Environment Setup

1. **IDE Setup**
    - Install IntelliJ IDEA or Eclipse
    - Install Lombok plugin
    - Enable annotation processing

2. **Code Style**
    - Use the provided `.editorconfig` file
    - Follow Google Java Style Guide

3. **Git Workflow**
    - Create feature branches from `develop`
    - Use conventional commits
    - Submit pull requests for review

### Building the Project

```bash
# Clean and build
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Build with specific profile
mvn clean install -P dev
```

### Running Locally

```bash
# Run with default profile
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## 🧪 Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UserServiceTest

# Run with coverage
mvn test jacoco:report
```

### Test Categories

- **Unit Tests**: Test individual components in isolation
- **Integration Tests**: Test component interactions
- **API Tests**: Test API endpoints
- **Security Tests**: Test authentication and authorization

## 🚢 Deployment

### Deployment Options

1. **Standalone JAR**
   ```bash
   java -jar target/auth-app.jar
   ```

2. **Docker Container**
   ```bash
   docker run -p 8080:8080 auth-app
   ```

3. **Kubernetes**
   ```bash
   kubectl apply -f kubernetes/deployment.yaml
   ```

### Deployment Environments

- **Development**: For active development
- **Staging**: For pre-production testing
- **Production**: For live deployment

## 📈 Monitoring

### Health Checks

- **Endpoint**: `/actuator/health`
- **Metrics**: `/actuator/metrics`
- **Info**: `/actuator/info`

### Logging

- Structured logging with JSON format
- Log levels configurable at runtime
- Integration with centralized logging systems

## 🔧 Troubleshooting

### Common Issues

1. **Authentication Failures**
    - Check JWT token expiration
    - Verify user credentials
    - Check rate limiting status

2. **Database Connection Issues**
    - Verify database credentials
    - Check database availability
    - Ensure proper schema initialization

3. **Performance Issues**
    - Check Redis connection
    - Monitor database query performance
    - Review application logs for errors

### Support

For technical support, please contact the development team or create an issue in the repository.

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please adhere to the coding standards and include appropriate tests.

## 📄 License

This project is licensed under the MIT License—see the [LICENSE](LICENSE) file for details.

## 📬 Contact

For inquiries, contact **immortals-ume** at [srivastavakapil34@gmail.com](mailto:srivastavakapil34@gmail.com).

*Happy Coding! 🚀*