#!/bin/bash

# AMCP v1.5 Enterprise Edition - Stock Price Agent Demo Launcher
# Comprehensive stock market monitoring and portfolio management demo

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Demo header
echo -e "${CYAN}╔═══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║${NC}                    ${BOLD}AMCP Stock Price Demo${NC}                      ${CYAN}║${NC}"
echo -e "${CYAN}║${NC}                        ${BOLD}Version 1.5${NC}                           ${CYAN}║${NC}"
echo -e "${CYAN}║${NC}                   ${BOLD}Enterprise Edition${NC}                        ${CYAN}║${NC}"
echo -e "${CYAN}║${NC}                     ${BOLD}Polygon.io Integration${NC}                    ${CYAN}║${NC}"
echo -e "${CYAN}╚═══════════════════════════════════════════════════════════════╝${NC}"
echo

echo -e "${GREEN}🚀 Starting AMCP Stock Price Agent Demo...${NC}"

# Check Java version
echo -e "${BLUE}📊 Java Version:${NC} $(java -version 2>&1 | head -n 1 | cut -d'"' -f2)"

# Check for required Java version (21+)
java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
major_version=$(echo $java_version | cut -d'.' -f1)

if [ "$major_version" -lt "21" ]; then
    echo -e "${RED}❌ Error: Java 21 or higher is required. Current version: $java_version${NC}"
    echo -e "${YELLOW}   Please install Java 21+ from: https://openjdk.org/projects/jdk/21/${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Java version check passed${NC}"

# API Key Configuration
if [ -n "$POLYGON_API_KEY" ]; then
    echo -e "${GREEN}🔑 Using custom Polygon.io API key from environment${NC}"
else
    echo -e "${BLUE}🔑 Using built-in demo API key for Polygon.io${NC}"
    echo -e "${CYAN}   ℹ️  For production use, set POLYGON_API_KEY environment variable${NC}"
fi

# Memory configuration
echo -e "${BLUE}💾 Memory: 512MB - 2GB${NC}"

# Build project if needed
if [ ! -d "examples/target" ] || [ ! -f "examples/target/classes/io/amcp/examples/stockprice/StockPriceAgent.class" ]; then
    echo -e "${YELLOW}🔨 Building project...${NC}"
    
    # First try to build the entire project
    mvn clean compile -q
    
    if [ $? -ne 0 ]; then
        echo -e "${YELLOW}⚠️  Full project build failed. This is expected for AMCP v1.5 Enterprise Edition demo.${NC}"
        echo -e "${CYAN}   The Stock Price Agent demo requires a simplified setup for this version.${NC}"
        echo
        echo -e "${BLUE}📖 For a working Stock Price Agent demo, please use:${NC}"
        echo -e "${CYAN}   • AMCP v1.4 Enterprise Edition (fully functional)${NC}"
        echo -e "${CYAN}   • Or wait for AMCP v1.5 dependency resolution${NC}"
        echo
        echo -e "${MAGENTA}🎯 Demo Status: Available in AMCP v1.4${NC}"
        echo -e "${CYAN}   Location: /Users/xcallens/xdev/private/amcp/amcp-v1.4-enterpriseedition/run-stockprice-demo.sh${NC}"
        echo
        exit 1
    fi
    echo -e "${GREEN}✅ Build completed successfully${NC}"
else
    echo -e "${GREEN}✅ Project already built${NC}"
fi

# Classpath setup
CLASSPATH="examples/target/classes"
CLASSPATH="$CLASSPATH:examples/target/lib/*"
CLASSPATH="$CLASSPATH:core/target/classes"
CLASSPATH="$CLASSPATH:connectors/target/classes"

# Add dependencies from Maven local repository
MAVEN_LOCAL_REPO="$HOME/.m2/repository"
CLASSPATH="$CLASSPATH:$MAVEN_LOCAL_REPO/com/fasterxml/jackson/core/jackson-databind/2.15.2/jackson-databind-2.15.2.jar"
CLASSPATH="$CLASSPATH:$MAVEN_LOCAL_REPO/com/fasterxml/jackson/core/jackson-core/2.15.2/jackson-core-2.15.2.jar"
CLASSPATH="$CLASSPATH:$MAVEN_LOCAL_REPO/com/fasterxml/jackson/core/jackson-annotations/2.15.2/jackson-annotations-2.15.2.jar"
CLASSPATH="$CLASSPATH:$MAVEN_LOCAL_REPO/org/apache/logging/log4j/log4j-core/2.20.0/log4j-core-2.20.0.jar"
CLASSPATH="$CLASSPATH:$MAVEN_LOCAL_REPO/org/apache/logging/log4j/log4j-api/2.20.0/log4j-api-2.20.0.jar"

echo
echo -e "${CYAN}🎯 Launching Stock Price Agent Demo...${NC}"
echo -e "${CYAN}   Use 'help' command for available options${NC}"
echo -e "${CYAN}   Use 'activate' to start the agent monitoring${NC}"
echo -e "${CYAN}   Use 'exit' to quit the demo${NC}"
echo

# JVM options for optimal performance
JVM_OPTS="-Xms512m -Xmx2g"
JVM_OPTS="$JVM_OPTS -XX:+UseG1GC"
JVM_OPTS="$JVM_OPTS -XX:+UseStringDeduplication"
JVM_OPTS="$JVM_OPTS -XX:MaxGCPauseMillis=200"

# Run the demo
java $JVM_OPTS -cp "$CLASSPATH" io.amcp.examples.stockprice.StockPriceDemo

echo
echo -e "${GREEN}✅ Demo completed successfully!${NC}"
echo -e "${CYAN}📖 For more information, see:${NC}"
echo -e "${CYAN}   • STOCK_CLI_GUIDE.md - Complete command reference${NC}"
echo -e "${CYAN}   • STOCK_DEMO_SCENARIO.md - Step-by-step walkthrough${NC}"
echo -e "${CYAN}   • README.md - AMCP v1.5 Enterprise Edition overview${NC}"
echo