#!/bin/bash

VAULT_CONTAINER_NAME="spring_boot_vault_container"
VAULT_PATH="secret/auth-app"
VAULT_ADDR="http://127.0.0.1:8200"

echo "🔐 Writing application.yml secrets to Vault at path: $VAULT_PATH"

docker exec -e VAULT_ADDR=$VAULT_ADDR -e VAULT_TOKEN=root $VAULT_CONTAINER_NAME vault kv put $VAULT_PATH \
  spring.liquibase.default-schema=public \
  spring.liquibase.liquibase-schema=public \
  spring.liquibase.change-log=db/changelog-master.xml \
  spring.liquibase.enabled=false \
  spring.liquibase.url=jdbc:postgresql://localhost:5432/user_db \
  spring.liquibase.user=user \
  spring.liquibase.password=admin \
  spring.liquibase.driver-class-name=org.postgresql.Driver \
  spring.jpa.show-sql=true \
  spring.jpa.properties.hibernate.format_sql=true \
  spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect \
  spring.jpa.properties.hibernate.envers.audit_table_suffix=_AUD \
  spring.jpa.properties.hibernate.envers.revision_field_name=rev \
  spring.jpa.properties.hibernate.envers.revision_type_field_name=rev_type \
  spring.jpa.properties.hibernate.envers.store_data_at_delete=true \
  spring.jpa.properties.hibernate.envers.default_schema=user_audit \
  datasource.write.url=jdbc:postgresql://localhost:5432/user_db \
  datasource.write.username=user \
  datasource.write.password=admin \
  datasource.write.driver-class-name=org.postgresql.Driver \
  datasource.read.url=jdbc:postgresql://localhost:5432/user_db \
  datasource.read.username=replicator \
  datasource.read.password=test \
  datasource.read.driver-class-name=org.postgresql.Driver \
  auth.jwt-issuer=https://your-issuer.example.com \
  auth.access-token-expiry-ms=900000 \
  auth.refresh-token-expiry-ms=604800000 \
  jwt-private-key="$(< /Users/ks/IdeaProjects/microservices/auth-app/src/main/resources/jwt-keys/private_key.pem)" \
  jwt-public-key="$(< /Users/ks/IdeaProjects/microservices/auth-app/src/main/resources/jwt-keys/public_key.pem)" \
  cache.redis.host=localhost \
  cache.redis.port=6379 \
  cache.redis.database=0 \
  cache.redis.command-timeout=2s \
  cache.redis.use-ssl=false \
  cache.redis.pool-max-total=16 \
  cache.redis.pool-max-idle=16 \
  cache.redis.pool-min-idle=4 \
  cache.redis.pool-max-wait=1s \
  cache.redis.auto-reconnect=true \
  cache.redis.enabled=true
