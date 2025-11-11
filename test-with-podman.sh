#!/bin/bash

# AMCP v1.6 - Podman Testing Script
# Starts Kafka with Podman and runs comprehensive tests

set -e

echo "=================================================="
echo "AMCP v1.6 - Podman Testing"
echo "=================================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="http://localhost:8080"
HEALTH_LIVE_URL="$BASE_URL/q/health/live"
HEALTH_READY_URL="$BASE_URL/q/health/ready"
METRICS_URL="$BASE_URL/q/metrics"
A2A_STATUS_URL="$BASE_URL/a2a/status"
A2A_MESSAGE_URL="$BASE_URL/a2a/message"

# Test counters
TESTS_PASSED=0
TESTS_FAILED=0

# Helper function to print test result
test_result() {
    local test_name=$1
    local result=$2
    
    if [ "$result" -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $test_name"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}✗${NC} $test_name"
        ((TESTS_FAILED++))
    fi
}

# Helper function to check if service is running
check_service() {
    local url=$1
    local max_attempts=60
    local attempt=1
    
    echo "Checking if service is running..."
    while [ $attempt -le $max_attempts ]; do
        if curl -s "$url" > /dev/null 2>&1; then
            echo -e "${GREEN}✓ Service is running${NC}"
            return 0
        fi
        echo "Attempt $attempt/$max_attempts: Waiting for service..."
        sleep 1
        ((attempt++))
    done
    
    echo -e "${RED}✗ Service did not start within ${max_attempts} seconds${NC}"
    return 1
}

# Step 1: Check Podman
echo ""
echo -e "${BLUE}Step 1: Checking Podman${NC}"
echo "---"
if ! command -v podman &> /dev/null; then
    echo -e "${RED}✗ Podman not found${NC}"
    exit 1
fi
PODMAN_VERSION=$(podman --version)
echo -e "${GREEN}✓${NC} $PODMAN_VERSION"

# Step 2: Clean up existing containers
echo ""
echo -e "${BLUE}Step 2: Cleaning up existing containers${NC}"
echo "---"
podman stop amcp-zookeeper amcp-kafka 2>/dev/null || true
podman rm amcp-zookeeper amcp-kafka 2>/dev/null || true
echo -e "${GREEN}✓ Cleanup complete${NC}"

# Step 3: Start Zookeeper
echo ""
echo -e "${BLUE}Step 3: Starting Zookeeper with Podman${NC}"
echo "---"
podman run -d \
  --name amcp-zookeeper \
  -e ZOOKEEPER_CLIENT_PORT=2181 \
  -e ZOOKEEPER_TICK_TIME=2000 \
  -p 2181:2181 \
  docker.io/confluentinc/cp-zookeeper:7.5.0 > /dev/null 2>&1 || {
    echo -e "${RED}✗ Failed to start Zookeeper${NC}"
    exit 1
}
echo -e "${GREEN}✓ Zookeeper started${NC}"
sleep 5

# Step 4: Start Kafka
echo ""
echo -e "${BLUE}Step 4: Starting Kafka with Podman${NC}"
echo "---"
podman run -d \
  --name amcp-kafka \
  -e KAFKA_BROKER_ID=1 \
  -e KAFKA_ZOOKEEPER_CONNECT=localhost:2181 \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  -e KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1 \
  -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 \
  -e KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS=0 \
  -e KAFKA_AUTO_CREATE_TOPICS_ENABLE=true \
  -p 9092:9092 \
  docker.io/confluentinc/cp-kafka:7.5.0 > /dev/null 2>&1 || {
    echo -e "${RED}✗ Failed to start Kafka${NC}"
    podman logs amcp-kafka
    exit 1
}
echo -e "${GREEN}✓ Kafka started${NC}"
sleep 10

# Step 5: Verify Kafka is running
echo ""
echo -e "${BLUE}Step 5: Verifying Kafka is running${NC}"
echo "---"
if podman ps | grep -q amcp-kafka; then
    echo -e "${GREEN}✓ Kafka container is running${NC}"
else
    echo -e "${RED}✗ Kafka container is not running${NC}"
    podman logs amcp-kafka
    exit 1
fi

# Step 6: Check if AMCP application is running
echo ""
echo -e "${BLUE}Step 6: Checking if AMCP application is running${NC}"
echo "---"
check_service "$HEALTH_LIVE_URL" || {
    echo -e "${RED}✗ AMCP application is not running${NC}"
    echo ""
    echo "Please start AMCP in another terminal:"
    echo "  cd amcp-examples"
    echo "  mvn quarkus:dev"
    echo ""
    exit 1
}

# Step 7: Run Phase 1 Tests
echo ""
echo "=================================================="
echo "PHASE 1: MicroProfile Health & Metrics Tests"
echo "=================================================="
echo ""

echo "Test 1: Liveness Check (/q/health/live)"
echo "---"
RESPONSE=$(curl -s "$HEALTH_LIVE_URL")
echo "Response: $RESPONSE"
if echo "$RESPONSE" | grep -q "amcp-agent-mesh"; then
    test_result "Liveness check returns amcp-agent-mesh" 0
else
    test_result "Liveness check returns amcp-agent-mesh" 1
fi

if echo "$RESPONSE" | grep -q "context_id\|instance_id"; then
    test_result "Liveness check includes context_id" 0
else
    test_result "Liveness check includes context_id" 1
fi

if echo "$RESPONSE" | grep -q "agents_active"; then
    test_result "Liveness check includes agents_active" 0
else
    test_result "Liveness check includes agents_active" 1
fi

echo ""
echo "Test 2: Readiness Check (/q/health/ready)"
echo "---"
RESPONSE=$(curl -s "$HEALTH_READY_URL")
echo "Response: $RESPONSE"
if echo "$RESPONSE" | grep -q "amcp-agent-mesh-ready"; then
    test_result "Readiness check returns amcp-agent-mesh-ready" 0
else
    test_result "Readiness check returns amcp-agent-mesh-ready" 1
fi

if echo "$RESPONSE" | grep -q "agents_registered"; then
    test_result "Readiness check includes agents_registered" 0
else
    test_result "Readiness check includes agents_registered" 1
fi

if echo "$RESPONSE" | grep -q "ready_for_traffic"; then
    test_result "Readiness check includes ready_for_traffic" 0
else
    test_result "Readiness check includes ready_for_traffic" 1
fi

echo ""
echo "Test 3: Prometheus Metrics (/q/metrics)"
echo "---"
RESPONSE=$(curl -s "$METRICS_URL")
if echo "$RESPONSE" | grep -q "amcp_agents_total"; then
    test_result "Metrics includes amcp_agents_total" 0
else
    test_result "Metrics includes amcp_agents_total" 1
fi

if echo "$RESPONSE" | grep -q "amcp_broker_connected"; then
    test_result "Metrics includes amcp_broker_connected" 0
else
    test_result "Metrics includes amcp_broker_connected" 1
fi

if echo "$RESPONSE" | grep -q "amcp_mesh_running"; then
    test_result "Metrics includes amcp_mesh_running" 0
else
    test_result "Metrics includes amcp_mesh_running" 1
fi

# Step 8: Run Phase 2 Tests
echo ""
echo "=================================================="
echo "PHASE 2: A2A Gateway Tests"
echo "=================================================="
echo ""

echo "Test 4: A2A Gateway Status (/a2a/status)"
echo "---"
RESPONSE=$(curl -s "$A2A_STATUS_URL")
echo "Response: $RESPONSE"
if echo "$RESPONSE" | grep -q "A2A Protocol Bridge"; then
    test_result "A2A status returns service name" 0
else
    test_result "A2A status returns service name" 1
fi

if echo "$RESPONSE" | grep -q "version"; then
    test_result "A2A status includes version" 0
else
    test_result "A2A status includes version" 1
fi

if echo "$RESPONSE" | grep -q "status"; then
    test_result "A2A status includes status field" 0
else
    test_result "A2A status includes status field" 1
fi

echo ""
echo "Test 5: A2A Message Reception (/a2a/message)"
echo "---"
MESSAGE_PAYLOAD='{
  "id": "test-msg-001",
  "sender": "external-agent",
  "receiver": "amcp://weather",
  "performative": "REQUEST",
  "content": {
    "action": "get_weather",
    "city": "Paris"
  },
  "contentType": "application/json"
}'

RESPONSE=$(curl -s -X POST "$A2A_MESSAGE_URL" \
  -H "Content-Type: application/json" \
  -d "$MESSAGE_PAYLOAD")
echo "Response: $RESPONSE"

if echo "$RESPONSE" | grep -q "accepted\|sent"; then
    test_result "A2A message accepted" 0
else
    test_result "A2A message accepted" 1
fi

if echo "$RESPONSE" | grep -q "test-msg-001"; then
    test_result "A2A message ID returned" 0
else
    test_result "A2A message ID returned" 1
fi

echo ""
echo "Test 6: A2A Conversations (/a2a/conversations)"
echo "---"
RESPONSE=$(curl -s "$BASE_URL/a2a/conversations")
echo "Response: $RESPONSE"
if [ ! -z "$RESPONSE" ]; then
    test_result "A2A conversations endpoint responds" 0
else
    test_result "A2A conversations endpoint responds" 1
fi

# Step 9: Summary
echo ""
echo "=================================================="
echo "Test Summary"
echo "=================================================="
echo -e "Passed: ${GREEN}$TESTS_PASSED${NC}"
echo -e "Failed: ${RED}$TESTS_FAILED${NC}"
echo "Total:  $((TESTS_PASSED + TESTS_FAILED))"
echo ""

# Step 10: Cleanup
echo "=================================================="
echo "Cleanup"
echo "=================================================="
echo ""
read -p "Do you want to stop Kafka containers? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Stopping containers..."
    podman stop amcp-kafka amcp-zookeeper
    podman rm amcp-kafka amcp-zookeeper
    echo -e "${GREEN}✓ Containers stopped and removed${NC}"
else
    echo -e "${YELLOW}ℹ Containers are still running${NC}"
    echo "To stop them later, run:"
    echo "  podman stop amcp-kafka amcp-zookeeper"
    echo "  podman rm amcp-kafka amcp-zookeeper"
fi

echo ""
if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}✗ Some tests failed${NC}"
    exit 1
fi
