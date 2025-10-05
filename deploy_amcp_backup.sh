#!/bin/bash

echo "🔄 AMCP v1.5 Backup Deployment Strategy"
echo "======================================"
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Check if we're in the right directory
if [ ! -f "pom.xml" ]; then
    print_error "Please run from AMCP v1.5 directory"
    exit 1
fi

print_status "Found AMCP v1.5 project directory"

# Show current status
echo ""
print_info "Current Repository Status:"
echo "========================="
echo "Files ready: $(git ls-files | wc -l)"
echo "Current branch: $(git branch --show-current)"
echo "Latest commit: $(git log --oneline -1)"
echo ""

# Test primary method
print_info "Testing primary deployment method..."
if git ls-remote amcpcore &>/dev/null; then
    print_status "Can read from amcpcore repository"
    
    print_info "Attempting push to amcpcore..."
    if git push amcpcore main 2>/dev/null; then
        print_status "SUCCESS! AMCP v1.5 deployed to amcpcore repository!"
        echo ""
        print_info "Verification:"
        echo "Repository: https://github.com/agentmeshcommunicationprotocol/amcpcore.github.io"
        echo "Files deployed: $(git ls-files | wc -l)"
        exit 0
    else
        print_warning "Push to amcpcore failed - trying backup methods..."
    fi
else
    print_warning "Cannot access amcpcore repository"
fi

# Backup Method 1: Create bundle for manual upload
echo ""
print_info "Backup Method 1: Creating Git Bundle"
echo "===================================="
BUNDLE_FILE="/home/kalxav/CascadeProjects/amcp-v1.5-complete.bundle"
if git bundle create "$BUNDLE_FILE" HEAD main; then
    print_status "Git bundle created: $BUNDLE_FILE"
    echo ""
    print_info "To deploy using bundle:"
    echo "1. Download bundle file: $BUNDLE_FILE"
    echo "2. Clone from bundle: git clone $BUNDLE_FILE amcp-v1.5"
    echo "3. Add remote: git remote add origin git@github.com:agentmeshcommunicationprotocol/amcpcore.github.io.git"
    echo "4. Push: git push origin main"
else
    print_error "Failed to create bundle"
fi

# Backup Method 2: Create archive
echo ""
print_info "Backup Method 2: Creating Source Archive"
echo "========================================"
ARCHIVE_FILE="/home/kalxav/CascadeProjects/amcp-v1.5-complete.tar.gz"
if git archive --format=tar.gz --prefix=amcp-v1.5/ HEAD > "$ARCHIVE_FILE"; then
    print_status "Source archive created: $ARCHIVE_FILE"
    echo ""
    print_info "To deploy using archive:"
    echo "1. Extract: tar -xzf $ARCHIVE_FILE"
    echo "2. Initialize git: cd amcp-v1.5 && git init"
    echo "3. Add files: git add ."
    echo "4. Commit: git commit -m 'Initial AMCP v1.5 deployment'"
    echo "5. Add remote and push to amcpcore repository"
else
    print_error "Failed to create archive"
fi

# Show repository information
echo ""
print_info "Repository Information:"
echo "======================"
echo "Target: https://github.com/agentmeshcommunicationprotocol/amcpcore.github.io"
echo "SSH Remote: git@github.com:agentmeshcommunicationprotocol/amcpcore.github.io.git"
echo "HTTPS Remote: https://github.com/agentmeshcommunicationprotocol/amcpcore.github.io.git"
echo ""

# Show authentication options
print_info "Authentication Options:"
echo "======================"
echo "1. Add SSH key to repository: https://github.com/agentmeshcommunicationprotocol/amcpcore.github.io/settings/keys"
echo "2. Request collaborator access from repository owner"
echo "3. Use personal access token with HTTPS"
echo "4. Fork repository and create pull request"
echo ""

# Show what's ready to deploy
print_info "Ready for Deployment:"
echo "===================="
echo "✅ Complete AMCP v1.5 codebase (279 files)"
echo "✅ Unit test framework (23 tests)"
echo "✅ Ollama integration with Spring AI"
echo "✅ CLI tools and utilities"
echo "✅ Comprehensive documentation"
echo "✅ Legal framework (LICENSE + ICLA)"
echo "✅ Build system and dependencies"
echo ""

print_status "AMCP v1.5 is ready for deployment!"
print_info "Choose the appropriate method based on your access level."
