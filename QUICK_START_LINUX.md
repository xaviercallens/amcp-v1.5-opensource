# 🚀 AMCP v1.5 - Quick Start Guide (Linux & macOS)

Get started with AMCP v1.5 Open Source Edition in under 5 minutes!

---

## 🎯 Prerequisites

- **Java 21+** (OpenJDK or Oracle JDK)
- **Maven 3.8+** for build management  
- **Ollama** (optional, for LLM features) - [Install Guide](https://ollama.ai/)

---

## 🚀 Installation

### For Linux Users 🐧

```bash
# Clone the repository
git clone https://github.com/xaviercallens/amcp-v1.5-opensource.git
cd amcp-v1.5-opensource

# Run automated Linux setup (installs Java 21, Maven, configures environment)
./setup-linux.sh

# Apply environment changes
source ~/.bashrc  # or ~/.zshrc if using zsh

# Build the project
mvn clean install -DskipTests
```

**📖 See [LINUX_DEPLOYMENT.md](LINUX_DEPLOYMENT.md) for detailed Linux-specific instructions including:**
- Distribution-specific setup (Ubuntu, RHEL, Fedora, Arch, etc.)
- Systemd service configuration
- Docker deployment on Linux
- Performance tuning
- Troubleshooting

### For macOS Users 🍎

```bash
# Clone the repository
git clone https://github.com/xaviercallens/amcp-v1.5-opensource.git
cd amcp-v1.5-opensource

# Setup Java 21 (if needed)
./setup-java21.sh

# Build the project
mvn clean install -DskipTests
```

---

## 🎮 Launch AMCP CLI

```bash
# Build and launch (first time or after changes)
./amcp-cli --build

# Or just launch if already built
./amcp-cli
```

### What You'll See

```
╔══════════════════════════════════════════════════════════════════╗
║                    AMCP Interactive CLI v1.5                     ║
║              Agent Mesh Communication Protocol                   ║
║                     Open Source Edition                          ║
╚══════════════════════════════════════════════════════════════════╝

🚀 AMCP Interactive CLI is ready!
Type 'help' for available commands or 'agents' to see registered agents.
Press Tab for autocompletion, Ctrl+C to exit.

🟢[eventbroker] 🔴[openweather] 🔴[polygon.io] amcp>
```

**Status Indicators:**
- 🟢 **Green**: Service is active and working
- 🔴 **Red**: Service not configured (needs API keys)

---

## 🔧 Optional: Configure API Keys

For full functionality with weather and stock data:

### Linux (Bash)

```bash
# Add to ~/.bashrc for permanent setup
echo 'export POLYGON_API_KEY="your_polygon_api_key_here"' >> ~/.bashrc
echo 'export OPENWEATHER_API_KEY="your_openweather_api_key_here"' >> ~/.bashrc
source ~/.bashrc
```

### macOS (Zsh)

```bash
# Add to ~/.zshrc for permanent setup
echo 'export POLYGON_API_KEY="your_polygon_api_key_here"' >> ~/.zshrc
echo 'export OPENWEATHER_API_KEY="your_openweather_api_key_here"' >> ~/.zshrc
source ~/.zshrc
```

### Temporary (Current Session Only)

```bash
export POLYGON_API_KEY="your_key"
export OPENWEATHER_API_KEY="your_key"
./amcp-cli
```

**Get Free API Keys:**
- Polygon.io: https://polygon.io/
- OpenWeather: https://openweathermap.org/api

---

## 🎯 Essential CLI Commands

Once inside the AMCP CLI:

```bash
help                    # Show all available commands
agents                  # List all available agents
agent activate travel   # Activate specific agent
travel "Plan trip to Tokyo for 3 days"  # Interact with agents
stock AAPL             # Get stock price (needs API key)
weather "London"       # Get weather (needs API key)
status                 # Show system status
quit                   # Exit CLI
```

---

## 💬 Run Demo Applications

### MeshChat - Conversational AI

```bash
./run-meshchat-demo.sh

# Try example queries:
> "What's the weather in Paris?"
> "Tell me about agent mobility"
> "How does AMCP handle events?"
```

### LLM Orchestrator

```bash
./run-orchestrator-demo.sh

# Demonstrates:
# • Dynamic task planning with TinyLlama
# • Intelligent agent discovery
# • Parallel task execution
# • CloudEvents v1.0 messaging
```

### Weather Monitoring

```bash
./run-weather-demo.sh

# Features:
# • Multi-city weather monitoring
# • Real-time alerts
# • Event-driven updates
```

---

## 🐳 Docker Deployment

### Quick Start with Docker Compose

```bash
# Launch full stack with monitoring
cd deploy/docker
docker-compose up -d

# Services available:
# • AMCP agent contexts: http://localhost:8080
# • Prometheus metrics: http://localhost:8081/metrics
# • Grafana dashboards: http://localhost:3000 (admin/admin)

# Check agent status
curl http://localhost:8080/api/v1.5/agents

# Shut down
docker-compose down
```

---

## 🏗️ Development Workflow

```bash
# Build without tests (faster)
mvn clean compile -DskipTests

# Run tests
mvn test

# Run with code quality checks
mvn test -P quality

# Run integration tests
mvn test -P integration

# Generate coverage report
mvn jacoco:report
# View: target/site/jacoco/index.html
```

---

## 📝 Configuration

### Broker Configuration

```properties
# config.properties
amcp.event.broker.type=memory  # Development (default)
amcp.event.broker.type=kafka   # Production
amcp.event.broker.type=nats    # Lightweight
```

### LLM Configuration

```properties
# Ollama for local LLM integration
amcp.ai.ollama.enabled=true
amcp.ai.ollama.base-url=http://localhost:11434
amcp.ai.ollama.model=tinyllama:1.1b
```

---

## 🧪 Testing Your Setup

```bash
# Run quick test
mvn test -Dtest=AgentContextTest

# Run all unit tests
mvn test

# Run integration tests (requires Docker)
mvn test -P integration
```

---

## 🆘 Troubleshooting

### Build Issues

```bash
# Clean and rebuild
mvn clean install -DskipTests

# Check Java version
java -version  # Must be 21+

# Check Maven version
mvn -version   # Should be 3.8+
```

### Runtime Issues

```bash
# Check logs
tail -f logs/amcp.log

# Verify ports are free (Linux)
sudo ss -tulpn | grep -E '8080|8081|3000'

# Verify ports are free (macOS)
lsof -i :8080,8081,3000
```

### Demo Not Starting

```bash
# Rebuild examples
mvn clean compile -pl examples -am -DskipTests

# Check Ollama is running (if using LLM features)
curl http://localhost:11434/api/version
```

### Linux-Specific Issues

See [LINUX_DEPLOYMENT.md](LINUX_DEPLOYMENT.md) for:
- Java version management
- Port conflicts
- Permission issues
- Firewall configuration
- SELinux troubleshooting
- Systemd service setup

---

## 📚 Next Steps

- **Architecture**: Read the [full README](README.md) for architecture details
- **Demos**: Explore [demo guides](DEMO_SCRIPTS_README.md) for advanced scenarios
- **API Reference**: Check [API documentation](docs/API_REFERENCE.md) for development
- **Contributing**: Review [contributing guidelines](CONTRIBUTING.md) to contribute
- **Linux Deployment**: See [LINUX_DEPLOYMENT.md](LINUX_DEPLOYMENT.md) for production setup

---

## 🎉 You're Ready!

Your AMCP Open Source Edition is now installed and ready to use!

**Quick launch:** `./amcp-cli --build` and start exploring! 🚀

### Support

- **GitHub**: https://github.com/xaviercallens/amcp-v1.5-opensource
- **Issues**: [Report issues](https://github.com/xaviercallens/amcp-v1.5-opensource/issues)
- **Discussions**: [Join the community](https://github.com/xaviercallens/amcp-v1.5-opensource/discussions)

---

**Platform-Specific Guides:**
- 🐧 **Linux**: [LINUX_DEPLOYMENT.md](LINUX_DEPLOYMENT.md)
- 🍎 **macOS**: Current setup scripts optimized for macOS
- ☁️ **Cloud**: [DEPLOYMENT.md](DEPLOYMENT.md) for Kubernetes and cloud platforms
