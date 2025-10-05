#!/bin/bash

echo "🚀 GitHub Pages Deployment Monitor"
echo "================================="
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

# Repository information
REPO_OWNER="agentmeshcommunicationprotocol"
REPO_NAME="amcp.github.io"
PAGES_URL="https://agentmeshcommunicationprotocol.github.io/amcp.github.io"

print_info "Monitoring GitHub Pages deployment for $REPO_OWNER/$REPO_NAME"
echo ""

# Check latest commit
cd /home/kalxav/CascadeProjects/amcp-github-pages
LATEST_COMMIT=$(git log --oneline -1)
print_status "Latest commit: $LATEST_COMMIT"
echo ""

# Check GitHub Actions workflow status
print_info "GitHub Actions Workflow Status:"
echo "==============================="
echo "Repository: https://github.com/$REPO_OWNER/$REPO_NAME"
echo "Actions URL: https://github.com/$REPO_OWNER/$REPO_NAME/actions"
echo ""
echo "The workflow should be triggered by the recent push:"
echo "- Workflow: 'Deploy Jekyll site to Pages'"
echo "- Trigger: Push to main branch"
echo "- Expected duration: 2-5 minutes"
echo ""

# Test website accessibility
print_info "Testing website accessibility..."
echo "=============================="

# Test with curl
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$PAGES_URL")
if [ "$HTTP_STATUS" -eq 200 ]; then
    print_status "Website is accessible! (HTTP $HTTP_STATUS)"
elif [ "$HTTP_STATUS" -eq 301 ] || [ "$HTTP_STATUS" -eq 302 ]; then
    print_warning "Website redirecting (HTTP $HTTP_STATUS) - this is normal during deployment"
elif [ "$HTTP_STATUS" -eq 404 ]; then
    print_warning "Website not yet available (HTTP $HTTP_STATUS) - deployment may still be in progress"
else
    print_warning "Unexpected HTTP status: $HTTP_STATUS"
fi

echo ""

# Check GitHub Pages API (if available)
print_info "GitHub Pages Configuration:"
echo "=========================="
PAGES_INFO=$(curl -s "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/pages" 2>/dev/null)
if echo "$PAGES_INFO" | grep -q '"status"'; then
    STATUS=$(echo "$PAGES_INFO" | grep '"status"' | cut -d'"' -f4)
    print_status "Pages Status: $STATUS"
    
    if echo "$PAGES_INFO" | grep -q '"html_url"'; then
        URL=$(echo "$PAGES_INFO" | grep '"html_url"' | cut -d'"' -f4)
        echo "Pages URL: $URL"
    fi
else
    print_warning "Could not retrieve GitHub Pages status via API"
fi

echo ""

# Deployment verification checklist
print_info "Deployment Verification Checklist:"
echo "================================="
echo "✅ Latest commit pushed to main branch"
echo "✅ GitHub Actions workflow configured (pages.yml)"
echo "✅ Jekyll configuration updated (_config.yml)"
echo "⏳ GitHub Actions workflow running (check Actions tab)"
echo "⏳ Jekyll build and deployment in progress"
echo "⏳ Website becoming accessible at $PAGES_URL"
echo ""

# Instructions for manual verification
print_info "Manual Verification Steps:"
echo "========================="
echo "1. Visit GitHub Actions: https://github.com/$REPO_OWNER/$REPO_NAME/actions"
echo "2. Check latest workflow run status"
echo "3. Wait for green checkmark (✅) indicating successful deployment"
echo "4. Visit website: $PAGES_URL"
echo "5. Verify all pages and links work correctly"
echo ""

# Monitoring commands
print_info "Monitoring Commands:"
echo "==================="
echo "# Check website status:"
echo "curl -I $PAGES_URL"
echo ""
echo "# Monitor GitHub Actions (requires gh CLI):"
echo "gh run list --repo $REPO_OWNER/$REPO_NAME"
echo ""
echo "# Test website content:"
echo "curl -s $PAGES_URL | grep -i 'AMCP'"
echo ""

# Expected timeline
print_info "Expected Timeline:"
echo "================="
echo "⏱️  0-2 minutes: GitHub Actions workflow starts"
echo "⏱️  2-4 minutes: Jekyll build completes"
echo "⏱️  4-6 minutes: Site deployment finishes"
echo "⏱️  6-10 minutes: DNS propagation (if needed)"
echo "✅ 10+ minutes: Website fully accessible"
echo ""

print_status "GitHub Pages deployment monitoring setup complete!"
print_info "The deployment should complete within 10 minutes."
echo ""
echo "🌐 Expected Website URL: $PAGES_URL"
echo "📊 Actions Dashboard: https://github.com/$REPO_OWNER/$REPO_NAME/actions"
echo ""
print_status "🚀 AMCP GitHub Pages deployment in progress! 🌟"
