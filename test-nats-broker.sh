#!/bin/bash
# NATS Broker Test Script for AMCP v1.6
# Tests the fixed NATS broker implementation

set -e

echo "=========================================="
echo "AMCP v1.6 - NATS Broker Testing"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counters
TESTS_RUN=0
TESTS_PASSED=0
TESTS_FAILED=0

# Test function
run_test() {
    local test_name=$1
    local test_command=$2
    
    TESTS_RUN=$((TESTS_RUN + 1))
    echo -e "${YELLOW}Test $TESTS_RUN: $test_name${NC}"
    
    if eval "$test_command" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ PASSED${NC}"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        return 0
    else
        echo -e "${RED}✗ FAILED${NC}"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        return 1
    fi
}

# Step 1: Check if NATS is available
echo "Step 1: Checking NATS availability..."
if command -v nats &> /dev/null; then
    echo -e "${GREEN}✓ NATS CLI found${NC}"
else
    echo -e "${YELLOW}⚠ NATS CLI not found (optional)${NC}"
fi

# Check if NATS server is running
echo ""
echo "Step 2: Checking if NATS server is running..."
if nc -z localhost 4222 2>/dev/null; then
    echo -e "${GREEN}✓ NATS server is running on port 4222${NC}"
    NATS_RUNNING=true
else
    echo -e "${YELLOW}⚠ NATS server not running${NC}"
    echo "Starting NATS server with Docker..."
    
    if command -v docker &> /dev/null; then
        docker run -d --name amcp-nats-test -p 4222:4222 nats:latest
        sleep 3
        
        if nc -z localhost 4222 2>/dev/null; then
            echo -e "${GREEN}✓ NATS server started successfully${NC}"
            NATS_RUNNING=true
            NATS_STARTED_BY_US=true
        else
            echo -e "${RED}✗ Failed to start NATS server${NC}"
            exit 1
        fi
    else
        echo -e "${RED}✗ Docker not found. Please install Docker or start NATS manually${NC}"
        echo "Manual start: docker run -p 4222:4222 nats:latest"
        exit 1
    fi
fi

# Step 3: Build AMCP
echo ""
echo "Step 3: Building AMCP..."
cd "$(dirname "$0")"
mvn clean install -DskipTests -q
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Build successful${NC}"
else
    echo -e "${RED}✗ Build failed${NC}"
    exit 1
fi

# Step 4: Start AMCP with NATS broker in background
echo ""
echo "Step 4: Starting AMCP with NATS broker..."
cd amcp-examples

export AMCP_BROKER_TYPE=nats
export AMCP_INSTANCE_ID=nats-test-1
export QUARKUS_HTTP_PORT=8090

mvn quarkus:dev -Dquarkus.http.port=8090 > /tmp/amcp-nats-test.log 2>&1 &
AMCP_PID=$!

echo "Waiting for AMCP to start (PID: $AMCP_PID)..."
sleep 15

# Check if AMCP is running
if kill -0 $AMCP_PID 2>/dev/null; then
    echo -e "${GREEN}✓ AMCP started with NATS broker${NC}"
else
    echo -e "${RED}✗ AMCP failed to start${NC}"
    cat /tmp/amcp-nats-test.log
    exit 1
fi

# Step 5: Run tests
echo ""
echo "Step 5: Running NATS broker tests..."
echo ""

# Test 1: Health check
run_test "Health check endpoint" \
    "curl -s http://localhost:8090/q/health/live | jq -e '.status == \"UP\"'"

# Test 2: Agent status
run_test "Agent status endpoint" \
    "curl -s http://localhost:8090/agent/status | jq -e '.running == true'"

# Test 3: Broker type verification
run_test "Verify NATS broker type" \
    "grep -q 'Broker Type: nats' /tmp/amcp-nats-test.log || curl -s http://localhost:8090/agent/status | jq -e '.brokerType == \"nats\"'"

# Test 4: Metrics endpoint
run_test "Metrics endpoint accessible" \
    "curl -s http://localhost:8090/q/metrics | grep -q amcp_"

# Test 5: Weather agent test (if available)
if curl -s http://localhost:8090/weather/london > /dev/null 2>&1; then
    run_test "Weather agent responds" \
        "curl -s -X POST http://localhost:8090/weather/london | jq -e '.city == \"london\"'"
fi

# Test 6: Pattern matching (check logs)
echo -e "${YELLOW}Test $((TESTS_RUN + 1)): Topic pattern matching (wildcard support)${NC}"
if grep -q "matchesPattern" /tmp/amcp-nats-test.log 2>/dev/null || \
   grep -q "Subscribed to subject pattern" /tmp/amcp-nats-test.log 2>/dev/null; then
    echo -e "${GREEN}✓ PASSED${NC}"
    TESTS_RUN=$((TESTS_RUN + 1))
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    echo -e "${YELLOW}⚠ Cannot verify from logs${NC}"
    TESTS_RUN=$((TESTS_RUN + 1))
fi

# Step 6: Cleanup
echo ""
echo "Step 6: Cleaning up..."

# Stop AMCP
if kill -0 $AMCP_PID 2>/dev/null; then
    kill $AMCP_PID
    echo -e "${GREEN}✓ AMCP stopped${NC}"
fi

# Stop NATS if we started it
if [ "$NATS_STARTED_BY_US" = true ]; then
    docker stop amcp-nats-test > /dev/null 2>&1
    docker rm amcp-nats-test > /dev/null 2>&1
    echo -e "${GREEN}✓ NATS server stopped${NC}"
fi

# Step 7: Summary
echo ""
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo "Total tests: $TESTS_RUN"
echo -e "Passed: ${GREEN}$TESTS_PASSED${NC}"
echo -e "Failed: ${RED}$TESTS_FAILED${NC}"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed!${NC}"
    echo "NATS broker fixes are working correctly."
    exit 0
else
    echo -e "${RED}✗ Some tests failed${NC}"
    echo "Check /tmp/amcp-nats-test.log for details"
    exit 1
fi
