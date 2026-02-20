# Common Starter

Common starter module for Spring Boot microservices. It provides shared exceptions, API response models, utilities, and auto-configuration so teams can ship services with consistent behavior and minimal boilerplate.

## What problem does it solve?

Microservices often repeat the same patterns for errors, responses, validation, and utility helpers. This starter centralizes those patterns so every service behaves consistently and developers do not reimplement the basics.

## Key features

- Standard exception hierarchy with HTTP status mapping
- Global exception handler for REST controllers
- Consistent API response and pagination models
- Correlation ID generation and propagation
- Utility helpers for date/time, strings, validation, and UUIDs
- Auto-configuration with sensible defaults

## Tech stack

- Java
- Spring Boot
- Maven

## Prerequisites

- JDK 17+ (or your project standard)
- Maven 3.8+

## Setup and run

1. Add the dependency to your service `pom.xml`:

```xml
<dependency>
  <groupId>com.immortals.platform</groupId>
  <artifactId>common-starter</artifactId>
  <version>1.0.0</version>
</dependency>
```

2. (Optional) Configure defaults in `application.yml`:

```yaml
platform:
  common:
    exception-handling:
      enabled: true
      include-stack-trace: false
    correlation-id:
      enabled: true
      header-name: X-Correlation-ID
      generate-if-missing: true
```

3. Start your Spring Boot service as usual.

## Project structure (high level)

```
common-starter/
  src/main/java/com/immortals/platform/common/
    exception/   # Exceptions + global handler
    dto/         # ApiResponse, ErrorResponse, pagination models
    util/        # Date, string, validation, UUID helpers
    config/      # Auto-configuration
```

## How to use

- Throw platform exceptions (for example `ResourceNotFoundException`) in your services.
- Return `ApiResponse` or `PageableResponse` from controllers for a consistent API shape.
- Use utility classes in `util/` instead of duplicating common logic.

Example (exception + response):

```java
@GetMapping("/users/{id}")
public ResponseEntity<ApiResponse<UserDTO>> getUser(@PathVariable String id) {
    User user = userService.findById(id);
    return ResponseEntity.ok(ApiResponse.success(UserDTO.from(user)));
}
```

## Common commands

From the repository root:

```bash
mvn -pl common-starter -am clean install
```

## Contributing

- Keep changes focused and documented.
- Add or update tests when you change behavior.
- Follow existing package naming and code style.

## License

Copyright © 2024 Immortals Platform

Licensed under the Apache License, Version 2.0
