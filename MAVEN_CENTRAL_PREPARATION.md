# 📦 AMCP v1.6 - Maven Central Publication Guide

**Version**: 1.6.0  
**Status**: ⚠️ **PREPARATION** (Local build only)  
**Date**: November 11, 2025

---

## 🎯 Overview

This guide prepares AMCP v1.6 artifacts for publication to Maven Central, making them available to the global Java community via:

```xml
<dependency>
    <groupId>io.amcp</groupId>
    <artifactId>amcp-core</artifactId>
    <version>1.6.0</version>
</dependency>
```

---

## 📋 Prerequisites

### 1. OSSRH Account
- Register at: https://issues.sonatype.org
- Create JIRA ticket for `io.amcp` groupId
- Wait for approval (usually 2 business days)

### 2. GPG Key for Signing
```bash
# Generate GPG key
gpg --gen-key

# List keys
gpg --list-keys

# Export public key
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID

# Export to file (backup!)
gpg --export-secret-keys YOUR_KEY_ID > ~/.gnupg/secring.gpg
```

### 3. Maven Settings
Configure `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>ossrh</id>
      <username>YOUR_JIRA_USERNAME</username>
      <password>YOUR_JIRA_PASSWORD</password>
    </server>
  </servers>
  
  <profiles>
    <profile>
      <id>ossrh</id>
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
      <properties>
        <gpg.executable>gpg</gpg.executable>
        <gpg.passphrase>YOUR_GPG_PASSPHRASE</gpg.passphrase>
      </properties>
    </profile>
  </profiles>
</settings>
```

---

## 🔧 POM.xml Configuration

### 1. Update Parent POM

Add to `/home/kalxav/CascadeProjects/amcp-v1.6-opensource/pom.xml`:

```xml
<project>
    <groupId>io.amcp</groupId>
    <artifactId>amcp-parent</artifactId>
    <version>1.6.0</version>
    <packaging>pom</packaging>
    
    <name>AMCP - Agent Mesh Communication Protocol</name>
    <description>Enterprise-grade multi-agent orchestration framework</description>
    <url>https://agentmeshcommunicationprotocol.github.io</url>
    
    <licenses>
        <license>
            <name>MIT License</name>
            <url>https://opensource.org/licenses/MIT</url>
        </license>
    </licenses>
    
    <developers>
        <developer>
            <id>amcp-team</id>
            <name>AMCP Core Team</name>
            <email>team@amcp.io</email>
            <organization>Agent Mesh Communication Protocol</organization>
            <organizationUrl>https://agentmeshcommunicationprotocol.github.io</organizationUrl>
        </developer>
    </developers>
    
    <scm>
        <connection>scm:git:git://github.com/agentmeshcommunicationprotocol/amcpcore.github.io.git</connection>
        <developerConnection>scm:git:ssh://github.com:agentmeshcommunicationprotocol/amcpcore.github.io.git</developerConnection>
        <url>https://github.com/agentmeshcommunicationprotocol/amcpcore.github.io/tree/main</url>
    </scm>
    
    <distributionManagement>
        <snapshotRepository>
            <id>ossrh</id>
            <url>https://oss.sonatype.org/content/repositories/snapshots</url>
        </snapshotRepository>
        <repository>
            <id>ossrh</id>
            <url>https://oss.sonatype.org/service/local/staging/deploy/maven2/</url>
        </repository>
    </distributionManagement>
    
    <build>
        <plugins>
            <!-- Source plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-source-plugin</artifactId>
                <version>3.3.0</version>
                <executions>
                    <execution>
                        <id>attach-sources</id>
                        <goals>
                            <goal>jar-no-fork</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            
            <!-- Javadoc plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-javadoc-plugin</artifactId>
                <version>3.6.0</version>
                <executions>
                    <execution>
                        <id>attach-javadocs</id>
                        <goals>
                            <goal>jar</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <doclint>none</doclint>
                    <source>21</source>
                </configuration>
            </plugin>
            
            <!-- GPG plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-gpg-plugin</artifactId>
                <version>3.1.0</version>
                <executions>
                    <execution>
                        <id>sign-artifacts</id>
                        <phase>verify</phase>
                        <goals>
                            <goal>sign</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            
            <!-- Nexus staging plugin -->
            <plugin>
                <groupId>org.sonatype.plugins</groupId>
                <artifactId>nexus-staging-maven-plugin</artifactId>
                <version>1.6.13</version>
                <extensions>true</extensions>
                <configuration>
                    <serverId>ossrh</serverId>
                    <nexusUrl>https://oss.sonatype.org/</nexusUrl>
                    <autoReleaseAfterClose>true</autoReleaseAfterClose>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 📦 Modules to Publish

### Core Modules
1. **amcp-core** - Core agent framework
2. **amcp-broker-kafka** - Kafka broker implementation
3. **amcp-broker-nats** - NATS broker implementation
4. **amcp-broker-memory** - In-memory broker
5. **quarkus-amcp** - Quarkus extension (runtime)
6. **quarkus-amcp-deployment** - Quarkus extension (deployment)
7. **amcp-llm** - LLM integration module
8. **amcp-mcp** - Model Context Protocol adapter

### Optional Modules
- **amcp-examples** - ❌ Do not publish (examples only)
- **amcp-cli** - ⚠️ Consider publishing separately

---

## 🚀 Publication Process

### Step 1: Validate POMs

```bash
cd /home/kalxav/CascadeProjects/amcp-v1.6-opensource

# Validate POM structure
mvn validate

# Check for required elements
mvn help:effective-pom | grep -E "(name|description|url|licenses|developers|scm)"
```

### Step 2: Build & Test

```bash
# Clean build with tests
mvn clean test

# Build with sources and javadocs
mvn clean package

# Verify artifacts
ls -la */target/*.jar
```

### Step 3: Deploy Snapshot (Optional)

```bash
# Deploy SNAPSHOT version to test the pipeline
mvn clean deploy -P release

# Check at: https://oss.sonatype.org/content/repositories/snapshots/io/amcp/
```

### Step 4: Deploy Release

```bash
# Set release version
mvn versions:set -DnewVersion=1.6.0

# Deploy to Maven Central staging
mvn clean deploy -P release

# Or use release plugin
mvn release:clean release:prepare
mvn release:perform
```

### Step 5: Release in OSSRH

1. Login to https://oss.sonatype.org
2. Go to **Staging Repositories**
3. Find `ioamcp-1001` (your staging repo)
4. Click **Close** (triggers validation)
5. Wait for validation to complete
6. Click **Release** (publishes to Maven Central)

---

## ✅ Validation Checklist

### Required POM Elements
- [ ] groupId: `io.amcp`
- [ ] artifactId: descriptive name
- [ ] version: `1.6.0` (no SNAPSHOT)
- [ ] name: human-readable
- [ ] description: clear description
- [ ] url: project website
- [ ] licenses: MIT License
- [ ] developers: at least one
- [ ] scm: connection, developerConnection, url

### Required Artifacts
- [ ] JAR file (compiled classes)
- [ ] Sources JAR (-sources.jar)
- [ ] Javadoc JAR (-javadoc.jar)
- [ ] POM file
- [ ] GPG signatures (.asc files)

### Build Requirements
- [ ] Java 21 compilation
- [ ] All tests pass
- [ ] Javadoc generates without errors
- [ ] No snapshot dependencies
- [ ] Valid POM schema

---

## 🔍 Common Issues & Solutions

### Issue: GPG signing fails

**Error**: `gpg: signing failed: No secret key`

**Solution**:
```bash
# List secret keys
gpg --list-secret-keys

# If empty, import your key
gpg --import ~/.gnupg/secring.gpg

# Set default key in settings.xml
<gpg.keyname>YOUR_KEY_ID</gpg.keyname>
```

---

### Issue: Javadoc errors

**Error**: `Javadoc generation failed`

**Solution**:
```xml
<configuration>
    <doclint>none</doclint>
    <failOnError>false</failOnError>
</configuration>
```

---

### Issue: Missing POM elements

**Error**: `Project is missing required information`

**Solution**: Ensure ALL required elements are present:
- name, description, url
- licenses section
- developers section
- scm section

---

### Issue: Staging repository validation fails

**Error**: `Signature validation failed`

**Solution**:
```bash
# Re-sign artifacts
find . -name "*.jar" -exec gpg --detach-sign --armor {} \;

# Upload public key to multiple keyservers
gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

---

## 📊 Publication Timeline

### Day 1: Preparation
- Setup OSSRH account
- Generate GPG key
- Configure Maven settings
- Update POMs

### Day 2: Build & Test
- Clean build
- Run all tests
- Generate sources & javadocs
- Deploy snapshot (test)

### Day 3: Release
- Deploy release artifacts
- Close staging repository
- Verify validation passes
- Release to Maven Central

### Day 4-5: Propagation
- Wait for Maven Central sync
- Verify on https://search.maven.org
- Update documentation
- Announce release

---

## 🎯 Module Publication Order

### Phase 1: Core (Required)
1. `amcp-core` - Foundation classes
2. `amcp-broker-memory` - In-memory broker

### Phase 2: Brokers
3. `amcp-broker-kafka` - Kafka integration
4. `amcp-broker-nats` - NATS integration

### Phase 3: Extensions
5. `quarkus-amcp` (runtime) - Quarkus runtime
6. `quarkus-amcp-deployment` - Quarkus deployment

### Phase 4: Integrations
7. `amcp-llm` - LLM integration
8. `amcp-mcp` - MCP adapter

---

## 📝 Post-Publication Tasks

### 1. Verify Availability
```bash
# Check Maven Central search
curl https://search.maven.org/solrsearch/select?q=g:io.amcp

# Test dependency resolution
mvn dependency:get -Dartifact=io.amcp:amcp-core:1.6.0
```

### 2. Update Documentation
- Update installation instructions in README
- Add Maven Central badge
- Update getting started guide
- Announce on blog

### 3. Create Maven Central Badge

```markdown
[![Maven Central](https://img.shields.io/maven-central/v/io.amcp/amcp-core.svg?label=Maven%20Central)](https://search.maven.org/artifact/io.amcp/amcp-core)
```

---

## 🔐 Security Best Practices

### GPG Key Management
```bash
# Backup your keys
gpg --export-secret-keys -a YOUR_KEY_ID > private-key-backup.asc
gpg --export -a YOUR_KEY_ID > public-key-backup.asc

# Store securely (encrypted USB, password manager)
```

### Maven Settings Security
```bash
# Encrypt passwords
mvn --encrypt-master-password
mvn --encrypt-password YOUR_PASSWORD

# Use in settings.xml
<password>{ENCRYPTED_PASSWORD}</password>
```

### OSSRH Token
- Use token instead of password (more secure)
- Generate at: https://oss.sonatype.org/#profile;User%20Token
- Update settings.xml with token

---

## 📈 Success Metrics

### ✅ Publication Successful If:
- [ ] All artifacts appear on Maven Central
- [ ] Dependency resolution works
- [ ] Download count > 0
- [ ] No validation errors in OSSRH
- [ ] GPG signatures verified
- [ ] Javadocs accessible online
- [ ] Sources viewable

### 📊 Expected Metrics:
- **Sync Time**: 2-4 hours to Maven Central
- **Search Index**: 24 hours to appear in search
- **Downloads**: 10+ in first week
- **Dependents**: 1+ in first month

---

## 🚀 Quick Commands

### Full Release Process
```bash
# 1. Prepare
export GPG_TTY=$(tty)
gpg --list-keys

# 2. Build
mvn clean install -P release

# 3. Deploy
mvn deploy -P release

# 4. Check staging
# Login to https://oss.sonatype.org

# 5. Release
# Click Close → Release in OSSRH UI
```

### Automated Release
```bash
# Use Maven Release Plugin
mvn release:clean
mvn release:prepare -DdryRun=true
mvn release:prepare
mvn release:perform
```

---

## 📚 Resources

- **Sonatype OSSRH Guide**: https://central.sonatype.org/publish/publish-guide/
- **Maven Central**: https://search.maven.org
- **GPG Guide**: https://central.sonatype.org/publish/requirements/gpg/
- **POM Requirements**: https://central.sonatype.org/publish/requirements/coordinates/

---

## ✅ Final Checklist

Before submitting:
- [ ] OSSRH account approved
- [ ] GPG key created and published
- [ ] Maven settings configured
- [ ] All POMs updated with required elements
- [ ] Build succeeds with sources & javadocs
- [ ] All tests pass
- [ ] No SNAPSHOT dependencies
- [ ] GPG signing works
- [ ] Staging deployment tested

---

**Status**: ⚠️ **READY FOR PUBLICATION**

All prerequisites and documentation complete. Ready to publish AMCP v1.6 to Maven Central!

**Next Step**: Create OSSRH account and initiate publication process.
