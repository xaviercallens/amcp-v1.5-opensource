# Windsurf Implementation Guide for AMCP v1.6
## Step-by-Step Commands and Actions

**Version**: 1.0  
**Date**: 2024-11-10  
**Target**: AMCP v1.6 with Quarkus Extension  

---

## 📋 How to Use This Guide

This guide provides **exact commands and Windsurf IDE actions** for each implementation phase. Each step includes:

- 🔧 **Command**: Exact command to run
- 📝 **Action**: What to do in Windsurf
- ✅ **Verify**: How to validate success
- 🧪 **Test**: Testing commands

---

## Phase 0: Foundation & Setup

### Step 0.1: Create Feature Branch

#### Windsurf Actions
```bash
# In Windsurf terminal
cd /home/kalxav/CascadeProjects/amcp-v1.5-opensource

# Create and checkout feature branch
git checkout -b feature/amcp-v1.6-quarkus-implementation

# Verify branch
git branch
```

#### Verify
```bash
# Should show: * feature/amcp-v1.6-quarkus-implementation
git status
```

---

### Step 0.2: Create Project Structure

#### Windsurf Actions
1. **Create directory structure**
```bash
# Create main module directories
mkdir -p amcp-core/src/{main,test}/java
mkdir -p amcp-core/src/{main,test}/resources
mkdir -p amcp-mobility/src/{main,test}/java
mkdir -p amcp-events/src/{main,test}/java
mkdir -p amcp-security/src/{main,test}/java
mkdir -p amcp-protocols/src/{main,test}/java

# Create Quarkus extension structure
mkdir -p amcp-quarkus-extension/deployment/src/{main,test}/java
mkdir -p amcp-quarkus-extension/runtime/src/{main,test}/java

# Create connector modules
mkdir -p amcp-connectors/kafka/src/{main,test}/java
mkdir -p amcp-connectors/nats/src/{main,test}/java
mkdir -p amcp-connectors/solace/src/{main,test}/java
mkdir -p amcp-connectors/memory/src/{main,test}/java

# Create examples
mkdir -p amcp-examples/quarkus-hello/src/{main,test}/java
mkdir -p amcp-examples/quarkus-hello/src/main/resources

# Create docs structure
mkdir -p docs/{architecture,api,guides,decisions}
```

#### Verify
```bash
# List structure
tree -L 3 -d
```

---

### Step 0.3: Create Parent POM

#### Windsurf Action
**Create file**: `/amcp-v1.6-opensource/pom.xml`

**Content**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>io.amcp</groupId>
    <artifactId>amcp-parent</artifactId>
    <version>1.6.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>AMCP Parent</name>
    <description>Agent Mesh Communication Protocol v1.6</description>

    <modules>
        <module>amcp-core</module>
        <module>amcp-mobility</module>
        <module>amcp-events</module>
        <module>amcp-security</module>
        <module>amcp-protocols</module>
        <module>amcp-quarkus-extension</module>
        <module>amcp-connectors</module>
        <module>amcp-examples</module>
    </modules>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        
        <!-- Quarkus -->
        <quarkus.version>3.6.0</quarkus.version>
        
        <!-- CloudEvents -->
        <cloudevents.version>2.5.0</cloudevents.version>
        
        <!-- Testing -->
        <junit.version>5.10.1</junit.version>
        <mockito.version>5.7.0</mockito.version>
        <testcontainers.version>1.19.3</testcontainers.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Quarkus BOM -->
            <dependency>
                <groupId>io.quarkus.platform</groupId>
                <artifactId>quarkus-bom</artifactId>
                <version>${quarkus.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

#### Test
```bash
# Validate POM
mvn validate

# Should show: BUILD SUCCESS
```

---

### Step 0.4: Set Up CI/CD

#### Windsurf Action
**Create file**: `.github/workflows/amcp-v16-ci.yml`

**Content**:
```yaml
name: AMCP v1.6 CI

on:
  push:
    branches: [ feature/amcp-v1.6-quarkus-implementation ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        java: ['21']
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK ${{ matrix.java }}
        uses: actions/setup-java@v4
        with:
          java-version: ${{ matrix.java }}
          distribution: 'temurin'
          cache: maven
      
      - name: Build with Maven
        run: mvn clean install -DskipTests
      
      - name: Run Unit Tests
        run: mvn test
      
      - name: Run Integration Tests
        run: mvn verify -P integration-tests
      
      - name: Code Coverage
        run: mvn jacoco:report
      
      - name: Upload Coverage
        uses: codecov/codecov-action@v3
        with:
          files: ./target/site/jacoco/jacoco.xml
```

#### Verify
```bash
# Validate workflow
cat .github/workflows/amcp-v16-ci.yml | grep "name:"
```

---

## Phase 1: Core Refactoring

### Step 1.1: Create Core API Module

#### Windsurf Actions

1. **Create core module POM**

**File**: `amcp-core/pom.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>io.amcp</groupId>
        <artifactId>amcp-parent</artifactId>
        <version>1.6.0-SNAPSHOT</version>
    </parent>

    <artifactId>amcp-core</artifactId>
    <packaging>jar</packaging>
    
    <name>AMCP Core</name>
    <description>Core agent framework APIs</description>

    <dependencies>
        <!-- Logging -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        
        <!-- Testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

2. **Create core API interfaces**

**File**: `amcp-core/src/main/java/io/amcp/core/api/Agent.java`
```java
package io.amcp.core.api;

import java.util.concurrent.CompletableFuture;

/**
 * Base interface for all AMCP agents.
 * 
 * @since 1.6.0
 */
public interface Agent {
    
    /**
     * Gets the unique identifier for this agent.
     */
    String getId();
    
    /**
     * Called when the agent is activated.
     */
    void onActivate();
    
    /**
     * Called when the agent is deactivated.
     */
    void onDeactivate();
    
    /**
     * Handles an incoming event.
     */
    CompletableFuture<Void> handleEvent(Event event);
}
```

**File**: `amcp-core/src/main/java/io/amcp/core/api/Event.java`
```java
package io.amcp.core.api;

import java.time.Instant;
import java.util.Map;

/**
 * Represents an event in the agent mesh.
 */
public interface Event {
    String getId();
    String getTopic();
    String getSource();
    Instant getTimestamp();
    Map<String, Object> getMetadata();
    <T> T getPayload(Class<T> type);
}
```

#### Test
```bash
# Compile core module
cd amcp-core
mvn clean compile

# Should show: BUILD SUCCESS
```

---

### Step 1.2: Create Broker Abstraction

#### Windsurf Action

**File**: `amcp-core/src/main/java/io/amcp/core/api/Broker.java`
```java
package io.amcp.core.api;

import java.util.concurrent.CompletableFuture;

/**
 * Abstraction for message brokers (Kafka, NATS, etc.)
 */
public interface Broker {
    
    /**
     * Connects to the broker.
     */
    CompletableFuture<Void> connect();
    
    /**
     * Publishes an event to a topic.
     */
    CompletableFuture<Void> publish(String topic, Event event);
    
    /**
     * Subscribes to a topic pattern.
     */
    CompletableFuture<Subscription> subscribe(String topicPattern, EventHandler handler);
    
    /**
     * Disconnects from the broker.
     */
    CompletableFuture<Void> disconnect();
    
    /**
     * Checks if connected.
     */
    boolean isConnected();
}
```

**File**: `amcp-core/src/main/java/io/amcp/core/spi/BrokerProvider.java`
```java
package io.amcp.core.spi;

import io.amcp.core.api.Broker;
import java.util.Map;

/**
 * SPI for broker implementations.
 */
public interface BrokerProvider {
    
    /**
     * Gets the broker type (kafka, nats, solace, memory).
     */
    String getType();
    
    /**
     * Creates a broker instance with the given configuration.
     */
    Broker createBroker(Map<String, Object> config);
    
    /**
     * Checks if this provider supports the given type.
     */
    boolean supports(String type);
}
```

#### Test
```bash
# Compile with new interfaces
mvn clean compile

# Create simple test
mvn test
```

---

## Phase 2: Quarkus Extension

### Step 2.1: Create Extension Module Structure

#### Windsurf Actions

1. **Create extension parent POM**

**File**: `amcp-quarkus-extension/pom.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>io.amcp</groupId>
        <artifactId>amcp-parent</artifactId>
        <version>1.6.0-SNAPSHOT</version>
    </parent>

    <artifactId>amcp-quarkus-extension-parent</artifactId>
    <packaging>pom</packaging>
    
    <name>AMCP Quarkus Extension Parent</name>

    <modules>
        <module>deployment</module>
        <module>runtime</module>
    </modules>
</project>
```

2. **Create deployment module**

**File**: `amcp-quarkus-extension/deployment/pom.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>io.amcp</groupId>
        <artifactId>amcp-quarkus-extension-parent</artifactId>
        <version>1.6.0-SNAPSHOT</version>
    </parent>

    <artifactId>amcp-quarkus-extension-deployment</artifactId>
    
    <name>AMCP Quarkus Extension - Deployment</name>

    <dependencies>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-core-deployment</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-arc-deployment</artifactId>
        </dependency>
        <dependency>
            <groupId>io.amcp</groupId>
            <artifactId>amcp-quarkus-extension</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</project>
```

#### Test
```bash
# Build extension parent
cd amcp-quarkus-extension
mvn clean install -DskipTests
```

---

### Step 2.2: Implement Agent Discovery

#### Windsurf Action

**File**: `amcp-quarkus-extension/deployment/src/main/java/io/amcp/quarkus/deployment/AmcpProcessor.java`

```java
package io.amcp.quarkus.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class AmcpProcessor {
    
    private static final Logger log = Logger.getLogger(AmcpProcessor.class);
    private static final DotName AGENT = DotName.createSimple("io.amcp.core.api.Agent");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem("amcp");
    }

    @BuildStep
    List<ReflectiveClassBuildItem> discoverAgents(CombinedIndexBuildItem index) {
        List<ReflectiveClassBuildItem> reflectiveClasses = new ArrayList<>();
        
        // Find all Agent implementations
        Collection<ClassInfo> agents = index.getIndex().getAllKnownImplementors(AGENT);
        
        log.infof("Discovered %d agent classes", agents.size());
        
        for (ClassInfo agentClass : agents) {
            if (!agentClass.isAbstract()) {
                String className = agentClass.name().toString();
                log.infof("Registering agent for reflection: %s", className);
                
                reflectiveClasses.add(new ReflectiveClassBuildItem(
                    true,  // methods
                    true,  // fields
                    className
                ));
            }
        }
        
        return reflectiveClasses;
    }
}
```

#### Test
```bash
# Compile deployment module
mvn clean compile

# Verify processor
grep "AmcpProcessor" target/classes/io/amcp/quarkus/deployment/AmcpProcessor.class
```

---

### Step 2.3: Create HelloWorld Example

#### Windsurf Actions

1. **Create example project**

**File**: `amcp-examples/quarkus-hello/pom.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>io.amcp</groupId>
        <artifactId>amcp-parent</artifactId>
        <version>1.6.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <artifactId>amcp-quarkus-hello</artifactId>
    
    <name>AMCP Quarkus HelloWorld Example</name>

    <dependencies>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-arc</artifactId>
        </dependency>
        <dependency>
            <groupId>io.amcp</groupId>
            <artifactId>amcp-quarkus-extension</artifactId>
            <version>${project.version}</version>
        </dependency>
        
        <!-- Testing -->
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-junit5</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>io.quarkus</groupId>
                <artifactId>quarkus-maven-plugin</artifactId>
                <version>${quarkus.version}</version>
            </plugin>
        </plugins>
    </build>
</project>
```

2. **Create HelloWorldAgent**

**File**: `amcp-examples/quarkus-hello/src/main/java/io/amcp/examples/HelloWorldAgent.java`

```java
package io.amcp.examples;

import io.amcp.core.api.Agent;
import io.amcp.core.api.Event;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class HelloWorldAgent implements Agent {
    
    private static final Logger log = Logger.getLogger(HelloWorldAgent.class);
    
    @Override
    public String getId() {
        return "hello-world-agent";
    }
    
    @Override
    public void onActivate() {
        log.info("🌍 HelloWorld Agent is alive!");
    }
    
    @Override
    public void onDeactivate() {
        log.info("👋 HelloWorld Agent is shutting down");
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            if ("hello.request".equals(event.getTopic())) {
                String name = event.getPayload(String.class);
                log.infof("📨 Received: %s", name);
                // Response would be published here
            }
        });
    }
}
```

3. **Create test**

**File**: `amcp-examples/quarkus-hello/src/test/java/io/amcp/examples/HelloWorldAgentTest.java`

```java
package io.amcp.examples;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class HelloWorldAgentTest {
    
    @Inject
    HelloWorldAgent agent;
    
    @Test
    public void testAgentInjection() {
        assertNotNull(agent);
        assertEquals("hello-world-agent", agent.getId());
    }
    
    @Test
    public void testAgentActivation() {
        // This test verifies the agent can be activated
        assertDoesNotThrow(() -> agent.onActivate());
    }
}
```

#### Test
```bash
# Build example
cd amcp-examples/quarkus-hello
mvn clean package

# Run in dev mode
mvn quarkus:dev

# In another terminal, run tests
mvn test
```

---

## Testing Commands Summary

### Unit Tests
```bash
# Test specific module
cd amcp-core
mvn test

# Test all modules
mvn test

# Test with coverage
mvn test jacoco:report

# View coverage
open target/site/jacoco/index.html
```

### Integration Tests
```bash
# Run with Testcontainers
mvn verify -P integration-tests

# Run specific integration test
mvn verify -Dit.test=HelloWorldIntegrationTest
```

### Performance Tests
```bash
# Run performance benchmarks
mvn test -P performance-tests

# With profiling
mvn test -P performance-tests -Djmh.profilers=gc
```

---

## Validation Checklist

### Phase 0 Validation
```bash
# Check structure
tree -L 2

# Check POMs
mvn validate

# Check CI
cat .github/workflows/amcp-v16-ci.yml
```

### Phase 1 Validation
```bash
# Check core module
cd amcp-core && mvn clean compile

# Check APIs
ls -la src/main/java/io/amcp/core/api/

# Check SPI
ls -la src/main/java/io/amcp/core/spi/
```

### Phase 2 Validation
```bash
# Check extension
cd amcp-quarkus-extension && mvn clean install

# Check example
cd amcp-examples/quarkus-hello && mvn quarkus:dev

# Check agent discovery
grep "Discovered.*agent" target/quarkus.log
```

---

## Common Commands

### Build Commands
```bash
# Clean build
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Build specific module
cd amcp-core && mvn clean install

# Build native
mvn package -Pnative
```

### Development Commands
```bash
# Run Quarkus dev mode
mvn quarkus:dev

# Debug mode
mvn quarkus:dev -Ddebug=5005

# Live reload test
# Edit code while quarkus:dev is running
```

### Git Commands
```bash
# Commit progress
git add .
git commit -m "phase-X: description"

# Push to remote
git push origin feature/amcp-v1.6-quarkus-implementation

# Create checkpoint tag
git tag -a checkpoint-phase-1 -m "Phase 1 complete"
git push --tags
```

---

## Next Phase Instructions

After completing each phase, run:

```bash
# 1. Verify all tests pass
mvn clean verify

# 2. Check code quality
mvn sonar:sonar

# 3. Commit progress
git add .
git commit -m "Completed Phase X: [description]"
git push

# 4. Create phase tag
git tag -a phase-X-complete -m "Phase X Complete"
git push --tags

# 5. Move to next phase
# Open next phase documentation
```

---

**Guide Version**: 1.0  
**Last Updated**: 2024-11-10  
**Status**: Ready for Use
