#!/bin/bash

# AMCP v1.5 Enterprise Edition Travel Planner Demo Launcher
# Compiles and runs the travel planning command-line interface

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔═══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║           AMCP v1.5 Enterprise Edition Launcher              ║${NC}"
echo -e "${BLUE}║             Travel Planner Agent Demo System                 ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════════════════════╝${NC}"

# Check if we're in the right directory
if [[ ! -d "examples" ]] || [[ ! -d "core" ]]; then
    echo -e "${RED}Error: Please run this script from the AMCP project root directory${NC}"
    echo "Expected directory structure:"
    echo "  amcp-v1.5-enterprise-edition/"
    echo "    ├── core/"
    echo "    ├── examples/"
    echo "    └── run-travel-demo.sh (this script)"
    exit 1
fi

echo -e "${YELLOW}🔨 Building AMCP Core Components...${NC}"

# Build all components with Maven (skip tests to avoid compilation issues)
if mvn compile -Dmaven.test.skip=true -q > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Core components compiled successfully${NC}"
    echo -e "${GREEN}✅ Travel planner examples compiled successfully${NC}"
else
    echo -e "${RED}❌ Failed to compile project with Maven${NC}"
    exit 1
fi

echo -e "${YELLOW}🚀 Starting Travel Planner Agent Demo...${NC}"
echo ""

# Run the travel planner demo using Maven-compiled classes
java -cp "examples/target/classes:core/target/classes:connectors/target/classes" io.amcp.examples.travel.TravelPlannerDemo

echo ""
echo -e "${GREEN}✅ Travel Planner Demo completed successfully!${NC}"
echo -e "${BLUE}Thank you for trying AMCP v1.5 Enterprise Edition Travel Planner Demo${NC}"