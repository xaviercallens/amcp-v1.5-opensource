# 🐧 Add Comprehensive Linux Deployment Support

## 📋 Summary

This PR adds comprehensive Linux deployment documentation and automation scripts to make AMCP v1.5 more accessible to the Linux community. Previously, the documentation was primarily focused on macOS, which limited adoption among Linux developers and enterprises.

## 🎯 Motivation

- **Gap**: Existing documentation (`setup-java21.sh`) was macOS-specific with hardcoded Homebrew paths
- **Need**: Linux is the dominant platform for enterprise deployments and cloud infrastructure
- **Impact**: This enhancement makes AMCP accessible to a broader open source community

## ✨ Changes

### 1. **New Documentation: `LINUX_DEPLOYMENT.md`**
Comprehensive Linux deployment guide covering:
- ✅ Supported distributions (Ubuntu, Debian, RHEL, CentOS, Fedora, Arch, Amazon Linux, openSUSE)
- ✅ Distribution-specific installation instructions
- ✅ Systemd service configuration for production deployments
- ✅ Docker deployment on Linux
- ✅ Performance tuning (JVM, kernel parameters, file descriptors, huge pages)
- ✅ Troubleshooting (Java versions, ports, permissions, firewall, SELinux)

### 2. **New Script: `setup-linux.sh`**
Automated setup script that:
- ✅ Auto-detects Linux distribution (Ubuntu, Debian, RHEL, Fedora, Arch, etc.)
- ✅ Installs Java 21 using distribution-specific package managers
- ✅ Installs Maven if not present
- ✅ Automatically finds and sets JAVA_HOME
- ✅ Configures shell environment (.bashrc/.zshrc)
- ✅ Interactive prompts for user confirmation
- ✅ Colorized output with clear status indicators
- ✅ Makes all scripts executable

### 3. **New Guide: `QUICK_START_LINUX.md`**
Unified quick start guide with:
- ✅ Platform-specific instructions (Linux & macOS)
- ✅ API key configuration for both Bash and Zsh
- ✅ Docker deployment quick start
- ✅ Development workflow commands
- ✅ Troubleshooting section with platform-specific solutions

### 4. **Updated: `README.md`**
- ✅ Added platform-specific setup sections
- ✅ Clear separation between Linux (🐧) and macOS (🍎) instructions
- ✅ Direct links to detailed Linux documentation

### 5. **Updated: `QUICK_START.md`**
- ✅ Added Linux-specific setup instructions
- ✅ Reference to comprehensive Linux deployment guide

## 📊 Impact

### Before
```bash
# setup-java21.sh - macOS only
export JAVA_HOME="/opt/homebrew/Cellar/openjdk@21/21.0.8/libexec/openjdk.jdk/Contents/Home"
# ❌ Fails on Linux systems
```

### After
```bash
# Linux users
./setup-linux.sh  # Auto-detects distro and installs everything
source ~/.bashrc
mvn clean install -DskipTests
./amcp-cli --build

# macOS users
./setup-java21.sh  # Existing script still works
```

## 🧪 Testing

Tested on:
- ✅ Ubuntu 22.04 LTS
- ✅ Debian 12 (Bookworm)
- ✅ Fedora 40
- ✅ Arch Linux (rolling)

The setup script successfully:
- Detects distribution correctly
- Installs Java 21 and Maven
- Configures environment variables
- Builds AMCP without errors

## 📝 Documentation Quality

All new documentation follows best practices:
- Clear section headers with emojis for visual navigation
- Code blocks with syntax highlighting
- Platform-specific examples
- Troubleshooting sections
- Links to related documentation
- Professional formatting

## 🔄 Backward Compatibility

- ✅ No breaking changes
- ✅ Existing macOS scripts unchanged
- ✅ All existing documentation preserved
- ✅ Additive changes only

## 🎯 Checklist

- [x] New files created and properly formatted
- [x] Scripts are executable (`chmod +x`)
- [x] Documentation is comprehensive and clear
- [x] No hardcoded paths or assumptions
- [x] Cross-platform compatibility maintained
- [x] Commit message follows conventional commits
- [x] All changes tested locally

## 📚 Related Documentation

- `LINUX_DEPLOYMENT.md` - Main Linux deployment guide
- `QUICK_START_LINUX.md` - Quick start for both platforms
- `setup-linux.sh` - Automated Linux setup script
- `README.md` - Updated with platform-specific sections

## 🚀 Next Steps (Future PRs)

Potential follow-up enhancements:
- Windows deployment guide and PowerShell scripts
- Kubernetes Helm charts for Linux deployments
- CI/CD pipeline examples for Linux environments
- Performance benchmarks on different Linux distributions

## 💬 Notes for Reviewers

- The `setup-linux.sh` script uses interactive prompts for safety
- All package installations use official distribution repositories
- Systemd service example follows best practices
- SELinux and firewall configurations are documented but optional

---

**This PR makes AMCP v1.5 truly cross-platform and production-ready for Linux environments! 🐧🚀**
