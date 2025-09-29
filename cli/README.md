# AMCP Interactive CLI - Real-time Agent Mesh Interface

## 🚀 Overview

The **AMCP Interactive CLI** provides a comprehensive command-line interface for real-time interaction with all agents in the Agent Mesh Communication Protocol (AMCP) v1.5 Enterprise Edition. This powerful tool enables developers, operators, and administrators to interact with agents, monitor system status, troubleshoot issues, and manage multi-agent workflows seamlessly.

## 🎯 Features

### 🤖 **Multi-Agent Support**
- **Travel Planner Agent**: Flight and hotel booking coordination
- **Stock Price Agent**: Real-time stock data via Polygon.io
- **Weather Agent**: Weather information via OpenWeatherMap
- **Chat Agent**: Interactive conversation capabilities
- **MultiAgent System**: Coordinated multi-agent workflows
- **Orchestrator Agent**: Agent coordination and workflow management

### 💬 **Interactive Features**
- **Real-time Agent Communication**: Direct interaction with all available agents
- **Command History**: Persistent history with search and autocompletion
- **Session Management**: Save, load, and share interaction sessions
- **Copy/Paste Support**: Easy command and response handling
- **Smart Autocompletion**: Context-aware command and agent name completion

### 📊 **Monitoring & Diagnostics**
- **Real-time Status Monitoring**: Live agent and API status updates
- **API Health Checks**: Continuous monitoring of external APIs (Polygon.io, OpenWeather)
- **Performance Metrics**: Response times, success rates, and throughput statistics
- **Agent Behavior Analysis**: Detailed activity logs and event tracing
- **System Resource Monitoring**: Memory, CPU, and network usage

### 🔧 **Troubleshooting Tools**
- **Event Tracing**: Real-time event flow visualization
- **Agent Diagnostics**: Health checks and performance analysis
- **Connectivity Testing**: Network and API endpoint validation
- **Configuration Validation**: Environment and setup verification
- **Log Export**: Comprehensive debugging information export

### 📖 **Help & Documentation**
- **Interactive Help System**: Comprehensive command documentation
- **Example Library**: Pre-built interaction examples
- **Command Demonstrations**: Live examples for all major features
- **Contextual Guidance**: Smart suggestions and tips

## 🛠️ Installation

### Prerequisites
- **Java 11+** (OpenJDK or Oracle JDK)
- **Maven 3.6+** (for building from source)
- **AMCP v1.5 Enterprise Edition** project

### Environment Setup
```bash
# Required API Keys (optional but recommended)
export POLYGON_API_KEY="your_polygon_api_key"      # For stock data
export OPENWEATHER_API_KEY="your_openweather_key"  # For weather data

# Optional Configuration
export JAVA_HOME="/path/to/java"
export AMCP_LOG_LEVEL="INFO"  # DEBUG, INFO, WARN, ERROR
```

## 🚀 Quick Start

### Method 1: Using the Launcher Script (Recommended)
```bash
# From AMCP project root
./amcp-cli

# With build (first time or after changes)
./amcp-cli --build

# With debug mode and verbose output
./amcp-cli --debug --verbose

# With custom memory settings
./amcp-cli --memory 4g

# Show all options
./amcp-cli --help
```

### Method 2: Manual Build and Run
```bash
# Build the CLI module
cd cli
mvn clean package

# Run the CLI
java -cp target/classes:../core/target/classes:../examples/target/classes:lib/* \
     io.amcp.cli.AMCPInteractiveCLI
```

## 📋 Command Reference

### 🏠 **General Commands**
```bash
help                    # Show all commands
help <command>          # Show specific command help
version                 # Show version information
clear                   # Clear screen
exit / quit             # Exit CLI
```

### 🤖 **Agent Management**
```bash
agents                  # List all available agents
agent status <name>     # Show agent status
agent activate <name>   # Activate an agent
agent deactivate <name> # Deactivate an agent
agent info <name>       # Show agent details
```

### 💬 **Agent Interaction**
```bash
travel <query>          # Interact with Travel Planner
stock <symbol>          # Get stock price
weather <location>      # Get weather information
chat <message>          # Chat with AI agent
orchestrate <task>      # Submit task to orchestrator
```

### 📊 **Status & Monitoring**
```bash
status                  # Show system status
status agents           # Show all agent status
status apis            # Show API status
status system          # Show system resources
monitor start/stop     # Control real-time monitoring
```

### 🔍 **Troubleshooting**
```bash
diagnose <agent>       # Diagnose agent health
trace on/off           # Enable/disable event tracing
connectivity           # Test connectivity
config validate        # Validate configuration
logs export <file>     # Export troubleshooting logs
logs clear             # Clear all logs
```

### 📖 **Examples & Demos**
```bash
examples               # List all examples
example <name>         # Run a specific example
demo travel           # Travel planning demo
demo stock            # Stock trading demo
demo weather          # Weather monitoring demo
demo multiagent       # Multi-agent coordination demo
```

### 📝 **Session Management**
```bash
history                # Show command history
history search <term>  # Search command history
history save <file>    # Save history to file
history load <file>    # Load history from file
session save <name>    # Save current session
session load <name>    # Load saved session
session list           # List saved sessions
```

## 🔄 **Interactive Workflows**

### Travel Planning Example
```bash
# Start CLI
./amcp-cli

# Activate travel agent
> agent activate travel

# Plan a trip
> travel "Plan a business trip from San Francisco to New York for 3 days next week"

# Check status
> status agents

# View agent activity
> diagnose travel
```

### Stock Monitoring Example
```bash
# Set up environment with API key
export POLYGON_API_KEY="your_key"

# Start CLI
./amcp-cli --build

# Get stock information
> stock AAPL
> stock "Tesla latest price and volume"

# Monitor API status
> status apis

# Test connectivity
> connectivity
```

### Multi-Agent Orchestration
```bash
# Activate orchestrator and agents
> agent activate orchestrator
> agent activate travel
> agent activate weather

# Submit complex task
> orchestrate "Plan a trip to London with weather considerations"

# Monitor execution
> trace on
> status agents

# View detailed diagnostics
> diagnose orchestrator
```

## 🎨 **Advanced Features**

### Event Tracing
```bash
# Enable real-time event tracing
> trace on

# Perform some actions
> travel "Book a hotel in Paris"
> weather "London forecast"

# Disable tracing
> trace off

# Export trace data
> logs export trace-export.txt
```

### Performance Monitoring
```bash
# Start continuous monitoring
> monitor start

# Check API metrics
> status apis

# View system resources
> status system

# Stop monitoring
> monitor stop
```

### Session Management
```bash
# Save a complex interaction session
> session save my-workflow

# Load it later
> session load my-workflow

# Share with team members
> history save shared-commands.txt
```

## 🔧 **Configuration**

### Launcher Script Options
```bash
./amcp-cli [OPTIONS]

Options:
  -h, --help           Show help message
  -v, --verbose        Enable verbose output
  -d, --debug          Enable debug mode
  -p, --profile PROF   Set profile (development, production)
  -m, --memory SIZE    Set max memory (e.g., 2g, 1024m)
  -j, --java-opts OPTS Additional Java options
  --build              Build project before running
  --clean              Clean build before running
  --no-banner          Skip banner display
```

### Environment Variables
```bash
# API Configuration
POLYGON_API_KEY         # Polygon.io API key for stock data
OPENWEATHER_API_KEY     # OpenWeatherMap API key for weather data

# System Configuration
JAVA_HOME              # Java installation directory
AMCP_LOG_LEVEL         # Logging level (DEBUG, INFO, WARN, ERROR)
AMCP_PROFILE           # Runtime profile (development, production)

# CLI Configuration
AMCP_CLI_HISTORY_SIZE  # Maximum history entries (default: 1000)
AMCP_CLI_SESSION_DIR   # Session storage directory
```

## 🔍 **Troubleshooting Guide**

### Common Issues

#### 1. **Java Not Found**
```bash
# Error: Java not found
# Solution: Install Java 11+ or set JAVA_HOME
export JAVA_HOME="/path/to/java"
./amcp-cli --verbose
```

#### 2. **Build Failures**
```bash
# Error: Build failed
# Solution: Check Maven and dependencies
mvn --version
./amcp-cli --clean --build --verbose
```

#### 3. **API Key Issues**
```bash
# Error: API calls failing
# Solution: Set API keys and test connectivity
export POLYGON_API_KEY="your_key"
export OPENWEATHER_API_KEY="your_key"
./amcp-cli
> connectivity
```

#### 4. **Agent Not Responding**
```bash
# Error: Agent not responding
# Solution: Check agent status and diagnose
> agent status travel
> diagnose travel
> trace on
> travel "test message"
```

### Debugging Commands
```bash
# Enable debug mode
./amcp-cli --debug

# Validate configuration
> config validate

# Test all connectivity
> connectivity

# Export complete diagnostics
> logs export debug-$(date +%Y%m%d-%H%M%S).txt
```

### Log Locations
```
~/.amcp/logs/                    # CLI logs directory
~/.amcp/logs/amcp-cli.log        # Main CLI log
~/.amcp/logs/troubleshooting.log # Troubleshooting log
~/.amcp/sessions/                # Saved sessions
~/.amcp/history/                 # Command history
```

## 🚀 **Performance Tips**

### Memory Optimization
```bash
# For large workloads
./amcp-cli --memory 4g

# For resource-constrained environments
./amcp-cli --memory 512m --java-opts "-XX:+UseG1GC"
```

### Development Mode
```bash
# Fast startup for development
./amcp-cli --profile development --no-banner

# With automatic recompilation
./amcp-cli --build --profile development
```

## 📚 **Integration Examples**

### Scripted Automation
```bash
#!/bin/bash
# Automated agent testing script

./amcp-cli --no-banner << 'EOF'
agent activate travel
agent activate stock
agent activate weather
travel "Plan trip to Tokyo"
stock TSLA
weather "Tokyo forecast"
status agents
logs export automation-$(date +%Y%m%d).txt
exit
EOF
```

### CI/CD Integration
```yaml
# GitHub Actions example
- name: Test AMCP CLI
  run: |
    ./amcp-cli --build --no-banner << 'EOF'
    connectivity
    config validate
    example travel
    exit
    EOF
```

## 🤝 **Support & Contributing**

- **Documentation**: See `docs/` directory for detailed guides
- **Issues**: Report bugs and feature requests on GitHub
- **Contributing**: Follow the contribution guidelines in `CONTRIBUTING.md`
- **License**: MIT License - see `LICENSE` file

## 📄 **License**

This project is licensed under the MIT License - see the [LICENSE](../LICENSE) file for details.

---

**AMCP v1.5 Enterprise Edition** - Empowering intelligent agent mesh communication with enterprise-grade reliability and scalability.