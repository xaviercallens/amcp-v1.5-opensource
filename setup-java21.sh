#!/bin/bash

# AMCP v1.5 Open Source Edition - Java 21 Environment Setup
# This script ensures the correct Java environment for AMCP development

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}🔧 Setting up Java 21 environment for AMCP v1.5 Open Source Edition...${NC}"

# Set Java 21 environment
export JAVA_HOME="/opt/homebrew/Cellar/openjdk@21/21.0.8/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

# Verify Java version
JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
if [[ "$JAVA_VERSION" == "21" ]]; then
    echo -e "${GREEN}✅ Java 21 is active${NC}"
    echo -e "${GREEN}   JAVA_HOME: $JAVA_HOME${NC}"
    echo -e "${GREEN}   Java Version: $(java -version 2>&1 | head -n1)${NC}"
else
    echo -e "${YELLOW}⚠️  Warning: Java $JAVA_VERSION is active instead of Java 21${NC}"
fi

# Verify Maven
MVN_JAVA=$(mvn -version 2>/dev/null | grep "Java version" | cut -d' ' -f3)
if [[ "$MVN_JAVA" == "21.0.8" ]]; then
    echo -e "${GREEN}✅ Maven is using Java 21${NC}"
else
    echo -e "${YELLOW}⚠️  Maven Java version: $MVN_JAVA${NC}"
fi

echo -e "${BLUE}🚀 Environment ready for AMCP v1.5 development!${NC}"
echo ""
echo "Available commands:"
echo "  mvn clean package                    # Build AMCP"
echo "  ./scripts/build.sh                   # Full build with options"
echo "  ./run-travel-demo.sh                 # Run travel demo"
echo "  ./run-multiagent-demo.sh             # Run multi-agent demo"
echo ""