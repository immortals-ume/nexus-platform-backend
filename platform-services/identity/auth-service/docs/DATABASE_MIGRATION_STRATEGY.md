# Database Migration Strategy for Auth Service

## Overview

This document outlines the database migration strategy for the auth-service using Liquibase. The strategy ensures consistent, version-controlled database schema changes across all environments.

## Liquibase Configuration

### Configuration Location
- **Master Changelog**: `src/main/resources/db/changelog-master.xml`
- **Individual Changesets**: `src/main/resources/db/changelog-v1/`
- **Configuration**: `config-server/src/main/resources/configurations/auth-service.yml`

### Key Configuration Settings

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Hibernate validates schema but doesn't modify it
  liquibase:
    enabled: true
    change-log: classpath:db/changelog-master.xml
    default-schema: user_auth
    liquibase-schema: public
    drop-first: false
    contexts: default
    labels: v1
```

## Versioning Strategy

### Naming Convention

All migration files follow this naming pattern:
```
{sequence}-{description}.xml
```

Examples:
- `001-create-auth-schema.xml`
- `002-create-countries-table.xml`
- `003-create-states-table.xml`

### ChangeSet ID Convention

Each changeSet has a unique ID following this pattern:
```
{sequence}-{description}
```

Example:
```xml
<changeSet id="001-create-schemas" author="auth-service">
    <!-- changes here -->
</changeSet>
```

### Version Folders

Migrations are organized by major version:
- `changelog-v1/` - Version 1.x migrations
- `changelog-v2/` - Version 2.x migrations (future)

## Current Schema Structure

### Schemas
- `user_auth` - Main authentication and user data
- `user_audit` - Audit tables for Hibernate Envers

### Tables

#### Core Tables
1. **users** - User accounts and authentication data
2. **role** - User roles (ADMIN, USER, etc.)
3. **permission** - Granular permissions
4. **user_role** - Many-to-many relationship between users and roles
5. **role_permission** - Many-to-many relationship between roles and permissions

#### Location Tables
6. **countries** - Country master data
7. **states** - State/province master data
8. **cities** - City master data
9. **user_address** - User address information

#### Audit Tables
10. **revinfo** - Revision information for audit trail
11. **users_aud** - Audit history for users table
12. **role_aud** - Audit history for role table
13. **permission_aud** - Audit history for permission table

### Sequences
- `auth.user_sequence` - User ID generation
- `auth.user_address_sequence` - User address ID generation
- `user_audit.revinfo_seq` - Revision number generation

## Migration Workflow

### Adding New Migrations

1. **Create a new changeset file**:
   ```bash
   touch src/main/resources/db/changelog-v1/012-add-new-feature.xml
   ```

2. **Write the changeset**:
   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <databaseChangeLog
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
           xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                         http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

       <changeSet id="012-add-new-feature" author="your-name">
           <!-- Your changes here -->
           <rollback>
               <!-- Rollback instructions -->
           </rollback>
       </changeSet>
   </databaseChangeLog>
   ```

3. **Include in master changelog**:
   ```xml
   <include file="db/changelog-v1/012-add-new-feature.xml"/>
   ```

4. **Test locally**:
   ```bash
   mvn liquibase:update
   ```

### Best Practices

1. **Never modify existing changesets** - Once a changeset is deployed, create a new one for changes
2. **Always include rollback** - Provide rollback instructions for each changeset
3. **Use descriptive IDs** - Make changeset IDs self-documenting
4. **Test migrations** - Test both forward and rollback migrations
5. **Keep changesets atomic** - Each changeset should represent a single logical change
6. **Use contexts and labels** - Tag changesets for environment-specific migrations

### Rollback Strategy

To rollback the last migration:
```bash
mvn liquibase:rollback -Dliquibase.rollbackCount=1
```

To rollback to a specific date:
```bash
mvn liquibase:rollback -Dliquibase.rollbackDate=2024-01-01
```

To rollback to a specific tag:
```bash
mvn liquibase:rollback -Dliquibase.rollbackTag=v1.0.0
```

## Environment-Specific Migrations

### Using Contexts

For environment-specific migrations, use contexts:

```xml
<changeSet id="013-dev-test-data" author="auth-service" context="dev">
    <!-- Development test data -->
</changeSet>
```

Run with context:
```bash
mvn liquibase:update -Dliquibase.contexts=dev
```

### Using Labels

For feature-specific migrations, use labels:

```xml
<changeSet id="014-feature-x" author="auth-service" labels="feature-x">
    <!-- Feature X changes -->
</changeSet>
```

## Liquibase Maven Plugin

### Available Commands

```bash
# Update database to latest version
mvn liquibase:update

# Generate SQL for review (without executing)
mvn liquibase:updateSQL

# Rollback last N changes
mvn liquibase:rollback -Dliquibase.rollbackCount=N

# Show status of all changesets
mvn liquibase:status

# Generate diff between database and entities
mvn liquibase:diff

# Clear checksums (use with caution)
mvn liquibase:clearCheckSums

# Validate changelog
mvn liquibase:validate
```

## Monitoring and Troubleshooting

### Liquibase Tables

Liquibase creates two tracking tables:
- `DATABASECHANGELOG` - Records all executed changesets
- `DATABASECHANGELOGLOCK` - Prevents concurrent migrations

### Common Issues

1. **Checksum validation failed**
   - Cause: Changeset was modified after execution
   - Solution: Never modify executed changesets; create new ones

2. **Lock not released**
   - Cause: Previous migration failed or was interrupted
   - Solution: `mvn liquibase:releaseLocks`

3. **Schema not found**
   - Cause: Schema doesn't exist
   - Solution: Ensure schema creation changeset runs first

## Integration with CI/CD

### Pre-deployment Validation

```bash
# Validate changelog syntax
mvn liquibase:validate

# Generate SQL for review
mvn liquibase:updateSQL > migration-preview.sql
```

### Automated Deployment

The application automatically runs Liquibase migrations on startup when:
- `spring.liquibase.enabled=true`
- Application has database connectivity

### Manual Deployment

For production environments, consider running migrations manually:

```bash
# Set liquibase.enabled=false in production config
# Run migrations separately before deployment
mvn liquibase:update -Dspring.profiles.active=production
```

## Requirements Mapping

This migration strategy addresses the following requirements:

- **Requirement 11.1**: Database schema versioning with Liquibase
- **Requirement 11.2**: Automated migration execution on application startup
- **Requirement 11.3**: Rollback capability for failed migrations
- **Requirement 11.4**: Migration history tracking and audit trail

## References

- [Liquibase Documentation](https://docs.liquibase.com/)
- [Liquibase Best Practices](https://www.liquibase.org/get-started/best-practices)
- [Spring Boot Liquibase Integration](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration-tool.liquibase)
