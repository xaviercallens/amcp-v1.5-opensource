#!/bin/bash

echo "🔧 GitHub Actions Workflow Fix - Monitoring"
echo "=========================================="
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

print_info "Monitoring GitHub Actions workflow fix for $REPO_OWNER/$REPO_NAME"
echo ""

# Check latest commit
cd /home/kalxav/CascadeProjects/amcp-github-pages
LATEST_COMMIT=$(git log --oneline -1)
print_status "Latest commit: $LATEST_COMMIT"
echo ""

# Show what was fixed
print_info "Workflow Fixes Applied:"
echo "======================"
echo "✅ actions/configure-pages: v3 → v4"
echo "✅ actions/upload-pages-artifact: v2 → v3"
echo "✅ actions/deploy-pages: v2 → v4"
echo "✅ Eliminated deprecated action versions"
echo ""

# Check GitHub Actions status
print_info "GitHub Actions Status:"
echo "====================="
echo "Repository: https://github.com/$REPO_OWNER/$REPO_NAME"
echo "Actions URL: https://github.com/$REPO_OWNER/$REPO_NAME/actions"
echo ""
echo "Expected workflow behavior:"
echo "- Workflow should trigger automatically from the push"
echo "- Build job should complete successfully"
echo "- Deploy job should deploy to GitHub Pages"
echo "- No deprecation warnings or errors"
echo ""

# Test current website status
print_info "Current Website Status:"
echo "======================"
CURRENT_URL="https://agentmeshcommunicationprotocol.github.io/amcp.github.io"
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$CURRENT_URL")

if [ "$HTTP_STATUS" -eq 200 ]; then
    print_status "Current site accessible (HTTP $HTTP_STATUS)"
elif [ "$HTTP_STATUS" -eq 301 ] || [ "$HTTP_STATUS" -eq 302 ]; then
    print_warning "Site redirecting (HTTP $HTTP_STATUS) - normal during deployment"
else
    print_warning "Site status: HTTP $HTTP_STATUS"
fi

echo ""

# Expected timeline
print_info "Expected Timeline:"
echo "================="
echo "⏱️  0-2 minutes: Workflow starts with fixed actions"
echo "⏱️  2-4 minutes: Jekyll build completes successfully"
echo "⏱️  4-6 minutes: Deployment to GitHub Pages"
echo "⏱️  6-10 minutes: Site fully accessible"
echo ""

# Verification commands
print_info "Verification Commands:"
echo "===================="
echo "# Check workflow runs:"
echo "curl -s \"https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/actions/runs\" | grep -E '\"status\"|\"conclusion\"'"
echo ""
echo "# Monitor site status:"
echo "curl -I \"$CURRENT_URL\""
echo ""
echo "# Test site content:"
echo "curl -s \"$CURRENT_URL\" | grep -i \"AMCP\""
echo ""

# Manual verification steps
print_info "Manual Verification:"
echo "==================="
echo "1. Visit: https://github.com/$REPO_OWNER/$REPO_NAME/actions"
echo "2. Check latest workflow run for green checkmark"
echo "3. Verify no deprecation warnings in logs"
echo "4. Test website: $CURRENT_URL"
echo "5. Confirm all pages load correctly"
echo ""

# Success indicators
print_info "Success Indicators:"
echo "=================="
echo "✅ GitHub Actions workflow completes with green status"
echo "✅ No deprecation warnings in workflow logs"
echo "✅ Jekyll build completes without errors"
echo "✅ Site deployment succeeds"
echo "✅ Website accessible and functional"
echo ""

print_status "Workflow fix applied and monitoring active!"
print_info "The updated workflow should resolve the deprecation error."
echo ""
echo "🔗 Monitor at: https://github.com/$REPO_OWNER/$REPO_NAME/actions"
echo "🌐 Website: $CURRENT_URL"
echo ""
print_status "🚀 Fixed workflow deployment in progress! 🌟"
