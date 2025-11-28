# nexus-platform-backend
Production-ready enterprise microservices platform built with Spring Boot 3, Spring Cloud, and cloud-native patterns

## Java Version Compatibility

The Nexus microbackend platform supports multiple Java versions to provide flexibility for different development and deployment environments.

### Supported Java Versions

- **Java 17** (LTS) - Minimum required version
- **Java 21** (LTS) - Recommended for production (default)
- **Java 23** - Latest features and performance improvements

All services compile to Java 17 bytecode for maximum compatibility while allowing you to build and run with any supported version.

### Building with Different Java Versions

#### Default Build (Java 17 bytecode)

```bash
# Uses Java version from your JAVA_HOME
mvn clean install
```

#### Build with Specific Java Version

```bash
# Build with Java 17
mvn clean install -Djava.version=17

# Build with Java 21
mvn clean install -Djava.version=21

# Build with Java 23
mvn clean install -Djava.version=23
```

#### Verify Your Java Version

```bash
java -version
mvn -version
```

### Docker Base Image Configuration

The platform allows you to configure the Java runtime used in Docker containers via Maven properties.

#### Default Docker Build (Java 21)

```bash
mvn clean package
```

#### Build with Specific Java Runtime

```bash
# Java 17 runtime
mvn clean package -Ddocker.base.image=eclipse-temurin:17-jre-alpine

# Java 21 runtime (default)
mvn clean package -Ddocker.base.image=eclipse-temurin:21-jre-alpine

# Java 23 runtime
mvn clean package -Ddocker.base.image=eclipse-temurin:23-jre-alpine
```

#### Custom Base Images

You can use any compatible Java base image:

```bash
# Amazon Corretto
mvn clean package -Ddocker.base.image=amazoncorretto:21-alpine

# Azul Zulu
mvn clean package -Ddocker.base.image=azul/zulu-openjdk:21-jre

# Oracle GraalVM
mvn clean package -Ddocker.base.image=ghcr.io/graalvm/jdk:ol9-java21
```

### Configuration Properties

The following Maven properties control Java version behavior:

| Property | Default | Description |
|----------|---------|-------------|
| `java.version` | 17 | Java language level for source and target |
| `maven.compiler.source` | `${java.version}` | Java source compatibility |
| `maven.compiler.target` | `${java.version}` | Java target compatibility |
| `maven.compiler.release` | `${java.version}` | Java release version (recommended) |
| `docker.base.image` | `eclipse-temurin:21-jre-alpine` | Docker base image for containers |

### Troubleshooting

#### Build Fails with "Unsupported class file major version"

**Problem**: You're trying to run code compiled with a newer Java version on an older runtime.

**Solution**: Ensure your runtime Java version matches or exceeds the version used for compilation:

```bash
# Check runtime version
java -version

# Rebuild with compatible version
mvn clean install -Djava.version=17
```

#### Maven Enforcer Rejects Java Version

**Problem**: Build fails with "Detected JDK Version: X is not in the allowed range [17,)".

**Solution**: Upgrade to Java 17 or higher:

```bash
# Check current version
java -version

# Set JAVA_HOME to Java 17+ installation
export JAVA_HOME=/path/to/java17
```

#### Docker Container Fails to Start

**Problem**: Container exits with Java version errors.

**Solution**: Ensure the Docker base image matches your application requirements:

```bash
# Rebuild with correct base image
mvn clean package -Ddocker.base.image=eclipse-temurin:21-jre-alpine

# Verify Java version in container
docker run --rm your-image:tag java -version
```

#### Compilation Errors with Preview Features

**Problem**: Code uses preview features that aren't available in all Java versions.

**Solution**: Preview features are disabled by default for cross-version compatibility. If you need them:

1. Ensure all developers use the same Java version
2. Enable preview features explicitly:
   ```xml
   <compilerArgs>
       <arg>--enable-preview</arg>
   </compilerArgs>
   ```
3. Document the specific Java version requirement in your service README

#### IDE Shows Wrong Java Version

**Problem**: IDE reports different Java version than Maven build.

**Solution**: Configure your IDE to match Maven settings:

- **IntelliJ IDEA**: File → Project Structure → Project SDK and Language Level
- **Eclipse**: Project Properties → Java Compiler → Compiler compliance level
- **VS Code**: Update `java.configuration.runtimes` in settings.json

#### Different Java Versions on CI/CD

**Problem**: Build works locally but fails in CI/CD pipeline.

**Solution**: Ensure CI/CD environment uses compatible Java version:

```yaml
# GitHub Actions example
- uses: actions/setup-java@v3
  with:
    distribution: 'temurin'
    java-version: '21'

# GitLab CI example
image: eclipse-temurin:21-jdk

# Jenkins example
tools {
    jdk 'Java 21'
}
```

### Best Practices

1. **Use Java 21 for Production**: It's the latest LTS version with optimal performance and security updates.

2. **Compile to Java 17 Bytecode**: This ensures maximum compatibility across environments (default behavior).

3. **Match Docker Runtime to Build Version**: While not strictly required, using the same Java version for build and runtime avoids potential issues.

4. **Avoid Preview Features**: They break cross-version compatibility and aren't suitable for production code.

5. **Test Across Versions**: If your team uses different Java versions, test builds with all supported versions.

6. **Document Version Requirements**: If a specific service requires a particular Java version, document it in the service's README.

### Version-Specific Features

While the platform supports Java 17-23, be aware of version-specific features:

#### Java 17 (Baseline)
- Sealed classes
- Pattern matching for switch (preview)
- Strong encapsulation of JDK internals

#### Java 21 (Recommended)
- Virtual threads (Project Loom)
- Sequenced collections
- Pattern matching for switch (finalized)
- Record patterns
- String templates (preview)

#### Java 23 (Latest)
- Structured concurrency (preview)
- Scoped values (preview)
- Vector API improvements

**Note**: Using features from Java 21+ will prevent the code from running on Java 17 runtimes. Stick to Java 17 features for maximum compatibility. 
