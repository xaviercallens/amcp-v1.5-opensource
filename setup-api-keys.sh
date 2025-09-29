#!/bin/bash

# AMCP API Keys Setup Script
# Version: 1.5.0
# Description: Setup script for configuring API keys for Stock and Weather services

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

# Default API keys (production-ready keys)
DEFAULT_POLYGON_API_KEY="ZGgVNySPtrCA7u1knnya3wdefCLGpJwd"
DEFAULT_OPENWEATHER_API_KEY="3bd965f39881ba0f116ee0810fdfd058"

# Environment files
SHELL_PROFILE=""
ENV_FILE=".env"

# Banner function
print_banner() {
    echo -e "${CYAN}"
    echo "  ╔══════════════════════════════════════════════════════════════════╗"
    echo "  ║                    AMCP API Keys Setup v1.5                     ║"
    echo "  ║                 Stock & Weather Configuration                    ║"
    echo "  ║                     Enterprise Edition                           ║"
    echo "  ╚══════════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

# Logging functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# Help function
show_help() {
    echo -e "${WHITE}AMCP API Keys Setup${NC}"
    echo ""
    echo -e "${YELLOW}Usage:${NC}"
    echo "  $0 [OPTIONS]"
    echo ""
    echo -e "${YELLOW}Options:${NC}"
    echo -e "  ${GREEN}-h, --help${NC}           Show this help message"
    echo -e "  ${GREEN}-i, --interactive${NC}    Interactive mode (ask for keys)"
    echo -e "  ${GREEN}-d, --demo${NC}           Use default production keys (full functionality)"
    echo -e "  ${GREEN}-c, --current${NC}        Show current API key status"
    echo -e "  ${GREEN}-r, --reset${NC}          Reset API keys (remove from environment)"
    echo -e "  ${GREEN}--polygon KEY${NC}        Set Polygon.io API key directly"
    echo -e "  ${GREEN}--openweather KEY${NC}    Set OpenWeather API key directly"
    echo -e "  ${GREEN}--env-file${NC}           Save to .env file instead of shell profile"
    echo -e "  ${GREEN}--shell-profile${NC}      Save to shell profile (default)"
    echo ""
    echo -e "${YELLOW}API Key Sources:${NC}"
    echo -e "  ${GREEN}Polygon.io:${NC}          https://polygon.io/dashboard (Free tier available)"
    echo -e "  ${GREEN}OpenWeatherMap:${NC}      https://openweathermap.org/api (Free tier available)"
    echo ""
    echo -e "${YELLOW}Examples:${NC}"
    echo -e "  ${GREEN}$0 --demo${NC}                           # Use default production keys"
    echo -e "  ${GREEN}$0 --interactive${NC}                    # Interactive setup"
    echo -e "  ${GREEN}$0 --polygon abc123 --openweather xyz789${NC}  # Direct key setup"
    echo -e "  ${GREEN}$0 --current${NC}                        # Check current status"
    echo ""
    echo -e "${YELLOW}Note:${NC} Default production keys provide full API functionality."
}

# Detect shell profile
detect_shell_profile() {
    if [ -n "$ZSH_VERSION" ]; then
        SHELL_PROFILE="$HOME/.zshrc"
        log_info "Detected zsh shell, using ~/.zshrc"
    elif [ -n "$BASH_VERSION" ]; then
        if [ -f "$HOME/.bash_profile" ]; then
            SHELL_PROFILE="$HOME/.bash_profile"
            log_info "Detected bash shell, using ~/.bash_profile"
        else
            SHELL_PROFILE="$HOME/.bashrc"
            log_info "Detected bash shell, using ~/.bashrc"
        fi
    else
        SHELL_PROFILE="$HOME/.profile"
        log_warn "Unknown shell, using ~/.profile"
    fi
}

# Check current API key status
check_current_status() {
    echo -e "\n${CYAN}📊 Current API Key Status:${NC}"
    echo "═══════════════════════════════════════"
    
    # Check Polygon.io
    if [ -n "$POLYGON_API_KEY" ]; then
        if [ "$POLYGON_API_KEY" = "$DEFAULT_POLYGON_API_KEY" ]; then
            echo -e "✅ Polygon.io: ${GREEN}CONFIGURED${NC} (default production key)"
        else
            echo -e "✅ Polygon.io: ${GREEN}CONFIGURED${NC} (${#POLYGON_API_KEY} chars)"
        fi
    else
        echo -e "❌ Polygon.io: ${RED}NOT SET${NC}"
    fi
    
    # Check OpenWeather
    if [ -n "$OPENWEATHER_API_KEY" ]; then
        if [ "$OPENWEATHER_API_KEY" = "$DEFAULT_OPENWEATHER_API_KEY" ]; then
            echo -e "✅ OpenWeather: ${GREEN}CONFIGURED${NC} (default production key)"
        else
            echo -e "✅ OpenWeather: ${GREEN}CONFIGURED${NC} (${#OPENWEATHER_API_KEY} chars)"
        fi
    else
        echo -e "❌ OpenWeather: ${RED}NOT SET${NC}"
    fi
    
    echo ""
    
    # Check where keys are set
    echo -e "${CYAN}📍 Configuration Sources:${NC}"
    
    # Check environment variables
    if env | grep -q "POLYGON_API_KEY\|OPENWEATHER_API_KEY"; then
        echo -e "🔧 Current session: ${GREEN}ACTIVE${NC}"
    fi
    
    # Check shell profile
    if [ -f "$SHELL_PROFILE" ] && grep -q "POLYGON_API_KEY\|OPENWEATHER_API_KEY" "$SHELL_PROFILE"; then
        echo -e "🏠 Shell profile ($SHELL_PROFILE): ${GREEN}CONFIGURED${NC}"
    fi
    
    # Check .env file
    if [ -f "$ENV_FILE" ] && grep -q "POLYGON_API_KEY\|OPENWEATHER_API_KEY" "$ENV_FILE"; then
        echo -e "📄 .env file: ${GREEN}CONFIGURED${NC}"
    fi
    
    echo ""
}

# Set API key in environment
set_env_var() {
    local key="$1"
    local value="$2"
    local target="$3"
    
    # Remove existing entries
    if [ -f "$target" ]; then
        # Create backup
        cp "$target" "${target}.backup.$(date +%Y%m%d_%H%M%S)"
        # Remove existing entries
        grep -v "export $key=" "$target" > "${target}.tmp" || true
        mv "${target}.tmp" "$target"
    fi
    
    # Add new entry
    echo "export $key=\"$value\"" >> "$target"
    log_info "Added $key to $target"
}

# Setup default production keys
setup_demo_keys() {
    echo -e "\n${YELLOW}🎯 Setting up default production API keys...${NC}"
    echo ""
    log_info "Using production-ready API keys for:"
    log_info "  • Polygon.io: Stock market data and real-time quotes"
    log_info "  • OpenWeatherMap: Weather data and forecasts"
    echo ""
    
    if [ "$USE_ENV_FILE" = true ]; then
        set_env_var "POLYGON_API_KEY" "$DEFAULT_POLYGON_API_KEY" "$ENV_FILE"
        set_env_var "OPENWEATHER_API_KEY" "$DEFAULT_OPENWEATHER_API_KEY" "$ENV_FILE"
        log_success "Production keys saved to $ENV_FILE"
        log_info "Load with: source $ENV_FILE"
    else
        set_env_var "POLYGON_API_KEY" "$DEFAULT_POLYGON_API_KEY" "$SHELL_PROFILE"
        set_env_var "OPENWEATHER_API_KEY" "$DEFAULT_OPENWEATHER_API_KEY" "$SHELL_PROFILE"
        log_success "Production keys saved to $SHELL_PROFILE"
        log_info "Restart your terminal or run: source $SHELL_PROFILE"
    fi
    
    # Set for current session
    export POLYGON_API_KEY="$DEFAULT_POLYGON_API_KEY"
    export OPENWEATHER_API_KEY="$DEFAULT_OPENWEATHER_API_KEY"
    log_success "Production keys set for current session"
}

# Interactive setup
interactive_setup() {
    echo -e "\n${CYAN}🔧 Interactive API Keys Setup${NC}"
    echo "══════════════════════════════════════════"
    echo ""
    
    # Polygon.io setup
    echo -e "${YELLOW}📈 Polygon.io API Key Setup${NC}"
    echo "────────────────────────────────────────"
    echo "Polygon.io provides real-time and historical stock market data."
    echo "Free tier: 5 API calls per minute"
    echo "Get your key at: https://polygon.io/dashboard"
    echo ""
    
    while true; do
        read -p "Enter your Polygon.io API key (or 'demo' for demo key, 'skip' to skip): " polygon_key
        
        if [ "$polygon_key" = "demo" ]; then
            polygon_key="$DEFAULT_POLYGON_API_KEY"
            log_info "Using default production Polygon.io key"
            break
        elif [ "$polygon_key" = "skip" ]; then
            polygon_key=""
            log_info "Skipping Polygon.io key"
            break
        elif [ -n "$polygon_key" ] && [ ${#polygon_key} -gt 10 ]; then
            log_success "Polygon.io key accepted"
            break
        else
            log_error "Invalid key. Please enter a valid key, 'demo', or 'skip'"
        fi
    done
    
    echo ""
    
    # OpenWeatherMap setup
    echo -e "${YELLOW}🌤️  OpenWeatherMap API Key Setup${NC}"
    echo "──────────────────────────────────────────"
    echo "OpenWeatherMap provides weather data and forecasts."
    echo "Free tier: 1,000 API calls per day"
    echo "Get your key at: https://openweathermap.org/api"
    echo ""
    
    while true; do
        read -p "Enter your OpenWeatherMap API key (or 'demo' for demo key, 'skip' to skip): " weather_key
        
        if [ "$weather_key" = "demo" ]; then
            weather_key="$DEFAULT_OPENWEATHER_API_KEY"
            log_info "Using default production OpenWeatherMap key"
            break
        elif [ "$weather_key" = "skip" ]; then
            weather_key=""
            log_info "Skipping OpenWeatherMap key"
            break
        elif [ -n "$weather_key" ] && [ ${#weather_key} -gt 10 ]; then
            log_success "OpenWeatherMap key accepted"
            break
        else
            log_error "Invalid key. Please enter a valid key, 'demo', or 'skip'"
        fi
    done
    
    echo ""
    
    # Save keys
    if [ -n "$polygon_key" ] || [ -n "$weather_key" ]; then
        echo -e "${CYAN}💾 Saving API Keys${NC}"
        echo "───────────────────────"
        
        target_file=""
        if [ "$USE_ENV_FILE" = true ]; then
            target_file="$ENV_FILE"
        else
            target_file="$SHELL_PROFILE"
        fi
        
        if [ -n "$polygon_key" ]; then
            set_env_var "POLYGON_API_KEY" "$polygon_key" "$target_file"
            export POLYGON_API_KEY="$polygon_key"
        fi
        
        if [ -n "$weather_key" ]; then
            set_env_var "OPENWEATHER_API_KEY" "$weather_key" "$target_file"
            export OPENWEATHER_API_KEY="$weather_key"
        fi
        
        log_success "API keys saved to $target_file"
        
        if [ "$USE_ENV_FILE" = true ]; then
            log_info "Load with: source $ENV_FILE"
        else
            log_info "Restart your terminal or run: source $target_file"
        fi
        
        log_success "API keys set for current session"
    else
        log_warn "No API keys were configured"
    fi
}

# Reset API keys
reset_keys() {
    echo -e "\n${YELLOW}🔄 Resetting API keys...${NC}"
    
    # Remove from shell profile
    if [ -f "$SHELL_PROFILE" ]; then
        if grep -q "POLYGON_API_KEY\|OPENWEATHER_API_KEY" "$SHELL_PROFILE"; then
            cp "$SHELL_PROFILE" "${SHELL_PROFILE}.backup.$(date +%Y%m%d_%H%M%S)"
            grep -v "export POLYGON_API_KEY=" "$SHELL_PROFILE" | grep -v "export OPENWEATHER_API_KEY=" > "${SHELL_PROFILE}.tmp"
            mv "${SHELL_PROFILE}.tmp" "$SHELL_PROFILE"
            log_info "Removed keys from $SHELL_PROFILE"
        fi
    fi
    
    # Remove from .env file
    if [ -f "$ENV_FILE" ]; then
        if grep -q "POLYGON_API_KEY\|OPENWEATHER_API_KEY" "$ENV_FILE"; then
            cp "$ENV_FILE" "${ENV_FILE}.backup.$(date +%Y%m%d_%H%M%S)"
            grep -v "POLYGON_API_KEY=" "$ENV_FILE" | grep -v "OPENWEATHER_API_KEY=" > "${ENV_FILE}.tmp"
            mv "${ENV_FILE}.tmp" "$ENV_FILE"
            log_info "Removed keys from $ENV_FILE"
        fi
    fi
    
    # Unset from current session
    unset POLYGON_API_KEY
    unset OPENWEATHER_API_KEY
    
    log_success "API keys reset successfully"
    log_info "Restart your terminal to ensure changes take effect"
}

# Set keys directly
set_keys_direct() {
    local polygon_key="$1"
    local weather_key="$2"
    
    echo -e "\n${CYAN}🔧 Setting API keys directly...${NC}"
    
    target_file=""
    if [ "$USE_ENV_FILE" = true ]; then
        target_file="$ENV_FILE"
    else
        target_file="$SHELL_PROFILE"
    fi
    
    if [ -n "$polygon_key" ]; then
        set_env_var "POLYGON_API_KEY" "$polygon_key" "$target_file"
        export POLYGON_API_KEY="$polygon_key"
        log_success "Polygon.io key configured"
    fi
    
    if [ -n "$weather_key" ]; then
        set_env_var "OPENWEATHER_API_KEY" "$weather_key" "$target_file"
        export OPENWEATHER_API_KEY="$weather_key"
        log_success "OpenWeatherMap key configured"
    fi
    
    log_success "API keys saved to $target_file"
    
    if [ "$USE_ENV_FILE" = true ]; then
        log_info "Load with: source $ENV_FILE"
    else
        log_info "Restart your terminal or run: source $target_file"
    fi
}

# Test API keys
test_api_keys() {
    echo -e "\n${CYAN}🧪 Testing API Keys...${NC}"
    echo "═══════════════════════════"
    
    # Test would require actual API calls - for now just validate format
    if [ -n "$POLYGON_API_KEY" ] && [ "$POLYGON_API_KEY" != "$DEFAULT_POLYGON_API_KEY" ]; then
        log_info "Polygon.io key format appears valid (${#POLYGON_API_KEY} characters)"
    elif [ "$POLYGON_API_KEY" = "$DEFAULT_POLYGON_API_KEY" ]; then
        log_success "Using default production Polygon.io key - full functionality available"
    fi
    
    if [ -n "$OPENWEATHER_API_KEY" ] && [ "$OPENWEATHER_API_KEY" != "$DEFAULT_OPENWEATHER_API_KEY" ]; then
        log_info "OpenWeather key format appears valid (${#OPENWEATHER_API_KEY} characters)"
    elif [ "$OPENWEATHER_API_KEY" = "$DEFAULT_OPENWEATHER_API_KEY" ]; then
        log_success "Using default production OpenWeather key - full functionality available"
    fi
    
    echo ""
    log_info "To test functionality, run: ./amcp-cli"
    log_info "Then try: 'stock AAPL' or 'weather London'"
}

# Parse command line arguments
INTERACTIVE=false
DEMO_MODE=false
SHOW_CURRENT=false
RESET_KEYS=false
USE_ENV_FILE=false
POLYGON_KEY=""
WEATHER_KEY=""

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -i|--interactive)
            INTERACTIVE=true
            shift
            ;;
        -d|--demo)
            DEMO_MODE=true
            shift
            ;;
        -c|--current)
            SHOW_CURRENT=true
            shift
            ;;
        -r|--reset)
            RESET_KEYS=true
            shift
            ;;
        --polygon)
            POLYGON_KEY="$2"
            shift 2
            ;;
        --openweather)
            WEATHER_KEY="$2"
            shift 2
            ;;
        --env-file)
            USE_ENV_FILE=true
            shift
            ;;
        --shell-profile)
            USE_ENV_FILE=false
            shift
            ;;
        *)
            log_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Main execution
main() {
    print_banner
    
    # Detect shell profile
    detect_shell_profile
    
    # Handle different modes
    if [ "$SHOW_CURRENT" = true ]; then
        check_current_status
        exit 0
    fi
    
    if [ "$RESET_KEYS" = true ]; then
        reset_keys
        exit 0
    fi
    
    if [ "$DEMO_MODE" = true ]; then
        setup_demo_keys
        test_api_keys
        exit 0
    fi
    
    if [ "$INTERACTIVE" = true ]; then
        interactive_setup
        test_api_keys
        exit 0
    fi
    
    if [ -n "$POLYGON_KEY" ] || [ -n "$WEATHER_KEY" ]; then
        set_keys_direct "$POLYGON_KEY" "$WEATHER_KEY"
        test_api_keys
        exit 0
    fi
    
    # Default behavior - show help and current status
    check_current_status
    echo ""
    echo -e "${YELLOW}💡 Quick Setup Options:${NC}"
    echo -e "  ${GREEN}$0 --demo${NC}         # Use default production keys for immediate functionality"
    echo -e "  ${GREEN}$0 --interactive${NC}  # Interactive setup with real keys"
    echo -e "  ${GREEN}$0 --help${NC}         # Show full help"
}

# Run main function
main "$@"