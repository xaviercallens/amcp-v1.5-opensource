#!/bin/bash

echo "🔑 AMCP Core Repository Authentication Setup"
echo "==========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Check current directory
if [ ! -f "pom.xml" ]; then
    print_error "Please run this script from the AMCP v1.5 directory"
    exit 1
fi

print_status "Found AMCP v1.5 project directory"

# Check SSH key
if [ -f ~/.ssh/id_ed25519_amcp.pub ]; then
    print_status "SSH key found"
    echo ""
    print_info "Your SSH Public Key:"
    echo "===================="
    cat ~/.ssh/id_ed25519_amcp.pub
    echo ""
    print_info "Copy this key and add it to:"
    echo "https://github.com/agentmeshcommunicationprotocol/amcpcore.github.io/settings/keys"
    echo ""
else
    print_error "SSH key not found. Please run the SSH key setup first."
    exit 1
fi

# Check current remotes
echo "📋 Current Git Remotes:"
echo "======================"
git remote -v
echo ""

# Test SSH connection
print_info "Testing SSH connection to GitHub..."
if ssh -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
    print_status "SSH authentication working"
else
    print_warning "SSH authentication may have issues"
fi

# Check if amcpcore remote exists
if git remote | grep -q "amcpcore"; then
    print_status "AMCP Core remote already configured"
else
    print_info "Adding AMCP Core remote..."
    git remote add amcpcore git@github.com:agentmeshcommunicationprotocol/amcpcore.github.io.git
    print_status "AMCP Core remote added"
fi

echo ""
print_info "Repository Status:"
echo "=================="
echo "Repository: https://github.com/agentmeshcommunicationprotocol/amcpcore.github.io"
echo "Remote name: amcpcore"
echo "Authentication: SSH key"
echo ""

# Test push access
print_info "Testing push access..."
if git ls-remote amcpcore &>/dev/null; then
    print_status "Repository access confirmed"
    echo ""
    print_info "Ready to push! Run:"
    echo "git push amcpcore main"
else
    print_error "Cannot access repository. Please:"
    echo "1. Add SSH key to repository settings"
    echo "2. Ensure you have push permissions"
    echo "3. Contact repository owner if needed"
fi

echo ""
print_info "Next Steps:"
echo "==========="
echo "1. Add SSH key to repository (if not done)"
echo "2. Run: git push amcpcore main"
echo "3. Verify push: git log --oneline -5"
echo ""
print_status "Setup script completed!"
