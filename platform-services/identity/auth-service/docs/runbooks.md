# Operational Runbooks

This document provides step-by-step procedures for common operational tasks related to the Auth App. These runbooks are
designed to help operations teams manage the application in production environments.

## Table of Contents

- [Deployment](#deployment)
    - [Standard Deployment](#standard-deployment)
    - [Rolling Update](#rolling-update)
    - [Rollback Procedure](#rollback-procedure)
- [Monitoring](#monitoring)
    - [Health Check](#health-check)
    - [Log Analysis](#log-analysis)
    - [Performance Monitoring](#performance-monitoring)
- [Database Operations](#database-operations)
    - [Database Backup](#database-backup)
    - [Database Restore](#database-restore)
    - [Database Migration](#database-migration)
- [Security Operations](#security-operations)
    - [JWT Secret Rotation](#jwt-secret-rotation)
    - [SSL Certificate Renewal](#ssl-certificate-renewal)
    - [Security Audit](#security-audit)
- [Scaling](#scaling)
    - [Horizontal Scaling](#horizontal-scaling)
    - [Vertical Scaling](#vertical-scaling)
- [Troubleshooting](#troubleshooting)
    - [High CPU Usage](#high-cpu-usage)
    - [Memory Leaks](#memory-leaks)
    - [Connection Issues](#connection-issues)
    - [Authentication Failures](#authentication-failures)
- [Disaster Recovery](#disaster-recovery)
    - [Failover Procedure](#failover-procedure)
    - [Data Recovery](#data-recovery)

## Deployment

### Standard Deployment

**Purpose**: Deploy a new version of the Auth App to production.

**Prerequisites**:

- New version has passed all tests in staging environment
- Deployment window has been scheduled
- Database backup has been performed

**Steps**:

1. **Prepare Deployment Package**
   ```bash
   # Build the application
   mvn clean package -DskipTests
   
   # Verify the build
   ls -la target/auth-app.jar
   ```

2. **Deploy to Production**
   ```bash
   # Copy the JAR to the production server
   scp target/auth-app.jar user@prod-server:/opt/auth-app/
   
   # SSH into the production server
   ssh user@prod-server
   
   # Stop the current instance
   sudo systemctl stop auth-app
   
   # Backup the current version
   cp /opt/auth-app/auth-app.jar /opt/auth-app/backups/auth-app-$(date +%Y%m%d%H%M%S).jar
   
   # Replace with the new version
   cp /opt/auth-app/auth-app.jar.new /opt/auth-app/auth-app.jar
   
   # Start the service
   sudo systemctl start auth-app
   
   # Verify the service is running
   sudo systemctl status auth-app
   ```

3. **Verify Deployment**
   ```bash
   # Check application logs
   tail -f /var/log/auth-app/application.log
   
   # Verify the application is responding
   curl -I http://localhost:8080/actuator/health
   ```

4. **Post-Deployment Tasks**
    - Monitor application metrics for 15 minutes
    - Verify key functionality through smoke tests
    - Update deployment documentation

### Rolling Update

**Purpose**: Update the Auth App with zero downtime in a containerized environment.

**Prerequisites**:

- Kubernetes or similar container orchestration platform
- New container image is available in the registry

**Steps**:

1. **Verify the New Image**
   ```bash
   # Check that the image exists
   docker pull your-registry/auth-app:new-version
   ```

2. **Update Kubernetes Deployment**
   ```bash
   # Update the deployment with the new image
   kubectl set image deployment/auth-app auth-app=your-registry/auth-app:new-version
   
   # Watch the rollout status
   kubectl rollout status deployment/auth-app
   ```

3. **Verify the Deployment**
   ```bash
   # Check that all pods are running the new version
   kubectl get pods -l app=auth-app -o jsonpath='{.items[*].spec.containers[*].image}'
   
   # Verify the application health
   kubectl exec -it $(kubectl get pods -l app=auth-app -o jsonpath='{.items[0].metadata.name}') -- curl localhost:8080/actuator/health
   ```

4. **Post-Deployment Tasks**
    - Monitor application metrics
    - Run smoke tests
    - Update deployment documentation

### Rollback Procedure

**Purpose**: Revert to a previous version in case of issues with a new deployment.

**Prerequisites**:

- Previous version is available
- Issue with current deployment has been identified

**Steps**:

1. **For Traditional Deployment**
   ```bash
   # SSH into the production server
   ssh user@prod-server
   
   # Stop the current instance
   sudo systemctl stop auth-app
   
   # Restore the previous version
   cp /opt/auth-app/backups/auth-app-previous.jar /opt/auth-app/auth-app.jar
   
   # Start the service
   sudo systemctl start auth-app
   
   # Verify the service is running
   sudo systemctl status auth-app
   ```

2. **For Kubernetes Deployment**
   ```bash
   # Rollback to the previous version
   kubectl rollout undo deployment/auth-app
   
   # Watch the rollout status
   kubectl rollout status deployment/auth-app
   
   # Verify the rollback
   kubectl get pods -l app=auth-app -o jsonpath='{.items[*].spec.containers[*].image}'
   ```

3. **Post-Rollback Tasks**
    - Document the issue that required rollback
    - Verify application functionality
    - Schedule a fix for the issue

## Monitoring

### Health Check

**Purpose**: Verify the health of the Auth App and its dependencies.

**Steps**:

1. **Check Application Health**
   ```bash
   # Using curl
   curl -X GET http://localhost:8080/actuator/health | jq
   
   # Expected output
   # {
   #   "status": "UP",
   #   "components": {
   #     "db": {
   #       "status": "UP"
   #     },
   #     "redis": {
   #       "status": "UP"
   #     },
   #     "diskSpace": {
   #       "status": "UP"
   #     }
   #   }
   # }
   ```

2. **Check Database Connection**
   ```bash
   # Using PostgreSQL client
   psql -h localhost -U postgres -d auth_db -c "SELECT 1"
   ```

3. **Check Redis Connection**
   ```bash
   # Using Redis CLI
   redis-cli ping
   # Expected output: PONG
   ```

4. **Check Memory Usage**
   ```bash
   # Get JVM memory metrics
   curl -X GET http://localhost:8080/actuator/metrics/jvm.memory.used | jq
   ```

5. **Check Thread Status**
   ```bash
   # Get thread metrics
   curl -X GET http://localhost:8080/actuator/metrics/jvm.threads.live | jq
   ```

### Log Analysis

**Purpose**: Analyze application logs to identify issues or monitor behavior.

**Steps**:

1. **Access Application Logs**
   ```bash
   # View recent logs
   tail -n 100 /var/log/auth-app/application.log
   
   # Follow logs in real-time
   tail -f /var/log/auth-app/application.log
   
   # Search for errors
   grep "ERROR" /var/log/auth-app/application.log
   ```

2. **Filter Logs by Component**
   ```bash
   # Filter authentication-related logs
   grep "AuthenticationController" /var/log/auth-app/application.log
   
   # Filter rate limiting logs
   grep "RateLimitFilter" /var/log/auth-app/application.log
   ```

3. **Analyze Logs with ELK Stack** (if available)
    - Access Kibana dashboard
    - Use search query: `service:auth-app AND level:ERROR`
    - Set appropriate time range
    - Analyze error patterns and frequency

4. **Export Logs for Analysis**
   ```bash
   # Export logs for a specific time period
   grep "2023-08-04" /var/log/auth-app/application.log > /tmp/auth-app-20230804.log
   ```

### Performance Monitoring

**Purpose**: Monitor and analyze the performance of the Auth App.

**Steps**:

1. **Check System Resource Usage**
   ```bash
   # CPU and memory usage
   top -p $(pgrep -f auth-app.jar)
   
   # Disk usage
   df -h /opt/auth-app
   ```

2. **Monitor JVM Metrics**
   ```bash
   # Get heap memory usage
   curl -X GET http://localhost:8080/actuator/metrics/jvm.memory.used | jq
   
   # Get garbage collection metrics
   curl -X GET http://localhost:8080/actuator/metrics/jvm.gc.pause | jq
   ```

3. **Monitor HTTP Requests**
   ```bash
   # Get HTTP request metrics
   curl -X GET http://localhost:8080/actuator/metrics/http.server.requests | jq
   ```

4. **Check Database Performance**
   ```bash
   # Connect to PostgreSQL
   psql -h localhost -U postgres -d auth_db
   
   # Check active connections
   SELECT count(*) FROM pg_stat_activity WHERE datname = 'auth_db';
   
   # Check slow queries
   SELECT * FROM pg_stat_activity WHERE state = 'active' AND now() - query_start > interval '5 seconds';
   ```

5. **Check Redis Performance**
   ```bash
   # Connect to Redis
   redis-cli
   
   # Check memory usage
   INFO memory
   
   # Check client connections
   INFO clients
   ```

## Database Operations

### Database Backup

**Purpose**: Create a backup of the Auth App database.

**Prerequisites**:

- PostgreSQL client installed
- Sufficient disk space for backup
- Database credentials

**Steps**:

1. **Create a Full Database Backup**
   ```bash
   # Using pg_dump
   pg_dump -h localhost -U postgres -d auth_db -F c -f /backup/auth_db_$(date +%Y%m%d%H%M%S).dump
   
   # Verify the backup file
   ls -lh /backup/auth_db_*.dump
   ```

2. **Create a Schema-Only Backup**
   ```bash
   # Backup only the schema
   pg_dump -h localhost -U postgres -d auth_db --schema-only -f /backup/auth_db_schema_$(date +%Y%m%d%H%M%S).sql
   ```

3. **Create a Data-Only Backup**
   ```bash
   # Backup only the data
   pg_dump -h localhost -U postgres -d auth_db --data-only -f /backup/auth_db_data_$(date +%Y%m%d%H%M%S).sql
   ```

4. **Automate Regular Backups**
   ```bash
   # Create a cron job for daily backups
   echo "0 2 * * * postgres pg_dump -h localhost -U postgres -d auth_db -F c -f /backup/auth_db_\$(date +\%Y\%m\%d).dump" | sudo tee -a /etc/cron.d/db-backup
   ```

5. **Rotate Backups**
   ```bash
   # Keep only the last 7 daily backups
   find /backup -name "auth_db_*.dump" -type f -mtime +7 -delete
   ```

### Database Restore

**Purpose**: Restore the Auth App database from a backup.

**Prerequisites**:

- PostgreSQL client installed
- Backup file available
- Database credentials

**Steps**:

1. **Prepare for Restore**
   ```bash
   # Stop the application
   sudo systemctl stop auth-app
   
   # Connect to PostgreSQL
   psql -h localhost -U postgres
   
   # Drop the existing database (if necessary)
   DROP DATABASE auth_db;
   
   # Create a new database
   CREATE DATABASE auth_db;
   
   # Exit PostgreSQL
   \q
   ```

2. **Restore from Full Backup**
   ```bash
   # Using pg_restore for custom format backups
   pg_restore -h localhost -U postgres -d auth_db /backup/auth_db_20230804.dump
   
   # Or using psql for SQL format backups
   psql -h localhost -U postgres -d auth_db -f /backup/auth_db_20230804.sql
   ```

3. **Verify the Restore**
   ```bash
   # Connect to the restored database
   psql -h localhost -U postgres -d auth_db
   
   # Check table counts
   SELECT count(*) FROM user_auth.users;
   SELECT count(*) FROM user_auth.role;
   ```

4. **Restart the Application**
   ```bash
   sudo systemctl start auth-app
   
   # Verify the application is running
   sudo systemctl status auth-app
   ```

### Database Migration

**Purpose**: Apply database schema changes to the Auth App database.

**Prerequisites**:

- Flyway or similar migration tool configured
- Migration scripts prepared
- Database backup completed

**Steps**:

1. **Prepare Migration Scripts**
   ```bash
   # Create a new migration script
   touch src/main/resources/db/migration/V2_1_0__add_new_column.sql
   
   # Edit the migration script
   echo "ALTER TABLE user_auth.users ADD COLUMN new_column VARCHAR(255);" > src/main/resources/db/migration/V2_1_0__add_new_column.sql
   ```

2. **Test Migration in Development**
   ```bash
   # Run Flyway migration
   mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/auth_db_dev -Dflyway.user=postgres -Dflyway.password=password
   ```

3. **Apply Migration in Production**
   ```bash
   # Take a database backup first
   pg_dump -h localhost -U postgres -d auth_db -F c -f /backup/auth_db_pre_migration_$(date +%Y%m%d%H%M%S).dump
   
   # Run Flyway migration
   mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/auth_db -Dflyway.user=postgres -Dflyway.password=password
   ```

4. **Verify Migration**
   ```bash
   # Connect to the database
   psql -h localhost -U postgres -d auth_db
   
   # Check migration history
   SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
   
   # Verify schema changes
   \d user_auth.users
   ```

5. **Rollback Plan (if needed)**
   ```bash
   # If migration fails, restore from backup
   pg_restore -h localhost -U postgres -d auth_db /backup/auth_db_pre_migration_20230804123456.dump
   ```

## Security Operations

### JWT Secret Rotation

**Purpose**: Rotate the JWT signing secret to enhance security.

**Prerequisites**:

- Access to application configuration
- Ability to restart the application

**Steps**:

1. **Generate a New Secret**
   ```bash
   # Generate a secure random string
   NEW_SECRET=$(openssl rand -base64 32)
   echo $NEW_SECRET
   ```

2. **Update Configuration**
   ```bash
   # Edit environment variables or configuration file
   sudo vi /opt/auth-app/.env
   
   # Update JWT_SECRET with the new value
   # JWT_SECRET=new_secret_value
   ```

3. **Implement Graceful Rotation**
   ```bash
   # Option 1: Update configuration to accept both old and new secrets temporarily
   # Edit application.yml to add oldJwtSecret property
   
   # Option 2: Use a secret management service that supports versioning
   ```

4. **Restart the Application**
   ```bash
   sudo systemctl restart auth-app
   ```

5. **Verify JWT Functionality**
   ```bash
   # Test authentication
   curl -X POST http://localhost:8080/api/v1/auth/login -d '{"username":"test","password":"password"}' -H "Content-Type: application/json"
   
   # Verify the token is accepted
   curl -X GET http://localhost:8080/api/v1/users/test -H "Authorization: Bearer <token>"
   ```

6. **Monitor for Issues**
   ```bash
   # Watch application logs
   tail -f /var/log/auth-app/application.log | grep -i "jwt\|token\|auth"
   ```

### SSL Certificate Renewal

**Purpose**: Renew SSL certificates before they expire.

**Prerequisites**:

- Access to SSL certificate management
- Domain control verification

**Steps**:

1. **Check Certificate Expiration**
   ```bash
   # Check expiration date
   openssl s_client -connect your-domain.com:443 -servername your-domain.com </dev/null 2>/dev/null | openssl x509 -noout -dates
   ```

2. **Generate a New Certificate**
   ```bash
   # Using Let's Encrypt/Certbot
   certbot certonly --webroot -w /var/www/html -d your-domain.com
   
   # Or using OpenSSL for CSR
   openssl req -new -newkey rsa:2048 -nodes -keyout your-domain.key -out your-domain.csr
   ```

3. **Install the New Certificate**
   ```bash
   # Copy certificates to the appropriate location
   sudo cp /etc/letsencrypt/live/your-domain.com/fullchain.pem /opt/auth-app/ssl/
   sudo cp /etc/letsencrypt/live/your-domain.com/privkey.pem /opt/auth-app/ssl/
   
   # Update permissions
   sudo chown auth-app:auth-app /opt/auth-app/ssl/*.pem
   sudo chmod 600 /opt/auth-app/ssl/*.pem
   ```

4. **Update Configuration (if needed)**
   ```bash
   # Edit SSL configuration
   sudo vi /opt/auth-app/config/ssl.conf
   
   # Update certificate paths if necessary
   ```

5. **Restart Web Server or Load Balancer**
   ```bash
   # For Nginx
   sudo systemctl restart nginx
   
   # For Apache
   sudo systemctl restart apache2
   ```

6. **Verify Certificate Installation**
   ```bash
   # Check certificate
   openssl s_client -connect your-domain.com:443 -servername your-domain.com </dev/null 2>/dev/null | openssl x509 -noout -dates
   
   # Verify HTTPS access
   curl -I https://your-domain.com
   ```

### Security Audit

**Purpose**: Perform a security audit of the Auth App.

**Steps**:

1. **Review Access Logs**
   ```bash
   # Check for suspicious access patterns
   grep "login" /var/log/auth-app/application.log | grep "failed"
   
   # Look for rate limit hits
   grep "rate limit exceeded" /var/log/auth-app/application.log
   ```

2. **Check User Accounts**
   ```bash
   # Connect to the database
   psql -h localhost -U postgres -d auth_db
   
   # Check for recently created admin accounts
   SELECT * FROM user_auth.users JOIN user_auth.user_role ON users.user_id = user_role.user_id JOIN user_auth.role ON user_role.role_id = role.role_id WHERE role.role_name = 'ROLE_ADMIN' AND users.created_date > now() - interval '30 days';
   
   # Check for locked accounts
   SELECT * FROM user_auth.users WHERE account_locked = true;
   ```

3. **Review Permissions**
   ```bash
   # Check role-permission mappings
   SELECT r.role_name, p.permission_name FROM user_auth.role r JOIN user_auth.role_permission rp ON r.role_id = rp.role_id JOIN user_auth.permission p ON rp.permission_id = p.permission_id ORDER BY r.role_name;
   ```

4. **Scan for Vulnerabilities**
   ```bash
   # Run OWASP ZAP scan
   zap-cli quick-scan --self-contained --start-options "-config api.disablekey=true" https://your-domain.com
   
   # Run dependency check
   mvn org.owasp:dependency-check-maven:check
   ```

5. **Check SSL Configuration**
   ```bash
   # Test SSL security
   nmap --script ssl-enum-ciphers -p 443 your-domain.com
   
   # Or use SSL Labs
   # Visit https://www.ssllabs.com/ssltest/analyze.html?d=your-domain.com
   ```

6. **Document Findings**
   ```bash
   # Create audit report
   echo "# Security Audit Report - $(date +%Y-%m-%d)" > /tmp/security-audit.md
   echo "## Findings" >> /tmp/security-audit.md
   # Add findings and recommendations
   ```

## Scaling

### Horizontal Scaling

**Purpose**: Increase capacity by adding more instances of the Auth App.

**Prerequisites**:

- Load balancer configured
- Shared database and Redis instances

**Steps**:

1. **Prepare for Scaling**
   ```bash
   # Verify the application is stateless
   # Ensure all instances use the same database and Redis
   ```

2. **Add New Instances (Traditional)**
   ```bash
   # Deploy the application to a new server
   scp auth-app.jar user@new-server:/opt/auth-app/
   
   # Configure the new instance
   scp .env user@new-server:/opt/auth-app/
   
   # Start the new instance
   ssh user@new-server "sudo systemctl start auth-app"
   ```

3. **Add New Instances (Kubernetes)**
   ```bash
   # Scale the deployment
   kubectl scale deployment auth-app --replicas=5
   
   # Verify the scaling
   kubectl get pods -l app=auth-app
   ```

4. **Update Load Balancer**
   ```bash
   # For manual load balancer configuration
   sudo vi /etc/nginx/conf.d/auth-app.conf
   
   # Add the new server to the upstream block
   # upstream auth-app {
   #   server server1:8080;
   #   server server2:8080;
   #   server new-server:8080;
   # }
   
   # Reload Nginx
   sudo systemctl reload nginx
   ```

5. **Verify Load Distribution**
   ```bash
   # Check that requests are distributed
   for i in {1..10}; do curl -I http://load-balancer-ip/actuator/health; done
   ```

### Vertical Scaling

**Purpose**: Increase capacity by adding more resources to existing instances.

**Prerequisites**:

- Ability to modify server resources
- Planned downtime (if required)

**Steps**:

1. **Assess Current Resource Usage**
   ```bash
   # Check current CPU and memory usage
   top -p $(pgrep -f auth-app.jar)
   
   # Check JVM memory settings
   ps -ef | grep auth-app.jar | grep -o "Xmx[0-9]*[mMgG]"
   ```

2. **Modify JVM Settings**
   ```bash
   # Edit the service file
   sudo vi /etc/systemd/system/auth-app.service
   
   # Update memory settings
   # ExecStart=/usr/bin/java -Xms1G -Xmx2G -jar /opt/auth-app/auth-app.jar
   
   # Reload systemd
   sudo systemctl daemon-reload
   ```

3. **Increase Server Resources**
   ```bash
   # For cloud VMs, resize the instance according to provider instructions
   
   # For physical servers, add RAM or CPU as needed
   ```

4. **Restart the Application**
   ```bash
   sudo systemctl restart auth-app
   ```

5. **Verify Resource Allocation**
   ```bash
   # Check new memory allocation
   ps -ef | grep auth-app.jar | grep -o "Xmx[0-9]*[mMgG]"
   
   # Monitor performance
   top -p $(pgrep -f auth-app.jar)
   ```

## Troubleshooting

### High CPU Usage

**Purpose**: Diagnose and resolve high CPU usage issues.

**Steps**:

1. **Identify CPU Usage**
   ```bash
   # Check overall CPU usage
   top
   
   # Check specific process CPU usage
   top -p $(pgrep -f auth-app.jar)
   ```

2. **Capture Thread Dump**
   ```bash
   # Get the process ID
   PID=$(pgrep -f auth-app.jar)
   
   # Capture thread dump
   jstack $PID > /tmp/thread-dump-$(date +%Y%m%d%H%M%S).txt
   
   # Or using JMX
   curl -X GET http://localhost:8080/actuator/threaddump > /tmp/thread-dump-$(date +%Y%m%d%H%M%S).json
   ```

3. **Analyze Thread Dump**
   ```bash
   # Look for threads in RUNNABLE state
   grep -A 10 "RUNNABLE" /tmp/thread-dump-*.txt
   
   # Look for blocked threads
   grep -A 10 "BLOCKED" /tmp/thread-dump-*.txt
   ```

4. **Check for Infinite Loops or Inefficient Code**
   ```bash
   # Look for suspicious patterns in the thread dump
   # Check application logs for errors
   tail -n 1000 /var/log/auth-app/application.log | grep -i "error\|exception"
   ```

5. **Monitor JVM Metrics**
   ```bash
   # Check garbage collection
   curl -X GET http://localhost:8080/actuator/metrics/jvm.gc.pause | jq
   ```

6. **Take Action**
   ```bash
   # Restart the application if necessary
   sudo systemctl restart auth-app
   
   # Apply code fixes if identified
   # Scale horizontally if needed
   ```

### Memory Leaks

**Purpose**: Diagnose and resolve memory leaks.

**Steps**:

1. **Identify Memory Usage**
   ```bash
   # Check memory usage
   free -m
   
   # Check JVM memory usage
   curl -X GET http://localhost:8080/actuator/metrics/jvm.memory.used | jq
   ```

2. **Capture Heap Dump**
   ```bash
   # Get the process ID
   PID=$(pgrep -f auth-app.jar)
   
   # Capture heap dump
   jmap -dump:format=b,file=/tmp/heap-dump-$(date +%Y%m%d%H%M%S).hprof $PID
   ```

3. **Analyze Heap Dump**
   ```bash
   # Use tools like Eclipse Memory Analyzer (MAT) or VisualVM
   # Look for objects with high retention
   ```

4. **Monitor Memory Over Time**
   ```bash
   # Check for increasing memory usage
   watch -n 10 'curl -s http://localhost:8080/actuator/metrics/jvm.memory.used | jq'
   ```

5. **Check for Memory-Intensive Operations**
   ```bash
   # Review application logs
   grep -i "memory\|heap\|gc" /var/log/auth-app/application.log
   ```

6. **Take Action**
   ```bash
   # Restart the application if necessary
   sudo systemctl restart auth-app
   
   # Apply code fixes if identified
   # Adjust JVM memory settings if needed
   ```

### Connection Issues

**Purpose**: Diagnose and resolve connection issues with dependencies.

**Steps**:

1. **Check Database Connectivity**
   ```bash
   # Test PostgreSQL connection
   psql -h db-host -U postgres -d auth_db -c "SELECT 1"
   
   # Check connection pool
   curl -X GET http://localhost:8080/actuator/metrics/hikaricp.connections | jq
   ```

2. **Check Redis Connectivity**
   ```bash
   # Test Redis connection
   redis-cli -h redis-host ping
   ```

3. **Check Network Connectivity**
   ```bash
   # Test network connectivity
   ping db-host
   ping redis-host
   
   # Check for network issues
   traceroute db-host
   ```

4. **Check Firewall Rules**
   ```bash
   # Check if ports are open
   telnet db-host 5432
   telnet redis-host 6379
   
   # Check firewall rules
   sudo iptables -L
   ```

5. **Check Connection Timeouts**
   ```bash
   # Review application logs for timeout errors
   grep -i "timeout\|connection refused\|connection reset" /var/log/auth-app/application.log
   ```

6. **Take Action**
   ```bash
   # Restart the application if necessary
   sudo systemctl restart auth-app
   
   # Update connection settings if needed
   # Fix network or firewall issues
   ```

### Authentication Failures

**Purpose**: Diagnose and resolve authentication issues.

**Steps**:

1. **Check Authentication Logs**
   ```bash
   # Review authentication logs
   grep -i "authentication\|login\|token" /var/log/auth-app/application.log
   
   # Look for specific errors
   grep -i "invalid token\|expired token\|authentication failed" /var/log/auth-app/application.log
   ```

2. **Verify JWT Configuration**
   ```bash
   # Check JWT secret is set correctly
   grep -i "jwt" /opt/auth-app/.env
   
   # Check token expiration settings
   grep -i "expiration" /opt/auth-app/application.yml
   ```

3. **Test Authentication Endpoints**
   ```bash
   # Test login endpoint
   curl -X POST http://localhost:8080/api/v1/auth/login -d '{"username":"test","password":"password"}' -H "Content-Type: application/json"
   
   # Test with a token
   curl -X GET http://localhost:8080/api/v1/users/test -H "Authorization: Bearer <token>"
   ```

4. **Check User Accounts**
   ```bash
   # Connect to the database
   psql -h localhost -U postgres -d auth_db
   
   # Check user status
   SELECT user_name, account_non_locked, account_locked, active_ind FROM user_auth.users WHERE user_name = 'test';
   ```

5. **Check Rate Limiting**
   ```bash
   # Check if rate limiting is blocking requests
   grep -i "rate limit" /var/log/auth-app/application.log
   
   # Check Redis rate limit keys
   redis-cli -h redis-host keys "*rate-limit*"
   ```

6. **Take Action**
   ```bash
   # Reset rate limit if necessary
   redis-cli -h redis-host del "rate-limit:127.0.0.1"
   
   # Unlock account if necessary
   psql -h localhost -U postgres -d auth_db -c "UPDATE user_auth.users SET account_locked = false, account_non_locked = true WHERE user_name = 'test';"
   
   # Fix configuration issues
   ```

## Disaster Recovery

### Failover Procedure

**Purpose**: Switch to a backup system in case of primary system failure.

**Prerequisites**:

- Standby system configured
- Database replication set up
- Load balancer with failover capability

**Steps**:

1. **Verify Primary System Failure**
   ```bash
   # Check if primary system is unreachable
   ping primary-host
   
   # Check application status
   curl -I http://primary-host:8080/actuator/health
   ```

2. **Check Standby System Readiness**
   ```bash
   # Verify standby system is running
   curl -I http://standby-host:8080/actuator/health
   
   # Check database replication status
   psql -h standby-db -U postgres -c "SELECT pg_last_wal_receive_lsn(), pg_last_wal_replay_lsn(), pg_last_xact_replay_timestamp();"
   ```

3. **Perform Failover**
   ```bash
   # Update load balancer configuration
   sudo vi /etc/nginx/conf.d/auth-app.conf
   
   # Comment out primary server and uncomment standby
   # upstream auth-app {
   #   # server primary-host:8080;
   #   server standby-host:8080;
   # }
   
   # Reload load balancer
   sudo systemctl reload nginx
   ```

4. **Promote Standby Database (if needed)**
   ```bash
   # For PostgreSQL
   sudo -u postgres pg_ctl promote -D /var/lib/postgresql/data
   ```

5. **Verify Failover Success**
   ```bash
   # Check application health on standby
   curl -I http://load-balancer-ip/actuator/health
   
   # Test basic functionality
   curl -X POST http://load-balancer-ip/api/v1/auth/login -d '{"username":"test","password":"password"}' -H "Content-Type: application/json"
   ```

6. **Update DNS (if needed)**
   ```bash
   # Update DNS records to point to new active system
   # This depends on your DNS provider
   ```

7. **Notify Stakeholders**
   ```bash
   # Send notification about the failover
   echo "Failover completed at $(date)" | mail -s "Auth App Failover Notification" team@example.com
   ```

### Data Recovery

**Purpose**: Recover data in case of data corruption or loss.

**Prerequisites**:

- Regular database backups
- Point-in-time recovery capability

**Steps**:

1. **Assess Data Loss**
   ```bash
   # Identify the extent of data loss
   # Determine the last known good state
   ```

2. **Stop the Application**
   ```bash
   sudo systemctl stop auth-app
   ```

3. **Restore from Backup**
   ```bash
   # For full database restore
   pg_restore -h localhost -U postgres -d auth_db /backup/auth_db_20230804.dump
   
   # For point-in-time recovery (if available)
   # This depends on your PostgreSQL configuration
   ```

4. **Verify Data Integrity**
   ```bash
   # Connect to the database
   psql -h localhost -U postgres -d auth_db
   
   # Check table counts
   SELECT count(*) FROM user_auth.users;
   SELECT count(*) FROM user_auth.role;
   
   # Check recent data
   SELECT * FROM user_auth.users ORDER BY created_date DESC LIMIT 10;
   ```

5. **Start the Application**
   ```bash
   sudo systemctl start auth-app
   ```

6. **Verify Application Functionality**
   ```bash
   # Check application health
   curl -I http://localhost:8080/actuator/health
   
   # Test basic functionality
   curl -X POST http://localhost:8080/api/v1/auth/login -d '{"username":"test","password":"password"}' -H "Content-Type: application/json"
   ```

7. **Document the Recovery Process**
   ```bash
   # Document what happened, what was restored, and any data that couldn't be recovered
   echo "# Data Recovery Report - $(date +%Y-%m-%d)" > /tmp/recovery-report.md
   # Add details about the recovery process
   ```