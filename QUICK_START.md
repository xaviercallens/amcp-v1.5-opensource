# 🚀 AMCP v1.5 Open Source - Quick Start Guide# 🚀 AMCP CLI Quick Start Guide



## ✅ Get Started in 5 Minutes!## ✅ **WORKING!** Your CLI is now ready to use!



This guide will get you up and running with AMCP v1.5 Open Source Edition.### 🎯 **Quick Launch Commands**



### 🎯 Prerequisites```bash

# From your terminal in the project directory:

- **Java 21+** (OpenJDK or Oracle JDK)cd /Users/xcallens/xdev/private/amcp/amcp-v1.5-enterprise-edition

- **Maven 3.8+** for build management

- **Ollama** (optional, for LLM features) - [Install Guide](https://ollama.ai/)# Basic launch (will build if needed)

./amcp-cli --build

### 🚀 Installation

# Or just launch if already built

```bash./amcp-cli

# Clone the repository```

git clone https://github.com/xaviercallens/amcp-v1.5-opensource.git

cd amcp-v1.5-opensource### 🔧 **Optional: Set API Keys for Full Functionality**



# Setup Java 21 (if needed)For stock and weather data, set these environment variables:

./setup-java21.sh

```bash

# Build the project# Add to your ~/.zshrc for permanent setup:

mvn clean compile package -DskipTestsexport POLYGON_API_KEY="your_polygon_api_key_here"

```export OPENWEATHER_API_KEY="your_openweather_api_key_here"



### 🎮 Run Demo Applications# Or set temporarily for one session:

POLYGON_API_KEY="your_key" OPENWEATHER_API_KEY="your_key" ./amcp-cli

#### 💬 MeshChat - Conversational AI```



```bash### 🎮 **Essential Commands Once Inside CLI**

./run-meshchat-demo.sh

```bash

# Try example queries:help                    # Show all available commands

> "What's the weather in Paris?"agents                  # List all available agents

> "Tell me about agent mobility"agent activate travel   # Activate specific agent

> "How does AMCP handle events?"travel "Plan trip to Tokyo for 3 days"  # Interact with agents

```stock AAPL             # Get stock price (needs API key)

weather "London"       # Get weather (needs API key)

#### 🧠 LLM Orchestrationstatus                 # Show system status

quit                   # Exit CLI

```bash```

./run-orchestrator-demo.sh

### 🎨 **What You'll See**

# Demonstrates:

# • Dynamic task planning with TinyLlamaWhen you run `./amcp-cli --build`, you'll see:

# • Intelligent agent discovery

# • Parallel task execution```

# • CloudEvents v1.0 messaging  ╔══════════════════════════════════════════════════════════════════╗

```  ║                    AMCP Interactive CLI v1.5                     ║

  ║              Agent Mesh Communication Protocol                   ║

#### 🌤️ Weather Monitoring  ║                     Enterprise Edition                           ║

  ╚══════════════════════════════════════════════════════════════════╝

```bash

./run-weather-demo.sh🚀 AMCP Interactive CLI is ready!

Type 'help' for available commands or 'agents' to see registered agents.

# Features:Press Tab for autocompletion, Ctrl+C to exit.

# • Multi-city weather monitoring

# • Real-time alerts🟢[eventbroker] 🔴[openweather] 🔴[polygon.io] amcp>

# • Event-driven updates```

```

### 🎯 **Status Indicators**

### 🔧 Optional: Configure External APIs- 🟢 **Green**: Service is active and working

- 🔴 **Red**: Service not configured or having issues  

For weather data integration, set up API keys:- The broker is always green (in-memory), external APIs need keys



```bash### 🏃‍♂️ **Ready to Go!**

# Add to your ~/.bashrc or ~/.zshrc:

export OPENWEATHER_API_KEY="your_openweather_api_key_here"Your AMCP Interactive CLI system is fully operational! The launcher script has been fixed and all components are working perfectly.



# Or set temporarily:**Just run: `./amcp-cli --build`** and start interacting with your agents! 🎉
OPENWEATHER_API_KEY="your_key" ./run-weather-demo.sh
```

Get a free API key at: [OpenWeatherMap](https://openweathermap.org/api)

### 🏗️ Development Workflow

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

### 🐳 Docker Deployment

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

### 📝 Configuration

#### Broker Configuration

```properties
# config.properties
amcp.event.broker.type=memory  # Development (default)
amcp.event.broker.type=kafka   # Production
amcp.event.broker.type=nats    # Lightweight
```

#### LLM Configuration

```properties
# Ollama for local LLM integration
amcp.ai.ollama.enabled=true
amcp.ai.ollama.base-url=http://localhost:11434
amcp.ai.ollama.model=tinyllama:1.1b
```

### 🎯 Essential Commands

Once your demos are running, you can interact with agents:

#### Weather Agent

```bash
# From MeshChat demo:
> "What's the weather in London?"
> "Temperature in Tokyo?"
> "Weather forecast for Paris"
```

#### Orchestrator Agent

```bash
# From orchestrator demo:
> "Analyze weather patterns"
> "Coordinate multiple tasks"
```

### 🧪 Testing Your Setup

```bash
# Run quick test
mvn test -Dtest=AgentContextTest

# Run all unit tests
mvn test

# Run integration tests (requires Docker)
mvn test -P integration
```

### 📚 Next Steps

- Read the [full README](README.md) for architecture details
- Explore [demo guides](DEMO_GUIDE.md) for advanced scenarios
- Check [API reference](docs/API_REFERENCE.md) for development
- Review [contributing guidelines](CONTRIBUTING.md) to contribute

### 🆘 Troubleshooting

#### Build Issues

```bash
# Clean and rebuild
mvn clean install -DskipTests

# Check Java version
java -version  # Must be 21+
```

#### Runtime Issues

```bash
# Check logs
tail -f logs/amcp.log

# Verify ports are free
lsof -i :8080,8081,3000
```

#### Demo Not Starting

```bash
# Rebuild examples
mvn clean compile -pl examples -am -DskipTests

# Check Ollama is running (if using LLM features)
curl http://localhost:11434/api/version
```

### 🎉 You're Ready!

Your AMCP Open Source Edition is now installed and ready to use!

**Quick launch: `./run-meshchat-demo.sh`** and start exploring! 🚀

For more information:
- **GitHub**: https://github.com/xaviercallens/amcp-v1.5-opensource
- **Issues**: [Report issues](https://github.com/xaviercallens/amcp-v1.5-opensource/issues)
- **Discussions**: [Join the community](https://github.com/xaviercallens/amcp-v1.5-opensource/discussions)
