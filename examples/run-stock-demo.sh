#!/bin/bash

# AMCP v1.5 Enterprise Edition Stock Price Demo Launcher
# Compiles and runs the stock price monitoring command-line interface

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔═══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║           AMCP v1.5 Enterprise Edition Launcher              ║${NC}"
echo -e "${BLUE}║             Stock Price Agent Demo System                    ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════════════════════╝${NC}"

# Navigate to project root if we're in examples directory
if [[ "$(basename "$(pwd)")" == "examples" ]]; then
    cd ..
fi

# Check if we're in the right directory
if [[ ! -d "examples" ]] || [[ ! -d "core" ]]; then
    echo -e "${RED}Error: Please run this script from the AMCP project root directory${NC}"
    echo "Expected directory structure:"
    echo "  amcp-v1.5-enterprise-edition/"
    echo "    ├── core/"
    echo "    ├── examples/"
    echo "    └── examples/run-stock-demo.sh (this script)"
    exit 1
fi

echo -e "${YELLOW}🔨 Building AMCP Core Components...${NC}"

# Build all components with Maven (skip tests to avoid compilation issues)
if mvn compile -Dmaven.test.skip=true -q > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Core components compiled successfully${NC}"
    echo -e "${GREEN}✅ Stock price examples compiled successfully${NC}"
else
    echo -e "${RED}❌ Failed to compile project with Maven${NC}"
    exit 1
fi

echo -e "${YELLOW}🚀 Starting Stock Price Agent Demo...${NC}"
echo ""

# Run the stock price demo using Maven-compiled classes
java -cp "examples/target/classes:core/target/classes:connectors/target/classes" io.amcp.examples.stockprice.StockPriceDemo

echo ""
echo -e "${GREEN}✅ Stock Price Demo completed successfully!${NC}"
echo -e "${BLUE}Thank you for trying AMCP v1.5 Enterprise Edition Stock Price Demo${NC}"