#!/bin/bash

# AMCP v1.6 - Simple Endpoint Test Suite
# Tests health, metrics, and A2A endpoints without requiring full Maven startup

set -e

echo "=================================================="
echo "AMCP v1.6 - Simple Endpoint Test Suite"
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
HEALTH_URL="$BASE_URL/q/health/live"
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
    local max_attempts=5
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s "$url" > /dev/null 2>&1; then
            return 0
        fi
        sleep 1
        ((attempt++))
    done
    
    return 1
}

echo -e "${BLUE}Checking if AMCP service is running...${NC}"
echo ""

if ! check_service "$HEALTH_URL"; then
    echo -e "${RED}✗ AMCP service is not running on $BASE_URL${NC}"
    echo ""
    echo "Please start AMCP first:"
    echo "  cd amcp-examples"
    echo "  mvn quarkus:dev"
    echo ""
    exit 1
fi

echo -e "${GREEN}✓ AMCP service is running${NC}"
echo ""

# Phase 1: Health & Metrics Tests
echo -e "${BLUE}Phase 1: Health & Metrics Tests${NC}"
echo "---"

# Test 1: Liveness Probe
echo "Testing liveness probe..."
RESPONSE=$(curl -s "$HEALTH_URL")
if echo "$RESPONSE" | grep -q "UP"; then
    test_result "Liveness probe returns UP" 0
    if echo "$RESPONSE" | grep -q "amcp-agent-mesh"; then
        test_result "Liveness probe includes amcp-agent-mesh" 0
    else
        test_result "Liveness probe includes amcp-agent-mesh" 1
    fi
else
    test_result "Liveness probe returns UP" 1
fi

# Test 2: Readiness Probe
echo "Testing readiness probe..."
RESPONSE=$(curl -s "$BASE_URL/q/health/ready")
if echo "$RESPONSE" | grep -q "UP"; then
    test_result "Readiness probe returns UP" 0
    if echo "$RESPONSE" | grep -q "amcp-agent-mesh-ready"; then
        test_result "Readiness probe includes amcp-agent-mesh-ready" 0
    else
        test_result "Readiness probe includes amcp-agent-mesh-ready" 1
    fi
else
    test_result "Readiness probe returns UP" 1
fi

# Test 3: Prometheus Metrics
echo "Testing Prometheus metrics..."
RESPONSE=$(curl -s "$METRICS_URL")
if echo "$RESPONSE" | grep -q "amcp_"; then
    test_result "Metrics endpoint returns amcp_ metrics" 0
    
    # Check specific metrics
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
else
    test_result "Metrics endpoint returns amcp_ metrics" 1
fi

echo ""

# Phase 2: A2A Gateway Tests
echo -e "${BLUE}Phase 2: A2A Gateway Tests${NC}"
echo "---"

# Test 4: A2A Status
echo "Testing A2A status endpoint..."
RESPONSE=$(curl -s "$A2A_STATUS_URL")
if echo "$RESPONSE" | grep -q "A2A Protocol Bridge"; then
    test_result "A2A status returns service name" 0
    if echo "$RESPONSE" | grep -q "1.6.0"; then
        test_result "A2A status includes version 1.6.0" 0
    else
        test_result "A2A status includes version 1.6.0" 1
    fi
else
    test_result "A2A status returns service name" 1
fi

# Test 5: A2A Message Reception
echo "Testing A2A message reception..."
RESPONSE=$(curl -s -X POST "$A2A_MESSAGE_URL" \
    -H "Content-Type: application/json" \
    -d '{
        "id": "test-msg-001",
        "sender": "test-agent",
        "receiver": "amcp://weather",
        "performative": "REQUEST",
        "content": {"test": "data"}
    }')

if echo "$RESPONSE" | grep -q "accepted\|sent"; then
    test_result "A2A message accepted" 0
    if echo "$RESPONSE" | grep -q "test-msg-001"; then
        test_result "A2A message ID returned" 0
    else
        test_result "A2A message ID returned" 1
    fi
else
    test_result "A2A message accepted" 1
fi

# Test 6: A2A Conversations
echo "Testing A2A conversations endpoint..."
RESPONSE=$(curl -s "$BASE_URL/a2a/conversations")
if [ ! -z "$RESPONSE" ]; then
    test_result "A2A conversations endpoint responds" 0
else
    test_result "A2A conversations endpoint responds" 1
fi

echo ""

# Phase 3: Performance Tests
echo -e "${BLUE}Phase 3: Performance Tests${NC}"
echo "---"

# Test 7: Latency Measurement
echo "Measuring latency (5 samples)..."
LATENCIES=()
for i in {1..5}; do
    START=$(date +%s%N)
    curl -s -X POST "$A2A_MESSAGE_URL" \
        -H "Content-Type: application/json" \
        -d "{
            \"id\": \"latency-$i\",
            \"sender\": \"test-agent\",
            \"receiver\": \"amcp://weather\",
            \"performative\": \"REQUEST\",
            \"content\": {\"index\": $i}
        }" > /dev/null
    END=$(date +%s%N)
    LATENCY=$(( (END - START) / 1000000 ))
    LATENCIES+=($LATENCY)
    echo "  Sample $i: ${LATENCY}ms"
done

# Calculate average
AVG_LATENCY=$((( ${LATENCIES[@]/%/+}0 ) / ${#LATENCIES[@]}))
echo "Average Latency: ${AVG_LATENCY}ms"

if [ $AVG_LATENCY -lt 100 ]; then
    test_result "Latency acceptable (<100ms)" 0
else
    test_result "Latency acceptable (<100ms)" 1
fi

# Test 8: Throughput Test
echo "Testing throughput (100 messages)..."
START_TIME=$(date +%s)
for i in {1..100}; do
    curl -s -X POST "$A2A_MESSAGE_URL" \
        -H "Content-Type: application/json" \
        -d "{
            \"id\": \"throughput-$i\",
            \"sender\": \"test-agent\",
            \"receiver\": \"amcp://weather\",
            \"performative\": \"REQUEST\",
            \"content\": {\"index\": $i}
        }" > /dev/null &
    
    # Limit concurrent requests
    if [ $((i % 10)) -eq 0 ]; then
        wait
    fi
done
wait
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

if [ $DURATION -gt 0 ]; then
    THROUGHPUT=$((100 / DURATION))
    echo "Throughput: $THROUGHPUT msg/sec (100 messages in ${DURATION}s)"
    test_result "Throughput test completed" 0
else
    test_result "Throughput test completed" 1
fi

# Test 9: Error Handling
echo "Testing error handling..."
RESPONSE=$(curl -s -X POST "$A2A_MESSAGE_URL" \
    -H "Content-Type: application/json" \
    -d '{"invalid": "message"}')

if echo "$RESPONSE" | grep -q "error\|Error"; then
    test_result "Error handling for invalid message" 0
else
    test_result "Error handling for invalid message" 1
fi

echo ""

# Summary
echo "=================================================="
echo "Test Summary"
echo "=================================================="
echo -e "Passed: ${GREEN}$TESTS_PASSED${NC}"
echo -e "Failed: ${RED}$TESTS_FAILED${NC}"
echo "Total:  $((TESTS_PASSED + TESTS_FAILED))"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed!${NC}"
    echo ""
    echo "AMCP v1.6 Deliverables Validated:"
    echo "  ✓ Phase 1: Health & Metrics - WORKING"
    echo "  ✓ Phase 2: A2A Gateway - WORKING"
    echo "  ✓ Performance: ACCEPTABLE"
    echo "  ✓ Error Handling: WORKING"
    echo ""
    exit 0
else
    echo -e "${RED}✗ Some tests failed${NC}"
    exit 1
fi
