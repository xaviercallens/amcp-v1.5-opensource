# AMCP v1.5 Open Source Edition

<div align="center">

![AMCP Logo](https://img.shields.io/badge/AMCP-v1.5-blue?style=for-the-badge)
[![MIT License](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21-orange?style=for-the-badge)](https://openjdk.org/projects/jdk/21/)
[![Build Status](https://img.shields.io/github/workflow/status/xaviercallens/amcp-v1.5-opensource/CI?style=for-the-badge)](https://github.com/xaviercallens/amcp-v1.5-opensource/actions)

**The Future of Multi-Agent Systems is Here**

*Enterprise-grade mobile agent framework with LLM orchestration, now open source*

[🚀 Quick Start](#-quick-start) • [📚 Documentation](#-documentation) • [🏗️ Architecture](#️-architecture) • [🎯 Examples](#-examples) • [🤝 Contributing](#-contributing)

</div>

---

## 🌟 Why AMCP?

AMCP (Agent Mobility Communication Protocol) v1.5 isn't just another agent framework—it's a **paradigm shift** that brings enterprise-grade multi-agent systems to everyone.

### 🆚 How AMCP Stands Out

| Feature | AMCP v1.5 | LangChain | AutoGen | CrewAI |
|---------|-----------|-----------|---------|--------|
| **Agent Mobility** | ✅ IBM Aglet-style strong mobility | ❌ | ❌ | ❌ |
| **Multi-Language** | ✅ Java, Python, Rust, C# | 🟡 Python-first | 🟡 Python-first | 🟡 Python-first |
| **Event-Driven** | ✅ Pub/Sub at core | 🟡 Chain-based | 🟡 Sequential | 🟡 Workflow-based |
| **Enterprise Ready** | ✅ Kafka, NATS, Solace | ❌ | ❌ | ❌ |
| **LLM Integration** | ✅ Ollama, OpenAI, Azure | ✅ | ✅ | ✅ |
| **Protocol Bridge** | ✅ A2A, CloudEvents | ❌ | ❌ | ❌ |
| **Kubernetes Native** | ✅ Helm, Istio ready | 🟡 Basic | 🟡 Basic | 🟡 Basic |

### 🎯 Perfect For

- **Startups** building next-gen AI products
- **Developers** creating distributed AI systems
- **Enterprises** scaling agent deployments
- **Researchers** exploring multi-agent coordination
- **Teams** migrating from monolithic AI to distributed agents

---

## 🚀 Quick Start

Get your first agent running in **under 5 minutes**:

### Prerequisites
- **Java 21+** (required)
- Maven 3.8+
- Docker (optional, for advanced demos)

### One-Command Setup

```bash
# Clone and setup
git clone https://github.com/xaviercallens/amcp-v1.5-opensource.git
cd amcp-v1.5-opensource

# Auto-setup Java 21 and build
./get-started.sh
```

### Your First Agent

```java
// HelloWorldAgent.java
public class HelloWorldAgent extends AbstractMobileAgent {
    @Override
    public void onActivate() {
        super.onActivate();
        subscribe("hello.**");
        logMessage("🌍 HelloWorld Agent is alive!");
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            if (event.getTopic().equals("hello.request")) {
                String message = event.getPayload(String.class);
                logMessage("📨 Received: " + message);
                
                // Respond to the sender
                publishEvent("hello.response", "Hello, " + message + "!");
            }
        });
    }
}
```

**Run it:**
```bash
mvn compile exec:java -Dexec.mainClass="HelloWorldAgent"
```

That's it! Your agent is now part of a distributed mesh, ready to communicate with other agents across the network.

---

## 🏗️ Architecture

AMCP v1.5 implements a **revolutionary agent mobility model** inspired by IBM Aglets, enhanced with modern cloud-native patterns:

```
┌─────────────────────────────────────────────────────────────┐
│                    AMCP Agent Mesh                           │
├─────────────────┬─────────────────┬─────────────────────────┤
│   Agent Layer   │  Protocol Layer │    Transport Layer      │
│                 │                 │                         │
│ ┌─────────────┐ │ ┌─────────────┐ │ ┌─────────────────────┐ │
│ │WeatherAgent │ │ │ CloudEvents │ │ │   Kafka/NATS/       │ │
│ │TravelAgent  │ │ │   Events    │ │ │   Solace/Memory     │ │
│ │OrchestrAtor │ │ │ A2A Bridge  │ │ │                     │ │
│ └─────────────┘ │ └─────────────┘ │ └─────────────────────┘ │
└─────────────────┴─────────────────┴─────────────────────────┘
```

### Core Innovation: **Strong Mobility**

Unlike traditional frameworks, AMCP agents can **physically move** between contexts:

```java
// Agent migrates from cloud to edge device
agent.dispatch("edge-device-paris")
     .thenRun(() -> logMessage("Now running on Paris edge!"));

// Clone for high availability
agent.clone("backup-datacenter")
     .thenRun(() -> logMessage("Backup created!"));
```

### Event-Driven at Core

Every interaction is an **event**, enabling:
- **Loose coupling** between agents
- **Scalable pub/sub** patterns  
- **Protocol interoperability**
- **Async-first** design

---

## 🎯 Examples & Demos

### 🌤️ Real-Time Weather Agent
```bash
./scripts/demos/run-weather-demo.sh
```
Connects to OpenWeatherMap, monitors 5 cities, publishes weather events

### 🤖 LLM-Powered Chat Agent  
```bash
./scripts/demos/run-meshchat-demo.sh
```
**TinyLlama integration** - conversational AI agent with memory

### 🎼 Multi-Agent Orchestration
```bash
./scripts/demos/run-orchestrator-demo.sh
```
**Smart routing** - LLM analyzes requests and routes to appropriate specialized agents

### 🎮 Interactive Demo Launcher
```bash
./amcp-demos.sh
```
**Guided experience** with 4 learning tracks from beginner to power user

---

## ⚡ Performance & Scale

### Benchmarks (Java 21, 8-core, 16GB RAM)

| Metric | Local (Memory) | Kafka Cluster | NATS Cluster |
|--------|---------------|---------------|--------------|
| **Throughput** | 50K+ events/sec | 25K+ events/sec | 35K+ events/sec |
| **Latency (P99)** | <1ms | <5ms | <3ms |
| **Agent Startup** | <100ms | <200ms | <150ms |
| **Migration Time** | <50ms | <500ms | <300ms |

### Real Production Scale
- ✅ **1000+ agents** per context tested
- ✅ **Multi-region** deployments on AWS/Azure/GCP
- ✅ **Kubernetes** autoscaling with HPA
- ✅ **Zero-downtime** agent migrations

---

## 🔧 Integration Ecosystem

### 🧠 LLM Platforms
```java
// Ollama (Local LLMs)
aiConnector.generateResponse("Analyze this data", "tinyllama");

// OpenAI integration  
openAIAgent.processQuery("What's the weather in Tokyo?");

// Azure OpenAI
azureAgent.enhanceResponse(userQuery, context);
```

### 📨 Message Brokers
```properties
# Kafka for enterprise scale
amcp.event.broker.type=kafka
amcp.kafka.bootstrap.servers=kafka-cluster:9092

# NATS for cloud-native
amcp.event.broker.type=nats
amcp.nats.servers=nats://nats-cluster:4222

# Solace for financial services  
amcp.event.broker.type=solace
amcp.solace.host=solace.company.com
```

### 🌐 Protocol Bridges
- **Google A2A** - Bidirectional compatibility
- **CloudEvents 1.0** - Azure Event Grid, AWS EventBridge
- **MCP (Model Context Protocol)** - Tool integration standard

---

## 🐳 Deployment Options

### Local Development
```bash
# In-memory broker (fastest)
./get-started.sh
```

### Docker Compose
```bash
cd deploy/docker
docker-compose up -d
# Includes: Kafka, Prometheus, Grafana, AMCP contexts
```

### Kubernetes (Production)
```bash
helm repo add amcp https://charts.amcp.io
helm install amcp-mesh amcp/amcp-platform \
  --set broker.type=kafka \
  --set monitoring.enabled=true
```

### Cloud Platforms
- **AWS EKS** - CloudFormation templates included
- **Azure AKS** - ARM templates + Service Bus integration  
- **Google GKE** - Deployment manifests + Pub/Sub connector

---

## 📚 Documentation

### 🎓 Learning Path
1. **[Quick Start Guide](docs/guides/QUICK_START.md)** - 5-minute setup
2. **[Developer Guide](docs/guides/DEVELOPER_GUIDE.md)** - Build your first agent
3. **[Architecture Guide](docs/guides/ARCHITECTURE.md)** - Deep technical dive
4. **[API Reference](docs/guides/API_REFERENCE.md)** - Complete method documentation

### 📖 Specialized Guides
- **[LLM Integration](docs/guides/LLM_INTEGRATION.md)** - Ollama, OpenAI, Azure
- **[Deployment Guide](docs/guides/DEPLOYMENT.md)** - K8s, Docker, Cloud  
- **[Security Guide](docs/guides/SECURITY.md)** - Authentication, authorization
- **[Migration Guide](docs/guides/MIGRATION.md)** - Agent mobility patterns

---

## 🤝 Contributing

We welcome contributions from **developers**, **startups**, and **enterprises**!

### 🎯 Contribution Areas

| Area | Skill Level | Impact |
|------|-------------|--------|
| **Language SDKs** | Intermediate | 🔥 High - Enable new ecosystems |
| **Broker Connectors** | Advanced | 🔥 High - Enterprise adoption |
| **Example Agents** | Beginner | 🟡 Medium - Community growth |
| **Documentation** | All levels | 🟡 Medium - Developer experience |
| **Performance** | Expert | 🔥 High - Production readiness |

### 🚀 Quick Contribution
```bash
# 1. Fork the repo
git clone https://github.com/your-username/amcp-v1.5-opensource.git

# 2. Create feature branch
git checkout -b feature/my-awesome-agent

# 3. Develop & test
mvn clean compile test
./scripts/demos/run-test-demo.sh

# 4. Submit PR with tests
```

### 📋 Development Setup
```bash
# Install development tools
./scripts/setup-dev-environment.sh

# Run full test suite
mvn clean test -P quality -P integration

# Check code quality
mvn spotbugs:check checkstyle:check pmd:check
```

### 🏆 Recognition
- **Contributors** get credited in releases
- **Major contributors** get maintainer status
- **Corporate contributors** get enterprise support priority

---

## 🎯 Use Cases

### 🏢 Enterprise Scenarios
- **Financial Services**: Risk analysis agents with regulatory compliance
- **Healthcare**: Patient data processing with HIPAA-compliant mobility
- **Manufacturing**: IoT sensor agents migrating to edge for real-time control
- **Retail**: Inventory optimization agents across store networks

### 🚀 Startup Innovation
- **AI-First SaaS**: Multi-tenant agent platforms
- **EdTech**: Personalized learning agents that adapt to students
- **FinTech**: Trading strategy agents with real-time market data
- **HealthTech**: Diagnostic agents collaborating across medical specialties

### 🔬 Research Applications
- **Multi-Agent Reinforcement Learning** at scale
- **Distributed AI** algorithm validation
- **Agent coordination** protocol research
- **Emergent behavior** studies in large agent populations

---

## 🛣️ Roadmap

### 🎯 v1.5 (Current) - Developer Experience
- ✅ Multi-language SDKs (Python, Rust, C#)
- ✅ LangChain/Semantic Kernel integration
- ✅ Enhanced CLI tools and dashboard
- ✅ CloudEvents 1.0 compliance

### 🚀 v1.6 (Q2 2024) - Enterprise Features  
- 🔄 Advanced security policies and governance
- 🔄 Kubernetes operator for auto-scaling
- 🔄 Enhanced monitoring and alerting
- 🔄 Service mesh integration (Istio, Linkerd)

### 🌟 v2.0 (Q4 2024) - Cloud Platform
- 🔄 AMCP Cloud SaaS platform
- 🔄 Visual agent workflow designer
- 🔄 Marketplace for community agents
- 🔄 Enterprise admin console

---

## 💬 Community & Support

### 🌐 Get Connected
- **GitHub Discussions**: [Ask questions, share projects](https://github.com/xaviercallens/amcp-v1.5-opensource/discussions)
- **Discord Server**: [Real-time community chat](https://discord.gg/amcp)
- **Stack Overflow**: Tag your questions with `amcp`
- **LinkedIn**: Follow [@AMCPFramework](https://linkedin.com/company/amcp)

### 📧 Enterprise Inquiries
- **Email**: enterprise@amcp.io
- **Schedule Call**: [calendly.com/amcp-enterprise](https://calendly.com/amcp-enterprise)

### 🐛 Bug Reports & Feature Requests
- **Issues**: [GitHub Issues](https://github.com/xaviercallens/amcp-v1.5-opensource/issues)
- **Security**: security@amcp.io (GPG key available)

---

## 📜 License & Legal

**MIT License** - Use AMCP freely in commercial and open source projects.

```
Copyright (c) 2024 AMCP Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software...
```

**Patent Promise**: We commit to not assert patents against open source implementations of AMCP.

---

## 🙏 Acknowledgments

AMCP v1.5 builds on decades of research and engineering:

- **IBM Aglets** - Pioneer of mobile agent systems
- **Google A2A** - Modern agent communication protocols  
- **CloudEvents CNCF** - Event standardization
- **Spring Framework** - Enterprise Java patterns
- **Kubernetes Community** - Cloud-native orchestration

Special thanks to our **enterprise partners**, **open source contributors**, and the broader **multi-agent systems research community**.

---

<div align="center">

**Ready to build the future of AI?**

[🚀 Start Building](./get-started.sh) • [📖 Read Docs](docs/) • [💬 Join Community](https://discord.gg/amcp)

---

*Made with ❤️ by the AMCP community*

[![GitHub stars](https://img.shields.io/github/stars/xaviercallens/amcp-v1.5-opensource?style=social)](https://github.com/xaviercallens/amcp-v1.5-opensource/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/xaviercallens/amcp-v1.5-opensource?style=social)](https://github.com/xaviercallens/amcp-v1.5-opensource/network)

</div>