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
