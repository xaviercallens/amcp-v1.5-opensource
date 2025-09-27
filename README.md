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
