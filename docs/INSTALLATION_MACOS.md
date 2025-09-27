# AMCP v1.5 - macOS Installation Guide

## Overview

This guide provides comprehensive instructions for setting up and running the Agent Mesh Communication Protocol (AMCP) v1.5 framework on macOS. AMCP v1.5 is a Java-based framework for building distributed agent systems with event-driven communication.

## Prerequisites

### System Requirements
- **macOS Version**: 10.14 (Mojave) or later
- **Architecture**: Intel x64 or Apple Silicon (M1/M2) supported
- **RAM**: Minimum 4GB, recommended 8GB+
- **Disk Space**: At least 2GB free space

### Required Software

#### 1. Java Development Kit (JDK)
AMCP v1.5 requires Java 8 or later.

**Option A: Install via Homebrew (Recommended)**
```bash
# Install Homebrew if not already installed
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install OpenJDK 8
brew install openjdk@8

# Add to PATH
echo 'export PATH="/opt/homebrew/opt/openjdk@8/bin:$PATH"' >> ~/.zshrc
# For Intel Macs:
echo 'export PATH="/usr/local/opt/openjdk@8/bin:$PATH"' >> ~/.zshrc

# Reload shell configuration
source ~/.zshrc
```

**Option B: Manual Installation**
1. Download OpenJDK 8 from [Adoptium](https://adoptium.net/)
2. Install the `.pkg` file
3. Verify installation: `java -version`

**Option C: Install Latest JDK**
```bash
# Install latest OpenJDK
brew install openjdk

# Link for system Java wrappers
sudo ln -sfn /opt/homebrew/opt/openjdk/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk.jdk
```

#### 2. Apache Maven
Maven is required for building and managing dependencies.

```bash
# Install Maven via Homebrew
brew install maven

# Verify installation
mvn --version
```

**Manual Installation:**
1. Download Maven from [Apache Maven](https://maven.apache.org/download.cgi)
2. Extract to `/opt/apache-maven-x.x.x`
3. Add to PATH in `~/.zshrc`:
   ```bash
   export M2_HOME=/opt/apache-maven-3.9.4
   export PATH=$M2_HOME/bin:$PATH
   ```

#### 3. Git (for development)
```bash
# Install Git via Homebrew
brew install git

# Or use Xcode Command Line Tools
xcode-select --install
```

## Installation Steps

### Step 1: Download AMCP v1.5

**Option A: Clone from GitHub**
```bash
git clone https://github.com/your-org/amcp-v1.5-opensource.git
cd amcp-v1.5-opensource
```

**Option B: Download ZIP**
1. Download the ZIP from GitHub releases
2. Extract to your preferred directory
3. Navigate to the extracted folder

### Step 2: Verify Project Structure
```bash
# Verify directory structure
tree -L 3 amcp-v1.5-opensource/
```

Expected structure:
```
amcp-v1.5-opensource/
├── README.md
├── pom.xml
├── core/
│   ├── pom.xml
│   └── src/main/java/io/amcp/
├── examples/
│   ├── pom.xml
│   └── src/main/java/io/amcp/examples/
├── connectors/
│   ├── pom.xml
│   └── src/main/java/io/amcp/connectors/
├── docs/
├── scripts/
└── .github/
```

### Step 3: Build the Project

**Full Build and Test**
```bash
cd amcp-v1.5-opensource

# Clean and compile all modules
mvn clean compile

# Run tests (optional)
mvn test

# Create JAR files
mvn package

# Install to local Maven repository
mvn install
```

**Quick Build (Skip Tests)**
```bash
mvn clean install -DskipTests
```

### Step 4: Verify Installation

**Run the Greeting Agent Example**
```bash
# Using Maven exec plugin
mvn -pl examples exec:java -Dexec.mainClass="io.amcp.examples.GreetingAgent"

# Or using Java directly
java -cp "examples/target/classes:core/target/classes" io.amcp.examples.GreetingAgent
```

**Run the Travel Planner Example**
```bash
mvn -pl examples exec:java -Dexec.mainClass="io.amcp.examples.TravelPlannerAgent"
```

**Run the Weather System Example**
```bash
mvn -pl examples exec:java -Dexec.mainClass="io.amcp.examples.weather.WeatherSystemLauncher"
```

## Configuration

### Environment Variables

Create a `~/.amcp` configuration file:
```bash
mkdir -p ~/.amcp
cat > ~/.amcp/config.properties << EOF
# AMCP v1.5 Configuration
amcp.version=1.5.0
amcp.agent.default.namespace=default
amcp.logging.level=INFO
amcp.event.broker.type=in-memory

# Weather Service Configuration (optional)
openweather.api.key=your-api-key-here
weather.update.interval=300000

# Agent Configuration
agent.scheduler.pool.size=4
agent.event.timeout=30000
EOF
```

### JVM Options

For optimal performance, add JVM options:
```bash
export JAVA_OPTS="-Xmx2g -Xms512m -server -Dfile.encoding=UTF-8 -Djava.awt.headless=true"
```

## Quick Start Guide

### 1. Basic Agent Example
```bash
cd amcp-v1.5-opensource

# Compile and run the greeting agent
./scripts/run-greeting-agent.sh
```

### 2. Interactive Travel Planner
```bash
# Run the enhanced travel planner
./scripts/run-travel-planner.sh

# Available commands in the planner:
# - plan: Create travel plans
# - dest: Add destinations  
# - weather: Get weather info
# - search: Search locations
# - demo: Run demonstration
```

### 3. Weather Collection System
```bash
# Start the weather monitoring system
./scripts/run-weather-system.sh

# Commands:
# - start: Begin weather collection
# - add <location>: Monitor new location
# - status: Show system status
```

## Development Setup

### IDE Configuration

**IntelliJ IDEA**
1. Open the `amcp-v1.5-opensource` folder
2. Import as Maven project
3. Set Project SDK to Java 8+
4. Configure code style:
   - Tab size: 4 spaces
   - Continuation indent: 8 spaces

**Visual Studio Code**
1. Install Java Extension Pack
2. Install Maven for Java extension
3. Open folder in VS Code
4. Use `Cmd+Shift+P` → "Java: Reload Projects"

### Running Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AgentTest

# Run tests with coverage (if configured)
mvn test jacoco:report
```

### Building Documentation
```bash
# Generate Javadoc
mvn javadoc:javadoc

# Generate site documentation
mvn site

# Open documentation
open target/site/index.html
```

## Troubleshooting

### Common Issues

**Issue 1: "Could not find or load main class"**
```bash
# Solution: Verify classpath and compilation
mvn clean compile
java -cp "target/classes:core/target/classes" io.amcp.examples.GreetingAgent
```

**Issue 2: Maven build failures**
```bash
# Clear Maven cache
rm -rf ~/.m2/repository/io/amcp
mvn clean install -U
```

**Issue 3: Java version conflicts**
```bash
# Check Java version
java -version
mvn -version

# Set JAVA_HOME explicitly
export JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk-8.jdk/Contents/Home
```

**Issue 4: Permission denied on scripts**
```bash
# Make scripts executable
chmod +x scripts/*.sh
```

### Performance Tuning

**For Development**
```bash
export MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=256m"
export JAVA_OPTS="-Xmx1g -Xms256m"
```

**For Production**
```bash
export JAVA_OPTS="-Xmx4g -Xms1g -server -XX:+UseG1GC -XX:MaxGCPauseMillis=100"
```

### Logging Configuration

Create `logback.xml` in `src/main/resources`:
```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <logger name="io.amcp" level="DEBUG"/>
    
    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
```

## Advanced Configuration

### Clustering Setup (Future)
AMCP v1.5 includes foundations for clustering support:

```bash
# Enable clustering mode
export AMCP_CLUSTER_MODE=true
export AMCP_CLUSTER_NODES="node1:8080,node2:8080"
```

### Custom Agent Development

Create a new agent:
```java
package com.yourcompany.agents;

import io.amcp.core.*;
import java.util.concurrent.CompletableFuture;

public class MyCustomAgent implements Agent {
    private AgentID agentId = AgentID.create("MyAgent");
    private AgentContext context;
    private AgentLifecycle lifecycle = AgentLifecycle.INACTIVE;
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            // Custom event handling logic
            System.out.println("Received: " + event.getTopic());
        });
    }
    
    // Implement other Agent interface methods...
}
```

## Production Deployment

### Packaging for Distribution
```bash
# Create distribution package
mvn clean package -P production

# Create Docker image (if Dockerfile exists)
docker build -t amcp-v1.5:latest .
```

### System Service Setup

Create a LaunchDaemon for macOS:
```bash
sudo cp scripts/io.amcp.agent.plist /Library/LaunchDaemons/
sudo launchctl load /Library/LaunchDaemons/io.amcp.agent.plist
```

## Getting Help

### Documentation
- **API Documentation**: `docs/api/`
- **Architecture Guide**: `docs/architecture.md`
- **Examples**: `examples/src/main/java/io/amcp/examples/`

### Support Channels
- **GitHub Issues**: Report bugs and feature requests
- **Discussions**: Community support and questions
- **Wiki**: Additional documentation and guides

### Version Information
```bash
# Check AMCP version
java -cp "core/target/classes" io.amcp.core.Version

# Check Maven project version
mvn help:evaluate -Dexpression=project.version -q -DforceStdout
```

## Next Steps

1. **Explore Examples**: Run all example applications
2. **Read Documentation**: Study the architecture and API docs
3. **Create Your First Agent**: Build a custom agent for your use case
4. **Join the Community**: Contribute to the project on GitHub
5. **Deploy to Production**: Use the deployment guides for your environment

## Updates and Maintenance

### Updating AMCP
```bash
# Pull latest changes (if using Git)
git pull origin main

# Rebuild project
mvn clean install

# Update dependencies
mvn versions:use-latest-versions
```

### Monitoring
Monitor AMCP applications using:
- JVM metrics with `jstat`
- Application logs
- Custom metrics published by agents

This completes the comprehensive macOS installation guide for AMCP v1.5. Follow these steps to get started with agent-based programming using the AMCP framework.