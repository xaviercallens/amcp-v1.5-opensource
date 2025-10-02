# Contributing to AMCP v1.5 Open Source Edition

Thank you for your interest in contributing to the Agent Mesh Communication Protocol (AMCP) v1.5 Open Source Edition! This project is designed to be community-driven and welcomes contributions from developers, researchers, and anyone interested in distributed agent systems.

## 🎯 Project Vision

AMCP v1.5 Open Source Edition aims to provide a powerful, accessible framework for building distributed agent-based systems. We focus on:

- **Developer-friendly** architecture with minimal setup
- **Educational value** for learning multi-agent systems
- **Innovation platform** for agent mesh experiments
- **Community-driven** feature development
- **Startup-ready** scalable foundation

## 🚀 Getting Started

### Prerequisites

- **Java 21+** (OpenJDK recommended)
- **Maven 3.8+**
- **Git** for version control
- **Docker** (optional, for containerized development)
- **Ollama** (optional, for LLM integration)

### Development Setup

1. **Fork and Clone**
   ```bash
   git clone https://github.com/YOUR_USERNAME/amcp-v1.5-opensource.git
   cd amcp-v1.5-opensource
   ```

2. **Build the Project**
   ```bash
   ./setup-java21.sh
   mvn clean compile package
   ```

3. **Run Tests**
   ```bash
   mvn test -P quality
   mvn test -P integration
   ```

4. **Try the Demos**
   ```bash
   ./run-meshchat-demo.sh
   ./run-weather-demo.sh
   ./run-orchestrator-demo.sh
   ```

## 🤝 How to Contribute

### Types of Contributions

We welcome various types of contributions:

- **🐛 Bug Reports** - Help us identify and fix issues
- **💡 Feature Requests** - Suggest new capabilities
- **📝 Documentation** - Improve guides, examples, and API docs
- **🧪 Tests** - Add test coverage or improve test quality
- **🔧 Code Contributions** - Implement features, fix bugs, refactor
- **🎯 Example Agents** - Create new agent examples
- **🔌 Connectors** - Build integrations with external services
- **📊 Performance** - Optimize performance and scalability

### Development Workflow

1. **Create an Issue**
   - For bugs, provide reproduction steps
   - For features, describe use cases and benefits
   - For questions, use GitHub Discussions

2. **Fork and Branch**
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b bugfix/issue-number
   ```

3. **Develop and Test**
   - Follow our coding standards
   - Write comprehensive tests
   - Update documentation
   - Ensure 95% test coverage

4. **Commit and Push**
   ```bash
   git commit -m "feat: add new agent example for IoT integration"
   git push origin feature/your-feature-name
   ```

5. **Create Pull Request**
   - Use descriptive title and description
   - Reference related issues
   - Include test results
   - Request review from maintainers

## 🏗️ Architecture Guidelines

### Core Principles

- **Event-Driven**: All agent communication through asynchronous events
- **Mobility-First**: Support for agent migration and state transfer
- **Pluggable**: Modular brokers, connectors, and components
- **Observable**: Built-in metrics and monitoring capabilities
- **Testable**: Comprehensive testing with TestContainers

### Code Structure

```
src/main/java/io/amcp/
├── core/           # Agent interfaces and lifecycle
├── messaging/      # Event brokers and messaging
├── mobility/       # Agent migration and mobility
├── cloudevents/    # CloudEvents v1.0 compliance
└── util/          # Utilities and helpers

examples/src/main/java/io/amcp/examples/
├── meshchat/      # Conversational AI agents
├── weather/       # Weather monitoring agents
├── orchestrator/  # LLM orchestration examples
└── multiagent/    # Multi-agent coordination
```

### Coding Standards

- **Java 21 features** encouraged where appropriate
- **Async patterns** using CompletableFuture
- **Thread-safe** concurrent collections
- **CloudEvents compliant** event formats
- **Comprehensive logging** with structured messages
- **Error handling** with graceful degradation

## 🧪 Testing Guidelines

### Test Requirements

- **Unit Tests**: 95% code coverage minimum
- **Integration Tests**: TestContainers for external dependencies
- **Performance Tests**: Benchmark critical paths
- **Example Tests**: Verify all demo scenarios work

### Test Structure

```java
@Test
void shouldHandleEventCorrectly() {
    // Given
    Agent agent = new TestAgent();
    Event event = Event.builder()
        .topic("test.topic")
        .payload("test data")
        .build();
    
    // When
    CompletableFuture<Void> result = agent.handleEvent(event);
    
    // Then
    assertThat(result).succeedsWithin(Duration.ofSeconds(5));
    verify(mockBroker).publishEvent(any());
}
```

### Running Tests

```bash
# All tests
mvn test

# Specific test categories
mvn test -P unit-tests
mvn test -P integration
mvn test -P performance

# Coverage report
mvn jacoco:report
# View: target/site/jacoco/index.html
```

## 📝 Documentation Guidelines

### Documentation Types

- **API Documentation**: Comprehensive Javadoc for all public APIs
- **User Guides**: Step-by-step tutorials and examples
- **Architecture Docs**: Design decisions and patterns
- **Contributing Guides**: Development and contribution processes

### Writing Standards

- **Clear and concise** language
- **Code examples** for all concepts
- **Up-to-date** with latest changes
- **Accessible** to different skill levels

## 🔌 Creating New Components

### New Agent Examples

1. Create agent class extending `Agent` or `MobileAgent`
2. Implement required lifecycle methods
3. Add comprehensive tests
4. Create demo script
5. Update documentation

Example structure:
```java
public class MyExampleAgent implements Agent {
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        // Implementation
    }
    
    // Other required methods
}
```

### New Connectors

1. Extend `AbstractToolConnector`
2. Implement MCP protocol integration
3. Add authentication support
4. Include comprehensive tests
5. Document API usage

### New Brokers

1. Implement `EventBroker` interface
2. Add to `EventBrokerFactory`
3. Include configuration properties
4. Add integration tests
5. Update documentation

## 🎯 Feature Priorities

### Current Focus Areas

1. **LLM Integration** - Expand AI orchestration capabilities
2. **Agent Examples** - More real-world use cases
3. **Performance** - Optimization and scalability
4. **Documentation** - Comprehensive guides and tutorials
5. **Testing** - Improve coverage and test quality

### Roadmap Items

- **WebAssembly Support** - Ultra-lightweight agent runtime
- **Multi-Language SDKs** - Python, JavaScript, Rust bindings
- **Visual Tools** - Agent mesh visualization and debugging
- **Cloud Integrations** - AWS, Azure, GCP connectors

## 🐛 Reporting Issues

### Bug Reports

Please include:
- **Clear description** of the issue
- **Steps to reproduce** the problem
- **Expected vs actual** behavior
- **Environment details** (Java version, OS, etc.)
- **Log output** if relevant
- **Minimal test case** if possible

### Feature Requests

Please include:
- **Use case description** - what problem does this solve?
- **Proposed solution** - how should it work?
- **Alternatives considered** - other approaches
- **Implementation ideas** - technical approach (optional)

## 🏆 Recognition

### Contributor Levels

- **🌟 Contributor** - Made meaningful contributions
- **🚀 Core Contributor** - Regular, significant contributions
- **🏅 Maintainer** - Trusted with project governance
- **🎯 Expert** - Domain expertise in specific areas

### Recognition Program

- Monthly contributor highlights
- Annual contributor awards
- Conference speaking opportunities
- Mentorship program participation

## 📞 Community

### Communication Channels

- **GitHub Issues** - Bug reports and feature requests
- **GitHub Discussions** - General questions and ideas
- **Discord** - Real-time chat and community (coming soon)
- **Mailing List** - Announcements and development discussions

### Community Guidelines

- **Be respectful** and inclusive
- **Stay on topic** in discussions
- **Help others** learn and contribute
- **Share knowledge** and experiences
- **Give constructive feedback**

## 📄 License

By contributing to AMCP v1.5 Open Source Edition, you agree that your contributions will be licensed under the Apache License 2.0.

## 🙏 Thank You

Thank you for contributing to AMCP v1.5 Open Source Edition! Your contributions help make distributed agent systems more accessible and powerful for developers and researchers worldwide.

---

**Happy Coding! 🚀**

*Questions? Open an issue or start a discussion. We're here to help!*