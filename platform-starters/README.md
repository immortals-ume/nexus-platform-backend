# Platform Starters

Production-ready Spring Boot starters providing enterprise-grade functionality for microservices.

## 📦 Available Starters

| Starter | Description | Version |
|---------|-------------|---------|
| **cache-starter** | Multi-provider caching with resilience patterns | 1.0.0 |
| **common-starter** | Common utilities, exceptions, and base classes | 1.0.0 |
| **domain-starter** | Domain-driven design utilities | 1.0.0 |
| **messaging-starter** | Kafka event-driven messaging | 1.0.0 |

## 🚀 Quick Start

### Using Published Packages

Add to your project's `pom.xml`:

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/YOUR_GITHUB_USERNAME/YOUR_REPO_NAME</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.immortals.platform</groupId>
        <artifactId>cache-starter</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

### Authentication

Add to your `~/.m2/settings.xml`:

```xml
<settings>
    <servers>
        <server>
            <id>github</id>
            <username>YOUR_GITHUB_USERNAME</username>
            <password>YOUR_GITHUB_TOKEN</password>
        </server>
    </servers>
</settings>
```

## 📚 Starter Documentation

### Cache Starter

Enterprise-grade caching with multiple providers and resilience patterns.

**Features:**
- Multi-provider support (Caffeine, Redis, Multi-level)
- Circuit breaker and stampede protection
- Compression and encryption
- Declarative caching with annotations
- Comprehensive metrics and health checks

**Configuration:**

```yaml
immortals:
  cache:
    type: redis
    default-ttl: 1h
    redis-properties:
      host: localhost
      port: 6379
      resilience:
        circuit-breaker:
          enabled: true
        stampede-protection:
          enabled: true
```

**Usage:**

```java
@Service
public class UserService {
    
    @Cacheable(namespace = "users", key = "#userId", ttl = 3600)
    public User getUser(String userId) {
        return userRepository.findById(userId);
    }
    
    @CachePut(namespace = "users", key = "#user.id")
    public User updateUser(User user) {
        return userRepository.save(user);
    }
    
    @CacheEvict(namespace = "users", key = "#userId")
    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }
}
```

**Full Documentation:** [cache-starter/README.md](cache-starter/README.md)

### Common Starter

Common utilities, exception handling, and API response models.

**Features:**
- Standard exception hierarchy
- Global exception handler
- API response wrappers
- Utility classes (DateTimeUtils, StringUtils, ValidationUtils)

**Usage:**

```java
@RestController
public class UserController {
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> getUser(@PathVariable String id) {
        User user = userService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}
```

**Full Documentation:** [common-starter/README.md](common-starter/README.md)

### Messaging Starter

Event-driven communication with Apache Kafka.

**Features:**
- Automatic serialization/deserialization
- Idempotency checking
- Retry logic with exponential backoff
- Dead letter queue handling
- Comprehensive metrics

**Configuration:**

```yaml
platform:
  messaging:
    kafka:
      bootstrap-servers: localhost:9092
    retry:
      enabled: true
      max-attempts: 3
    dlq:
      enabled: true
```

**Usage:**

```java
@Service
public class OrderService {
    
    private final EventPublisher eventPublisher;
    
    public void createOrder(Order order) {
        orderRepository.save(order);
        
        DomainEvent<OrderCreated> event = DomainEvent.<OrderCreated>builder()
            .eventType("OrderCreated")
            .aggregateId(order.getId())
            .payload(new OrderCreated(order))
            .build();
        
        eventPublisher.publish("order-events", event);
    }
}
```

**Full Documentation:** [messaging-starter/README.md](messaging-starter/README.md)

### Domain Starter

Domain-driven design utilities and base classes.

**Full Documentation:** [domain-starter/Readme.md](domain-starter/Readme.md)

---

## 🔧 Publishing to GitHub Packages

### Prerequisites

1. **GitHub Personal Access Token** with `write:packages` permission
2. **Maven 3.6+** and **Java 17+**

### Quick Setup (Automated)

```bash
cd nexus-microbackend/platform-starters
./setup-publishing.sh
```

This will:
- Create/update your `~/.m2/settings.xml`
- Update all POM files with your repository info
- Set up environment variables

### Publish Packages

```bash
./publish-to-github.sh
```

### Manual Setup

#### 1. Create GitHub Token

1. Go to: https://github.com/settings/tokens
2. Click "Generate new token (classic)"
3. Select scopes:
   - ✅ `write:packages` (Upload packages)
   - ✅ `read:packages` (Download packages)
   - ✅ `repo` (Full control - if repo is private)
4. Copy the token

#### 2. Configure Maven Settings

Create/edit `~/.m2/settings.xml`:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              http://maven.apache.org/xsd/settings-1.0.0.xsd">
    
    <servers>
        <server>
            <id>github</id>
            <username>YOUR_GITHUB_USERNAME</username>
            <password>YOUR_GITHUB_TOKEN</password>
        </server>
    </servers>
    
    <profiles>
        <profile>
            <id>github</id>
            <repositories>
                <repository>
                    <id>github</id>
                    <url>https://maven.pkg.github.com/YOUR_GITHUB_USERNAME/YOUR_REPO_NAME</url>
                    <snapshots>
                        <enabled>true</enabled>
                    </snapshots>
                </repository>
            </repositories>
        </profile>
    </profiles>
    
    <activeProfiles>
        <activeProfile>github</activeProfile>
    </activeProfiles>
</settings>
```

**Replace:**
- `YOUR_GITHUB_USERNAME` with your GitHub username
- `YOUR_GITHUB_TOKEN` with the token you created
- `YOUR_REPO_NAME` with your repository name

#### 3. Update POM Files

Replace in all `pom.xml` files:
- `YOUR_GITHUB_USERNAME` → your GitHub username
- `YOUR_REPO_NAME` → your repository name (e.g., `nexus-composite`)

#### 4. Build and Deploy

```bash
cd nexus-microbackend/platform-starters

mvn clean install

mvn deploy -DskipTests
```

### Using Environment Variables

Instead of hardcoding credentials:

```xml
<servers>
    <server>
        <id>github</id>
        <username>${env.GITHUB_USERNAME}</username>
        <password>${env.GITHUB_TOKEN}</password>
    </server>
</servers>
```

Then set:

```bash
export GITHUB_USERNAME=your-username
export GITHUB_TOKEN=your-token
export GITHUB_REPO=your-repo-name
```

### Verify Publication

Check your packages at:
```
https://github.com/YOUR_USERNAME/YOUR_REPO/packages
```

---

## 🤖 Automated Publishing (GitHub Actions)

The repository includes a GitHub Actions workflow for automated publishing.

### Publish on Release

```bash
git tag v1.0.0
git push origin v1.0.0
```

Create a release on GitHub, and packages will be published automatically!

### Manual Trigger

1. Go to: **Actions** → **Publish Platform Starters**
2. Click **"Run workflow"**
3. Enter version (e.g., `1.0.0`)
4. Click **"Run workflow"**

### GitHub Actions Workflow

The workflow is located at `.github/workflows/publish-platform-starters.yml` and:
- Builds all modules
- Updates version numbers
- Publishes to GitHub Packages
- Creates deployment summary

---

## 🔄 Version Management

### Update Version

```bash
mvn versions:set -DnewVersion=1.1.0
mvn versions:commit
```

### Release Version

```bash
mvn versions:set -DnewVersion=1.0.0
mvn clean deploy -DskipTests
git tag v1.0.0
git push origin v1.0.0
```

### Snapshot Version

```bash
mvn versions:set -DnewVersion=1.1.0-SNAPSHOT
mvn clean deploy -DskipTests
```

---

## 👥 For Package Consumers

### Setup Authentication

Users need to authenticate to download packages.

Add to `~/.m2/settings.xml`:

```xml
<servers>
    <server>
        <id>github</id>
        <username>THEIR_GITHUB_USERNAME</username>
        <password>THEIR_GITHUB_TOKEN</password>
    </server>
</servers>
```

### Add Repository

Add to project's `pom.xml`:

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/YOUR_GITHUB_USERNAME/YOUR_REPO_NAME</url>
    </repository>
</repositories>
```

### Add Dependencies

```xml
<dependencies>
    <dependency>
        <groupId>com.immortals.platform</groupId>
        <artifactId>cache-starter</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <dependency>
        <groupId>com.immortals.platform</groupId>
        <artifactId>common-starter</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <dependency>
        <groupId>com.immortals.platform</groupId>
        <artifactId>messaging-starter</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

---

## 🔒 Security Best Practices

1. **Never commit tokens to Git**
2. **Use environment variables** for sensitive data
3. **Rotate tokens regularly**
4. **Use minimal token permissions**
5. **Enable 2FA** on GitHub account
6. **Review token usage** periodically

---

## 🚨 Troubleshooting

### 401 Unauthorized

**Cause:** Invalid credentials

**Solution:**
- Verify GitHub token is valid and not expired
- Check token has `write:packages` permission
- Ensure username/token in `settings.xml` are correct

### 403 Forbidden

**Cause:** Insufficient permissions

**Solution:**
- Verify you have write access to the repository
- Check repository URL is correct
- Ensure package name matches repository

### 404 Package Not Found

**Cause:** Package doesn't exist or wrong URL

**Solution:**
- Verify repository URL in `distributionManagement`
- Check package was successfully published
- Ensure authentication is configured

### Connection Timeout

**Cause:** Network issues

**Solution:**
- Check internet connection
- Verify GitHub is accessible
- Try using VPN if behind corporate firewall
- Check proxy settings

### Package Already Exists

**Cause:** Cannot overwrite existing versions

**Solution:**
- Increment version number
- Or delete existing version from GitHub
- Use snapshot versions for development

---

## 📊 Package Visibility

GitHub Packages can be:
- **Public**: Anyone can download (with authentication)
- **Private**: Only repository collaborators can access

To make packages public:
1. Go to package settings
2. Change visibility to "Public"
3. Confirm the change

---

## 🧹 Cleanup Old Versions

### Via GitHub UI

1. Go to package page
2. Click on the version
3. Click "Delete version"
4. Confirm deletion

### Via GitHub CLI

```bash
gh api -X DELETE /user/packages/maven/com.immortals.platform.cache-starter/versions/VERSION_ID
```

---

## 🏗️ Building Locally

### Build All Starters

```bash
cd nexus-microbackend/platform-starters
mvn clean install
```

### Build Specific Starter

```bash
cd nexus-microbackend/platform-starters/cache-starter
mvn clean install
```

### Skip Tests

```bash
mvn clean install -DskipTests
```

### Run Tests Only

```bash
mvn test
```

---

## 📝 Best Practices

1. **Use Semantic Versioning**: `MAJOR.MINOR.PATCH`
2. **Tag Releases**: Create Git tags for each release
3. **Update Changelog**: Document changes in each version
4. **Test Before Publishing**: Run full test suite
5. **Use CI/CD**: Automate publishing with GitHub Actions
6. **Secure Tokens**: Never commit tokens to Git
7. **Document Breaking Changes**: Clearly mark incompatible changes
8. **Review Dependencies**: Keep dependencies up to date
9. **Monitor Usage**: Track download metrics
10. **Provide Examples**: Include usage examples in documentation

---

## 📖 Additional Resources

- [GitHub Packages Documentation](https://docs.github.com/en/packages)
- [Maven Deploy Plugin](https://maven.apache.org/plugins/maven-deploy-plugin/)
- [GitHub Actions for Maven](https://docs.github.com/en/actions/guides/building-and-testing-java-with-maven)
- [Semantic Versioning](https://semver.org/)

---

## ✅ Pre-Publishing Checklist

Before publishing a new version:

- [ ] All tests pass: `mvn test`
- [ ] Version updated in all POMs
- [ ] CHANGELOG.md updated with changes
- [ ] README.md updated if needed
- [ ] Breaking changes documented
- [ ] Git tag created for release
- [ ] GitHub token is valid
- [ ] Maven settings.xml configured
- [ ] Local build successful: `mvn clean install`
- [ ] No snapshot dependencies (for releases)

---

## 🆘 Support

For issues or questions:
- 📖 Check starter-specific README files
- 🐛 Create a GitHub issue
- 💬 Contact platform team
- 📧 Email: kapil.srivastava1277999@gmail.com

---

## 📄 License

Copyright © 2024 Immortals Platform

Licensed under the Apache License, Version 2.0

---

## 🎉 Quick Commands Reference

```bash
./setup-publishing.sh

./publish-to-github.sh

mvn versions:set -DnewVersion=1.1.0

mvn clean install

mvn deploy -DskipTests

git tag v1.0.0 && git push origin v1.0.0
```

---

**Ready to publish?** Run `./publish-to-github.sh` and your packages will be available globally! 🚀
