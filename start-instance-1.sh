#!/bin/bash

echo "Starting AMCP Instance 1 (Port 8080) with Kafka..."
cd /home/kalxav/CascadeProjects/amcp-v1.6-opensource/amcp-examples

export AMCP_BROKER_TYPE=kafka
export AMCP_INSTANCE_ID=instance-1

mvn quarkus:dev -Dquarkus.http.port=8080
