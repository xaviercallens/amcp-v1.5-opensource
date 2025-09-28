#!/bin/bash

# AMCP v1.5 Enhanced Chat CLI Launcher
# Interactive multi-agent chat system with TinyLlama LLM integration

set -e

# Set Java 21 as JAVA_HOME if available
if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    export JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || /usr/libexec/java_home)
fi

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔══════════════════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║              AMCP v1.5 Enhanced Chat CLI Launcher                           ║${NC}"
echo -e "${BLUE}║                Multi-Agent Conversation System                              ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════════════════════════════════╝${NC}"

# Navigate to project root if we're in examples directory
if [[ "$(basename "$(pwd)")" == "examples" ]]; then
    cd ..
fi

echo -e "${YELLOW}🔨 Building AMCP Core Components...${NC}"

# Build core components first (skip tests to avoid compilation issues)
if mvn clean compile -pl core,connectors -DskipTests -q > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Core components compiled successfully${NC}"
else
    echo -e "${RED}❌ Failed to compile core modules${NC}"
    exit 1
fi

# Compile the Enhanced Chat CLI
echo -e "${YELLOW}🔨 Building Enhanced Chat CLI...${NC}"
if mvn compile -pl examples -am -DskipTests -Dmaven.compiler.includes="**/chat/**" -q > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Enhanced Chat CLI compiled successfully${NC}"
else
    echo -e "${RED}❌ Failed to compile Enhanced Chat CLI${NC}"
    echo -e "${YELLOW}📋 Trying alternative compilation approach...${NC}"
    
    # Compile manually with proper classpath
    mkdir -p examples/target/classes
    if javac -cp "core/target/classes:connectors/target/classes" \
        -d examples/target/classes \
        examples/src/main/java/io/amcp/examples/chat/EnhancedChatCLI.java; then
        echo -e "${GREEN}✅ Manual compilation successful${NC}"
    else
        echo -e "${RED}❌ Manual compilation failed${NC}"
        exit 1
    fi
fi

echo -e "${PURPLE}🚀 Starting AMCP Enhanced Chat CLI...${NC}"
echo -e "${BLUE}💡 TinyLlama LLM Integration Active${NC}"
echo ""

# Set up comprehensive classpath for all dependencies
CHAT_CLASSPATH="examples/target/classes:core/target/classes:connectors/target/classes:core/lib/*:connectors/lib/*:examples/lib/*"

# Check for OLLAMA availability (optional)
if command -v ollama &> /dev/null; then
    echo -e "${GREEN}🦙 OLLAMA detected - TinyLlama integration ready${NC}"
else
    echo -e "${YELLOW}⚠️  OLLAMA not found - using simulated responses${NC}"
    echo -e "${YELLOW}   Install OLLAMA and run 'ollama run tinyllama' for full LLM integration${NC}"
fi

echo ""
echo -e "${BLUE}🎯 Ready for multi-agent conversations!${NC}"
echo -e "${BLUE}📚 Example queries:${NC}"
echo -e "${BLUE}   • 'weather Nice' - Get weather in Nice, France${NC}"
echo -e "${BLUE}   • 'stock AMADEUS' - Amadeus stock price and options${NC}"
echo -e "${BLUE}   • 'travel Nice to New York' - Plan your trip${NC}"
echo ""

# Run the Enhanced Chat CLI
if java -cp "${CHAT_CLASSPATH}" io.amcp.examples.chat.EnhancedChatCLI "$@"; then
    echo ""
    echo -e "${GREEN}✅ Enhanced Chat CLI session completed successfully!${NC}"
    echo -e "${BLUE}Thank you for using AMCP v1.5 Enterprise Edition${NC}"
else
    echo ""
    echo -e "${RED}❌ Chat CLI execution failed${NC}"
    exit 1
fi

echo -e "${PURPLE}📋 Quick Start Guide:${NC}"
echo -e "${PURPLE}   1. Type 'help' for all available commands${NC}"
echo -e "${PURPLE}   2. Use 'examples' to see usage patterns${NC}"
echo -e "${PURPLE}   3. Try 'verbose' for real-time agent coordination${NC}"
echo -e "${PURPLE}   4. Use 'save <filename>' to preserve your sessions${NC}"