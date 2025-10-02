#!/bin/bash
# AMCP v1.5 Open Source Edition - Getting Started Script
# This script helps new developers set up AMCP quickly

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Banner
clear
echo -e "${CYAN}"
cat << "EOF"
╔══════════════════════════════════════════════════════════════════════════════╗
║                                                                              ║
║               Welcome to AMCP v1.5 Open Source Edition! 🚀                  ║
║                        Getting Started Assistant                             ║
║                                                                              ║
╚══════════════════════════════════════════════════════════════════════════════╝
EOF
echo -e "${NC}\n"

echo -e "${PURPLE}This script will guide you through setting up AMCP for development.${NC}\n"

# Step 1: Check Java
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Step 1/6: Checking Java Installation${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1}')
    if [ "$JAVA_VERSION" -ge 21 ]; then
        echo -e "${GREEN}✅ Java $JAVA_VERSION detected${NC}"
    else
        echo -e "${RED}❌ Java 21+ required. Found: Java $JAVA_VERSION${NC}"
        echo -e "${YELLOW}   Running setup-java21.sh...${NC}\n"
        ./setup-java21.sh
    fi
else
    echo -e "${RED}❌ Java not found${NC}"
    echo -e "${YELLOW}   Running setup-java21.sh...${NC}\n"
    ./setup-java21.sh
fi

# Step 2: Check Maven
echo -e "\n${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Step 2/6: Checking Maven Installation${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version | head -1 | awk '{print $3}')
    echo -e "${GREEN}✅ Maven $MVN_VERSION detected${NC}"
else
    echo -e "${RED}❌ Maven not found${NC}"
    echo -e "${YELLOW}   Please install Maven 3.8+: https://maven.apache.org/install.html${NC}"
    exit 1
fi

# Step 3: Build Project
echo -e "\n${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Step 3/6: Building AMCP Project${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

if [ -d "core/target/classes" ] && [ -d "connectors/target/classes" ]; then
    echo -e "${GREEN}✅ Project already built${NC}"
    read -p "Rebuild? (y/n): " rebuild
    if [ "$rebuild" = "y" ]; then
        echo -e "\n${CYAN}Building...${NC}"
        mvn clean install -DskipTests
    fi
else
    echo -e "${CYAN}Building AMCP (this may take 2-3 minutes on first run)...${NC}\n"
    mvn clean install -DskipTests
    
    if [ $? -eq 0 ]; then
        echo -e "\n${GREEN}✅ Build successful!${NC}"
    else
        echo -e "\n${RED}❌ Build failed. Please check errors above.${NC}"
        exit 1
    fi
fi

# Step 4: Check Optional Dependencies
echo -e "\n${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Step 4/6: Checking Optional Dependencies${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

# Check Docker
if command -v docker &> /dev/null; then
    echo -e "${GREEN}✅ Docker installed${NC}"
else
    echo -e "${YELLOW}⚠️  Docker not found (optional - needed for advanced demos)${NC}"
    echo -e "   Install: https://docs.docker.com/get-docker/"
fi

# Check Ollama
if command -v ollama &> /dev/null; then
    echo -e "${GREEN}✅ Ollama installed${NC}"
    
    if ollama list | grep -q "tinyllama"; then
        echo -e "${GREEN}✅ TinyLlama model available${NC}"
    else
        echo -e "${YELLOW}⚠️  TinyLlama model not found${NC}"
        read -p "   Install TinyLlama model? (y/n): " install_llama
        if [ "$install_llama" = "y" ]; then
            echo -e "${CYAN}   Pulling TinyLlama model...${NC}"
            ollama pull tinyllama
        fi
    fi
else
    echo -e "${YELLOW}⚠️  Ollama not found (optional - needed for LLM features)${NC}"
    echo -e "   Install: https://ollama.ai"
fi

# Step 5: Create IDE Configurations
echo -e "\n${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Step 5/6: IDE Setup${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

echo "Which IDE are you using?"
echo "  1) IntelliJ IDEA"
echo "  2) VS Code"
echo "  3) Eclipse"
echo "  4) Other/None"
read -p "Selection (1-4): " ide_choice

case $ide_choice in
    1)
        echo -e "\n${CYAN}IntelliJ IDEA Setup:${NC}"
        echo "1. File → Open → Select amcp-v1.5-opensource directory"
        echo "2. Trust the Maven project"
        echo "3. SDK: Project Structure → Set Java 21"
        echo "4. Run: examples → Right-click Demo class → Run"
        echo -e "${GREEN}✓ Ready to code!${NC}"
        ;;
    2)
        echo -e "\n${CYAN}VS Code Setup:${NC}"
        echo "1. Install 'Extension Pack for Java'"
        echo "2. Open amcp-v1.5-opensource folder"
        echo "3. Trust workspace"
        echo "4. Java: Configure Java Runtime → Set to Java 21"
        echo -e "${GREEN}✓ Ready to code!${NC}"
        ;;
    3)
        echo -e "\n${CYAN}Eclipse Setup:${NC}"
        echo "1. File → Import → Existing Maven Projects"
        echo "2. Select amcp-v1.5-opensource directory"
        echo "3. Right-click project → Maven → Update Project"
        echo -e "${GREEN}✓ Ready to code!${NC}"
        ;;
    4)
        echo -e "${YELLOW}No IDE-specific setup performed${NC}"
        ;;
esac

# Step 6: Quick Start Demo
echo -e "\n${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Step 6/6: Run Your First Demo${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

read -p "Would you like to run the Weather Agent demo now? (y/n): " run_demo

if [ "$run_demo" = "y" ]; then
    echo -e "\n${GREEN}🚀 Launching Weather Agent Demo...${NC}\n"
    ./run-weather-demo.sh
fi

# Summary
echo -e "\n${GREEN}"
cat << "EOF"
╔══════════════════════════════════════════════════════════════════════════════╗
║                                                                              ║
║                      🎉 Setup Complete! 🎉                                   ║
║                                                                              ║
╚══════════════════════════════════════════════════════════════════════════════╝
EOF
echo -e "${NC}"

echo -e "${CYAN}📚 What's Next?${NC}\n"
echo -e "${YELLOW}1. Try the demos:${NC}"
echo "   ./amcp-demos.sh                    # Interactive demo launcher"
echo "   ./run-weather-demo.sh              # Weather agent"
echo "   ./run-meshchat-demo.sh             # AI chat agent"
echo "   ./run-orchestrator-demo.sh         # LLM orchestration"
echo ""
echo -e "${YELLOW}2. Read the guides:${NC}"
echo "   cat QUICK_START.md                 # 5-minute quick start"
echo "   cat docs/DEVELOPER_GUIDE.md        # Complete tutorial"
echo "   cat docs/ARCHITECTURE.md           # System design"
echo ""
echo -e "${YELLOW}3. Build your first agent:${NC}"
echo "   Follow Tutorial 1 in docs/DEVELOPER_GUIDE.md"
echo ""
echo -e "${YELLOW}4. Join the community:${NC}"
echo "   GitHub: https://github.com/xaviercallens/amcp-v1.5-opensource"
echo "   Discussions: https://github.com/xaviercallens/amcp-v1.5-opensource/discussions"
echo ""
echo -e "${GREEN}Happy coding with AMCP! 🚀${NC}\n"
