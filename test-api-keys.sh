#!/bin/bash

# AMCP API Keys Test Script
# Quick test to verify production API keys are working

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🧪 AMCP Production API Keys Test${NC}"
echo "═══════════════════════════════════════"

# Load environment
if [ -f ".env" ]; then
    source .env
    echo -e "${GREEN}✅ Loaded .env file${NC}"
else
    echo -e "${RED}❌ .env file not found${NC}"
    exit 1
fi

# Check if keys are set
if [ -z "$POLYGON_API_KEY" ] || [ -z "$OPENWEATHER_API_KEY" ]; then
    echo -e "${RED}❌ API keys not loaded${NC}"
    echo "Run: source .env"
    exit 1
fi

echo -e "${GREEN}✅ API keys loaded successfully${NC}"
echo "  • Polygon.io: ${POLYGON_API_KEY:0:8}... (${#POLYGON_API_KEY} chars)"
echo "  • OpenWeather: ${OPENWEATHER_API_KEY:0:8}... (${#OPENWEATHER_API_KEY} chars)"
echo ""

# Test Polygon.io API (simple HTTP test)
echo -e "${YELLOW}🔍 Testing Polygon.io API connectivity...${NC}"
if command -v curl &> /dev/null; then
    POLYGON_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "https://api.polygon.io/v2/aggs/ticker/AAPL/prev?adjusted=true&apikey=$POLYGON_API_KEY" || echo "000")
    if [ "$POLYGON_STATUS" = "200" ]; then
        echo -e "${GREEN}✅ Polygon.io API: WORKING (HTTP 200)${NC}"
    else
        echo -e "${YELLOW}⚠️  Polygon.io API: HTTP $POLYGON_STATUS${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  curl not available - skipping HTTP test${NC}"
fi

# Test OpenWeatherMap API (simple HTTP test)
echo -e "${YELLOW}🔍 Testing OpenWeatherMap API connectivity...${NC}"
if command -v curl &> /dev/null; then
    WEATHER_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "https://api.openweathermap.org/data/2.5/weather?q=London&appid=$OPENWEATHER_API_KEY" || echo "000")
    if [ "$WEATHER_STATUS" = "200" ]; then
        echo -e "${GREEN}✅ OpenWeatherMap API: WORKING (HTTP 200)${NC}"
    else
        echo -e "${YELLOW}⚠️  OpenWeatherMap API: HTTP $WEATHER_STATUS${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  curl not available - skipping HTTP test${NC}"
fi

echo ""
echo -e "${BLUE}🚀 Ready to launch AMCP CLI!${NC}"
echo "Run: ./amcp-cli"
echo ""
echo -e "${GREEN}Test commands in CLI:${NC}"
echo "  • stock AAPL"
echo "  • weather London"
echo "  • status apis"