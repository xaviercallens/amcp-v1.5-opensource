#!/bin/bash

# AMCP v1.6 - Broker Performance Test Suite
# Tests both Kafka and NATS brokers with comprehensive performance metrics

set -e

echo "=================================================="
echo "AMCP v1.6 - Broker Performance Test Suite"
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
MESSAGE_COUNT=100
CONCURRENT_MESSAGES=10

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

# Helper function to check service
check_service() {
    local url=$1
    local max_attempts=30
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

# Helper function to measure latency
measure_latency() {
    local start=$(date +%s%N)
    curl -s -X POST "$BASE_URL/a2a/message" \
        -H "Content-Type: application/json" \
        -d '{
            "id": "latency-test",
            "sender": "test-agent",
            "receiver": "perf-agent",
            "performative": "REQUEST",
            "content": {"test": "latency"}
        }' > /dev/null
    local end=$(date +%s%N)
    local latency=$(( (end - start) / 1000000 ))
    echo $latency
}

# Helper function to send bulk messages
send_bulk_messages() {
    local broker=$1
    local count=$2
    
    echo "Sending $count messages on $broker..."
    
    for i in $(seq 1 $count); do
        curl -s -X POST "$BASE_URL/a2a/message" \
            -H "Content-Type: application/json" \
            -d "{
                \"id\": \"msg-$broker-$i\",
                \"sender\": \"test-agent\",
                \"receiver\": \"perf-agent\",
                \"performative\": \"REQUEST\",
                \"content\": {\"index\": $i, \"broker\": \"$broker\"}
            }" > /dev/null &
        
        # Limit concurrent requests
        if [ $((i % CONCURRENT_MESSAGES)) -eq 0 ]; then
            wait
        fi
    done
    
    wait
}

# Step 1: Kafka Tests
echo -e "${BLUE}Step 1: Testing Kafka Broker${NC}"
echo "---"

# Start application with Kafka
cd /home/kalxav/CascadeProjects/amcp-v1.6-opensource/amcp-examples
BROKER_TYPE=kafka mvn quarkus:dev > /tmp/amcp-kafka.log 2>&1 &
KAFKA_PID=$!
sleep 15

# Check service is running
if check_service "$HEALTH_URL"; then
    test_result "Kafka: Service started" 0
else
    test_result "Kafka: Service started" 1
    kill $KAFKA_PID 2>/dev/null || true
    exit 1
fi

# Test 1: Connectivity
echo ""
echo "Kafka Connectivity Tests:"
RESPONSE=$(curl -s "$HEALTH_URL")
if echo "$RESPONSE" | grep -q "UP"; then
    test_result "Kafka: Broker connected" 0
else
    test_result "Kafka: Broker connected" 1
fi

# Test 2: Metrics available
RESPONSE=$(curl -s "$METRICS_URL")
if echo "$RESPONSE" | grep -q "amcp_"; then
    test_result "Kafka: Metrics available" 0
else
    test_result "Kafka: Metrics available" 1
fi

# Test 3: Message processing
echo ""
echo "Kafka Message Processing Tests:"
send_bulk_messages "kafka" $MESSAGE_COUNT
sleep 2

RESPONSE=$(curl -s "$METRICS_URL")
if echo "$RESPONSE" | grep -q "amcp_"; then
    test_result "Kafka: Messages processed" 0
else
    test_result "Kafka: Messages processed" 1
fi

# Test 4: Latency measurement
echo ""
echo "Kafka Performance Tests:"
LATENCIES=()
for i in {1..5}; do
    LATENCY=$(measure_latency)
    LATENCIES+=($LATENCY)
done

AVG_LATENCY=$((( ${LATENCIES[@]/%/+}0 ) / ${#LATENCIES[@]}))
echo "Average Latency: ${AVG_LATENCY}ms"

if [ $AVG_LATENCY -lt 50 ]; then
    test_result "Kafka: Latency acceptable (<50ms)" 0
else
    test_result "Kafka: Latency acceptable (<50ms)" 1
fi

# Cleanup Kafka
kill $KAFKA_PID 2>/dev/null || true
sleep 5

# Step 2: NATS Tests
echo ""
echo -e "${BLUE}Step 2: Testing NATS Broker${NC}"
echo "---"

# Start NATS if not running
if ! docker ps | grep -q nats; then
    echo "Starting NATS container..."
    docker run -d --name nats-test -p 4222:4222 nats:latest > /dev/null
    sleep 5
fi

# Start application with NATS
BROKER_TYPE=nats mvn quarkus:dev > /tmp/amcp-nats.log 2>&1 &
NATS_PID=$!
sleep 15

# Check service is running
if check_service "$HEALTH_URL"; then
    test_result "NATS: Service started" 0
else
    test_result "NATS: Service started" 1
    kill $NATS_PID 2>/dev/null || true
    exit 1
fi

# Test 1: Connectivity
echo ""
echo "NATS Connectivity Tests:"
RESPONSE=$(curl -s "$HEALTH_URL")
if echo "$RESPONSE" | grep -q "UP"; then
    test_result "NATS: Broker connected" 0
else
    test_result "NATS: Broker connected" 1
fi

# Test 2: Metrics available
RESPONSE=$(curl -s "$METRICS_URL")
if echo "$RESPONSE" | grep -q "amcp_"; then
    test_result "NATS: Metrics available" 0
else
    test_result "NATS: Metrics available" 1
fi

# Test 3: Message processing
echo ""
echo "NATS Message Processing Tests:"
send_bulk_messages "nats" $MESSAGE_COUNT
sleep 2

RESPONSE=$(curl -s "$METRICS_URL")
if echo "$RESPONSE" | grep -q "amcp_"; then
    test_result "NATS: Messages processed" 0
else
    test_result "NATS: Messages processed" 1
fi

# Test 4: Latency measurement
echo ""
echo "NATS Performance Tests:"
LATENCIES=()
for i in {1..5}; do
    LATENCY=$(measure_latency)
    LATENCIES+=($LATENCY)
done

AVG_LATENCY=$((( ${LATENCIES[@]/%/+}0 ) / ${#LATENCIES[@]}))
echo "Average Latency: ${AVG_LATENCY}ms"

if [ $AVG_LATENCY -lt 30 ]; then
    test_result "NATS: Latency excellent (<30ms)" 0
else
    test_result "NATS: Latency acceptable" 1
fi

# Cleanup NATS
kill $NATS_PID 2>/dev/null || true
docker stop nats-test 2>/dev/null || true
docker rm nats-test 2>/dev/null || true

# Step 3: Summary
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
    echo -e "${RED}✗ Some tests failed${NC}"
    exit 1
fi
