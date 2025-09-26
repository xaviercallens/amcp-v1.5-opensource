FROM openjdk:8-jre-alpine

LABEL maintainer="AMCP Development Team <team@amcp.io>"
LABEL version="1.4.0"
LABEL description="AMCP v1.4 Agent Mesh Communication Protocol"

# Create application user
RUN addgroup -g 1000 amcp && \
    adduser -D -u 1000 -G amcp amcp

# Install required packages
RUN apk add --no-cache \
    curl \
    bash \
    tini

# Set working directory
WORKDIR /app

# Copy application JAR
COPY core/target/amcp-core-1.4.0.jar app.jar

# Copy configuration files
COPY deploy/docker/entrypoint.sh entrypoint.sh
RUN chmod +x entrypoint.sh

# Create necessary directories
RUN mkdir -p /app/config /app/secrets /app/logs /tmp && \
    chown -R amcp:amcp /app /tmp

# Switch to application user
USER amcp

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# Expose ports
EXPOSE 8080 8081 8082

# Use tini as init system
ENTRYPOINT ["/sbin/tini", "--"]
CMD ["./entrypoint.sh"]