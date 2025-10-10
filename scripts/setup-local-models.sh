#!/bin/bash

# AMCP Local Model Setup Script v1.5.1
# Automated setup for Qwen2.5:0.5b, Gemma 2B, and other lightweight models

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Print header
print_header() {
    echo -e "${BOLD}${BLUE}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BOLD}${BLUE}║                AMCP Local Model Setup v1.5.1                ║${NC}"
    echo -e "${BOLD}${BLUE}║            Automated Ollama & Model Installation            ║${NC}"
    echo -e "${BOLD}${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo
}

# Print step
print_step() {
    echo -e "${BOLD}${CYAN}[STEP] $1${NC}"
}

# Print success
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# Print warning
print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# Print error
print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check system resources
check_system_resources() {
    print_step "Checking system resources..."
    
    # Get available memory in GB
    if command_exists free; then
        TOTAL_RAM_KB=$(free | grep '^Mem:' | awk '{print $2}')
        TOTAL_RAM_GB=$((TOTAL_RAM_KB / 1024 / 1024))
        echo "Available RAM: ${TOTAL_RAM_GB}GB"
    else
        print_warning "Cannot determine RAM size on this system"
        TOTAL_RAM_GB=4  # Default assumption
    fi
    
    # Check disk space
    AVAILABLE_SPACE=$(df -BG . | tail -1 | awk '{print $4}' | sed 's/G//')
    echo "Available disk space: ${AVAILABLE_SPACE}GB"
    
    if [ "$AVAILABLE_SPACE" -lt 10 ]; then
        print_warning "Low disk space. Recommend at least 10GB for models."
    fi
    
    print_success "System resource check completed"
    echo
}

# Install Ollama
install_ollama() {
    print_step "Installing Ollama..."
    
    if command_exists ollama; then
        print_success "Ollama already installed"
        ollama --version
    else
        echo "Downloading and installing Ollama..."
        curl -fsSL https://ollama.com/install.sh | sh
        
        if command_exists ollama; then
            print_success "Ollama installed successfully"
        else
            print_error "Failed to install Ollama"
            exit 1
        fi
    fi
    echo
}

# Start Ollama service
start_ollama() {
    print_step "Starting Ollama service..."
    
    # Check if Ollama is already running
    if pgrep -x "ollama" > /dev/null; then
        print_success "Ollama service is already running"
    else
        echo "Starting Ollama service..."
        ollama serve &
        sleep 3
        
        if pgrep -x "ollama" > /dev/null; then
            print_success "Ollama service started"
        else
            print_warning "Ollama service may not have started properly"
        fi
    fi
    echo
}

# Install models based on system resources
install_models() {
    print_step "Installing recommended models based on system resources..."
    
    # Always install the new ultra-lightweight model
    echo -e "${BOLD}Installing NEW Qwen2.5:0.5b (Ultra-minimal - 0.4GB RAM)...${NC}"
    if ollama pull qwen2.5:0.5b; then
        print_success "Qwen2.5:0.5b installed successfully"
    else
        print_error "Failed to install Qwen2.5:0.5b"
    fi
    
    # Install based on available RAM
    if [ "$TOTAL_RAM_GB" -ge 2 ]; then
        echo -e "${BOLD}Installing Gemma 2B (Recommended - 1.4GB RAM)...${NC}"
        if ollama pull gemma:2b; then
            print_success "Gemma 2B installed successfully"
        else
            print_error "Failed to install Gemma 2B"
        fi
    fi
    
    if [ "$TOTAL_RAM_GB" -ge 1 ]; then
        echo -e "${BOLD}Installing Qwen2 1.5B (Ultra-lightweight - 0.9GB RAM)...${NC}"
        if ollama pull qwen2:1.5b; then
            print_success "Qwen2 1.5B installed successfully"
        else
            print_error "Failed to install Qwen2 1.5B"
        fi
    fi
    
    # Install larger models for systems with more RAM
    if [ "$TOTAL_RAM_GB" -ge 6 ]; then
        echo -e "${BOLD}Installing Qwen2 7B (Large context - 4.4GB RAM)...${NC}"
        if ollama pull qwen2:7b; then
            print_success "Qwen2 7B installed successfully"
        else
            print_warning "Failed to install Qwen2 7B (optional)"
        fi
    fi
    
    echo
}

# Test installed models
test_models() {
    print_step "Testing installed models..."
    
    echo "Available models:"
    ollama list
    echo
    
    # Test Qwen2.5:0.5b specifically
    echo -e "${BOLD}Testing NEW Qwen2.5:0.5b...${NC}"
    if echo "What is AI?" | ollama run qwen2.5:0.5b --timeout 30s >/dev/null 2>&1; then
        print_success "Qwen2.5:0.5b is working correctly"
    else
        print_warning "Qwen2.5:0.5b test failed or timed out"
    fi
    
    # Test other models if available
    if ollama list | grep -q "gemma:2b"; then
        echo -e "${BOLD}Testing Gemma 2B...${NC}"
        if echo "Hello" | ollama run gemma:2b --timeout 30s >/dev/null 2>&1; then
            print_success "Gemma 2B is working correctly"
        else
            print_warning "Gemma 2B test failed or timed out"
        fi
    fi
    
    echo
}

# Setup environment variables
setup_environment() {
    print_step "Setting up environment variables..."
    
    # Create environment configuration
    cat > ~/.amcp-env << EOF
# AMCP LLM Configuration
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_MODEL=qwen2.5:0.5b
export AMCP_GPU_LAYERS=auto
export AMCP_GPU_VRAM_MB=auto
export AMCP_LLM_TIMEOUT=120
export AMCP_CACHE_ENABLED=true
export AMCP_CACHE_TTL_HOURS=24
EOF
    
    print_success "Environment configuration created at ~/.amcp-env"
    echo "To load: source ~/.amcp-env"
    echo
}

# Create test script
create_test_script() {
    print_step "Creating test script..."
    
    cat > ~/test-amcp-models.sh << 'EOF'
#!/bin/bash

# Quick AMCP Model Test Script
echo "AMCP Model Quick Test"
echo "===================="

# Load environment if available
if [ -f ~/.amcp-env ]; then
    source ~/.amcp-env
fi

# Test Qwen2.5:0.5b
echo "Testing Qwen2.5:0.5b (NEW ultra-minimal model)..."
echo "What is artificial intelligence?" | ollama run qwen2.5:0.5b

echo -e "\n--- Test completed ---"
echo "For comprehensive testing, run the Java LocalModelTester:"
echo "cd /path/to/amcp && mvn exec:java -Dexec.mainClass=\"io.amcp.connectors.ai.terminal.LocalModelTester\""
EOF
    
    chmod +x ~/test-amcp-models.sh
    print_success "Test script created at ~/test-amcp-models.sh"
    echo
}

# Print usage instructions
print_usage() {
    print_step "Setup completed! Usage instructions:"
    
    echo -e "${BOLD}Quick Start:${NC}"
    echo "1. Load environment: source ~/.amcp-env"
    echo "2. Run quick test: ~/test-amcp-models.sh"
    echo "3. Run comprehensive tester:"
    echo "   cd $(pwd)"
    echo "   mvn exec:java -Dexec.mainClass=\"io.amcp.connectors.ai.terminal.LocalModelTester\""
    echo
    
    echo -e "${BOLD}Model Recommendations for your system (${TOTAL_RAM_GB}GB RAM):${NC}"
    if [ "$TOTAL_RAM_GB" -lt 1 ]; then
        echo "• Qwen2.5:0.5b (NEW) - Ultra-minimal, only 0.4GB RAM"
    elif [ "$TOTAL_RAM_GB" -lt 2 ]; then
        echo "• Qwen2.5:0.5b (NEW) - Ultra-minimal, only 0.4GB RAM"
        echo "• Qwen2 1.5B - Ultra-lightweight, 0.9GB RAM"
    elif [ "$TOTAL_RAM_GB" -lt 6 ]; then
        echo "• Gemma 2B (RECOMMENDED) - Fast and efficient, 1.4GB RAM"
        echo "• Qwen2.5:0.5b (NEW) - For minimal usage, 0.4GB RAM"
    else
        echo "• Gemma 2B (RECOMMENDED) - Fast and efficient, 1.4GB RAM"
        echo "• Qwen2 7B - Large context, multilingual, 4.4GB RAM"
        echo "• Qwen2.5:0.5b (NEW) - Ultra-fast responses, 0.4GB RAM"
    fi
    echo
    
    echo -e "${BOLD}Manual Commands:${NC}"
    echo "• List models: ollama list"
    echo "• Pull model: ollama pull qwen2.5:0.5b"
    echo "• Test model: echo 'Hello' | ollama run qwen2.5:0.5b"
    echo "• Remove model: ollama rm qwen2.5:0.5b"
    echo
    
    print_success "AMCP Local Model Setup completed successfully!"
}

# Main execution
main() {
    print_header
    
    # Check if running as root (not recommended for Ollama)
    if [ "$EUID" -eq 0 ]; then
        print_warning "Running as root. Ollama is typically run as a regular user."
        read -p "Continue anyway? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
    
    check_system_resources
    install_ollama
    start_ollama
    install_models
    test_models
    setup_environment
    create_test_script
    print_usage
}

# Run main function
main "$@"
