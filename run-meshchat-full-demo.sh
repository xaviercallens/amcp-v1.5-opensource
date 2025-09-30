#!/bin/bash

# AMCP v1.5 Enterprise Edition - Comprehensive MeshChat Demo
# This script demonstrates the complete MeshChat ecosystem with human-to-AI conversation
# orchestration across multiple specialist agents (Travel, Stock, Chat) with TinyLlama integration

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
DEMO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CORE_JAR="$DEMO_DIR/core/target/amcp-core-1.5.0.jar"
EXAMPLES_JAR="$DEMO_DIR/examples/target/amcp-examples-1.5.0.jar"
CONNECTORS_JAR="$DEMO_DIR/connectors/target/amcp-connectors-1.5.0.jar"
MAIN_CLASS="io.amcp.examples.meshchat.MeshChatCLI"

# Demo scenarios
TRAVEL_SCENARIO="I'm planning a trip to Japan for 2 weeks in spring. I need help with itinerary planning, flight booking guidance, and local recommendations. My budget is around $3000."
STOCK_SCENARIO="I'm interested in investing in tech stocks. Can you analyze AAPL, GOOGL, and MSFT for me? I'm looking for both current performance and future outlook."
MULTI_AGENT_SCENARIO="I'm planning a business trip to Tokyo for a tech conference. I need to book flights, find accommodation near the venue, and I want to check if I should invest in any Japanese tech stocks while I'm there."

print_header() {
    echo -e "${CYAN}================================================================================================${NC}"
    echo -e "${CYAN}$1${NC}"
    echo -e "${CYAN}================================================================================================${NC}"
    echo
}

print_section() {
    echo -e "${YELLOW}--- $1 ---${NC}"
    echo
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

check_prerequisites() {
    print_section "Checking Prerequisites"
    
    # Check Java
    if ! command -v java &> /dev/null; then
        print_error "Java is not installed or not in PATH"
        exit 1
    fi
    print_success "Java found: $(java -version 2>&1 | head -n 1)"
    
    # Check if project is built
    if [ ! -f "$CORE_JAR" ]; then
        print_info "Core JAR not found. Building project..."
        mvn clean compile jar:jar -DskipTests -Dmaven.test.skip=true -q
        if [ $? -ne 0 ]; then
            print_error "Build failed"
            exit 1
        fi
    fi
    print_success "Project artifacts are ready"
    
    # Check Ollama (if available)
    if command -v ollama &> /dev/null; then
        print_success "Ollama found: $(ollama --version 2>/dev/null || echo 'version unknown')"
        
        # Check if TinyLlama model is available
        if ollama list | grep -q "tinyllama"; then
            print_success "TinyLlama model is available"
        else
            print_info "TinyLlama model not found. Demo will use simulated responses."
        fi
    else
        print_info "Ollama not found. Demo will use simulated LLM responses."
    fi
    
    echo
}

run_build_verification() {
    print_section "Build Verification"
    
    print_info "Compiling all Java sources..."
    mvn clean compile -q
    if [ $? -eq 0 ]; then
        print_success "All sources compiled successfully"
    else
        print_error "Compilation failed"
        exit 1
    fi
    
    print_info "Running quick validation tests..."
    mvn test -Dtest="*Test" -q
    if [ $? -eq 0 ]; then
        print_success "Basic tests passed"
    else
        print_info "Some tests failed, but continuing with demo..."
    fi
    
    echo
}

start_demo_environment() {
    print_section "Starting Demo Environment"
    
    # Create demo configuration
    cat > demo-session.properties << EOF
# AMCP MeshChat Demo Configuration
amcp.event.broker.type=memory
amcp.context.name=meshchat-demo
amcp.security.enabled=false
amcp.discovery.enabled=true
amcp.memory.enabled=true
amcp.memory.session.timeout=3600
amcp.orchestrator.llm.provider=ollama
amcp.orchestrator.llm.model=tinyllama
amcp.orchestrator.llm.endpoint=http://localhost:11434
demo.mode=true
demo.simulate.responses=true
EOF
    
    print_success "Demo configuration created"
    
    # Build classpath
    CLASSPATH="$CORE_JAR"
    if [ -f "$EXAMPLES_JAR" ]; then
        CLASSPATH="$CLASSPATH:$EXAMPLES_JAR"
    else
        CLASSPATH="$CLASSPATH:examples/target/classes"
    fi
    if [ -f "$CONNECTORS_JAR" ]; then
        CLASSPATH="$CLASSPATH:$CONNECTORS_JAR"
    else
        CLASSPATH="$CLASSPATH:connectors/target/classes"
    fi
    
    export AMCP_CLASSPATH="$CLASSPATH"
    export AMCP_CONFIG="demo-session.properties"
    
    print_success "Environment variables set"
    echo
}

run_interactive_demo() {
    print_header "AMCP v1.5 MeshChat - Interactive Demo Session"
    
    print_info "Starting MeshChat CLI with full agent ecosystem..."
    print_info "Available agents: MeshChat, Travel Planner, Stock Analysis, Enhanced Orchestrator"
    print_info "Features: Agent Discovery, Conversation Memory, LLM Integration"
    echo
    
    # Start the CLI
    java -cp "$AMCP_CLASSPATH" \
         -Damcp.config="$AMCP_CONFIG" \
         -Damcp.demo.mode=true \
         "$MAIN_CLASS" interactive
}

run_automated_scenarios() {
    print_header "AMCP v1.5 MeshChat - Automated Demo Scenarios"
    
    print_section "Scenario 1: Travel Planning with TinyLlama"
    echo -e "${PURPLE}User Query:${NC} $TRAVEL_SCENARIO"
    echo
    print_info "Demonstrating: MeshChat → Orchestrator → Travel Planner Agent"
    
    java -cp "$AMCP_CLASSPATH" \
         -Damcp.config="$AMCP_CONFIG" \
         -Damcp.demo.mode=true \
         "$MAIN_CLASS" scenario "travel" "$TRAVEL_SCENARIO"
    
    echo
    read -p "Press Enter to continue to next scenario..."
    echo
    
    print_section "Scenario 2: Stock Analysis with Multi-Agent Coordination"
    echo -e "${PURPLE}User Query:${NC} $STOCK_SCENARIO"
    echo
    print_info "Demonstrating: MeshChat → Orchestrator → Stock Agent"
    
    java -cp "$AMCP_CLASSPATH" \
         -Damcp.config="$AMCP_CONFIG" \
         -Damcp.demo.mode=true \
         "$MAIN_CLASS" scenario "stock" "$STOCK_SCENARIO"
    
    echo
    read -p "Press Enter to continue to next scenario..."
    echo
    
    print_section "Scenario 3: Complex Multi-Agent Orchestration"
    echo -e "${PURPLE}User Query:${NC} $MULTI_AGENT_SCENARIO"
    echo
    print_info "Demonstrating: MeshChat → Orchestrator → Travel + Stock Agents"
    
    java -cp "$AMCP_CLASSPATH" \
         -Damcp.config="$AMCP_CONFIG" \
         -Damcp.demo.mode=true \
         "$MAIN_CLASS" scenario "multi" "$MULTI_AGENT_SCENARIO"
    
    echo
}

show_agent_architecture() {
    print_header "AMCP v1.5 MeshChat - Agent Architecture Overview"
    
    cat << 'EOF'
                    ┌─────────────────────────────────────────────────────────────┐
                    │                    Human User                                │
                    └─────────────────────┬───────────────────────────────────────┘
                                          │ Natural Language Commands
                                          ▼
                    ┌─────────────────────────────────────────────────────────────┐
                    │                 MeshChatAgent                               │
                    │  • Human-to-AI Gateway                                     │
                    │  • Session Management                                       │
                    │  • Conversation Memory                                      │
                    └─────────────────────┬───────────────────────────────────────┘
                                          │ Event-Driven Messaging
                                          ▼
                    ┌─────────────────────────────────────────────────────────────┐
                    │             EnhancedOrchestratorAgent                       │
                    │  • TinyLlama/Ollama Integration                            │
                    │  • Intelligent Task Routing                                │
                    │  • Agent Discovery & Coordination                          │
                    └─────────────────────┬───────────────────────────────────────┘
                                          │ Orchestrated Delegation
                    ┌─────────────────────┼───────────────────────────────────────┐
                    ▼                     ▼                                       ▼
        ┌───────────────────────┐ ┌───────────────────────┐ ┌───────────────────────┐
        │   TravelPlannerAgent  │ │      StockAgent       │ │   (Future Agents)     │
        │  • Destination DB     │ │  • Market Analysis    │ │  • Weather            │
        │  • Itinerary Planning │ │  • Investment Advice  │ │  • News               │
        │  • Booking Guidance   │ │  • Portfolio Mgmt     │ │  • Calendar           │
        └───────────────────────┘ └───────────────────────┘ └───────────────────────┘

    Key Features:
    ✓ Event-Driven Architecture (AMCP v1.5)
    ✓ Strong Agent Mobility (IBM Aglet-style)
    ✓ LLM Integration (TinyLlama via Ollama)
    ✓ Dynamic Agent Discovery
    ✓ Persistent Conversation Memory
    ✓ Multi-Agent Orchestration
    ✓ CloudEvents Compliance
    ✓ Enterprise Security Suite
EOF
    
    echo
}

show_technical_highlights() {
    print_header "AMCP v1.5 MeshChat - Technical Highlights"
    
    cat << 'EOF'
    🔧 Core Framework:
    • AMCP v1.5 Enterprise Edition
    • Event-driven publish/subscribe messaging
    • Asynchronous CompletableFuture operations
    • MobileAgent interface with strong mobility
    • Agent lifecycle management (activate/deactivate/migrate)

    🤖 AI Integration:
    • TinyLlama model via Ollama integration
    • Intelligent task planning and routing
    • Natural language understanding for human queries
    • Context-aware response generation

    📡 Agent Communication:
    • Topic-based event routing (travel.**, stock.**, chat.**)
    • Agent discovery protocol with capability matching
    • Cross-agent orchestration and delegation
    • Message correlation and tracing

    💾 Memory & Persistence:
    • Conversation session management
    • Message history with search and filtering
    • Context extraction and summarization
    • Background cleanup and optimization

    🛡️ Enterprise Features:
    • Security manager with role-based access
    • CloudEvents compliance for interoperability
    • Multi-tenant support preparation
    • Kubernetes deployment ready

    📊 Observability:
    • Comprehensive logging with structured format
    • Metrics collection and monitoring hooks
    • Agent health checking and status reporting
    • Performance tracking and optimization
EOF
    
    echo
}

cleanup_demo() {
    print_section "Demo Cleanup"
    
    # Remove temporary files
    rm -f demo-session.properties
    
    print_success "Demo environment cleaned up"
    echo
}

show_usage() {
    cat << EOF
Usage: $0 [COMMAND]

Commands:
    interactive     Start interactive CLI session (default)
    scenarios       Run automated demo scenarios
    architecture    Show agent architecture diagram
    technical       Show technical highlights
    build           Build and verify project
    help           Show this help message

Examples:
    $0                    # Start interactive demo
    $0 interactive        # Same as above
    $0 scenarios          # Run automated scenarios
    $0 architecture       # Show architecture overview
EOF
}

main() {
    local command="${1:-interactive}"
    
    case "$command" in
        "interactive"|"")
            check_prerequisites
            start_demo_environment
            run_interactive_demo
            cleanup_demo
            ;;
        "scenarios")
            check_prerequisites
            start_demo_environment
            run_automated_scenarios
            cleanup_demo
            ;;
        "architecture")
            show_agent_architecture
            ;;
        "technical")
            show_technical_highlights
            ;;
        "build")
            check_prerequisites
            run_build_verification
            ;;
        "help"|"-h"|"--help")
            show_usage
            ;;
        *)
            print_error "Unknown command: $command"
            echo
            show_usage
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"