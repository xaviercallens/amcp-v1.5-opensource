# Quick Release Commands
## Immediate Actions for Organization Release

**Target**: https://github.com/agentmeshcommunicationprotocol/amcpcore.github.io  
**Version**: 1.6.0  

---

## 🚀 Quick Start (Copy-Paste Ready)

### 1. Run Automated Setup
```bash
cd /home/kalxav/CascadeProjects/amcp-v1.5-opensource
./scripts/setup-organization-release.sh
```

---

## 📋 Manual Steps (If Needed)

### Step 1: Verify Remote
```bash
# Check remotes
git remote -v

# Add organization remote if missing
git remote add amcpcore https://github.com/agentmeshcommunicationprotocol/amcpcore.github.io.git

# Fetch
git fetch amcpcore
```

### Step 2: Create Release Branch
```bash
# Create and switch to release branch
git checkout -b release/v1.6.0

# Or if exists
git checkout release/v1.6.0
```

### Step 3: Prepare Release
```bash
# Ensure VERSION.txt exists
echo "1.6.0" > VERSION.txt

# Stage release files
git add VERSION.txt CHANGELOG.md .github/workflows/ scripts/

# Commit
git commit -m "chore: Configure organization release for v1.6.0

- Add organization release workflow
- Update VERSION.txt to 1.6.0
- Add setup scripts
- Prepare for organization deployment"
```

### Step 4: Push to Organization
```bash
# Push release branch to organization
git push amcpcore release/v1.6.0
```

### Step 5: Create Pull Request
```bash
# Using GitHub CLI (recommended)
gh pr create \
  --repo agentmeshcommunicationprotocol/amcpcore.github.io \
  --base main \
  --head release/v1.6.0 \
  --title "Release: AMCP v1.6.0 - Major Architecture Evolution" \
  --body "## AMCP v1.6.0 Release

**Major Features:**
- Strong Mobility Framework
- CloudEvents Integration
- Enterprise Security
- Enhanced LLM Orchestration

See CHANGELOG.md for complete details."

# Or get PR URL for manual creation
echo "Create PR at: https://github.com/agentmeshcommunicationprotocol/amcpcore.github.io/compare/main...release/v1.6.0"
```

### Step 6: After PR Merge
```bash
# Switch to main and pull
git checkout main
git pull amcpcore main

# Create tag
git tag -a v1.6.0 -m "AMCP v1.6.0 - Major Architecture Evolution

Major Features:
- Strong Mobility Framework with automatic state preservation
- CloudEvents v1.0 integration
- Enterprise security (mTLS, RBAC, audit logging)
- Enhanced LLM orchestration with intelligent fallback
- Advanced agent mesh with service discovery

Performance:
- 95% faster cached responses (50ms vs 500ms)
- 60% reduced memory usage (1GB vs 2.5GB)
- 10x concurrent request capacity

See CHANGELOG.md for complete details."

# Push tag to organization (triggers release workflow)
git push amcpcore v1.6.0

# Also push to personal backup
git push origin v1.6.0
```

---

## ✅ Verification Commands

### Check Release
```bash
# View release on GitHub
gh release view v1.6.0 --repo agentmeshcommunicationprotocol/amcpcore.github.io

# Or open in browser
open https://github.com/agentmeshcommunicationprotocol/amcpcore.github.io/releases/tag/v1.6.0
```

### Check Workflow
```bash
# List workflow runs
gh run list --repo agentmeshcommunicationprotocol/amcpcore.github.io

# Watch specific run
gh run watch --repo agentmeshcommunicationprotocol/amcpcore.github.io
```

### Check GitHub Pages
```bash
# Open documentation
open https://agentmeshcommunicationprotocol.github.io
```

---

## 🔄 Sync Repositories

### Keep Personal and Organization in Sync
```bash
# Pull from organization
git pull amcpcore main

# Push to personal
git push origin main

# Or vice versa
git pull origin main
git push amcpcore main
```

---

## 🛠️ Troubleshooting

### Permission Denied
```bash
# Ensure authenticated with GitHub
gh auth login

# Or use SSH instead of HTTPS
git remote set-url amcpcore git@github.com:agentmeshcommunicationprotocol/amcpcore.github.io.git
```

### Tag Already Exists
```bash
# Delete local tag
git tag -d v1.6.0

# Delete remote tag
git push amcpcore --delete v1.6.0

# Recreate
git tag -a v1.6.0 -m "Release message"
git push amcpcore v1.6.0
```

### Workflow Not Triggering
```bash
# Manually trigger workflow
gh workflow run organization-release.yml \
  --repo agentmeshcommunicationprotocol/amcpcore.github.io

# Check workflow file exists
ls -la .github/workflows/organization-release.yml
```

---

## 📊 Status Check

### Current Configuration
```bash
# Check remotes
git remote -v | grep amcpcore

# Check branch
git branch --show-current

# Check version
cat VERSION.txt

# Check uncommitted changes
git status
```

### Verify Workflows
```bash
# List workflows
ls -la .github/workflows/

# Validate workflow syntax
gh workflow view organization-release.yml \
  --repo agentmeshcommunicationprotocol/amcpcore.github.io
```

---

## 🎯 One-Line Release (After PR Merged)

```bash
git checkout main && git pull amcpcore main && git tag -a v1.6.0 -m "AMCP v1.6.0" && git push amcpcore v1.6.0 && echo "✅ Release v1.6.0 triggered!"
```

---

## 📞 Quick Help

- **Setup Guide**: `GITHUB_ORGANIZATION_RELEASE_SETUP.md`
- **Automated Script**: `./scripts/setup-organization-release.sh`
- **Organization Repo**: https://github.com/agentmeshcommunicationprotocol/amcpcore.github.io
- **Releases**: https://github.com/agentmeshcommunicationprotocol/amcpcore.github.io/releases

---

**Quick Reference Version**: 1.0  
**Last Updated**: 2024-11-10
