#!/bin/bash

# AMCP v1.5 Standalone OrchestratorAgent Demo Script
# This script demonstrates the OrchestratorAgent in isolation without requiring other agents

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🎯 AMCP v1.5 Standalone OrchestratorAgent Demo${NC}"
echo "=================================="

# Check Java version
echo -e "${YELLOW}🔍 Checking Java version...${NC}"
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo -e "${RED}❌ Java 21+ required, found Java $JAVA_VERSION${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Java version OK${NC}"

# Set JAVA_HOME to ensure Maven uses correct Java
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Check OLLAMA availability (optional for demo)
echo -e "${YELLOW}🔍 Checking OLLAMA availability...${NC}"
if command -v ollama >/dev/null 2>&1; then
    if pgrep -x "ollama" > /dev/null; then
        echo -e "${GREEN}✅ OLLAMA is running${NC}"
        
        # Check TinyLlama model
        if ollama list | grep -q "tinyllama"; then
            echo -e "${GREEN}✅ TinyLlama model is available${NC}"
        else
            echo -e "${YELLOW}⚠️  TinyLlama model not found - will demonstrate with fallback responses${NC}"
        fi
    else
        echo -e "${YELLOW}⚠️  OLLAMA not running - will demonstrate with fallback responses${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  OLLAMA not installed - will demonstrate with fallback responses${NC}"
fi

echo ""

# Navigate to project root
PROJECT_ROOT="$(dirname "$0")/.."
cd "$PROJECT_ROOT"

# Compile just the core and connectors manually
echo -e "${YELLOW}🔧 Building core classes...${NC}"
mkdir -p target/classes

# Compile core classes first
find core/src/main/java -name "*.java" -exec javac -cp "target/classes" -d target/classes {} + 2>/dev/null
find connectors/src/main/java -name "*.java" -exec javac -cp "target/classes" -d target/classes {} + 2>/dev/null

echo -e "${GREEN}✅ Core classes compiled${NC}"

# Compile the standalone demo
echo -e "${YELLOW}🔧 Compiling standalone demo...${NC}"
cd examples

# Ensure target directory exists
mkdir -p target/classes

# Compile the standalone demo specifically
javac -cp "../target/classes" \
      -d target/classes \
      src/main/java/io/amcp/examples/orchestrator/StandaloneOrchestratorDemo.java

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Standalone demo compiled successfully${NC}"
else
    echo -e "${RED}❌ Demo compilation failed${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}🚀 Starting Standalone OrchestratorAgent Demo...${NC}"
echo ""

# Run the demo
java -cp "../target/classes:target/classes" \
     io.amcp.examples.orchestrator.StandaloneOrchestratorDemo

echo ""
echo -e "${BLUE}👋 Demo completed!${NC}"