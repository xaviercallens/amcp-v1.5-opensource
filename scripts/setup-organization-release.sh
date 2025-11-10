#!/bin/bash

# AMCP Organization Release Setup Script
# Configure repository for release to agentmeshcommunicationprotocol/amcpcore.github.io

set -e

echo "🚀 AMCP Organization Release Setup"
echo "=================================="
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuration
ORG_REPO="https://github.com/agentmeshcommunicationprotocol/amcpcore.github.io.git"
ORG_REPO_SSH="git@github.com:agentmeshcommunicationprotocol/amcpcore.github.io.git"
ORG_NAME="agentmeshcommunicationprotocol"
REPO_NAME="amcpcore.github.io"
VERSION="1.6.0"

# Step 1: Check git configuration
echo -e "${BLUE}Step 1: Checking git configuration...${NC}"
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    echo -e "${RED}❌ Not a git repository${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Git repository confirmed${NC}"
echo ""

# Step 2: Check current remotes
echo -e "${BLUE}Step 2: Checking current remotes...${NC}"
git remote -v
echo ""

# Step 3: Add organization remote if not exists
echo -e "${BLUE}Step 3: Configuring organization remote...${NC}"
if git remote | grep -q "^amcpcore$"; then
    echo -e "${YELLOW}⚠️  Remote 'amcpcore' already exists${NC}"
    echo "Current URL: $(git remote get-url amcpcore)"
    read -p "Update remote URL? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        git remote set-url amcpcore $ORG_REPO
        echo -e "${GREEN}✅ Remote 'amcpcore' updated${NC}"
    fi
else
    echo "Adding organization remote..."
    git remote add amcpcore $ORG_REPO
    echo -e "${GREEN}✅ Remote 'amcpcore' added${NC}"
fi
echo ""

# Step 4: Fetch from organization
echo -e "${BLUE}Step 4: Fetching from organization repository...${NC}"
git fetch amcpcore
echo -e "${GREEN}✅ Fetched from organization${NC}"
echo ""

# Step 5: Check VERSION.txt
echo -e "${BLUE}Step 5: Checking VERSION.txt...${NC}"
if [ ! -f VERSION.txt ]; then
    echo "$VERSION" > VERSION.txt
    echo -e "${GREEN}✅ Created VERSION.txt with $VERSION${NC}"
else
    CURRENT_VERSION=$(cat VERSION.txt)
    echo "Current version: $CURRENT_VERSION"
    if [ "$CURRENT_VERSION" != "$VERSION" ]; then
        echo -e "${YELLOW}⚠️  Version mismatch${NC}"
        read -p "Update to $VERSION? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo "$VERSION" > VERSION.txt
            echo -e "${GREEN}✅ Updated VERSION.txt to $VERSION${NC}"
        fi
    else
        echo -e "${GREEN}✅ VERSION.txt is up to date${NC}"
    fi
fi
echo ""

# Step 6: Check CHANGELOG.md
echo -e "${BLUE}Step 6: Checking CHANGELOG.md...${NC}"
if [ ! -f CHANGELOG.md ]; then
    echo -e "${YELLOW}⚠️  CHANGELOG.md not found${NC}"
    echo "Creating basic CHANGELOG.md..."
    cat > CHANGELOG.md << EOF
# AMCP Changelog

## [$VERSION] - $(date +%Y-%m-%d)

### Added
- Strong Mobility Framework
- CloudEvents Integration
- Enterprise Security
- Enhanced LLM Orchestration
- Advanced Agent Mesh

See documentation for complete details.
EOF
    echo -e "${GREEN}✅ Created CHANGELOG.md${NC}"
else
    if grep -q "\[$VERSION\]" CHANGELOG.md; then
        echo -e "${GREEN}✅ CHANGELOG.md contains v$VERSION entry${NC}"
    else
        echo -e "${YELLOW}⚠️  CHANGELOG.md missing v$VERSION entry${NC}"
        echo "Please update CHANGELOG.md manually"
    fi
fi
echo ""

# Step 7: Check current branch
echo -e "${BLUE}Step 7: Checking current branch...${NC}"
CURRENT_BRANCH=$(git branch --show-current)
echo "Current branch: $CURRENT_BRANCH"

if [[ "$CURRENT_BRANCH" != "release/"* ]] && [[ "$CURRENT_BRANCH" != "main" ]]; then
    echo -e "${YELLOW}⚠️  Not on a release branch${NC}"
    read -p "Create release/v$VERSION branch? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        git checkout -b "release/v$VERSION"
        echo -e "${GREEN}✅ Created release/v$VERSION branch${NC}"
    fi
else
    echo -e "${GREEN}✅ On appropriate branch${NC}"
fi
echo ""

# Step 8: Check GitHub CLI
echo -e "${BLUE}Step 8: Checking GitHub CLI...${NC}"
if command -v gh &> /dev/null; then
    echo -e "${GREEN}✅ GitHub CLI installed${NC}"
    
    # Check authentication
    if gh auth status &> /dev/null; then
        echo -e "${GREEN}✅ GitHub CLI authenticated${NC}"
    else
        echo -e "${YELLOW}⚠️  GitHub CLI not authenticated${NC}"
        echo "Run: gh auth login"
    fi
else
    echo -e "${YELLOW}⚠️  GitHub CLI not installed${NC}"
    echo "Install with:"
    echo "  macOS: brew install gh"
    echo "  Linux: sudo apt install gh"
fi
echo ""

# Step 9: Summary and next steps
echo -e "${GREEN}=================================="
echo "✅ Setup Complete!"
echo -e "==================================${NC}"
echo ""
echo -e "${BLUE}Configuration Summary:${NC}"
echo "  Organization: $ORG_NAME"
echo "  Repository: $REPO_NAME"
echo "  Version: $VERSION"
echo "  Current Branch: $(git branch --show-current)"
echo ""
echo -e "${BLUE}Next Steps:${NC}"
echo ""
echo -e "${YELLOW}1. Review changes:${NC}"
echo "   git status"
echo "   git diff"
echo ""
echo -e "${YELLOW}2. Commit changes:${NC}"
echo "   git add VERSION.txt CHANGELOG.md .github/workflows/"
echo "   git commit -m \"chore: Configure organization release for v$VERSION\""
echo ""
echo -e "${YELLOW}3. Push to organization:${NC}"
echo "   git push amcpcore $(git branch --show-current)"
echo ""
echo -e "${YELLOW}4. Create Pull Request:${NC}"
echo "   gh pr create --repo $ORG_NAME/$REPO_NAME \\"
echo "     --base main \\"
echo "     --head $(git branch --show-current) \\"
echo "     --title \"Release: AMCP v$VERSION\" \\"
echo "     --body-file .github/pull_request_template.md"
echo ""
echo -e "${YELLOW}5. After PR is merged, create tag:${NC}"
echo "   git checkout main"
echo "   git pull amcpcore main"
echo "   git tag -a v$VERSION -m \"AMCP v$VERSION - Major Architecture Evolution\""
echo "   git push amcpcore v$VERSION"
echo ""
echo -e "${BLUE}📚 Documentation:${NC}"
echo "  - See: GITHUB_ORGANIZATION_RELEASE_SETUP.md"
echo "  - Release workflow: .github/workflows/organization-release.yml"
echo ""
echo -e "${GREEN}🎉 Ready to release!${NC}"
