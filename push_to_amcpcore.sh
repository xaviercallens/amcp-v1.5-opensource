#!/bin/bash

echo "🚀 Push AMCP v1.5 to Core Repository"
echo "===================================="
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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

# Verify we're in the right directory
if [ ! -f "pom.xml" ]; then
    echo "❌ Please run from AMCP v1.5 directory"
    exit 1
fi

print_status "Found AMCP v1.5 project"

# Show what will be pushed
echo ""
print_info "Repository Information:"
echo "======================"
echo "Source: $(pwd)"
echo "Target: https://github.com/agentmeshcommunicationprotocol/amcpcore.github.io"
echo "Branch: main"
echo ""

print_info "Files to be pushed:"
echo "=================="
git ls-files | wc -l | xargs echo "Total files:"
echo "Recent commits:"
git log --oneline -5
echo ""

# Test connection first
print_info "Testing repository access..."
if git ls-remote amcpcore &>/dev/null; then
    print_status "Repository access confirmed"
else
    echo "❌ Cannot access repository. Please add SSH key with write permissions."
    echo "   Go to: https://github.com/agentmeshcommunicationprotocol/amcpcore.github.io/settings/keys"
    exit 1
fi

# Push main branch
print_info "Pushing main branch..."
if git push amcpcore main; then
    print_status "Main branch pushed successfully!"
else
    echo "❌ Push failed. Check permissions and try again."
    exit 1
fi

# Push all branches if any exist
print_info "Checking for additional branches..."
OTHER_BRANCHES=$(git branch -r | grep -v "origin/main" | grep -v "HEAD" | wc -l)
if [ "$OTHER_BRANCHES" -gt 0 ]; then
    print_info "Pushing all branches..."
    git push amcpcore --all
fi

# Push tags if any exist
TAGS=$(git tag | wc -l)
if [ "$TAGS" -gt 0 ]; then
    print_info "Pushing tags..."
    git push amcpcore --tags
fi

echo ""
print_status "AMCP v1.5 successfully pushed to core repository!"
echo ""
print_info "Verification:"
echo "============"
echo "Repository: https://github.com/agentmeshcommunicationprotocol/amcpcore.github.io"
echo "Files pushed: $(git ls-files | wc -l)"
echo "Latest commit: $(git log --oneline -1)"
echo ""
print_status "Complete AMCP v1.5 codebase is now available in the organization repository!"
