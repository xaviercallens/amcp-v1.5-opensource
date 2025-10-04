#!/bin/bash

# AMCP v1.5 Open Source Edition - Linux Environment Setup
# This script automates the setup process for Linux distributions

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Detect Linux distribution
detect_distro() {
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        DISTRO=$ID
        VERSION=$VERSION_ID
    elif [ -f /etc/redhat-release ]; then
        DISTRO="rhel"
    elif [ -f /etc/debian_version ]; then
        DISTRO="debian"
    else
        DISTRO="unknown"
    fi
}

# Print banner
print_banner() {
    echo -e "${BLUE}"
    echo "╔══════════════════════════════════════════════════════════════════╗"
    echo "║          AMCP v1.5 Open Source Edition - Linux Setup            ║"
    echo "║        Agent Mesh Communication Protocol                         ║"
    echo "╚══════════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

# Check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Detect Java installation
detect_java() {
    if command_exists java; then
        JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
        echo -e "${GREEN}✅ Java $JAVA_VERSION detected${NC}"
        
        if [ "$JAVA_VERSION" -ge 21 ]; then
            echo -e "${GREEN}   Java version is compatible (21+)${NC}"
            return 0
        else
            echo -e "${YELLOW}⚠️  Java $JAVA_VERSION found, but Java 21+ is required${NC}"
            return 1
        fi
    else
        echo -e "${RED}❌ Java not found${NC}"
        return 1
    fi
}

# Find Java 21 installation
find_java_home() {
    # Common Java 21 installation paths for different distributions
    JAVA_PATHS=(
        "/usr/lib/jvm/java-21-openjdk-amd64"      # Ubuntu/Debian
        "/usr/lib/jvm/java-21-openjdk"            # RHEL/Fedora
        "/usr/lib/jvm/java-21"                    # Generic
        "/usr/lib/jvm/jdk-21"                     # Oracle JDK
        "/usr/lib/jvm/java-21-amazon-corretto"    # Amazon Corretto
        "/opt/java/openjdk"                       # Docker/Custom
    )
    
    for path in "${JAVA_PATHS[@]}"; do
        if [ -d "$path" ]; then
            export JAVA_HOME="$path"
            export PATH="$JAVA_HOME/bin:$PATH"
            echo -e "${GREEN}✅ JAVA_HOME set to: $JAVA_HOME${NC}"
            return 0
        fi
    done
    
    # Try to find using update-alternatives (Debian/Ubuntu)
    if command_exists update-alternatives; then
        JAVA_HOME=$(update-alternatives --query java 2>/dev/null | grep 'Value:' | cut -d' ' -f2 | sed 's|/bin/java||')
        if [ -n "$JAVA_HOME" ]; then
            export JAVA_HOME
            export PATH="$JAVA_HOME/bin:$PATH"
            echo -e "${GREEN}✅ JAVA_HOME set to: $JAVA_HOME${NC}"
            return 0
        fi
    fi
    
    # Try to find using alternatives (RHEL/Fedora)
    if command_exists alternatives; then
        JAVA_HOME=$(alternatives --display java 2>/dev/null | grep 'link currently points to' | awk '{print $5}' | sed 's|/bin/java||')
        if [ -n "$JAVA_HOME" ]; then
            export JAVA_HOME
            export PATH="$JAVA_HOME/bin:$PATH"
            echo -e "${GREEN}✅ JAVA_HOME set to: $JAVA_HOME${NC}"
            return 0
        fi
    fi
    
    echo -e "${YELLOW}⚠️  Could not automatically detect JAVA_HOME${NC}"
    return 1
}

# Install Java 21 based on distribution
install_java() {
    echo -e "${BLUE}📦 Installing Java 21...${NC}"
    
    case $DISTRO in
        ubuntu|debian)
            echo -e "${BLUE}   Installing for Ubuntu/Debian...${NC}"
            sudo apt update
            sudo apt install -y openjdk-21-jdk openjdk-21-jre
            ;;
        fedora|rhel|centos|rocky|almalinux)
            echo -e "${BLUE}   Installing for RHEL/Fedora...${NC}"
            sudo dnf install -y java-21-openjdk java-21-openjdk-devel
            ;;
        arch|manjaro)
            echo -e "${BLUE}   Installing for Arch Linux...${NC}"
            sudo pacman -S --noconfirm jdk21-openjdk
            sudo archlinux-java set java-21-openjdk
            ;;
        opensuse*)
            echo -e "${BLUE}   Installing for openSUSE...${NC}"
            sudo zypper install -y java-21-openjdk java-21-openjdk-devel
            ;;
        amzn)
            echo -e "${BLUE}   Installing for Amazon Linux...${NC}"
            sudo dnf install -y java-21-amazon-corretto java-21-amazon-corretto-devel
            ;;
        *)
            echo -e "${RED}❌ Unsupported distribution: $DISTRO${NC}"
            echo -e "${YELLOW}   Please install Java 21 manually${NC}"
            return 1
            ;;
    esac
    
    echo -e "${GREEN}✅ Java 21 installation complete${NC}"
}

# Check Maven installation
check_maven() {
    if command_exists mvn; then
        MVN_VERSION=$(mvn -version 2>/dev/null | head -n1 | awk '{print $3}')
        echo -e "${GREEN}✅ Maven $MVN_VERSION detected${NC}"
        return 0
    else
        echo -e "${YELLOW}⚠️  Maven not found${NC}"
        return 1
    fi
}

# Install Maven based on distribution
install_maven() {
    echo -e "${BLUE}📦 Installing Maven...${NC}"
    
    case $DISTRO in
        ubuntu|debian)
            sudo apt install -y maven
            ;;
        fedora|rhel|centos|rocky|almalinux)
            sudo dnf install -y maven
            ;;
        arch|manjaro)
            sudo pacman -S --noconfirm maven
            ;;
        opensuse*)
            sudo zypper install -y maven
            ;;
        amzn)
            sudo dnf install -y maven
            ;;
        *)
            echo -e "${RED}❌ Unsupported distribution: $DISTRO${NC}"
            return 1
            ;;
    esac
    
    echo -e "${GREEN}✅ Maven installation complete${NC}"
}

# Setup environment variables
setup_environment() {
    echo -e "${BLUE}🔧 Setting up environment variables...${NC}"
    
    # Detect shell configuration file
    if [ -n "$BASH_VERSION" ]; then
        SHELL_RC="$HOME/.bashrc"
    elif [ -n "$ZSH_VERSION" ]; then
        SHELL_RC="$HOME/.zshrc"
    else
        SHELL_RC="$HOME/.profile"
    fi
    
    # Backup existing configuration
    if [ -f "$SHELL_RC" ]; then
        cp "$SHELL_RC" "${SHELL_RC}.backup.$(date +%Y%m%d_%H%M%S)"
    fi
    
    # Add JAVA_HOME if not already present
    if ! grep -q "JAVA_HOME.*amcp" "$SHELL_RC" 2>/dev/null; then
        echo "" >> "$SHELL_RC"
        echo "# AMCP v1.5 - Java Environment" >> "$SHELL_RC"
        echo "export JAVA_HOME=\"$JAVA_HOME\"" >> "$SHELL_RC"
        echo "export PATH=\"\$JAVA_HOME/bin:\$PATH\"" >> "$SHELL_RC"
        echo -e "${GREEN}✅ Added JAVA_HOME to $SHELL_RC${NC}"
    else
        echo -e "${YELLOW}⚠️  JAVA_HOME already configured in $SHELL_RC${NC}"
    fi
    
    # Add optional API keys section
    if ! grep -q "AMCP API Keys" "$SHELL_RC" 2>/dev/null; then
        echo "" >> "$SHELL_RC"
        echo "# AMCP v1.5 - Optional API Keys (uncomment and add your keys)" >> "$SHELL_RC"
        echo "# export POLYGON_API_KEY=\"your_polygon_api_key_here\"" >> "$SHELL_RC"
        echo "# export OPENWEATHER_API_KEY=\"your_openweather_api_key_here\"" >> "$SHELL_RC"
    fi
    
    echo -e "${GREEN}✅ Environment configuration complete${NC}"
    echo -e "${YELLOW}   Run 'source $SHELL_RC' to apply changes${NC}"
}

# Make scripts executable
setup_scripts() {
    echo -e "${BLUE}🔧 Making scripts executable...${NC}"
    
    chmod +x amcp-cli 2>/dev/null || true
    chmod +x *.sh 2>/dev/null || true
    chmod +x scripts/*.sh 2>/dev/null || true
    
    echo -e "${GREEN}✅ Scripts are now executable${NC}"
}

# Print summary
print_summary() {
    echo ""
    echo -e "${GREEN}╔══════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║              Setup Complete! 🎉                                  ║${NC}"
    echo -e "${GREEN}╚══════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${BLUE}📋 Summary:${NC}"
    echo -e "   • Distribution: ${GREEN}$DISTRO $VERSION${NC}"
    echo -e "   • Java Version: ${GREEN}$(java -version 2>&1 | head -n1)${NC}"
    echo -e "   • Maven Version: ${GREEN}$(mvn -version 2>/dev/null | head -n1)${NC}"
    echo -e "   • JAVA_HOME: ${GREEN}$JAVA_HOME${NC}"
    echo ""
    echo -e "${BLUE}🚀 Next Steps:${NC}"
    echo -e "   1. Apply environment changes:"
    echo -e "      ${YELLOW}source ~/.bashrc${NC}  (or ~/.zshrc)"
    echo ""
    echo -e "   2. Build AMCP:"
    echo -e "      ${YELLOW}mvn clean install -DskipTests${NC}"
    echo ""
    echo -e "   3. Launch AMCP CLI:"
    echo -e "      ${YELLOW}./amcp-cli --build${NC}"
    echo ""
    echo -e "${BLUE}📚 Documentation:${NC}"
    echo -e "   • Linux Guide: ${YELLOW}LINUX_DEPLOYMENT.md${NC}"
    echo -e "   • Quick Start: ${YELLOW}QUICK_START.md${NC}"
    echo -e "   • Full README: ${YELLOW}README.md${NC}"
    echo ""
}

# Main execution
main() {
    print_banner
    
    echo -e "${BLUE}🔍 Detecting system configuration...${NC}"
    detect_distro
    echo -e "${GREEN}✅ Detected: $DISTRO $VERSION${NC}"
    echo ""
    
    # Check and install Java
    if ! detect_java; then
        echo -e "${YELLOW}📦 Java 21 is required. Attempting to install...${NC}"
        read -p "Install Java 21 now? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            install_java
            detect_java || {
                echo -e "${RED}❌ Java installation failed${NC}"
                exit 1
            }
        else
            echo -e "${RED}❌ Java 21 is required to continue${NC}"
            exit 1
        fi
    fi
    
    # Find and set JAVA_HOME
    find_java_home
    
    # Check and install Maven
    if ! check_maven; then
        echo -e "${YELLOW}📦 Maven is required. Attempting to install...${NC}"
        read -p "Install Maven now? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            install_maven
            check_maven || {
                echo -e "${RED}❌ Maven installation failed${NC}"
                exit 1
            }
        else
            echo -e "${YELLOW}⚠️  Maven is recommended but not required for running pre-built artifacts${NC}"
        fi
    fi
    
    # Setup environment
    setup_environment
    
    # Make scripts executable
    setup_scripts
    
    # Print summary
    print_summary
}

# Run main function
main
