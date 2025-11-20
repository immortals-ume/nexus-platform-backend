FROM maven:3.9-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests -Dmaven.repo.local=/app/.m2/repository

FROM openjdk:17-jre-slim
LABEL authors="immortals-ume" \
      version="1.0.5" \
      description="Auth Application" \
      maintainer="immortals-ume"

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    curl \
    ca-certificates \
    tzdata && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

RUN groupadd -g 1001 appgroup && \
    useradd -r -u 1001 -g appgroup -m -d /app -s /bin/bash appuser

COPY --from=build /app/target/*.jar auth-app.jar

RUN chown -R appuser:appgroup /app

ENV TZ=UTC \
    JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport" \
    SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080 \
    LOGGING_LEVEL_ROOT=INFO \
    LOGGING_PATTERN_CONSOLE="%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar auth-app-1.0.5.jar"]
