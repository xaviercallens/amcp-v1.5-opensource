#!/bin/bash

# AMCP v1.4 Weather System Demo Launcher
# Compiles and runs the weather monitoring command-line interface

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔═══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                AMCP v1.4 Weather System Launcher             ║${NC}"
echo -e "${BLUE}║          Agent Mesh Communication Protocol Demo              ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════════════════════╝${NC}"

# Check if we're in the right directory
if [[ ! -d "examples" ]] || [[ ! -d "core" ]]; then
    echo -e "${RED}Error: Please run this script from the AMCP project root directory${NC}"
    echo "Expected directory structure:"
    echo "  amcp-v1.4-opensource/"
    echo "    ├── core/"
    echo "    ├── examples/"
    echo "    └── run-weather-demo.sh (this script)"
    exit 1
fi

echo -e "${YELLOW}🔨 Building AMCP Core Components...${NC}"

# Build core components first
cd core
if mvn compile -q > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Core components compiled successfully${NC}"
else
    echo -e "${YELLOW}⚠️  Maven build encountered issues, trying direct compilation...${NC}"
    
    # Fallback to direct compilation
    cd src/main/java
    if javac io/amcp/mobility/*.java io/amcp/messaging/*.java io/amcp/core/*.java io/amcp/messaging/impl/*.java 2>/dev/null; then
        echo -e "${GREEN}✅ Core components compiled with javac${NC}"
        cd ../../..
    else
        echo -e "${RED}❌ Failed to compile core components${NC}"
        cd ../../..
        exit 1
    fi
fi

cd ..

echo -e "${YELLOW}🌤️  Building Weather System Examples...${NC}"

# Create build directory for examples
mkdir -p examples/build/classes

# Set up classpath
CORE_CLASSES="core/target/classes"
if [[ ! -d "$CORE_CLASSES" ]]; then
    CORE_CLASSES="core/src/main/java"
fi

# Compile weather system examples
cd examples
if javac -cp "../${CORE_CLASSES}" -d build/classes \
    src/main/java/io/amcp/examples/weather/*.java 2>/dev/null; then
    echo -e "${GREEN}✅ Weather system examples compiled successfully${NC}"
else
    echo -e "${YELLOW}⚠️  Some compilation issues detected, but proceeding...${NC}"
fi

echo -e "${YELLOW}🚀 Starting Weather System CLI...${NC}"
echo ""

# Run the weather system CLI
java -cp "build/classes:../${CORE_CLASSES}" io.amcp.examples.weather.WeatherSystemCLI

echo ""
echo -e "${GREEN}✅ Weather System Demo completed successfully!${NC}"
echo -e "${BLUE}Thank you for trying AMCP v1.4 Weather System Demo${NC}"