#!/bin/bash

set -e

BASE_URL="${BASE_URL:-http://localhost:8080}"
RESULTS_DIR="test-results-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$RESULTS_DIR"

echo "🧪 Starting Comprehensive Real Data Testing..."
echo "Base URL: $BASE_URL"
echo "Results Directory: $RESULTS_DIR"
echo ""

# Color codes
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Test counter
TESTS_PASSED=0
TESTS_FAILED=0

# Helper function to test endpoint
test_endpoint() {
    local name=$1
    local method=$2
    local endpoint=$3
    local data=$4
    local output_file=$5
    
    echo -e "${BLUE}Testing: $name${NC}"
    
    if [ "$method" = "POST" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data")
    else
        response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL$endpoint")
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)
    
    if [ "$http_code" = "200" ]; then
        echo "$body" | jq '.' > "$RESULTS_DIR/$output_file" 2>/dev/null || echo "$body" > "$RESULTS_DIR/$output_file"
        echo -e "${GREEN}✅ PASSED${NC} (HTTP $http_code)"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}❌ FAILED${NC} (HTTP $http_code)"
        echo "$body" > "$RESULTS_DIR/${output_file}.error"
        ((TESTS_FAILED++))
    fi
    echo ""
}

# ============================================
# WEATHER AGENT TESTS
# ============================================
echo -e "${YELLOW}=== WEATHER AGENT TESTS ===${NC}"
echo ""

test_endpoint \
    "Weather - Paris" \
    "POST" \
    "/weather/request" \
    '{"city": "Paris"}' \
    "weather_paris.json"

test_endpoint \
    "Weather - London" \
    "POST" \
    "/weather/request" \
    '{"city": "London"}' \
    "weather_london.json"

test_endpoint \
    "Weather - Tokyo" \
    "POST" \
    "/weather/request" \
    '{"city": "Tokyo"}' \
    "weather_tokyo.json"

test_endpoint \
    "Weather - New York" \
    "POST" \
    "/weather/request" \
    '{"city": "New York"}' \
    "weather_newyork.json"

test_endpoint \
    "Weather - Forecast (Paris)" \
    "POST" \
    "/weather/forecast" \
    '{"city": "Paris"}' \
    "weather_forecast_paris.json"

test_endpoint \
    "Weather - Status" \
    "GET" \
    "/weather/status" \
    "" \
    "weather_status.json"

# ============================================
# STOCK AGENT TESTS
# ============================================
echo -e "${YELLOW}=== STOCK AGENT TESTS ===${NC}"
echo ""

test_endpoint \
    "Stock - AAPL" \
    "POST" \
    "/stock/request" \
    '{"symbol": "AAPL"}' \
    "stock_aapl.json"

test_endpoint \
    "Stock - GOOGL" \
    "POST" \
    "/stock/request" \
    '{"symbol": "GOOGL"}' \
    "stock_googl.json"

test_endpoint \
    "Stock - MSFT" \
    "POST" \
    "/stock/request" \
    '{"symbol": "MSFT"}' \
    "stock_msft.json"

test_endpoint \
    "Stock - TSLA" \
    "POST" \
    "/stock/request" \
    '{"symbol": "TSLA"}' \
    "stock_tsla.json"

test_endpoint \
    "Stock - Quote (AAPL)" \
    "POST" \
    "/stock/quote" \
    '{"symbol": "AAPL"}' \
    "stock_quote_aapl.json"

test_endpoint \
    "Stock - Portfolio" \
    "POST" \
    "/stock/portfolio" \
    '{}' \
    "stock_portfolio.json"

test_endpoint \
    "Stock - Status" \
    "GET" \
    "/stock/status" \
    "" \
    "stock_status.json"

# ============================================
# DATA VALIDATION
# ============================================
echo -e "${YELLOW}=== DATA VALIDATION ===${NC}"
echo ""

# Check weather data
if [ -f "$RESULTS_DIR/weather_paris.json" ]; then
    temp=$(jq -r '.temperature // empty' "$RESULTS_DIR/weather_paris.json" 2>/dev/null)
    if [ ! -z "$temp" ]; then
        echo -e "${GREEN}✅ Weather data contains temperature: $temp°C${NC}"
    fi
    
    condition=$(jq -r '.condition // empty' "$RESULTS_DIR/weather_paris.json" 2>/dev/null)
    if [ ! -z "$condition" ]; then
        echo -e "${GREEN}✅ Weather data contains condition: $condition${NC}"
    fi
    
    source=$(jq -r '.dataSource // empty' "$RESULTS_DIR/weather_paris.json" 2>/dev/null)
    if [ ! -z "$source" ]; then
        echo -e "${GREEN}✅ Weather data source: $source${NC}"
    fi
fi

echo ""

# Check stock data
if [ -f "$RESULTS_DIR/stock_aapl.json" ]; then
    price=$(jq -r '.price // empty' "$RESULTS_DIR/stock_aapl.json" 2>/dev/null)
    if [ ! -z "$price" ]; then
        echo -e "${GREEN}✅ Stock data contains price: \$$price${NC}"
    fi
    
    change=$(jq -r '.change // empty' "$RESULTS_DIR/stock_aapl.json" 2>/dev/null)
    if [ ! -z "$change" ]; then
        echo -e "${GREEN}✅ Stock data contains change: $change${NC}"
    fi
    
    source=$(jq -r '.dataSource // empty' "$RESULTS_DIR/stock_aapl.json" 2>/dev/null)
    if [ ! -z "$source" ]; then
        echo -e "${GREEN}✅ Stock data source: $source${NC}"
    fi
fi

# ============================================
# SUMMARY
# ============================================
echo ""
echo -e "${YELLOW}=== TEST SUMMARY ===${NC}"
echo ""
echo -e "${GREEN}Passed: $TESTS_PASSED${NC}"
echo -e "${RED}Failed: $TESTS_FAILED${NC}"
echo ""
echo "Results saved to: $RESULTS_DIR"
echo ""

# Generate summary report
cat > "$RESULTS_DIR/SUMMARY.txt" << EOF
Comprehensive Real Data Testing Summary
======================================

Test Date: $(date)
Base URL: $BASE_URL
Results Directory: $RESULTS_DIR

Test Results:
- Passed: $TESTS_PASSED
- Failed: $TESTS_FAILED

Weather Agent Tests:
- Paris weather request
- London weather request
- Tokyo weather request
- New York weather request
- Forecast request
- Status check

Stock Agent Tests:
- AAPL quote request
- GOOGL quote request
- MSFT quote request
- TSLA quote request
- Detailed quote request
- Portfolio analysis
- Status check

Data Validation:
- Temperature values present
- Weather conditions present
- Stock prices present
- Change values present
- Data sources identified

All test results saved as JSON files in this directory.
EOF

echo -e "${GREEN}✅ Testing complete!${NC}"

# Exit with appropriate code
if [ $TESTS_FAILED -gt 0 ]; then
    exit 1
else
    exit 0
fi
