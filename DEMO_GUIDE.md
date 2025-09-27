# AMCP v1.5 Enterprise Edition - Demo & Test Suite

This comprehensive demo suite tests all major features of AMCP v1.5 Enterprise Edition including security, mobility, Kafka integration, A2A protocol bridge, MCP connectors, CloudEvents compliance, and more.

## 🚀 Quick Start

### Prerequisites
- Java 21+ LTS
- Maven 3.6+
- Docker (optional, for Kafka tests)
- 4GB+ RAM recommended

### Basic Usage

```bash
# Full interactive demo (recommended first run)
./run-full-demo.sh

# Automated demo (no user input required)
./run-full-demo.sh --auto

# Quick demo with 5-second tests
./run-full-demo.sh --auto -d 5

# Verbose logging enabled
./run-full-demo.sh -v
```

## 🎯 Feature Coverage

### 🔒 Advanced Security Suite
- **mTLS**: Mutual TLS authentication
- **RBAC**: Role-based access control
- **JWT**: Token-based authentication
- **Encryption**: End-to-end message encryption

```bash
# Test only security features
./run-full-demo.sh --no-mobility --no-kafka --no-a2a --no-mcp
```

### 🚀 Agent Mobility System
- **Strong Mobility**: Agents move with full state
- **Dispatch**: Send agents to remote hosts
- **Migration**: Move agents between hosts
- **Cloning**: Duplicate agents with state
- **Replication**: Create agent replicas across hosts

```bash
# Test only mobility features
./run-full-demo.sh --no-security --no-kafka --no-a2a --no-mcp
```

### 📨 Enhanced Kafka EventBroker
- **Production-Ready**: High-throughput messaging
- **Monitoring**: Built-in metrics and health checks
- **CloudEvents**: CNCF-compliant event format
- **Fault Tolerance**: Automatic failover

```bash
# Test only Kafka features (requires Docker or external Kafka)
./run-full-demo.sh --no-security --no-mobility --no-a2a --no-mcp
```

### 🌉 A2A Protocol Bridge
- **Google A2A**: Complete integration with Google's Agent-to-Agent protocol
- **Bidirectional**: AMCP ↔ A2A message translation
- **Protocol Translation**: Seamless format conversion

```bash
# Test only A2A bridge features
./run-full-demo.sh --no-security --no-mobility --no-kafka --no-mcp
```

### 🔧 MCP Tool Connectors
- **Model Context Protocol**: External tool integration
- **Authentication**: API key and OAuth support
- **Weather API**: Real-time weather data
- **Search API**: DuckDuckGo search integration

```bash
# Test only MCP connector features
./run-full-demo.sh --no-security --no-mobility --no-kafka --no-a2a
```

### ☁️ CloudEvents 1.0 Compliance
- **CNCF Standard**: Full CloudEvents v1.0 specification
- **Schema Validation**: Event format verification
- **Content Modes**: Structured and binary formats
- **Tracing**: Event correlation and tracking

### ✈️ Travel Planner System
- **Intelligent Planning**: Multi-modal route optimization
- **Weather Integration**: Real-time weather considerations
- **Cost Optimization**: Best price finding
- **Interactive CLI**: User-friendly command interface

### 🌤️ Weather System Integration
- **Multi-Provider**: Multiple weather API support
- **Caching**: Intelligent data caching
- **Forecasting**: Multi-day weather predictions
- **Rate Limiting**: API usage optimization

## 📊 Advanced Usage

### Custom Test Configurations

```bash
# Skip specific features
./run-full-demo.sh --no-security --no-kafka

# Extended testing duration
./run-full-demo.sh -d 30  # 30 seconds per test

# Verbose mode with automated execution
./run-full-demo.sh --auto -v

# Performance-focused testing
./run-full-demo.sh --no-travel --no-weather -d 20
```

### Configuration File

Edit `demo-config.properties` to customize:
- Test timeouts and parameters
- API endpoints and credentials
- Performance benchmark targets
- Logging and monitoring settings

### Interactive Mode Commands

When running interactive demos, try these commands:

#### Travel Planner Demo
```
status                                    # Show system status
plan "New York" "Paris" "2025-10-15"    # Create travel plan
weather "Paris"                          # Get weather info
plans                                    # List active plans
quit                                     # Exit demo
```

#### Weather System Demo
```
weather "New York"                       # Current weather
forecast "London" 5                      # 5-day forecast
status                                   # System status
quit                                     # Exit demo
```

## 📋 Test Reports

After each run, a comprehensive report is generated:
- **Filename**: `amcp-demo-report-YYYYMMDD-HHMMSS.md`
- **Content**: Feature test results, performance metrics, architecture validation
- **Format**: Markdown with tables and metrics

Example report sections:
- Executive Summary
- Feature Test Results Matrix
- Performance Benchmarks
- Architecture Validation
- Recommendations

## 🏗️ Architecture Components Tested

### Core Framework
- Agent lifecycle management
- Event-driven architecture
- Plugin-based extensibility

### Enterprise Features
- Advanced security suite
- High-availability deployment
- Comprehensive monitoring
- Performance optimization

### Integration Capabilities
- External protocol bridges (A2A)
- Tool connector ecosystem (MCP)
- Cloud-native event formats (CloudEvents)
- Production messaging (Kafka)

### Mobility System
- IBM Aglet-compatible strong mobility
- Cross-host agent migration
- State preservation and serialization
- Dynamic load balancing

## 🚨 Troubleshooting

### Common Issues

**Build Failures**:
```bash
# Clean rebuild
mvn clean compile -Dmaven.test.skip=true
```

**Port Conflicts**:
```bash
# Check for running processes
ps aux | grep java
pkill -f "io.amcp"
```

**Memory Issues**:
```bash
# Increase JVM heap size
export JAVA_OPTS="-Xmx2G -Xms1G"
./run-full-demo.sh
```

**Docker Not Available**:
```bash
# Run without Kafka tests
./run-full-demo.sh --no-kafka
```

### Performance Tuning

For optimal performance testing:
1. Close unnecessary applications
2. Increase JVM heap size (`-Xmx4G`)
3. Use SSD storage for better I/O
4. Run on dedicated test environment

## 📈 Expected Results

### Performance Benchmarks
- **Throughput**: 15,000+ messages/second
- **Latency**: <10ms p95, <20ms p99
- **Memory**: <512MB heap usage
- **Agent Migration**: <150ms average

### Feature Coverage
- ✅ 100% enterprise feature coverage
- ✅ All integration points tested
- ✅ Security validation passed
- ✅ Performance benchmarks met

## 🎉 Next Steps

After successful demo completion:
1. Review generated test report
2. Explore specific features in detail
3. Adapt configuration for your use case
4. Deploy to staging environment
5. Run in production with monitoring

## 📚 Additional Resources

- `README.md` - Project overview
- `ADVANCED_SECURITY_SUITE.md` - Security details
- `ENHANCED_KAFKA_EVENTBROKER.md` - Kafka integration
- `A2A_BRIDGE_COMPLETION.md` - A2A protocol details
- `CLOUDEVENTS_COMPLIANCE.md` - CloudEvents implementation
- `TESTING_FRAMEWORK_GUIDE.md` - Testing framework details

---
*AMCP v1.5 Enterprise Edition - Production-ready agent mesh communication*