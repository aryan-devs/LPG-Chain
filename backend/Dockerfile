# ==============================================================================
# Stage 1: Build the Spring Boot application using Maven and OpenJDK 17
# Build context expected at repository root: docker build -f backend/Dockerfile .
# ==============================================================================
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Cache Maven dependencies by copying pom.xml first
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy backend source code and package executable JAR
COPY backend/src ./src
RUN mvn clean package -DskipTests -B && \
    cp target/lpg-chain-backend-0.0.1-SNAPSHOT.jar app.jar

# ==============================================================================
# Stage 2: Minimal, secure JRE 17 runtime container
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Run as a dedicated non-root user for security best practices
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the packaged executable JAR from builder stage
COPY --from=builder --chown=appuser:appgroup /app/app.jar app.jar

# Ensure uploads directory exists and is writable by the non-root application user
RUN mkdir -p uploads/documents && chown -R appuser:appgroup uploads

USER appuser

# Port configuration: listens on PORT env var (e.g. Render / Cloud Run), defaulting to 8080
ENV PORT=8080
EXPOSE 8080

# Launch application with dynamic port mapping and container-friendly JVM options
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -Djava.security.egd=file:/dev/./urandom -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -jar app.jar"]
