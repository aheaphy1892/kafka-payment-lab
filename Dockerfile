# syntax=docker/dockerfile:1

# --- Build Stage ---
FROM gradle:9.2.1-jdk21 AS build

# Parameterize which Gradle subproject to build
ARG APP_MODULE=payment-api

WORKDIR /workspace
RUN chown -R gradle:gradle /workspace
USER gradle

# 1. Copy root Gradle build files & wrapper first
COPY --chown=gradle:gradle settings.gradle* build.gradle* gradle.properties* ./
COPY --chown=gradle:gradle gradle/ ./gradle/

# 2. Copy shared internal libraries
# Note: Using a wildcard won't fail the build if the folder doesn't exist yet
COPY --chown=gradle:gradle contracts*/ ./contracts/

# 3. Copy ONLY the target application module (isolates Docker layer cache per app!)
COPY --chown=gradle:gradle ${APP_MODULE}/ ./${APP_MODULE}/

# Cache Gradle dependencies across builds using BuildKit cache mount
RUN --mount=type=cache,target=/home/gradle/.gradle/caches,uid=1000,gid=1000 \
    --mount=type=cache,target=/home/gradle/.gradle/wrapper,uid=1000,gid=1000 \
    gradle --no-daemon :${APP_MODULE}:bootJar && \
    java -Djarmode=tools -jar /workspace/${APP_MODULE}/build/libs/${APP_MODULE}.jar extract --layers --launcher --destination /workspace/extracted

# --- Production Stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S appuser && adduser -S appuser -G appuser
USER appuser:appuser

# Copy extracted Spring Boot layers
COPY --from=build --chown=appuser:appuser /workspace/extracted/dependencies/ ./
COPY --from=build --chown=appuser:appuser /workspace/extracted/spring-boot-loader/ ./
COPY --from=build --chown=appuser:appuser /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=appuser:appuser /workspace/extracted/application/ ./

EXPOSE 8080

# Container-aware JVM memory settings
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]