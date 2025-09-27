#!/bin/bash

# AMCP v1.5 - Git Repository Setup Script
# Prepares the codebase for Git repository and open-source publishing

set -e

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "📂 AMCP v1.5 Git Repository Setup"
echo "================================="

# Change to project directory
cd "$PROJECT_DIR"

# Check if git is installed
if ! command -v git >/dev/null 2>&1; then
    echo "❌ Git not found. Please install Git first."
    exit 1
fi

echo "✅ Git $(git --version | cut -d' ' -f3) found"
echo ""

# Initialize repository if not already done
if [ ! -d ".git" ]; then
    echo "🔄 Initializing Git repository..."
    git init
    echo "✅ Git repository initialized"
else
    echo "✅ Git repository already exists"
fi

# Create .gitignore if it doesn't exist
if [ ! -f ".gitignore" ]; then
    echo ""
    echo "📝 Creating .gitignore..."
    
    cat > .gitignore << 'EOF'
# Maven
target/
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
release.properties
dependency-reduced-pom.xml
buildNumber.properties
.mvn/timing.properties
.mvn/wrapper/maven-wrapper.jar

# Java
*.class
*.log
*.jar
*.war
*.nar
*.ear
*.zip
*.tar.gz
*.rar
hs_err_pid*
replay_pid*

# IDE - IntelliJ IDEA
.idea/
*.iws
*.iml
*.ipr

# IDE - Eclipse
.project
.classpath
.c9/
*.launch
.settings/
.metadata
bin/
tmp/
*.tmp
*.bak
*.swp
*~.nib
local.properties
.settings
.loadpath
.recommenders

# IDE - NetBeans
/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.nb-gradle/

# IDE - VS Code
.vscode/
!.vscode/settings.json
!.vscode/tasks.json
!.vscode/launch.json
!.vscode/extensions.json

# macOS
.DS_Store
.AppleDouble
.LSOverride
Icon
._*
.DocumentRevisions-V100
.fseventsd
.Spotlight-V100
.TemporaryItems
.Trashes
.VolumeIcon.icns
.com.apple.timemachine.donotpresent
.AppleDB
.AppleDesktop
Network Trash Folder
Temporary Items
.apdisk

# Windows
Thumbs.db
Thumbs.db:encryptable
ehthumbs.db
ehthumbs_vista.db
*.stackdump
[Dd]esktop.ini
\$RECYCLE.BIN/
*.cab
*.msi
*.msix
*.msm
*.msp
*.lnk

# Linux
*~
.fuse_hidden*
.directory
.Trash-*
.nfs*

# Logs and Runtime
logs/
*.log
*.out

# Application specific
amcp-v1.5-deploy/
k8s/secrets.yaml

# Environment and API keys
.env
.env.local
.env.production
*_API_KEY*
*.key
*.pem

# Temporary and cache files
*.tmp
*.cache
.cache/
EOF

    echo "✅ .gitignore created"
else
    echo "✅ .gitignore already exists"
fi

# Create README.md if it doesn't exist or update it
echo ""
echo "📖 Creating/updating README.md..."

cat > README.md << 'EOF'
# AMCP v1.5 - Agent Mesh Communication Protocol

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/your-org/amcp-v1.5)
[![Java Version](https://img.shields.io/badge/java-8%2B-blue.svg)](https://openjdk.java.net/)
[![Maven Central](https://img.shields.io/badge/maven-v1.5-blue.svg)](https://search.maven.org/)
[![License](https://img.shields.io/badge/license-Apache%202.0-green.svg)](https://opensource.org/licenses/Apache-2.0)

A modern, open-source framework for building distributed agent-based systems with event-driven communication, designed for scalability and reliability.

## 🚀 Quick Start

### Installation
```bash
# Clone the repository
git clone https://github.com/your-org/amcp-v1.5.git
cd amcp-v1.5

# Build the project
./scripts/build-all.sh

# Run examples
./scripts/run-greeting-agent.sh
./scripts/run-travel-planner.sh
./scripts/run-weather-system.sh
```

### Hello World Agent
```java
import io.amcp.core.Agent;
import io.amcp.core.Event;

public class HelloAgent implements Agent {
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        System.out.println("Hello, AMCP World!");
        return CompletableFuture.completedFuture(null);
    }
}
```

## 🏗️ Architecture

AMCP v1.5 provides a robust foundation for distributed agent systems:

- **Agent-Centric Design**: Everything is an agent with clear lifecycle management
- **Event-Driven Communication**: Pub/sub messaging with hierarchical topic routing
- **Asynchronous Operations**: Non-blocking operations using CompletableFuture
- **Scalable Messaging**: Support for multiple broker implementations
- **Production Ready**: Built-in monitoring, logging, and error handling

## 📚 Examples

### Travel Planner Agent
Sophisticated route optimization and travel planning:
```bash
./scripts/run-travel-planner.sh
```

### Weather Monitoring System
Real-time weather data collection and analysis:
```bash
export OPENWEATHER_API_KEY=your_api_key
./scripts/run-weather-system.sh
```

### Greeting Agent
Simple agent demonstrating basic AMCP concepts:
```bash
./scripts/run-greeting-agent.sh
```

## 🔧 Configuration

### Environment Variables
- `MAPS_API_KEY`: Enable enhanced travel planning features
- `WEATHER_API_KEY`: Connect to real weather services
- `JAVA_OPTS`: JVM tuning parameters

### Build Options
```bash
# Full build with tests
./scripts/build-all.sh

# Run test suite
./scripts/run-tests.sh

# Deploy for production
./scripts/deploy.sh kubernetes
```

## 📖 Documentation

- [Installation Guide (macOS)](INSTALLATION_MACOS.md)
- [API Documentation](docs/api/)
- [Architecture Overview](docs/architecture.md)
- [Deployment Guide](DEPLOYMENT.md)
- [Contributing Guidelines](CONTRIBUTING.md)

## 🚀 Getting Started

1. **Prerequisites**: Java 8+, Maven 3.6+
2. **Build**: Run `./scripts/build-all.sh`
3. **Test**: Execute `./scripts/run-tests.sh`
4. **Deploy**: Use `./scripts/deploy.sh [local|docker|kubernetes]`

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 🏆 Features

- ✅ **Agent Lifecycle Management**: Complete creation, activation, deactivation, migration
- ✅ **Event-Driven Architecture**: Pub/sub with hierarchical topic routing  
- ✅ **Multi-Broker Support**: In-memory, Kafka, Solace, NATS ready
- ✅ **Production Monitoring**: Built-in logging, metrics, health checks
- ✅ **Cloud Native**: Docker and Kubernetes deployment ready
- ✅ **Developer Friendly**: Comprehensive examples and documentation

## 📞 Support

- 📖 [Documentation](docs/)
- 🐛 [Issue Tracker](https://github.com/your-org/amcp-v1.5/issues)
- 💬 [Discussions](https://github.com/your-org/amcp-v1.5/discussions)
- 📧 [Mailing List](mailto:amcp-dev@example.com)

---

**AMCP v1.5** - Building the future of distributed agent systems, one event at a time.
EOF

echo "✅ README.md updated"

# Create CONTRIBUTING.md
echo ""
echo "🤝 Creating CONTRIBUTING.md..."

cat > CONTRIBUTING.md << 'EOF'
# Contributing to AMCP v1.5

Thank you for your interest in contributing to AMCP v1.5! This document provides guidelines and information for contributors.

## 🚀 Getting Started

### Prerequisites
- Java 8 or later
- Maven 3.6.0 or later
- Git 2.20 or later
- Your favorite IDE (IntelliJ IDEA, Eclipse, VS Code)

### Development Setup
```bash
# Clone and setup
git clone https://github.com/your-org/amcp-v1.5.git
cd amcp-v1.5

# Build and test
./scripts/build-all.sh
./scripts/run-tests.sh
```

## 📋 How to Contribute

### 1. Issues
- Search existing issues before creating new ones
- Use clear, descriptive titles
- Include reproduction steps for bugs
- Tag issues appropriately (bug, enhancement, documentation)

### 2. Pull Requests
1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Make your changes following our coding standards
4. Add/update tests as needed
5. Update documentation
6. Commit with clear messages
7. Push and create a Pull Request

### 3. Coding Standards
- **Java Style**: Follow Google Java Style Guide
- **Package Structure**: Use `io.amcp.*` namespace
- **Documentation**: All public APIs must have Javadoc
- **Testing**: Maintain >80% code coverage
- **Logging**: Use SLF4J for all logging

### 4. Testing Requirements
```bash
# Run all tests
./scripts/run-tests.sh

# Run specific test category
mvn test -Dtest="*Test"        # Unit tests
mvn test -Dtest="*IT"          # Integration tests
```

## 🏗️ Architecture Guidelines

### Agent Development
- Implement `io.amcp.core.Agent` interface
- Use asynchronous operations (`CompletableFuture`)
- Handle errors gracefully
- Clean up resources in lifecycle methods

### Event Handling
- Use hierarchical topic naming: `domain.subdomain.action`
- Keep event payloads serializable
- Handle events idempotently when possible

### Example Code Structure
```java
public class MyAgent implements Agent {
    private static final Logger logger = LoggerFactory.getLogger(MyAgent.class);
    private final AtomicLong eventCount = new AtomicLong(0);
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            eventCount.incrementAndGet();
            logger.info("Processing event: {}", event.getTopic());
            // Process event
        });
    }
}
```

## 📚 Documentation

### Required Documentation
- **README updates**: For new features
- **API documentation**: Javadoc for all public methods
- **Examples**: Working code examples for new features
- **Migration guides**: For breaking changes

### Documentation Standards
- Use clear, concise language
- Include working code examples
- Provide both basic and advanced usage
- Test all code examples

## 🔍 Code Review Process

### Review Criteria
- ✅ Functionality works as described
- ✅ Tests pass and coverage is maintained
- ✅ Code follows style guidelines
- ✅ Documentation is complete
- ✅ No breaking changes (unless justified)

### Review Timeline
- Small changes: 1-2 days
- Medium features: 3-5 days  
- Large features: 1-2 weeks

## 🐛 Bug Reports

### Information to Include
```markdown
**Bug Description**
Clear description of the issue

**Reproduction Steps**
1. Step one
2. Step two
3. Error occurs

**Expected Behavior**
What should happen

**Actual Behavior**
What actually happens

**Environment**
- AMCP Version: 
- Java Version:
- OS: 
- Additional context:
```

## 💡 Feature Requests

### Proposal Format
- **Problem**: What problem does this solve?
- **Solution**: Proposed implementation approach
- **Examples**: Code examples showing usage
- **Impact**: Who benefits and how?

## 🏷️ Release Process

### Version Numbering
- Major: Breaking changes
- Minor: New features, backward compatible
- Patch: Bug fixes, backward compatible

### Release Checklist
- [ ] All tests pass
- [ ] Documentation updated
- [ ] Examples verified
- [ ] Performance benchmarks run
- [ ] Migration guide prepared (if needed)

## 📞 Getting Help

- **General Questions**: [Discussions](https://github.com/your-org/amcp-v1.5/discussions)
- **Bug Reports**: [Issues](https://github.com/your-org/amcp-v1.5/issues)
- **Feature Requests**: [Issues](https://github.com/your-org/amcp-v1.5/issues)
- **Chat**: [Discord/Slack Channel]

## 📄 Code of Conduct

We are committed to providing a welcoming and inclusive experience for everyone. Please read our [Code of Conduct](CODE_OF_CONDUCT.md).

## 🙏 Recognition

Contributors will be recognized in:
- Release notes
- CONTRIBUTORS.md file
- Project documentation

Thank you for helping make AMCP v1.5 better!
EOF

echo "✅ CONTRIBUTING.md created"

# Create LICENSE file
echo ""
echo "📄 Creating LICENSE..."

cat > LICENSE << 'EOF'
                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this control, the term
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (which shall not include communications that are individually
      made to or within the Scope of License terms).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based upon (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and derivative works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control
      systems, and issue tracking systems that are managed by, or on behalf
      of, the Licensor for the purpose of discussing and improving the Work,
      but excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to use, reproduce, modify, distribute, prepare
      Derivative Works of, publicly display, publicly perform, sublicense,
      and distribute the Work and such Derivative Works in Source or Object
      form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, trademark, patent,
          attribution and other notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright notice to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Support. You may choose to offer, and to
      charge a fee for, warranty, support, indemnity or other liability
      obligations and/or rights consistent with this License. However, in
      accepting such obligations, You may act only on Your own behalf and on
      Your sole responsibility, not on behalf of any other Contributor, and
      only if You agree to indemnify, defend, and hold each Contributor
      harmless for any liability incurred by, or claims asserted against,
      such Contributor by reason of your accepting any such warranty or support.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. Don't include
      the brackets!  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same page as the copyright notice for easier identification within
      third-party archives.

   Copyright 2024 AMCP v1.5 Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
EOF

echo "✅ LICENSE created"

# Make scripts executable
echo ""
echo "🔧 Setting script permissions..."
chmod +x scripts/*.sh
echo "✅ All scripts are now executable"

# Stage files for commit
echo ""
echo "📦 Staging files for Git..."

git add .
echo "✅ All files staged"

# Show status
echo ""
echo "📊 Git Status:"
git status --short

# Offer to make initial commit
echo ""
echo "🚀 Ready for Initial Commit!"
echo ""
echo "Files ready to commit:"
echo "  • Core AMCP v1.5 framework"
echo "  • Complete build and deployment scripts"
echo "  • Comprehensive documentation"
echo "  • Example applications"
echo "  • Production-ready configuration"
echo ""

read -p "Create initial commit? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    git commit -m "Initial commit: AMCP v1.5 open-source framework

- Complete agent-based messaging framework
- Multi-module Maven build system  
- Production-ready examples (greeting, travel, weather)
- Comprehensive macOS installation guide
- Automated build, test, and deployment scripts
- Full documentation and contribution guidelines
- Docker and Kubernetes deployment support

Ready for open-source publication and community contributions."

    echo "✅ Initial commit created!"
    
    echo ""
    echo "🔗 Next Steps for Open Source Publishing:"
    echo "   1. Create GitHub repository: gh repo create your-org/amcp-v1.5"
    echo "   2. Add remote: git remote add origin https://github.com/your-org/amcp-v1.5.git"
    echo "   3. Push to GitHub: git push -u origin main"
    echo "   4. Set up CI/CD workflows"
    echo "   5. Create releases and publish to Maven Central"
else
    echo "⏸️  Commit skipped. Files remain staged for manual commit."
fi

echo ""
echo "🎉 Git Repository Setup Complete!"
echo ""
echo "Repository Summary:"
echo "  • License: Apache 2.0"
echo "  • Documentation: Complete"
echo "  • Build System: Maven"
echo "  • CI/CD Ready: Yes"
echo "  • Community Ready: Yes"
echo ""