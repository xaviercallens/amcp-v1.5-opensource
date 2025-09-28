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

# Build core components first (skip tests to avoid compilation issues)
if mvn clean compile -pl core,connectors -DskipTests -q > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Core components compiled successfully${NC}"
else
    echo -e "${RED}❌ Failed to compile core modules${NC}"
    exit 1
fi

# Compile just the stock price demo classes
echo -e "${YELLOW}🔨 Building Stock Price Demo...${NC}"
if mvn compile -pl examples -am -DskipTests -Dmaven.compiler.includes="**/stockprice/**" -q > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Stock price demo compiled successfully${NC}"
else
    echo -e "${RED}❌ Failed to compile stock price demo${NC}"
    echo -e "${YELLOW}📋 Trying alternative compilation approach...${NC}"
    
    # Generate dependency classpath for connectors
    mvn dependency:build-classpath -pl connectors -Dmdep.outputFile=connectors/classpath.txt -q > /dev/null 2>&1
    
    # Compile manually with proper classpath
    mkdir -p examples/target/classes
    if javac -cp "core/target/classes:connectors/target/classes" \
        -d examples/target/classes \
        examples/src/main/java/io/amcp/examples/stockprice/StockPriceDemo.java; then
        echo -e "${GREEN}✅ Manual compilation successful${NC}"
    else
        echo -e "${RED}❌ Manual compilation failed${NC}"
        echo -e "${RED}❌ Manual compilation failed${NC}"
        exit 1
    fi
fi

echo -e "${GREEN}🚀 Starting Stock Price Agent Demo...${NC}"
echo ""

# Set up basic classpath (no external dependencies needed for this demo)
DEMO_CLASSPATH="examples/target/classes:core/target/classes:connectors/target/classes"

# Run the demo
if java -cp "${DEMO_CLASSPATH}" io.amcp.examples.stockprice.StockPriceDemo; then
    echo ""
    echo -e "${GREEN}✅ Stock Price Demo completed successfully!${NC}"
    echo -e "${BLUE}Thank you for trying AMCP v1.5 Enterprise Edition Stock Price Demo${NC}"
else
    echo ""
    echo -e "${RED}❌ Demo execution failed${NC}"
    exit 1
fi