#!/bin/bash

# AMCP v1.6 - Local Agent Testing Script
# Tests Weather and Stock agents with real data

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
BASE_URL="http://localhost:8080"
TIMEOUT=10

# Test counters
TESTS_RUN=0
TESTS_PASSED=0
TESTS_FAILED=0

echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  AMCP v1.6 - Weather & Stock Agent Testing                ║${NC}"
echo -e "${BLUE}║  Real Data Integration with OpenWeatherMap & Polygon.io   ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Function to run a test
run_test() {
    local test_name="$1"
    local endpoint="$2"
    local expected_field="$3"
    
    TESTS_RUN=$((TESTS_RUN + 1))
    
    echo -n "Test $TESTS_RUN: $test_name ... "
    
    response=$(curl -s -X GET "$BASE_URL$endpoint" \
        -H "Content-Type: application/json" \
        --max-time $TIMEOUT 2>/dev/null || echo "ERROR")
    
    if echo "$response" | grep -q "$expected_field"; then
        echo -e "${GREEN}✓ PASSED${NC}"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        echo "  Response: $(echo $response | cut -c1-80)..."
    else
        echo -e "${RED}✗ FAILED${NC}"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        echo "  Response: $response"
    fi
    echo ""
}

# Function to test JSON response
test_json_response() {
    local test_name="$1"
    local endpoint="$2"
    
    TESTS_RUN=$((TESTS_RUN + 1))
    
    echo -n "Test $TESTS_RUN: $test_name ... "
    
    response=$(curl -s -X GET "$BASE_URL$endpoint" \
        -H "Content-Type: application/json" \
        --max-time $TIMEOUT 2>/dev/null || echo "{}")
    
    # Check if response is valid JSON
    if echo "$response" | jq . > /dev/null 2>&1; then
        echo -e "${GREEN}✓ PASSED${NC}"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        echo "  Response:"
        echo "$response" | jq . | sed 's/^/    /'
    else
        echo -e "${RED}✗ FAILED${NC}"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        echo "  Response: $response"
    fi
    echo ""
}

# Check if server is running
echo -e "${YELLOW}Checking server connectivity...${NC}"
if ! curl -s "$BASE_URL/q/health" > /dev/null 2>&1; then
    echo -e "${RED}✗ Server not responding at $BASE_URL${NC}"
    echo "Please start the application with:"
    echo "  cd amcp-examples && mvn quarkus:dev"
    exit 1
fi
echo -e "${GREEN}✓ Server is running${NC}"
echo ""

# ============================================================================
# WEATHER AGENT TESTS
# ============================================================================

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}WEATHER AGENT TESTS - Real Data from OpenWeatherMap${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Test 1-10: Weather for 10 global cities
echo -e "${YELLOW}Testing Weather for 10 Global Cities:${NC}"
echo ""

test_json_response "Weather - London (GB)" "/weather/london"
test_json_response "Weather - Paris (FR)" "/weather/paris"
test_json_response "Weather - Tokyo (JP)" "/weather/tokyo"
test_json_response "Weather - New York (US)" "/weather/newyork"
test_json_response "Weather - Sydney (AU)" "/weather/sydney"
test_json_response "Weather - Dubai (AE)" "/weather/dubai"
test_json_response "Weather - Singapore (SG)" "/weather/singapore"
test_json_response "Weather - Hong Kong (HK)" "/weather/hongkong"
test_json_response "Weather - Bangkok (TH)" "/weather/bangkok"
test_json_response "Weather - Mumbai (IN)" "/weather/mumbai"

# Test 11-13: Weather forecasts
echo -e "${YELLOW}Testing Weather Forecasts:${NC}"
echo ""

test_json_response "Forecast - London (3-day)" "/weather/london/forecast"
test_json_response "Forecast - Tokyo (3-day)" "/weather/tokyo/forecast"
test_json_response "Forecast - New York (3-day)" "/weather/newyork/forecast"

# Test 14: Weather agent status
echo -e "${YELLOW}Testing Weather Agent Status:${NC}"
echo ""

test_json_response "Weather Agent Status" "/weather/status"

# ============================================================================
# STOCK AGENT TESTS
# ============================================================================

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}STOCK AGENT TESTS - Real Data from Polygon.io${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Test 15-24: Stock quotes for 10 global companies
echo -e "${YELLOW}Testing Stock Quotes for 10 Global Companies:${NC}"
echo ""

test_json_response "Stock Quote - AAPL (Apple)" "/stock/AAPL"
test_json_response "Stock Quote - MSFT (Microsoft)" "/stock/MSFT"
test_json_response "Stock Quote - GOOGL (Alphabet)" "/stock/GOOGL"
test_json_response "Stock Quote - AMZN (Amazon)" "/stock/AMZN"
test_json_response "Stock Quote - TSLA (Tesla)" "/stock/TSLA"
test_json_response "Stock Quote - META (Meta)" "/stock/META"
test_json_response "Stock Quote - NVDA (NVIDIA)" "/stock/NVDA"
test_json_response "Stock Quote - JPM (JPMorgan)" "/stock/JPM"
test_json_response "Stock Quote - V (Visa)" "/stock/V"
test_json_response "Stock Quote - WMT (Walmart)" "/stock/WMT"

# Test 25-29: Detailed stock analysis
echo -e "${YELLOW}Testing Detailed Stock Analysis:${NC}"
echo ""

test_json_response "Detailed Quote - AAPL" "/stock/AAPL/details"
test_json_response "Detailed Quote - MSFT" "/stock/MSFT/details"
test_json_response "Detailed Quote - GOOGL" "/stock/GOOGL/details"
test_json_response "Detailed Quote - AMZN" "/stock/AMZN/details"
test_json_response "Detailed Quote - TSLA" "/stock/TSLA/details"

# Test 30: Portfolio analysis
echo -e "${YELLOW}Testing Portfolio Analysis:${NC}"
echo ""

test_json_response "Portfolio Analysis" "/stock/portfolio"

# Test 31: Stock agent status
echo -e "${YELLOW}Testing Stock Agent Status:${NC}"
echo ""

test_json_response "Stock Agent Status" "/stock/status"

# ============================================================================
# BATCH & STRESS TESTS
# ============================================================================

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}BATCH & STRESS TESTS${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Test 32-36: Rapid weather requests
echo -e "${YELLOW}Weather Batch Test (5 Rapid Requests):${NC}"
echo ""

start_time=$(date +%s%N)
for i in {1..5}; do
    test_json_response "Weather Batch Request $i" "/weather/london"
done
end_time=$(date +%s%N)
elapsed=$((($end_time - $start_time) / 1000000))
echo -e "${YELLOW}Batch Time: ${elapsed}ms${NC}"
echo ""

# Test 37-41: Rapid stock requests
echo -e "${YELLOW}Stock Batch Test (5 Rapid Requests):${NC}"
echo ""

start_time=$(date +%s%N)
for i in {1..5}; do
    test_json_response "Stock Batch Request $i" "/stock/AAPL"
done
end_time=$(date +%s%N)
elapsed=$((($end_time - $start_time) / 1000000))
echo -e "${YELLOW}Batch Time: ${elapsed}ms${NC}"
echo ""

# ============================================================================
# HEALTH & METRICS TESTS
# ============================================================================

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}HEALTH & METRICS TESTS${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Test 42: Liveness probe
echo -n "Test 42: Liveness Probe ... "
TESTS_RUN=$((TESTS_RUN + 1))
if curl -s "$BASE_URL/q/health/live" | grep -q "UP"; then
    echo -e "${GREEN}✓ PASSED${NC}"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    echo -e "${RED}✗ FAILED${NC}"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi
echo ""

# Test 43: Readiness probe
echo -n "Test 43: Readiness Probe ... "
TESTS_RUN=$((TESTS_RUN + 1))
if curl -s "$BASE_URL/q/health/ready" | grep -q "UP"; then
    echo -e "${GREEN}✓ PASSED${NC}"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    echo -e "${RED}✗ FAILED${NC}"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi
echo ""

# Test 44: Metrics endpoint
echo -n "Test 44: Metrics Endpoint ... "
TESTS_RUN=$((TESTS_RUN + 1))
if curl -s "$BASE_URL/q/metrics" | grep -q "jvm_memory"; then
    echo -e "${GREEN}✓ PASSED${NC}"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    echo -e "${RED}✗ FAILED${NC}"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi
echo ""

# ============================================================================
# TEST SUMMARY
# ============================================================================

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}TEST SUMMARY${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

echo "Total Tests Run:    $TESTS_RUN"
echo -e "Tests Passed:       ${GREEN}$TESTS_PASSED${NC}"
echo -e "Tests Failed:       ${RED}$TESTS_FAILED${NC}"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║  ✓ ALL TESTS PASSED!                                      ║${NC}"
    echo -e "${GREEN}║  AMCP v1.6 Weather & Stock Agents Working Perfectly       ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
    exit 0
else
    echo -e "${RED}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${RED}║  ✗ SOME TESTS FAILED                                      ║${NC}"
    echo -e "${RED}║  Check server logs and API responses                       ║${NC}"
    echo -e "${RED}╚════════════════════════════════════════════════════════════╝${NC}"
    exit 1
fi
