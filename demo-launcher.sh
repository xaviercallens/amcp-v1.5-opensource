#!/bin/bash

# AMCP v1.5 Open Source Edition - Demo Launcher
# Quick launcher for common demo scenarios

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m'

show_menu() {
    clear
    echo -e "${BLUE}"
    cat << "EOF"
    ██████╗ ███╗   ███╗ ██████╗██████╗     ██╗   ██╗ ██╗ ███████╗
    ██╔══██╗████╗ ████║██╔════╝██╔══██╗    ██║   ██║███║ ██╔════╝
    ███████║██╔████╔██║██║     ██████╔╝    ██║   ██║╚██║ ███████╗
    ██╔══██║██║╚██╔╝██║██║     ██╔═══╝     ╚██╗ ██╔╝ ██║ ╚════██║
    ██║  ██║██║ ╚═╝ ██║╚██████╗██║          ╚████╔╝ ███████╗███████║
    ╚═╝  ╚═╝╚═╝     ╚═╝ ╚═════╝╚═╝           ╚═══╝  ╚══════╝╚══════╝
    
    AMCP v1.5 Open Source Edition - Demo Launcher
    
EOF
    echo -e "${NC}"
    
    echo -e "${WHITE}🚀 Choose Your Demo Experience:${NC}\n"
    
    echo -e "${CYAN}1.${NC} ${GREEN}Full Enterprise Demo${NC} - Complete feature showcase (30+ minutes)"
    echo -e "${CYAN}2.${NC} ${GREEN}Quick Overview${NC} - Essential features only (10 minutes)"
    echo -e "${CYAN}3.${NC} ${GREEN}Security Focus${NC} - Advanced security suite demonstration"
    echo -e "${CYAN}4.${NC} ${GREEN}Mobility & Messaging${NC} - Agent mobility and Kafka integration"
    echo -e "${CYAN}5.${NC} ${GREEN}Integration Suite${NC} - A2A Bridge and MCP connectors"
    echo -e "${CYAN}6.${NC} ${GREEN}Application Demos${NC} - Travel Planner and Weather System"
    echo -e "${CYAN}7.${NC} ${GREEN}Performance Testing${NC} - Benchmarks and testing framework"
    echo -e "${CYAN}8.${NC} ${GREEN}CloudEvents & Standards${NC} - Compliance and standardization"
    echo -e "${CYAN}9.${NC} ${GREEN}Custom Configuration${NC} - Manual feature selection"
    echo ""
    echo -e "${CYAN}0.${NC} ${YELLOW}Exit${NC}"
    echo ""
    echo -e "${WHITE}Enter your choice (0-9):${NC} "
}

run_demo() {
    local cmd="$1"
    local desc="$2"
    
    echo -e "\n${BLUE}🚀 Starting: $desc${NC}\n"
    echo -e "${YELLOW}Running: $cmd${NC}\n"
    
    eval "$cmd"
    
    echo -e "\n${GREEN}✅ Demo completed successfully!${NC}"
    echo -e "${YELLOW}Press Enter to return to main menu...${NC}"
    read -r
}

main() {
    while true; do
        show_menu
        read -r choice
        
        case $choice in
            1)
                run_demo "./run-full-demo.sh" "Full Enterprise Demo - All Features"
                ;;
            2)
                run_demo "./run-full-demo.sh --auto -d 5 --no-testing --no-performance" "Quick Overview - Essential Features"
                ;;
            3)
                run_demo "./run-full-demo.sh --no-mobility --no-kafka --no-a2a --no-mcp --no-travel --no-weather --no-testing --no-performance" "Security Focus - Advanced Security Suite"
                ;;
            4)
                run_demo "./run-full-demo.sh --no-security --no-a2a --no-mcp --no-cloudevents --no-travel --no-weather --no-testing --no-performance" "Mobility & Messaging - Core Infrastructure"
                ;;
            5)
                run_demo "./run-full-demo.sh --no-security --no-mobility --no-kafka --no-cloudevents --no-travel --no-weather --no-testing --no-performance" "Integration Suite - A2A and MCP"
                ;;
            6)
                run_demo "./run-full-demo.sh --no-security --no-mobility --no-kafka --no-a2a --no-mcp --no-cloudevents --no-testing --no-performance" "Application Demos - Travel and Weather"
                ;;
            7)
                run_demo "./run-full-demo.sh --no-security --no-mobility --no-kafka --no-a2a --no-mcp --no-cloudevents --no-travel --no-weather" "Performance Testing - Benchmarks and Framework"
                ;;
            8)
                run_demo "./run-full-demo.sh --no-security --no-mobility --no-kafka --no-a2a --no-mcp --no-travel --no-weather --no-testing --no-performance" "CloudEvents & Standards - Compliance Testing"
                ;;
            9)
                echo -e "\n${WHITE}Custom Configuration:${NC}"
                echo -e "${CYAN}Available flags:${NC}"
                echo "  --no-security --no-mobility --no-kafka --no-a2a"
                echo "  --no-mcp --no-cloudevents --no-travel --no-weather"
                echo "  --no-testing --no-performance"
                echo "  --auto -v -d <seconds>"
                echo ""
                echo -e "${WHITE}Enter your custom command:${NC}"
                echo -n "./run-full-demo.sh "
                read -r custom_args
                run_demo "./run-full-demo.sh $custom_args" "Custom Demo Configuration"
                ;;
            0)
                echo -e "\n${GREEN}Thank you for exploring AMCP v1.5 Open Source Edition!${NC}"
                echo -e "${CYAN}For more information, see DEMO_GUIDE.md${NC}\n"
                exit 0
                ;;
            *)
                echo -e "\n${YELLOW}Invalid choice. Please select 0-9.${NC}"
                sleep 2
                ;;
        esac
    done
}

# Check if we're in the right directory
if [[ ! -f "run-full-demo.sh" ]]; then
    echo -e "${RED}Error: run-full-demo.sh not found${NC}"
    echo "Please run this script from the AMCP project root directory"
    exit 1
fi

main