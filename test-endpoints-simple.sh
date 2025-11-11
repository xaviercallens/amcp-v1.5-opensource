#!/bin/bash

# AMCP v1.6 - Simple Endpoint Testing
# Tests health and metrics endpoints without Kafka

set -e

echo "=================================================="
echo "AMCP v1.6 - Simple Endpoint Testing"
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

# Step 1: Check if service is running
echo -e "${BLUE}Step 1: Checking if AMCP service is running${NC}"
echo "---"
max_attempts=30
attempt=1

while [ $attempt -le $max_attempts ]; do
    if curl -s "$HEALTH_LIVE_URL" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Service is running on port 8080${NC}"
        break
    fi
    echo "Attempt $attempt/$max_attempts: Waiting for service..."
    sleep 1
    ((attempt++))
done

if [ $attempt -gt $max_attempts ]; then
    echo -e "${RED}✗ Service did not start within ${max_attempts} seconds${NC}"
    echo ""
    echo "Make sure AMCP is running:"
    echo "  cd amcp-examples"
    echo "  mvn quarkus:dev"
    exit 1
fi

# Step 2: Test Liveness Endpoint
echo ""
echo -e "${BLUE}Step 2: Testing Liveness Endpoint${NC}"
echo "---"
echo "GET $HEALTH_LIVE_URL"
RESPONSE=$(curl -s "$HEALTH_LIVE_URL")
echo "Response:"
echo "$RESPONSE" | jq . 2>/dev/null || echo "$RESPONSE"
echo ""

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

if echo "$RESPONSE" | grep -q "agents_active\|agents_registered"; then
    test_result "Liveness check includes agents_active" 0
else
    test_result "Liveness check includes agents_active" 1
fi

# Step 3: Test Readiness Endpoint
echo ""
echo -e "${BLUE}Step 3: Testing Readiness Endpoint${NC}"
echo "---"
echo "GET $HEALTH_READY_URL"
RESPONSE=$(curl -s "$HEALTH_READY_URL")
echo "Response:"
echo "$RESPONSE" | jq . 2>/dev/null || echo "$RESPONSE"
echo ""

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

if echo "$RESPONSE" | grep -q "ready_for_traffic\|context_started"; then
    test_result "Readiness check includes ready_for_traffic" 0
else
    test_result "Readiness check includes ready_for_traffic" 1
fi

# Step 4: Test Prometheus Metrics
echo ""
echo -e "${BLUE}Step 4: Testing Prometheus Metrics Endpoint${NC}"
echo "---"
echo "GET $METRICS_URL"
RESPONSE=$(curl -s "$METRICS_URL")
echo "Metrics (first 50 lines):"
echo "$RESPONSE" | head -50
echo ""

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

# Step 5: Test A2A Status Endpoint
echo ""
echo -e "${BLUE}Step 5: Testing A2A Gateway Status Endpoint${NC}"
echo "---"
echo "GET $A2A_STATUS_URL"
RESPONSE=$(curl -s "$A2A_STATUS_URL" 2>/dev/null || echo "")

if [ -z "$RESPONSE" ]; then
    echo -e "${YELLOW}ℹ A2A endpoint not available (may require Kafka)${NC}"
    test_result "A2A status endpoint available" 1
else
    echo "Response:"
    echo "$RESPONSE" | jq . 2>/dev/null || echo "$RESPONSE"
    echo ""
    
    if echo "$RESPONSE" | grep -q "A2A\|service\|status"; then
        test_result "A2A status endpoint available" 0
    else
        test_result "A2A status endpoint available" 1
    fi
fi

# Step 6: Summary
echo ""
echo "=================================================="
echo "Test Summary"
echo "=================================================="
echo -e "Passed: ${GREEN}$TESTS_PASSED${NC}"
echo -e "Failed: ${RED}$TESTS_FAILED${NC}"
echo "Total:  $((TESTS_PASSED + TESTS_FAILED))"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed!${NC}"
    exit 0
else
    echo -e "${YELLOW}⚠ Some tests failed (may be due to missing Kafka)${NC}"
    exit 1
fi
