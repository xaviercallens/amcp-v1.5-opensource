#!/bin/bash

echo "🔍 AMCP GitHub Pages Status Check"
echo "================================="
echo ""

# Check repository status
echo "📊 Repository Information:"
echo "-------------------------"
REPO_INFO=$(curl -s "https://api.github.com/repos/agentmeshcommunicationprotocol/amcp.github.io")
echo "Repository: $(echo "$REPO_INFO" | grep '"name"' | head -1 | cut -d'"' -f4)"
echo "Default Branch: $(echo "$REPO_INFO" | grep '"default_branch"' | cut -d'"' -f4)"
echo "Has Pages: $(echo "$REPO_INFO" | grep '"has_pages"' | cut -d':' -f2 | tr -d ' ,')"
echo ""

# Check if files are present locally
echo "📁 Local Files Status:"
echo "---------------------"
cd /home/kalxav/CascadeProjects/amcp-github-pages
if [ -f "index.html" ]; then
    echo "✅ index.html - Present ($(wc -l < index.html) lines)"
else
    echo "❌ index.html - Missing"
fi

if [ -f "_config.yml" ]; then
    echo "✅ _config.yml - Present"
else
    echo "❌ _config.yml - Missing"
fi

if [ -f "README.md" ]; then
    echo "✅ README.md - Present ($(wc -l < README.md) lines)"
else
    echo "❌ README.md - Missing"
fi

echo ""

# Check git status
echo "📋 Git Status:"
echo "--------------"
echo "Current Branch: $(git branch --show-current)"
echo "Remote URL: $(git remote get-url origin)"
echo "Last Commit: $(git log --oneline -1)"
echo ""

# Check if GitHub Pages is enabled
echo "🌐 GitHub Pages Status:"
echo "----------------------"
PAGES_STATUS=$(echo "$REPO_INFO" | grep '"has_pages"' | cut -d':' -f2 | tr -d ' ,')
if [ "$PAGES_STATUS" = "true" ]; then
    echo "✅ GitHub Pages is ENABLED"
    echo "🌐 Website should be live at: https://agentmeshcommunicationprotocol.github.io/amcp.github.io"
else
    echo "❌ GitHub Pages is NOT ENABLED"
    echo "📋 Action Required: Enable GitHub Pages in repository settings"
    echo ""
    echo "🔧 To Enable GitHub Pages:"
    echo "1. Go to: https://github.com/agentmeshcommunicationprotocol/amcp.github.io/settings/pages"
    echo "2. Source: Deploy from a branch"
    echo "3. Branch: main"
    echo "4. Folder: / (root)"
    echo "5. Click Save"
fi

echo ""
echo "🎯 Next Steps:"
echo "-------------"
if [ "$PAGES_STATUS" = "false" ]; then
    echo "1. Enable GitHub Pages in repository settings (see above)"
    echo "2. Wait 5-10 minutes for deployment"
    echo "3. Visit: https://agentmeshcommunicationprotocol.github.io/amcp.github.io"
else
    echo "1. Check Actions tab for build status"
    echo "2. Visit: https://agentmeshcommunicationprotocol.github.io/amcp.github.io"
    echo "3. Verify all pages load correctly"
fi

echo ""
echo "📞 Repository Settings URL:"
echo "https://github.com/agentmeshcommunicationprotocol/amcp.github.io/settings/pages"
