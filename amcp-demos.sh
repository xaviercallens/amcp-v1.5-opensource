#!/bin/bash
# AMCP v1.5 Open Source Edition - Interactive Demo Launcher
# This script provides an easy way to explore all AMCP features

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Banner
clear
echo -e "${CYAN}"
cat << "EOF"
╔══════════════════════════════════════════════════════════════════════════════╗
║                                                                              ║
║                  🚀 AMCP v1.5 Open Source Edition 🚀                        ║
║                     Interactive Demo Launcher                                ║
║                                                                              ║
╚══════════════════════════════════════════════════════════════════════════════╝
EOF
echo -e "${NC}"

# Check Java 21
echo -e "${YELLOW}🔍 Checking prerequisites...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java not found. Please install Java 21+${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1}')
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo -e "${RED}❌ Java 21+ required. Found version: $JAVA_VERSION${NC}"
    echo -e "${YELLOW}💡 Run: ./setup-java21.sh${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Java $JAVA_VERSION detected${NC}"

# Check if compiled
if [ ! -d "core/target" ] || [ ! -d "connectors/target" ]; then
    echo -e "${YELLOW}⚠️  Project not compiled yet${NC}"
    echo -e "${BLUE}🔨 Building AMCP (this may take a minute)...${NC}"
    mvn clean compile -DskipTests -q
    echo -e "${GREEN}✅ Build complete${NC}"
fi

# Function to run a demo
run_demo() {
    local demo_script=$1
    local demo_name=$2
    
    echo ""
    echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}▶️  Starting: $demo_name${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
    echo ""
    
    # Check in both old and new locations for backward compatibility
    if [ -f "$demo_script" ]; then
        bash "$demo_script"
    elif [ -f "scripts/demos/$(basename $demo_script)" ]; then
        bash "scripts/demos/$(basename $demo_script)"
    else
        echo -e "${RED}❌ Demo script not found: $demo_script${NC}"
        read -p "Press Enter to continue..."
    fi
}

# Function to show demo info
show_demo_info() {
    local demo_num=$1
    case $demo_num in
        1)
            echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
            echo -e "${GREEN}📦 Demo 1: Weather Agent (Beginner)${NC}"
            echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
            echo ""
            echo -e "${CYAN}What you'll learn:${NC}"
            echo "  • Creating a simple agent"
            echo "  • Topic subscriptions and pattern matching"
            echo "  • Event handling basics"
            echo "  • External API integration"
            echo ""
            echo -e "${CYAN}What you'll see:${NC}"
            echo "  • Real-time weather data for any city"
            echo "  • Event-driven pub/sub messaging"
            echo "  • Simple CLI interaction"
            echo ""
            echo -e "${CYAN}Try these commands:${NC}"
            echo "  weather Tokyo"
            echo "  weather \"New York\""
            echo "  forecast Paris"
            echo ""
            ;;
        2)
            echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
            echo -e "${GREEN}💬 Demo 2: MeshChat AI (Intermediate)${NC}"
            echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
            echo ""
            echo -e "${CYAN}What you'll learn:${NC}"
            echo "  • LLM integration with Ollama/TinyLlama"
            echo "  • Context management across conversations"
            echo "  • Multi-turn dialogue handling"
            echo "  • Agent state persistence"
            echo ""
            echo -e "${CYAN}What you'll see:${NC}"
            echo "  • Natural language conversations"
            echo "  • Context-aware responses"
            echo "  • Session management"
            echo ""
            echo -e "${CYAN}Try asking:${NC}"
            echo "  > Tell me about Paris"
            echo "  > What's the weather like there?"
            echo "  > Plan a 3-day itinerary"
            echo ""
            ;;
        3)
            echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
            echo -e "${GREEN}🧠 Demo 3: LLM Orchestration (Advanced)${NC}"
            echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
            echo ""
            echo -e "${CYAN}What you'll learn:${NC}"
            echo "  • LLM-powered task planning"
            echo "  • Dynamic agent discovery"
            echo "  • Parallel task execution"
            echo "  • Result aggregation with correlation IDs"
            echo ""
            echo -e "${CYAN}What you'll see:${NC}"
            echo "  • Intelligent request decomposition"
            echo "  • Multi-agent coordination"
            echo "  • CloudEvents v1.0 compliance"
            echo ""
            echo -e "${CYAN}Try complex requests:${NC}"
            echo "  > Research weather in Tokyo and Paris, recommend best travel time"
            echo "  > Analyze weather patterns and suggest outdoor activities"
            echo ""
            echo -e "${YELLOW}⚠️  Requires: Ollama with TinyLlama model${NC}"
            ;;
        4)
            echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
            echo -e "${GREEN}🎛️  Demo 4: Interactive CLI (Full Power)${NC}"
            echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
            echo ""
            echo -e "${CYAN}What you'll learn:${NC}"
            echo "  • Full CLI command suite"
            echo "  • Session management and history"
            echo "  • Debugging and troubleshooting tools"
            echo "  • Multi-agent system monitoring"
            echo ""
            echo -e "${CYAN}Available commands:${NC}"
            echo "  agents              - List all agents"
            echo "  activate <agent>    - Start an agent"
            echo "  ask <agent> <query> - Query an agent"
            echo "  status              - System health"
            echo "  help                - Full command list"
            echo ""
            ;;
    esac
}

# Main menu
show_main_menu() {
    echo ""
    echo -e "${PURPLE}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${PURPLE}║              Choose Your AMCP Learning Path                   ║${NC}"
    echo -e "${PURPLE}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${GREEN}🎓 Beginner Track:${NC}"
    echo "  1) Weather Agent Demo          - Learn agent basics (5 min)"
    echo ""
    echo -e "${YELLOW}🚀 Intermediate Track:${NC}"
    echo "  2) MeshChat AI Demo            - LLM integration (10 min)"
    echo ""
    echo -e "${RED}🔥 Advanced Track:${NC}"
    echo "  3) LLM Orchestration Demo      - Multi-agent coordination (15 min)"
    echo ""
    echo -e "${CYAN}💻 Power Users:${NC}"
    echo "  4) Interactive CLI             - Full feature access"
    echo ""
    echo -e "${BLUE}📚 Additional Options:${NC}"
    echo "  5) View Quick Start Guide"
    echo "  6) Check System Status"
    echo "  7) Build/Rebuild Project"
    echo ""
    echo "  0) Exit"
    echo ""
    echo -e -n "${PURPLE}Select option (0-7): ${NC}"
}

# Quick Start Guide
show_quick_start() {
    echo ""
    echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}📖 AMCP Quick Start Guide${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
    echo ""
    echo -e "${YELLOW}1. Understanding Agents:${NC}"
    echo "   Agents are autonomous, event-driven components that:"
    echo "   • Subscribe to topics (e.g., 'weather.**')"
    echo "   • Handle events asynchronously"
    echo "   • Can move across contexts (dispatch/migrate)"
    echo ""
    echo -e "${YELLOW}2. Event-Driven Messaging:${NC}"
    echo "   Agents communicate via publish/subscribe:"
    echo "   • Publisher: agent.publish('topic', data)"
    echo "   • Subscriber: agent.subscribe('topic.*')"
    echo "   • No tight coupling between agents"
    echo ""
    echo -e "${YELLOW}3. Agent Mobility:${NC}"
    echo "   Unique AMCP feature - agents can move:"
    echo "   • dispatch(context) - Move to another server"
    echo "   • clone(context)    - Create a copy elsewhere"
    echo "   • migrate(options)  - Intelligent relocation"
    echo ""
    echo -e "${YELLOW}4. LLM Integration:${NC}"
    echo "   Built-in AI orchestration:"
    echo "   • TinyLlama for task planning"
    echo "   • Dynamic agent discovery"
    echo "   • Parallel execution"
    echo ""
    echo -e "${YELLOW}5. Next Steps:${NC}"
    echo "   • Run demos 1-3 in sequence"
    echo "   • Read: docs/DEVELOPER_GUIDE.md"
    echo "   • Build your first agent"
    echo ""
    read -p "Press Enter to continue..."
}

# System Status
check_system_status() {
    echo ""
    echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}🔍 System Status Check${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
    echo ""
    
    # Java version
    echo -e "${YELLOW}☕ Java:${NC}"
    java -version 2>&1 | head -1
    echo ""
    
    # Maven version
    echo -e "${YELLOW}📦 Maven:${NC}"
    mvn -version | head -1
    echo ""
    
    # Build status
    echo -e "${YELLOW}🔨 Build Status:${NC}"
    if [ -d "core/target/classes" ]; then
        echo -e "  ${GREEN}✅ core module compiled${NC}"
    else
        echo -e "  ${RED}❌ core module needs compilation${NC}"
    fi
    
    if [ -d "connectors/target/classes" ]; then
        echo -e "  ${GREEN}✅ connectors module compiled${NC}"
    else
        echo -e "  ${RED}❌ connectors module needs compilation${NC}"
    fi
    
    if [ -d "examples/target/classes" ]; then
        echo -e "  ${GREEN}✅ examples module compiled${NC}"
    else
        echo -e "  ${RED}❌ examples module needs compilation${NC}"
    fi
    
    if [ -d "cli/target/classes" ]; then
        echo -e "  ${GREEN}✅ cli module compiled${NC}"
    else
        echo -e "  ${RED}❌ cli module needs compilation${NC}"
    fi
    echo ""
    
    # Ollama check (optional)
    echo -e "${YELLOW}🤖 Ollama (Optional for LLM demos):${NC}"
    if command -v ollama &> /dev/null; then
        echo -e "  ${GREEN}✅ Ollama installed${NC}"
        if ollama list | grep -q "tinyllama"; then
            echo -e "  ${GREEN}✅ TinyLlama model available${NC}"
        else
            echo -e "  ${YELLOW}⚠️  TinyLlama model not found${NC}"
            echo -e "  ${BLUE}💡 Install: ollama pull tinyllama${NC}"
        fi
    else
        echo -e "  ${YELLOW}⚠️  Ollama not installed (needed for advanced demos)${NC}"
        echo -e "  ${BLUE}💡 Install: https://ollama.ai${NC}"
    fi
    echo ""
    
    read -p "Press Enter to continue..."
}

# Build/Rebuild
build_project() {
    echo ""
    echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}🔨 Building AMCP Project${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
    echo ""
    
    echo -e "${YELLOW}Building with tests...${NC}"
    mvn clean install
    
    if [ $? -eq 0 ]; then
        echo ""
        echo -e "${GREEN}✅ Build successful!${NC}"
    else
        echo ""
        echo -e "${RED}❌ Build failed. Check output above.${NC}"
    fi
    echo ""
    read -p "Press Enter to continue..."
}

# Main loop
while true; do
    show_main_menu
    read -r choice
    
    case $choice in
        1)
            show_demo_info 1
            read -p "Press Enter to start demo, or 'b' to go back: " confirm
            if [ "$confirm" != "b" ]; then
                run_demo "scripts/demos/run-weather-demo.sh" "Weather Agent Demo"
            fi
            ;;
        2)
            show_demo_info 2
            read -p "Press Enter to start demo, or 'b' to go back: " confirm
            if [ "$confirm" != "b" ]; then
                run_demo "scripts/demos/run-meshchat-demo.sh" "MeshChat AI Demo"
            fi
            ;;
        3)
            show_demo_info 3
            read -p "Press Enter to start demo, or 'b' to go back: " confirm
            if [ "$confirm" != "b" ]; then
                run_demo "scripts/demos/run-orchestrator-demo.sh" "LLM Orchestration Demo"
            fi
            ;;
        4)
            show_demo_info 4
            read -p "Press Enter to start CLI, or 'b' to go back: " confirm
            if [ "$confirm" != "b" ]; then
                run_demo "scripts/demos/run-meshchat-cli.sh" "Interactive CLI"
            fi
            ;;
        5)
            show_quick_start
            ;;
        6)
            check_system_status
            ;;
        7)
            build_project
            ;;
        0)
            echo ""
            echo -e "${GREEN}Thanks for exploring AMCP! 🚀${NC}"
            echo -e "${CYAN}Next steps:${NC}"
            echo "  • Check out docs/DEVELOPER_GUIDE.md"
            echo "  • Build your first agent"
            echo "  • Join our community: github.com/xaviercallens/amcp-v1.5-opensource"
            echo ""
            exit 0
            ;;
        *)
            echo -e "${RED}Invalid option. Please try again.${NC}"
            sleep 1
            ;;
    esac
done
