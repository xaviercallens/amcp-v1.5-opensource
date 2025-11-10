# Multi-stage build for AMCP v1.6 with Quarkus
# Stage 1: Build with Maven
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy project files
COPY . .

# Build the application
RUN mvn clean package -DskipTests -q

# Stage 2: Runtime with Quarkus
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy built application from builder
COPY --from=builder /build/amcp-examples/target/quarkus-app/lib/ ./lib/
COPY --from=builder /build/amcp-examples/target/quarkus-app/*.jar ./
COPY --from=builder /build/amcp-examples/target/quarkus-app/app/ ./app/
COPY --from=builder /build/amcp-examples/target/quarkus-app/quarkus/ ./quarkus/

# Set environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV QUARKUS_HTTP_PORT=8080
ENV AMCP_BROKER_TYPE=kafka

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/q/health || exit 1

# Expose port
EXPOSE 8080

# Run application
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
