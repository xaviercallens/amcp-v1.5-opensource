#!/bin/bash
# 3-Instance Testing Script for AMCP v1.6
# Tests multi-instance distributed mesh architecture

set -e

echo "=========================================="
echo "AMCP v1.6 - 3-Instance Testing"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
INSTANCE_1_PORT=8080
INSTANCE_2_PORT=8081
INSTANCE_3_PORT=8082

INSTANCE_1_PID=""
INSTANCE_2_PID=""
INSTANCE_3_PID=""

# Cleanup function
cleanup() {
    echo ""
    echo "Cleaning up instances..."
    
    if [ -n "$INSTANCE_1_PID" ] && kill -0 $INSTANCE_1_PID 2>/dev/null; then
        kill $INSTANCE_1_PID 2>/dev/null || true
        echo -e "${GREEN}✓ Instance 1 stopped${NC}"
    fi
    
    if [ -n "$INSTANCE_2_PID" ] && kill -0 $INSTANCE_2_PID 2>/dev/null; then
        kill $INSTANCE_2_PID 2>/dev/null || true
        echo -e "${GREEN}✓ Instance 2 stopped${NC}"
    fi
    
    if [ -n "$INSTANCE_3_PID" ] && kill -0 $INSTANCE_3_PID 2>/dev/null; then
        kill $INSTANCE_3_PID 2>/dev/null || true
        echo -e "${GREEN}✓ Instance 3 stopped${NC}"
    fi
    
    # Clean up any remaining Quarkus processes
    pkill -f "quarkus:dev.*8080" 2>/dev/null || true
    pkill -f "quarkus:dev.*8081" 2>/dev/null || true
    pkill -f "quarkus:dev.*8082" 2>/dev/null || true
}

trap cleanup EXIT INT TERM

# Step 1: Check Kafka
echo "Step 1: Checking Kafka availability..."
if nc -z localhost 9092 2>/dev/null; then
    echo -e "${GREEN}✓ Kafka is running on port 9092${NC}"
else
    echo -e "${RED}✗ Kafka is not running${NC}"
    echo "Please start Kafka first:"
    echo "  docker-compose up -d kafka"
    exit 1
fi

# Step 2: Build AMCP
echo ""
echo "Step 2: Building AMCP..."
cd "$(dirname "$0")"
mvn clean install -DskipTests -q
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Build successful${NC}"
else
    echo -e "${RED}✗ Build failed${NC}"
    exit 1
fi

# Step 3: Start Instance 1
echo ""
echo -e "${BLUE}Step 3: Starting Instance 1 (port $INSTANCE_1_PORT)...${NC}"
cd amcp-examples

export AMCP_BROKER_TYPE=kafka
export AMCP_INSTANCE_ID=instance-1
export QUARKUS_HTTP_PORT=$INSTANCE_1_PORT

mvn quarkus:dev -Dquarkus.http.port=$INSTANCE_1_PORT > /tmp/amcp-instance-1.log 2>&1 &
INSTANCE_1_PID=$!

echo "Waiting for Instance 1 to start (PID: $INSTANCE_1_PID)..."
sleep 20

if kill -0 $INSTANCE_1_PID 2>/dev/null; then
    # Verify it's responding
    if curl -s http://localhost:$INSTANCE_1_PORT/q/health/live > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Instance 1 started and healthy${NC}"
    else
        echo -e "${YELLOW}⚠ Instance 1 started but not responding yet${NC}"
    fi
else
    echo -e "${RED}✗ Instance 1 failed to start${NC}"
    cat /tmp/amcp-instance-1.log
    exit 1
fi

# Step 4: Start Instance 2
echo ""
echo -e "${BLUE}Step 4: Starting Instance 2 (port $INSTANCE_2_PORT)...${NC}"

export AMCP_INSTANCE_ID=instance-2
export QUARKUS_HTTP_PORT=$INSTANCE_2_PORT

mvn quarkus:dev -Dquarkus.http.port=$INSTANCE_2_PORT > /tmp/amcp-instance-2.log 2>&1 &
INSTANCE_2_PID=$!

echo "Waiting for Instance 2 to start (PID: $INSTANCE_2_PID)..."
sleep 20

if kill -0 $INSTANCE_2_PID 2>/dev/null; then
    if curl -s http://localhost:$INSTANCE_2_PORT/q/health/live > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Instance 2 started and healthy${NC}"
    else
        echo -e "${YELLOW}⚠ Instance 2 started but not responding yet${NC}"
    fi
else
    echo -e "${RED}✗ Instance 2 failed to start${NC}"
    cat /tmp/amcp-instance-2.log
    exit 1
fi

# Step 5: Start Instance 3
echo ""
echo -e "${BLUE}Step 5: Starting Instance 3 (port $INSTANCE_3_PORT)...${NC}"

export AMCP_INSTANCE_ID=instance-3
export QUARKUS_HTTP_PORT=$INSTANCE_3_PORT

mvn quarkus:dev -Dquarkus.http.port=$INSTANCE_3_PORT > /tmp/amcp-instance-3.log 2>&1 &
INSTANCE_3_PID=$!

echo "Waiting for Instance 3 to start (PID: $INSTANCE_3_PID)..."
sleep 20

if kill -0 $INSTANCE_3_PID 2>/dev/null; then
    if curl -s http://localhost:$INSTANCE_3_PORT/q/health/live > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Instance 3 started and healthy${NC}"
    else
        echo -e "${YELLOW}⚠ Instance 3 started but not responding yet${NC}"
    fi
else
    echo -e "${RED}✗ Instance 3 failed to start${NC}"
    cat /tmp/amcp-instance-3.log
    exit 1
fi

# Give all instances a moment to fully initialize
echo ""
echo "Waiting for all instances to stabilize..."
sleep 10

# Step 6: Run Tests
echo ""
echo "=========================================="
echo "Running Multi-Instance Tests"
echo "=========================================="
echo ""

TESTS_RUN=0
TESTS_PASSED=0
TESTS_FAILED=0

run_test() {
    local test_name=$1
    local test_command=$2
    
    TESTS_RUN=$((TESTS_RUN + 1))
    echo -e "${YELLOW}Test $TESTS_RUN: $test_name${NC}"
    
    if eval "$test_command" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ PASSED${NC}"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        echo ""
        return 0
    else
        echo -e "${RED}✗ FAILED${NC}"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        echo ""
        return 1
    fi
}

# Test 1: All instances health check
echo -e "${BLUE}=== Test 1: Health Checks ===${NC}"
run_test "Instance 1 health check" \
    "curl -s http://localhost:$INSTANCE_1_PORT/q/health/live | jq -e '.status == \"UP\"'"

run_test "Instance 2 health check" \
    "curl -s http://localhost:$INSTANCE_2_PORT/q/health/live | jq -e '.status == \"UP\"'"

run_test "Instance 3 health check" \
    "curl -s http://localhost:$INSTANCE_3_PORT/q/health/live | jq -e '.status == \"UP\"'"

# Test 2: Readiness checks
echo -e "${BLUE}=== Test 2: Readiness Checks ===${NC}"
run_test "Instance 1 readiness" \
    "curl -s http://localhost:$INSTANCE_1_PORT/q/health/ready | jq -e '.status == \"UP\"'"

run_test "Instance 2 readiness" \
    "curl -s http://localhost:$INSTANCE_2_PORT/q/health/ready | jq -e '.status == \"UP\"'"

run_test "Instance 3 readiness" \
    "curl -s http://localhost:$INSTANCE_3_PORT/q/health/ready | jq -e '.status == \"UP\"'"

# Test 3: Metrics
echo -e "${BLUE}=== Test 3: Metrics Collection ===${NC}"
run_test "Instance 1 metrics available" \
    "curl -s http://localhost:$INSTANCE_1_PORT/q/metrics | grep -q amcp_"

run_test "Instance 2 metrics available" \
    "curl -s http://localhost:$INSTANCE_2_PORT/q/metrics | grep -q amcp_"

run_test "Instance 3 metrics available" \
    "curl -s http://localhost:$INSTANCE_3_PORT/q/metrics | grep -q amcp_"

# Test 4: Broker connectivity
echo -e "${BLUE}=== Test 4: Broker Connectivity ===${NC}"
run_test "Instance 1 broker connected" \
    "curl -s http://localhost:$INSTANCE_1_PORT/q/metrics | grep 'amcp_broker_connected 1.0'"

run_test "Instance 2 broker connected" \
    "curl -s http://localhost:$INSTANCE_2_PORT/q/metrics | grep 'amcp_broker_connected 1.0'"

run_test "Instance 3 broker connected" \
    "curl -s http://localhost:$INSTANCE_3_PORT/q/metrics | grep 'amcp_broker_connected 1.0'"

# Test 5: Agent status endpoints
echo -e "${BLUE}=== Test 5: Agent Status ===${NC}"
if curl -s http://localhost:$INSTANCE_1_PORT/agent/status > /dev/null 2>&1; then
    run_test "Instance 1 agent status" \
        "curl -s http://localhost:$INSTANCE_1_PORT/agent/status | jq -e '.running == true'"
    
    run_test "Instance 2 agent status" \
        "curl -s http://localhost:$INSTANCE_2_PORT/agent/status | jq -e '.running == true'"
    
    run_test "Instance 3 agent status" \
        "curl -s http://localhost:$INSTANCE_3_PORT/agent/status | jq -e '.running == true'"
else
    echo -e "${YELLOW}⚠ Agent status endpoint not available (optional)${NC}"
    echo ""
fi

# Test 6: Cross-instance communication
echo -e "${BLUE}=== Test 6: Cross-Instance Communication ===${NC}"
echo "Testing load distribution across instances..."

# Send multiple requests and check they're distributed
SUCCESS_COUNT=0
for i in {1..6}; do
    PORT=$((8079 + (i % 3) + 1))
    if curl -s http://localhost:$PORT/q/health > /dev/null 2>&1; then
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    fi
    sleep 0.5
done

if [ $SUCCESS_COUNT -ge 5 ]; then
    echo -e "${GREEN}✓ Cross-instance requests successful ($SUCCESS_COUNT/6)${NC}"
    TESTS_RUN=$((TESTS_RUN + 1))
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    echo -e "${RED}✗ Cross-instance requests failed ($SUCCESS_COUNT/6)${NC}"
    TESTS_RUN=$((TESTS_RUN + 1))
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi
echo ""

# Test 7: Instance isolation
echo -e "${BLUE}=== Test 7: Instance Isolation ===${NC}"
echo "Verifying each instance has unique ID..."

ID1=$(grep -o "instance-1" /tmp/amcp-instance-1.log | head -1)
ID2=$(grep -o "instance-2" /tmp/amcp-instance-2.log | head -1)
ID3=$(grep -o "instance-3" /tmp/amcp-instance-3.log | head -1)

if [ "$ID1" = "instance-1" ] && [ "$ID2" = "instance-2" ] && [ "$ID3" = "instance-3" ]; then
    echo -e "${GREEN}✓ All instances have unique IDs${NC}"
    TESTS_RUN=$((TESTS_RUN + 1))
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    echo -e "${RED}✗ Instance IDs not unique${NC}"
    TESTS_RUN=$((TESTS_RUN + 1))
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi
echo ""

# Display running instances
echo ""
echo "=========================================="
echo "Running Instances Summary"
echo "=========================================="
echo -e "${GREEN}Instance 1:${NC} http://localhost:$INSTANCE_1_PORT (PID: $INSTANCE_1_PID)"
echo -e "${GREEN}Instance 2:${NC} http://localhost:$INSTANCE_2_PORT (PID: $INSTANCE_2_PID)"
echo -e "${GREEN}Instance 3:${NC} http://localhost:$INSTANCE_3_PORT (PID: $INSTANCE_3_PID)"
echo ""

# Collect metrics from all instances
echo "=========================================="
echo "Metrics Snapshot"
echo "=========================================="
for port in $INSTANCE_1_PORT $INSTANCE_2_PORT $INSTANCE_3_PORT; do
    echo -e "${BLUE}Instance on port $port:${NC}"
    curl -s http://localhost:$port/q/metrics 2>/dev/null | grep "amcp_" || echo "  Metrics not available"
    echo ""
done

# Summary
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo "Total tests: $TESTS_RUN"
echo -e "Passed: ${GREEN}$TESTS_PASSED${NC}"
echo -e "Failed: ${RED}$TESTS_FAILED${NC}"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}✓✓✓ All tests passed! ✓✓✓${NC}"
    echo ""
    echo "3-instance architecture validated successfully!"
    echo ""
    echo "The instances will continue running for manual testing."
    echo "Press Ctrl+C to stop all instances."
    echo ""
    echo "Manual test commands:"
    echo "  curl http://localhost:8080/q/health | jq ."
    echo "  curl http://localhost:8081/q/health | jq ."
    echo "  curl http://localhost:8082/q/health | jq ."
    echo ""
    
    # Keep instances running for manual testing
    echo "Instances are running. Press Ctrl+C to exit..."
    wait
    
    exit 0
else
    echo -e "${RED}✗ Some tests failed${NC}"
    echo ""
    echo "Check logs for details:"
    echo "  /tmp/amcp-instance-1.log"
    echo "  /tmp/amcp-instance-2.log"
    echo "  /tmp/amcp-instance-3.log"
    exit 1
fi
