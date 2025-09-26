#!/bin/bash

set -e

# Set default values
JAVA_OPTS="${JAVA_OPTS:--Xmx2g -Xms1g -XX:+UseG1GC -XX:+UseStringDeduplication}"
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-docker}"

# Configure Java options
export JAVA_OPTS="$JAVA_OPTS -Djava.security.egd=file:/dev/./urandom"
export JAVA_OPTS="$JAVA_OPTS -Djava.awt.headless=true"
export JAVA_OPTS="$JAVA_OPTS -Dfile.encoding=UTF-8"
export JAVA_OPTS="$JAVA_OPTS -Duser.timezone=UTC"

# Configure Spring Boot
export SPRING_PROFILES_ACTIVE="$SPRING_PROFILES_ACTIVE"

# Configure logging
export LOGGING_CONFIG="/app/config/logback-spring.xml"

# Configure AMCP
export AMCP_CONTEXT_ID="${AMCP_CONTEXT_ID:-$(hostname)}"
export AMCP_EVENT_BROKER_TYPE="${AMCP_EVENT_BROKER_TYPE:-memory}"

# Wait for dependencies (if configured)
if [ ! -z "$WAIT_FOR_SERVICES" ]; then
    echo "Waiting for services: $WAIT_FOR_SERVICES"
    for service in $WAIT_FOR_SERVICES; do
        echo "Waiting for $service..."
        while ! curl -f "$service/health" > /dev/null 2>&1; do
            echo "Service $service not ready, waiting..."
            sleep 5
        done
        echo "Service $service is ready!"
    done
fi

# Print configuration
echo "Starting AMCP Agent Context v1.4.0"
echo "Context ID: $AMCP_CONTEXT_ID"
echo "Event Broker: $AMCP_EVENT_BROKER_TYPE"
echo "Spring Profiles: $SPRING_PROFILES_ACTIVE"
echo "Java Options: $JAVA_OPTS"

# Start the application
exec java $JAVA_OPTS -jar app.jar