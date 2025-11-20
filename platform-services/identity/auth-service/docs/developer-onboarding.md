# Developer Onboarding Guide

This guide is designed to help new developers get up to speed with the Auth App project. It covers environment setup,
coding standards, development workflow, and common tasks.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Development Environment Setup](#development-environment-setup)
- [Project Structure](#project-structure)
- [Coding Standards](#coding-standards)
- [Development Workflow](#development-workflow)
- [Testing](#testing)
- [Common Development Tasks](#common-development-tasks)
- [Troubleshooting](#troubleshooting)
- [Additional Resources](#additional-resources)

## Prerequisites

Before you begin, ensure you have the following installed:

- Java 17 or higher
- Maven 3.8.x or higher
- Git
- Docker and Docker Compose
- PostgreSQL 14.x or higher (or Docker container)
- Redis 6.x or higher (or Docker container)
- IDE (IntelliJ IDEA recommended)

## Development Environment Setup

### 1. Clone the Repository

```bash
git clone https://github.com/immortals-ume/auth-app.git
cd auth-app
```

### 2. IDE Setup

#### IntelliJ IDEA

1. Open IntelliJ IDEA
2. Select "Open" and navigate to the cloned repository
3. Install recommended plugins:
    - Lombok
    - Spring Boot Assistant
    - SonarLint
    - JPA Buddy (optional)
4. Enable annotation processing:
    - Go to Settings/Preferences > Build, Execution, Deployment > Compiler > Annotation Processors
    - Check "Enable annotation processing"

#### Eclipse

1. Open Eclipse
2. Select "Import" > "Existing Maven Projects"
3. Navigate to the cloned repository and select the pom.xml file
4. Install Lombok plugin:
    - Download Lombok jar from https://projectlombok.org/download
    - Run the jar and follow the installation instructions
    - Restart Eclipse
5. Enable annotation processing:
    - Right-click on the project > Properties > Java Compiler > Annotation Processing
    - Check "Enable annotation processing"

### 3. Configure Environment Variables

Create a `.env` file in the project root with the following variables:

```
# Database Configuration
DB_URL=jdbc:postgresql://localhost:5432/auth_db
DB_USERNAME=postgres
DB_PASSWORD=your_password

# JWT Configuration
JWT_SECRET=your_jwt_secret_key
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=86400000

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379

# Mail Configuration (if needed)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
```

### 4. Set Up Local Database

#### Option 1: Using Docker

```bash
# Start PostgreSQL and Redis containers
docker-compose up -d postgres redis
```

#### Option 2: Using Local PostgreSQL

1. Create the database and schemas:

```bash
psql -U postgres
CREATE DATABASE auth_db;
\c auth_db
CREATE SCHEMA user_auth;
CREATE SCHEMA auth;
```

2. Run database migrations:

```bash
mvn flyway:migrate
```

### 5. Build the Project

```bash
mvn clean install
```

### 6. Run the Application

```bash
mvn spring-boot:run
```

The application will be available at http://localhost:8080.

## Project Structure

The Auth App follows a standard Spring Boot project structure with clear separation of concerns:

```
auth-app/
├── src/
│   ├── main/
│   │   ├── java/com/immortals/authapp/
│   │   │   ├── annotation/       # Custom annotations
│   │   │   ├── aop/              # Aspect-oriented programming components
│   │   │   ├── audit/            # Auditing functionality
│   │   │   ├── config/           # Configuration classes
│   │   │   ├── constants/        # Constants and enums
│   │   │   ├── context/          # Application context
│   │   │   ├── controller/       # REST controllers
│   │   │   ├── filter/           # Request filters
│   │   │   ├── helper/           # Helper classes
│   │   │   ├── manager/          # Manager classes
│   │   │   ├── model/            # Data models
│   │   │   ├── repository/       # Data repositories
│   │   │   ├── routing/          # Routing configuration
│   │   │   ├── security/         # Security configuration
│   │   │   ├── service/          # Business logic services
│   │   │   └── utils/            # Utility classes
│   │   └── resources/            # Application resources
│   └── test/                     # Test classes
├── docs/                         # Documentation
├── specs/                        # API specifications
└── pom.xml                       # Maven configuration
```

## Coding Standards

The Auth App follows Google Java Style Guide with some project-specific conventions:

### Naming Conventions

- **Classes**: PascalCase (e.g., `UserService`)
- **Interfaces**: PascalCase, often with "I" prefix (e.g., `IUserService`)
- **Methods**: camelCase (e.g., `getUserById`)
- **Variables**: camelCase (e.g., `userName`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_LOGIN_ATTEMPTS`)
- **Packages**: lowercase (e.g., `com.immortals.authapp.service`)

### Code Formatting

- Use 4 spaces for indentation (not tabs)
- Maximum line length: 120 characters
- Always use braces for control structures (if, for, while, etc.)
- Use trailing commas in multi-line lists and arrays

### Documentation

- All public classes and methods should have Javadoc comments
- Complex logic should be explained with inline comments
- Use `@author` tag to indicate code ownership

### Best Practices

- Follow SOLID principles
- Write unit tests for all business logic
- Keep methods small and focused
- Use dependency injection
- Handle exceptions properly
- Use logging appropriately

## Development Workflow

### 1. Branching Strategy

We follow a Git Flow-inspired branching strategy:

- `main`: Production-ready code
- `develop`: Integration branch for features
- `feature/*`: New features
- `bugfix/*`: Bug fixes
- `hotfix/*`: Urgent production fixes
- `release/*`: Release preparation

### 2. Development Process

1. Create a new branch from `develop`:
   ```bash
   git checkout develop
   git pull
   git checkout -b feature/your-feature-name
   ```

2. Implement your changes with regular commits
3. Write tests for your changes
4. Update documentation if necessary
5. Push your branch to the remote repository:
   ```bash
   git push -u origin feature/your-feature-name
   ```

6. Create a pull request to merge into `develop`
7. Address code review feedback
8. Once approved, merge your PR

### 3. Code Review Process

All code changes require at least one review before merging. Reviewers should check:

- Code quality and adherence to coding standards
- Test coverage
- Documentation
- Performance considerations
- Security implications

## Testing

### Types of Tests

- **Unit Tests**: Test individual components in isolation
- **Integration Tests**: Test component interactions
- **API Tests**: Test API endpoints
- **Security Tests**: Test authentication and authorization

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UserServiceTest

# Run with coverage report
mvn test jacoco:report
```

### Writing Tests

- Use JUnit 5 for testing
- Use Mockito for mocking dependencies
- Follow the AAA pattern (Arrange, Act, Assert)
- Test both happy paths and edge cases
- Aim for high test coverage (at least 80%)

## Common Development Tasks

### Adding a New API Endpoint

1. Define the endpoint in the appropriate OpenAPI specification file
2. Create or update the controller class
3. Implement the required service methods
4. Add appropriate validation
5. Implement security controls
6. Write tests for the new endpoint

### Adding a New Entity

1. Create the entity class in the `model/entity` package
2. Create a repository interface in the `repository` package
3. Create DTOs in the `model/dto` package
4. Implement service methods for CRUD operations
5. Add appropriate validation
6. Write tests for the new entity

### Implementing a New Feature

1. Understand the requirements
2. Design the solution
3. Update the data model if necessary
4. Implement the business logic in service classes
5. Create or update controllers for API endpoints
6. Write tests for the new feature
7. Update documentation

## Troubleshooting

### Common Issues

#### Database Connection Issues

- Check database credentials in your `.env` file
- Verify that the database server is running
- Check for firewall or network issues

#### Build Failures

- Run `mvn clean` and try again
- Check for dependency conflicts
- Verify Java version compatibility

#### Runtime Errors

- Check application logs for detailed error messages
- Verify environment variables are set correctly
- Check for configuration issues

### Debugging

- Use your IDE's debugging tools
- Add logging statements for troubleshooting
- Check application logs in `logs/` directory

## Additional Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/index.html)
- [JWT.io](https://jwt.io/)
- [Project Documentation](../README.md)
- [API Documentation](../specs/auth-api.yaml)
- [Data Models Documentation](data-models.md)
- [Architectural Decision Records](adr/)