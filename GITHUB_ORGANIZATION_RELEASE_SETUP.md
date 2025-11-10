# GitHub Organization Release Setup
## Configuring Windsurf & GitHub for AMCP Core Organization Release

**Organization Repository**: https://github.com/agentmeshcommunicationprotocol/amcpcore.github.io  
**Version**: 1.6.0  
**Date**: 2024-11-10  

---

## 📋 Current Configuration

### Git Remotes
```bash
origin       → https://github.com/xaviercallens/amcp-v1.5-opensource.git
amcpcore     → agentmeshcommunicationprotocol/amcpcore.github.io.git
github-pages → agentmeshcommunicationprotocol/agentmeshcommunicationprotocol.github.io.git
```

### Strategy
- **Development**: Personal repository (origin)
- **Release**: Organization repository (amcpcore)
- **Documentation**: GitHub Pages (github-pages)

---

## 🔧 Step 1: Verify Remote Configuration

### Check Current Remotes
```bash
cd /home/kalxav/CascadeProjects/amcp-v1.5-opensource
git remote -v
```

### Add Organization Remote (if not exists)
```bash
# Add amcpcore remote
git remote add amcpcore https://github.com/agentmeshcommunicationprotocol/amcpcore.github.io.git

# Verify
git remote -v
```

### Fetch Organization Repository
```bash
# Fetch all branches
git fetch amcpcore

# List remote branches
git branch -r | grep amcpcore
```

---

## 🚀 Step 2: Create Release Branch

### Option A: Fresh Release Branch
```bash
# Create v1.6 release branch from main
git checkout -b release/v1.6.0

# Ensure it's up to date
git pull origin main --rebase

# Push to organization
git push amcpcore release/v1.6.0
```

### Option B: From Existing Feature Branch
```bash
# If you have feature branch with v1.6 work
git checkout feature/amcp-v1.6-architecture-evolution

# Create release branch
git checkout -b release/v1.6.0

# Push to organization
git push amcpcore release/v1.6.0
```

---

## 📝 Step 3: Configure GitHub Workflows for Organization

### Create Organization-Specific Workflow

**File**: `.github/workflows/organization-release.yml`

```yaml
name: AMCP Organization Release

on:
  push:
    branches:
      - release/*
      - main
    tags:
      - 'v*'
  pull_request:
    branches:
      - main

env:
  ORGANIZATION: agentmeshcommunicationprotocol
  REPOSITORY: amcpcore.github.io

jobs:
  validate:
    name: Validate Release
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0
      
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
      
      - name: Validate Version
        run: |
          VERSION=$(cat VERSION.txt)
          echo "Release Version: $VERSION"
          
          if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
            echo "Invalid version format: $VERSION"
            exit 1
          fi
      
      - name: Build Project
        run: mvn clean install -DskipTests
      
      - name: Run Tests
        run: mvn test
      
      - name: Generate Documentation
        run: mvn javadoc:aggregate
      
      - name: Package Artifacts
        run: mvn package

  release:
    name: Create GitHub Release
    runs-on: ubuntu-latest
    needs: validate
    if: startsWith(github.ref, 'refs/tags/v')
    
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4
      
      - name: Extract Version
        id: version
        run: |
          VERSION=${GITHUB_REF#refs/tags/v}
          echo "version=$VERSION" >> $GITHUB_OUTPUT
      
      - name: Create Release Notes
        id: release_notes
        run: |
          VERSION=${{ steps.version.outputs.version }}
          
          # Extract from CHANGELOG
          NOTES=$(sed -n "/## \[$VERSION\]/,/## \[/p" CHANGELOG.md | head -n -1)
          
          echo "notes<<EOF" >> $GITHUB_OUTPUT
          echo "$NOTES" >> $GITHUB_OUTPUT
          echo "EOF" >> $GITHUB_OUTPUT
      
      - name: Create GitHub Release
        uses: actions/create-release@v1
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          tag_name: v${{ steps.version.outputs.version }}
          release_name: AMCP v${{ steps.version.outputs.version }}
          body: ${{ steps.release_notes.outputs.notes }}
          draft: false
          prerelease: false
      
      - name: Notify Release
        run: |
          echo "✅ Released AMCP v${{ steps.version.outputs.version }} to organization"
          echo "🔗 https://github.com/${{ env.ORGANIZATION }}/${{ env.REPOSITORY }}/releases"

  publish-docs:
    name: Publish Documentation
    runs-on: ubuntu-latest
    needs: release
    if: startsWith(github.ref, 'refs/tags/v')
    
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4
      
      - name: Set up Ruby
        uses: ruby/setup-ruby@v1
        with:
          ruby-version: '3.2'
          bundler-cache: true
      
      - name: Build Jekyll Site
        run: |
          bundle install
          bundle exec jekyll build
      
      - name: Deploy to GitHub Pages
        uses: peaceiris/actions-gh-pages@v3
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
          publish_dir: ./_site
          publish_branch: gh-pages
```

---

## 🔐 Step 4: Configure GitHub Secrets

### Required Secrets for Organization Repository

Go to: https://github.com/agentmeshcommunicationprotocol/amcpcore.github.io/settings/secrets/actions

Add these secrets:

1. **GITHUB_TOKEN** (automatic, no setup needed)
2. **MAVEN_GPG_PRIVATE_KEY** (for signing artifacts)
3. **MAVEN_GPG_PASSPHRASE** (for GPG key)
4. **SONAR_TOKEN** (for code quality)
5. **CODECOV_TOKEN** (for coverage)

### Set Repository Variables

Variables → Actions → New repository variable:

```
RELEASE_BRANCH: main
VERSION: 1.6.0
DOCUMENTATION_BRANCH: gh-pages
```

---

## 📋 Step 5: Create Pull Request to Organization

### Prepare Release Branch
```bash
# Ensure all changes committed
git status

# Ensure branch is up to date
git pull origin main --rebase

# Push to organization
git push amcpcore release/v1.6.0
```

### Create PR via GitHub CLI
```bash
# Install GitHub CLI if needed
# brew install gh (macOS)
# sudo apt install gh (Linux)

# Authenticate
gh auth login

# Create PR to organization repository
gh pr create \
  --repo agentmeshcommunicationprotocol/amcpcore.github.io \
  --base main \
  --head release/v1.6.0 \
  --title "Release: AMCP v1.6.0 - Major Architecture Evolution" \
  --body-file .github/pull_request_template.md
```

### Or Create PR via Web Interface

1. Go to: https://github.com/agentmeshcommunicationprotocol/amcpcore.github.io
2. Click: "Compare & pull request"
3. **Base**: `main`
4. **Compare**: `release/v1.6.0`
5. Fill in PR template
6. Click "Create pull request"

---

## 🏷️ Step 6: Create Release Tag

### After PR is Merged

```bash
# Switch to main branch
git checkout main

# Pull latest from organization
git pull amcpcore main

# Create annotated tag
git tag -a v1.6.0 -m "AMCP v1.6.0 - Major Architecture Evolution

Major Features:
- Strong Mobility Framework with automatic state preservation
- CloudEvents v1.0 integration
- Enterprise security (mTLS, RBAC, audit logging)
- Enhanced LLM orchestration with fallback system
- Advanced agent mesh with service discovery
- Developer experience improvements

Performance:
- 95% faster cached responses (50ms vs 500ms)
- 60% reduced memory usage (1GB vs 2.5GB)
- 10x concurrent request capacity

See CHANGELOG.md for complete details"

# Push tag to organization
git push amcpcore v1.6.0

# Also push to origin for backup
git push origin v1.6.0
```

---

## 📦 Step 7: Verify Release

### Check GitHub Release Page
```bash
# Open in browser
open https://github.com/agentmeshcommunicationprotocol/amcpcore.github.io/releases/tag/v1.6.0

# Or use GitHub CLI
gh release view v1.6.0 --repo agentmeshcommunicationprotocol/amcpcore.github.io
```

### Verify Release Assets
- [ ] Source code (zip)
- [ ] Source code (tar.gz)
- [ ] Release notes published
- [ ] Tag created
- [ ] Documentation updated

### Check GitHub Actions
```bash
# View workflow runs
gh run list --repo agentmeshcommunicationprotocol/amcpcore.github.io

# View specific run
gh run view <run-id> --repo agentmeshcommunicationprotocol/amcpcore.github.io
```

---

## 🔄 Step 8: Sync Repositories

### Keep Personal and Organization in Sync

```bash
# Pull from organization to personal
git pull amcpcore main
git push origin main

# Or pull from personal to organization
git pull origin main
git push amcpcore main
```

### Set Up Automatic Sync (Optional)

Create `.github/workflows/sync-repos.yml`:

```yaml
name: Sync Repositories

on:
  push:
    branches:
      - main
  workflow_dispatch:

jobs:
  sync:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4
        with:
          fetch-depth: 0
      
      - name: Sync to Organization
        run: |
          git remote add org https://github.com/agentmeshcommunicationprotocol/amcpcore.github.io.git
          git push org main
```

---

## 📊 Step 9: Configure Branch Protection

### For Organization Repository

Go to: https://github.com/agentmeshcommunicationprotocol/amcpcore.github.io/settings/branches

**Protect `main` branch**:

- [x] Require pull request reviews before merging
  - Required approvals: 1
- [x] Require status checks to pass
  - Required checks: build, test, security-scan
- [x] Require branches to be up to date
- [x] Include administrators
- [x] Restrict who can push
  - Add: Maintainers only

**Protect release branches** (`release/*`):

- [x] Require pull request reviews
- [x] Require status checks
- [x] Allow force pushes (for maintainers only)

---

## 🚀 Step 10: Complete Release Checklist

### Pre-Release
- [ ] All tests passing
- [ ] Documentation updated
- [ ] CHANGELOG.md updated
- [ ] VERSION.txt updated to 1.6.0
- [ ] All breaking changes documented
- [ ] Migration guide complete

### Release Process
- [ ] Release branch created
- [ ] PR to organization main
- [ ] PR approved and merged
- [ ] Tag created (v1.6.0)
- [ ] GitHub release published
- [ ] Release notes complete

### Post-Release
- [ ] GitHub Pages updated
- [ ] Documentation published
- [ ] Announcement prepared
- [ ] Community notified
- [ ] Personal repo synced

---

## 🎯 Quick Commands Reference

```bash
# Check current remote configuration
git remote -v

# Fetch organization repository
git fetch amcpcore

# Create release branch
git checkout -b release/v1.6.0

# Push to organization
git push amcpcore release/v1.6.0

# Create PR (via GitHub CLI)
gh pr create --repo agentmeshcommunicationprotocol/amcpcore.github.io \
  --base main --head release/v1.6.0

# Create tag
git tag -a v1.6.0 -m "AMCP v1.6.0"
git push amcpcore v1.6.0

# Verify release
gh release view v1.6.0 --repo agentmeshcommunicationprotocol/amcpcore.github.io

# Sync repositories
git pull amcpcore main && git push origin main
```

---

## 📞 Troubleshooting

### Issue: Permission Denied
```bash
# Ensure you have push access to organization
gh auth refresh -h github.com -s admin:org

# Or use SSH instead of HTTPS
git remote set-url amcpcore git@github.com:agentmeshcommunicationprotocol/amcpcore.github.io.git
```

### Issue: Tag Already Exists
```bash
# Delete local tag
git tag -d v1.6.0

# Delete remote tag
git push amcpcore --delete v1.6.0

# Recreate tag
git tag -a v1.6.0 -m "Release message"
git push amcpcore v1.6.0
```

### Issue: Workflow Not Running
```bash
# Check workflow status
gh workflow list --repo agentmeshcommunicationprotocol/amcpcore.github.io

# Manually trigger workflow
gh workflow run organization-release.yml \
  --repo agentmeshcommunicationprotocol/amcpcore.github.io
```

---

## 📚 Documentation Links

- **Organization Repository**: https://github.com/agentmeshcommunicationprotocol/amcpcore.github.io
- **Personal Repository**: https://github.com/xaviercallens/amcp-v1.5-opensource
- **GitHub Pages**: https://agentmeshcommunicationprotocol.github.io
- **Releases**: https://github.com/agentmeshcommunicationprotocol/amcpcore.github.io/releases

---

**Setup Guide Version**: 1.0  
**Last Updated**: 2024-11-10  
**Status**: Ready for Release Configuration
