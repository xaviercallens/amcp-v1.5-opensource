# AMCP v1.5 Open Source Edition# 🚀 AMCP v1.5 - Agent Mesh Communication Protocol# AMCP v1.5 Open Source Edition - Agent Mesh Communication Protocol# AMCP v1.5 Open Source Edition - Agent Mesh Communication Protocol



**Agent Mesh Communication Protocol** - A powerful, production-ready framework for building distributed multi-agent systems



[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

[![Java](https://img.shields.io/badge/Java-21+-blue.svg)](https://openjdk.org/)

[![Maven](https://img.shields.io/badge/Maven-3.8+-green.svg)](https://maven.apache.org/)[![Java Version](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.java.net/)

[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)

[![Build Status](https://img.shields.io/badge/Build-Passing-green.svg)]()[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

[![Coverage](https://img.shields.io/badge/Coverage-95%25-brightgreen.svg)]()

## 🌟 What is AMCP?

[![Open Source](https://img.shields.io/badge/Open%20Source-Community-brightgreen.svg)]()[![Java Version](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.java.net/)[![Java Version](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.java.net/)

AMCP (Agent Mesh Communication Protocol) is an **open-source framework** for building intelligent, distributed multi-agent systems. Inspired by IBM Aglets' strong mobility and enhanced with modern AI capabilities, AMCP makes it easy to create agents that can:



- **Communicate asynchronously** via event-driven messaging

- **Move dynamically** across contexts at runtime (strong mobility)> **Build intelligent, distributed multi-agent systems with the simplicity of function calls and the power of event-driven architecture.**[![Build Status](https://img.shields.io/badge/Build-Passing-green.svg)]()[![Build Status](https://img.shields.io/badge/Build-Passing-green.svg)]()

- **Integrate with LLMs** for intelligent decision-making (Ollama, TinyLlama, Spring AI)

- **Interoperate** with Google A2A protocol and CloudEvents 1.0

- **Scale seamlessly** from in-memory development to production Kafka/NATS clusters

---[![Open Source](https://img.shields.io/badge/Open%20Source-Community-brightgreen.svg)]()[![Coverage](https://img.shields.io/badge/Coverage-95%25-brightgreen.svg)]()

Whether you're building a smart home system, AI chatbot mesh, or distributed IoT platform, AMCP provides the foundation for reliable multi-agent coordination.



---

## 🎯 Why AMCP?[![Open Source](https://img.shields.io/badge/Open%20Source-Community-brightgreen.svg)]()

## 🚀 Quick Start



### Prerequisites

AMCP is an **open-source framework** that makes building multi-agent systems as easy as writing regular code, while giving you enterprise-grade capabilities out of the box:## 🚀 Overview[![Developer Friendly](https://img.shields.io/badge/Developer-Friendly-blue.svg)]()

- **Java 21+** (OpenJDK or Oracle JDK)

- **Maven 3.8+** for building

- **Ollama** (optional, for LLM features)

- **🧠 LLM-Native**: Built-in orchestration with TinyLlama/Ollama - agents that think and coordinate

### Get Started in 5 Minutes

- **🚀 True Mobility**: Agents can move across servers at runtime (dispatch, clone, migrate) - IBM Aglet-style

```bash

# Clone the repository- **🌐 Protocol Bridge**: Native Google A2A compatibility + CloudEvents 1.0 - integrate anything**AMCP v1.5 Open Source Edition** is a powerful framework for building distributed multi-agent systems. Designed for developers, startups, and the open source community, this edition provides core capabilities for creating intelligent agent-based applications.## 🚀 Open Source Overview

git clone https://github.com/xaviercallens/amcp-v1.5-opensource.git

cd amcp-v1.5-opensource- **📦 Zero Infrastructure**: Works with in-memory, Kafka, NATS, or Solace - your choice



# Setup Java 21 environment- **🔌 Tool Integration**: MCP protocol support - call any external API or service

./setup-java21.sh

- **🛡️ Production Ready**: 95% test coverage, formal verification, monitoring built-in

# Build the project

mvn clean compile### Key Features**AMCP v1.5 Open Source Edition** is a powerful, developer-friendly framework for building distributed agent-based systems. Designed for developers, startups, and the open source community, this edition provides the core capabilities of AMCP without enterprise complexity, making it perfect for learning, prototyping, and building innovative multi-agent applications.



# Run your first demo - Weather monitoring### Quick Comparison

./scripts/demos/run-weather-demo.sh

```



That's it! You now have a working multi-agent system with a Weather monitoring agent responding to events.| Feature | AMCP | LangChain | AutoGen | CrewAI |



---|---------|------|-----------|---------|--------|- **🧠 LLM-Powered Orchestration** - TinyLlama/Ollama integration for intelligent agent coordination### � Open Source Value Proposition



## ✨ Key Features| Agent Mobility (runtime migration) | ✅ | ❌ | ❌ | ❌ |



### 🎯 Core Capabilities| Distributed by Design | ✅ | ❌ | ⚠️ | ⚠️ |- **🌐 Multi-Protocol Integration** - Google A2A bridge, CloudEvents 1.0 compliance



#### **Event-Driven Architecture**| Multi-Protocol (A2A, CloudEvents) | ✅ | ❌ | ❌ | ❌ |

- Asynchronous pub/sub messaging

- Topic-based routing with hierarchical patterns (`weather.**`, `travel.request.*`)| LLM Orchestration | ✅ | ✅ | ✅ | ✅ |- **🚀 IBM Aglet-style Mobility** - Strong agent mobility (dispatch, clone, retract, migrate)- **🧠 LLM-Powered Orchestration** - TinyLlama/Ollama integration for intelligent agent coordination

- Multiple broker support: In-Memory, Kafka, NATS, Solace PubSub+

| Event-Driven Architecture | ✅ | ⚠️ | ⚠️ | ❌ |

#### **IBM Aglet-Style Strong Mobility**

- `dispatch()` - Move agent to remote context| Built-in Observability | ✅ | ⚠️ | ❌ | ❌ |- **📊 Built-in Observability** - Prometheus, Grafana integration- **🌐 Multi-Protocol Integration** - Google A2A bridge, CloudEvents 1.0 compliance, OAuth2/JWT

- `clone()` - Create agent copy in another context

- `retract()` - Recall agent from remote context

- `migrate()` - Intelligent migration with load balancing

- `replicate()` - High-availability replication---- **🧪 Comprehensive Testing** - TestContainers, 95% test coverage- **🚀 Developer-Ready Architecture** - Docker support, easy local development, minimal setup

- `federate()` - Collaborative agent federations



#### **LLM Integration**

- **Ollama/TinyLlama** integration for local AI## ⚡ Quick Start (5 minutes)- **💬 Interactive Examples** - MeshChat conversational AI, weather services- **📊 Built-in Observability** - Prometheus, Grafana integration for monitoring

- **Spring AI** support for multiple LLM providers

- **OrchestratorAgent** for intelligent task coordination

- **MeshChat** - Multi-agent conversational AI

### Prerequisites- **🧪 Comprehensive Testing** - TestContainers, performance benchmarks

#### **Protocol Interoperability**

- **Google A2A Protocol Bridge** - Bidirectional A2A compatibility- Java 21+ (OpenJDK recommended)

- **CloudEvents 1.0 Compliance** - Standard event metadata

- **MCP (Model Context Protocol)** - External tool integration- Maven 3.8+## 🏗️ Architecture- **� Interactive Agent Examples** - MeshChat conversational AI, weather services



### 🔧 Developer Features- (Optional) Docker for advanced demos



- **Interactive CLI** - Full-featured command-line interface- **🛡️ Production-Ready** - 95% test coverage, formal verification, scalable design

- **Multiple Language SDKs** - Java (stable), Python/Rust (planned)

- **Comprehensive Examples** - Weather, Travel, Stock, MeshChat agents### 1️⃣ Clone and Build

- **Hot Reload** - Agent deployment without restart

- **Testing Framework** - Unit, integration, and chaos testing```



### 🏗️ Production-Ready```bash



- **Kubernetes/Docker Deployment** - Production manifests includedgit clone https://github.com/xaviercallens/amcp-v1.5-opensource.git┌─────────────────────────────────────────────────────────────────────────────┐## 🎯 Open Source Features & Demonstrations

- **Monitoring & Observability** - Prometheus, Grafana integration

- **Security** - TLS/mTLS, OAuth2, topic-level ACLscd amcp-v1.5-opensource

- **High Availability** - Multi-region, failover, replication

│                    AMCP v1.5 Open Source Architecture                       │

---

# Setup Java 21 and build

## 📖 Table of Contents

./setup-java21.sh├─────────────────────────────────────────────────────────────────────────────┤### 🧠 **LLM-Powered Agent Orchestration**

- [Architecture Overview](#architecture-overview)

- [Getting Started](#getting-started)mvn clean install -DskipTests

- [Building Your First Agent](#building-your-first-agent)

- [Interactive Demos](#interactive-demos)```│  🧠 LLM Orchestration: TinyLlama, Ollama Integration                        │**Open Source Innovation**: Revolutionary AI-powered multi-agent coordination with local LLM integration.

- [LLM Integration](#llm-integration)

- [Deployment](#deployment)

- [Configuration](#configuration)

- [Documentation](#documentation)### 2️⃣ Run Your First Agent├─────────────────────────────────────────────────────────────────────────────┤

- [Contributing](#contributing)

- [License](#license)



---```bash│  🤖 Example Agents: MeshChat, Weather, Orchestrator                        │```bash



## 🏛️ Architecture Overview# Start the weather agent demo



```./run-weather-demo.sh├─────────────────────────────────────────────────────────────────────────────┤# Experience intelligent orchestration

┌─────────────────────────────────────────────────────────────┐

│                    AMCP Event Mesh                         │```

│  ┌─────────────┐  ┌─────────────┐  ┌──────────────┐      │

│  │  Weather    │  │   Travel    │  │  Stock       │      ││  🔗 Protocols: Google A2A Bridge, CloudEvents 1.0, OAuth2/JWT              │./run-orchestrator-demo.sh

│  │  Agent      │  │   Agent     │  │  Agent       │      │

│  └──────┬──────┘  └──────┬──────┘  └──────┬───────┘      │Expected output:

│         │                 │                 │               │

│         └─────────────────┼─────────────────┘               │```├─────────────────────────────────────────────────────────────────────────────┤

│                           │                                 │

│           ┌───────────────┴────────────────┐               │🚀 AMCP v1.5 Open Source Edition - Weather Agent Demo

│           │      EventBroker (Pluggable)    │               │

│           │  • InMemory  • Kafka  • NATS   │               │✓ AgentContext initialized│  🚀 Strong Mobility: dispatch(), clone(), retract(), migrate()              │# Key Capabilities:

│           └───────────────┬────────────────┘               │

│                           │                                 │✓ WeatherAgent activated

│         ┌─────────────────┼─────────────────┐               │

│         │                 │                 │               │✓ Subscribed to weather.** topics├─────────────────────────────────────────────────────────────────────────────┤# • Dynamic task planning with TinyLlama 1.1B model

│  ┌──────┴──────┐  ┌──────┴──────┐  ┌──────┴───────┐      │

│  │ Orchestrator│  │  MeshChat   │  │  Registry    │      │

│  │ Agent (LLM) │  │  Agent (AI) │  │  Agent       │      │

│  └─────────────┘  └─────────────┘  └──────────────┘      │Try: weather Paris│  📨 Multi-Broker Support:                                                   │# • Intelligent agent discovery and capability matching

└─────────────────────────────────────────────────────────────┘

```     weather "New York"



**Key Design Principles:**```│            ├─ InMemoryBroker (Development)                                 │# • Parallel task execution with correlation tracking

1. **Decoupled Communication** - Agents never call each other directly

2. **Strong Mobility** - Agents can move with full state preservation

3. **Broker Agnostic** - Switch from in-memory to Kafka without code changes

4. **CloudEvents Standard** - All events follow CloudEvents 1.0 spec### 3️⃣ Try LLM-Powered Orchestration│            ├─ KafkaBroker (Production)                                     │# • CloudEvents v1.0 compliant messaging



---



## 🎓 Getting Started```bash│            └─ NATSBroker (Lightweight)                                     │# • Context-aware decision making



### Installation# Requires Ollama with TinyLlama model installed



```bash./run-orchestrator-demo.sh├─────────────────────────────────────────────────────────────────────────────┤```

# 1. Clone the repository

git clone https://github.com/xaviercallens/amcp-v1.5-opensource.git```

cd amcp-v1.5-opensource

│  🔌 Tool Integration: MCP Protocol, Weather APIs, AI Services              │

# 2. Setup Java 21

./setup-java21.shThis demonstrates **intelligent multi-agent coordination** where an LLM-powered orchestrator:



# 3. Build all modules- Analyzes user requests├─────────────────────────────────────────────────────────────────────────────┤### 💬 **MeshChat - Open Source Conversational AI**

mvn clean compile

- Discovers capable agents dynamically

# Verify installation

mvn test -Dmaven.test.skip=true- Plans and executes tasks in parallel│  📊 Observability: Prometheus, Grafana                                     │**Community-driven** conversational AI system showcasing multi-agent orchestration for developers.

```

- Aggregates results with full traceability

### Your First Event

└─────────────────────────────────────────────────────────────────────────────┘

```java

// Create an event---

Event event = Event.builder()

    .topic("hello.world")``````bash

    .payload("Hello from AMCP!")

    .correlationId(UUID.randomUUID().toString())## 📖 Core Concepts (3 minutes read)

    .build();

# Launch comprehensive demo

// Publish to the mesh

eventBroker.publish(event);### 1. Agents: Autonomous, Stateful, Mobile

```

### Project Structure./run-meshchat-full-demo.sh

### Run Interactive Demos

```java

```bash

# Launch interactive demo menupublic class MyAgent extends AbstractMobileAgent {

./amcp-demos.sh

    @Override

# Or run specific demos

./scripts/demos/run-weather-demo.sh     # Weather monitoring (5 min)    public void onActivate() {```# Try enterprise scenarios:

./scripts/demos/run-meshchat-demo.sh    # AI chat with TinyLlama (10 min)

./scripts/demos/run-orchestrator-demo.sh # Multi-agent coordination (15 min)        // Subscribe to topics - hierarchical pattern matching

```

        subscribe("orders.new");amcp-v1.5-opensource/> "Plan a 5-day business trip to Tokyo with meetings and entertainment budget $3000"

---

        subscribe("orders.cancelled");

## 🏗️ Building Your First Agent

    }├── 🧠 core/                          # Core Framework> "Analyze Apple stock performance and provide investment recommendation"

### Simple Agent Example

    

```java

package io.amcp.examples;    @Override│   ├── src/main/java/io/amcp/> "Research weather patterns for my New York business trip next week"



import io.amcp.core.Agent;    public CompletableFuture<Void> handleEvent(Event event) {

import io.amcp.core.AgentContext;

import io.amcp.core.AgentID;        return CompletableFuture.runAsync(() -> {│   │   ├── core/                      # Agent interfaces, lifecycle```

import io.amcp.core.Event;

import io.amcp.core.lifecycle.AgentLifecycle;            switch (event.getTopic()) {



import java.util.concurrent.CompletableFuture;                case "orders.new":│   │   ├── mobility/                  # Strong mobility operations



/**                    processOrder(event.getPayload(Order.class));

 * A simple greeting agent that responds to hello events.

 */                    break;│   │   ├── messaging/                 # EventBroker system**Enterprise Features:**

public class GreetingAgent implements Agent {

    private AgentID agentId;                case "orders.cancelled":

    private AgentContext context;

    private AgentLifecycle state = AgentLifecycle.INACTIVE;                    cancelOrder(event.getPayload(OrderId.class));│   │   └── util/                      # Core utilities- 🧠 **Intelligent Routing**: TinyLlama/Ollama-powered agent selection



    @Override                    break;

    public void setAgentId(AgentID id) {

        this.agentId = id;            }│   └── src/test/java/                 # Unit tests- ✈️ **Travel Intelligence**: 11+ destinations with budget optimization

    }

        });

    @Override

    public AgentID getAgentId() {    }├── 🔌 connectors/                     # Tool Connectors- 📈 **Financial Analytics**: Real-time stock data via Polygon.io API

        return agentId;

    }}



    @Override```│   ├── src/main/java/io/amcp/connectors/- 💭 **Conversation Memory**: Persistent session management with context awareness

    public void setContext(AgentContext context) {

        this.context = context;

    }

**Key Features:**│   │   ├── ai/                        # LLM/AI connectors- 🔄 **Multi-Agent Coordination**: Seamless travel, financial, and chat agent integration

    @Override

    public AgentContext getContext() {- **Event-Driven**: Agents communicate via pub/sub (no tight coupling)

        return context;

    }- **Asynchronous**: All operations return `CompletableFuture<T>`│   │   ├── weather/                   # Weather APIs- 🎯 **Dynamic Discovery**: Runtime agent registration and capability matching



    @Override- **Mobile**: Use `dispatch()`, `clone()`, `migrate()` to move agents at runtime

    public CompletableFuture<Void> activate() {

        return CompletableFuture.runAsync(() -> {│   │   ├── mcp/                       # MCP protocol

            state = AgentLifecycle.ACTIVE;

            // Subscribe to hello events### 2. Mobility: Move Agents, Not Data

            context.subscribe("hello.**", this);

            System.out.println("[GreetingAgent] Activated and listening for greetings!");│   │   ├── ollama/                    # Ollama integration### 📈 **Real-Time Financial Integration**

        });

    }```java



    @Override// Move agent to edge for low-latency processing│   │   └── a2a/                       # A2A protocol bridge**Production-grade** financial data processing with live market integration.

    public CompletableFuture<Void> deactivate() {

        return CompletableFuture.runAsync(() -> {agent.dispatch("edge-datacenter-paris");

            context.unsubscribe("hello.**", this);

            state = AgentLifecycle.INACTIVE;│   └── src/test/java/                 # Connector tests

            System.out.println("[GreetingAgent] Deactivated");

        });// Clone agent for HA

    }

agent.clone("backup-datacenter");├── 🎯 examples/                       # Example Agents```bash

    @Override

    public CompletableFuture<Void> handleEvent(Event event) {

        return CompletableFuture.runAsync(() -> {

            String name = event.getPayload(String.class);// Intelligent migration with load balancing│   ├── src/main/java/io/amcp/examples/# Launch financial services demo

            System.out.println("[GreetingAgent] Received: " + name);

            agent.migrate(MigrationOptions.builder()

            // Respond with a greeting

            Event response = Event.builder()    .targetContext("optimal-location")│   │   ├── meshchat/                  # Conversational AI./run-stockprice-demo.sh

                .topic("greeting.response")

                .payload("Hello, " + name + "! Welcome to AMCP!")    .preserveState(true)

                .correlationId(event.getCorrelationId())

                .build();    .build());│   │   ├── weather/                   # Weather monitoring

            

            context.publish(response);```

        });

    }│   │   ├── orchestrator/              # LLM orchestration# Enterprise capabilities:



    @Override**Why This Matters:**

    public AgentLifecycle getState() {

        return state;- Process data where it lives (edge computing)│   │   └── multiagent/                # Multi-agent demos# • Live stock market data via Polygon.io

    }

}- Dynamic scaling and load balancing

```

- Disaster recovery and high availability│   └── src/test/java/                 # Example tests# • Real-time price monitoring and alerts

### Running Your Agent

- No data transfer overhead

```java

// Create event broker├── 🚀 deploy/                         # Deployment# • Investment analysis and recommendations

EventBroker broker = EventBrokerFactory.create("memory", new HashMap<>());

### 3. LLM Orchestration: Agents That Think

// Create agent context

AgentContext context = new AgentContext(broker);│   ├── docker/                        # Container configs# • Portfolio tracking and optimization



// Create and activate agent```java

GreetingAgent agent = new GreetingAgent();

agent.setAgentId(new AgentID("greeting-agent-001"));public class OrchestratorAgent extends AbstractMobileAgent {│   └── monitoring/                   # Observability# • Risk assessment and market insights

agent.setContext(context);

agent.activate().join();    @Override



// Send a test event    public CompletableFuture<String> handleComplexTask(String userRequest) {├── 📚 docs/                          # Documentation```

Event hello = Event.builder()

    .topic("hello.world")        // 1. LLM generates task plan

    .payload("Alice")

    .build();        return aiConnector.generateTaskPlan(userRequest)└── pom.xml                           # Maven config



broker.publish(hello);            // 2. Find agents with required capabilities

```

            .thenCompose(plan -> registry.findCapableAgents(plan.getRequiredCapabilities()))```### ✈️ **Enterprise Travel Planning System**

---

            // 3. Execute tasks in parallel

## 🎭 Interactive Demos

            .thenCompose(agents -> executeParallelTasks(plan, agents))**Mission-critical** travel orchestration with weather and logistics integration.

### 🌤️ Weather Monitoring System

            // 4. LLM synthesizes final response

Real-time weather monitoring with OpenWeatherMap integration:

            .thenCompose(results -> aiConnector.synthesizeResponse(results));## 🚀 Quick Start

```bash

./scripts/demos/run-weather-demo.sh    }

```

}```bash

**Features:**

- Multi-city monitoring (New York, Tokyo, London, Paris, Sydney)```

- Real-time weather updates

- Interactive CLI commands### Prerequisites# Experience intelligent travel planning

- Event-driven pub/sub pattern

**Built-In Support:**

**Try these commands:**

```- TinyLlama 1.1B (local, no API costs)./run-travel-demo.sh

weather> weather Tokyo

weather> status- Ollama integration (any open-source model)

weather> help

```- Task planning and decomposition- **Java 21+** (OpenJDK or Oracle JDK)



### 💬 MeshChat AI Assistant- Capability-based agent discovery



Multi-agent conversational AI with LLM orchestration:- **Maven 3.8+** for build management# Enterprise features:



```bash### 4. Protocol Bridge: Integrate Everything

# Requires Ollama with TinyLlama model

ollama pull tinyllama- **Docker 20.10+** (optional, for containerized deployment)# • Multi-destination itinerary optimization



# Launch MeshChat```java

./scripts/demos/run-meshchat-demo.sh

```// AMCP native event- **Ollama** (optional, for LLM integration) - [Install Guide](https://ollama.ai/)# • Real-time weather integration and alerts



**Features:**Event event = Event.builder()

- Natural language conversations with TinyLlama

- Multi-agent coordination (Weather, Travel, Stock agents)    .topic("payment.process")# • Budget management and cost optimization

- Context-aware responses

- Conversation memory    .payload(paymentRequest)



**Try asking:**    .correlationId(UUID.randomUUID().toString())### Installation & Demo# • Agent mobility demonstrations

```

> What's the weather in Paris?    .build();

> Plan a 3-day trip to Tokyo

> Tell me about AAPL stock# • Event-driven coordination across services

```

// Automatically bridges to:

### 🎯 LLM Orchestrator Demo

// - Google A2A agents```bash```

Advanced multi-agent task coordination:

// - CloudEvents systems (Azure Event Grid, AWS EventBridge)

```bash

./scripts/demos/run-orchestrator-demo.sh// - Legacy systems via adapters# Clone repository

```

```

**Features:**

- Intelligent task planning with TinyLlamagit clone https://github.com/xaviercallens/amcp-v1.5-opensource.git### 🔒 **Advanced Security Suite**

- Dynamic agent discovery and routing

- Parallel task execution---

- Response synthesis

cd amcp-v1.5-opensource**Enterprise-grade** security with comprehensive authentication and authorization.

---

## 🎮 Interactive Demos

## 🤖 LLM Integration



### Ollama/TinyLlama Integration

### Demo 1: Weather Agent (Simple)

AMCP includes built-in support for local LLM inference:

**What it shows:** Basic agent, topic subscriptions, event handling# Setup Java 21 (if needed)**Security Architecture:**

```java

// Create Ollama connector

OllamaSpringAIConnector aiConnector = new OllamaSpringAIConnector(

    "http://localhost:11434",  // Ollama endpoint```bash./setup-java21.sh- 🔐 **Multi-Factor Authentication (MFA)** - TOTP, SMS, Email, Hardware Keys, Backup Codes

    "tinyllama"                // Model name

);./run-weather-demo.sh



// Use in agent- 📋 **Certificate-Based Authentication (mTLS)** - X.509 validation, CRL/OCSP, Custom CA

public CompletableFuture<String> handleQuery(String query) {

    return aiConnector.generateResponse(query, conversationId);# Try commands:

}

```weather Tokyo# Build project- 🎫 **JWT Token Management** - Standards compliance, per-tenant signing, OAuth2/OIDC



### OrchestratorAgent Patternweather "San Francisco"



```javaforecast Parismvn clean compile package- �️ **Role-Based Access Control (RBAC)** - Fine-grained permissions, role hierarchies

public class IntelligentOrchestrator extends AbstractMobileAgent {

    private OllamaSpringAIConnector ai;```

    private AgentRegistry registry;

- 📊 **Security Audit & Compliance** - Comprehensive logging, SIEM integration, compliance reports

    @Override

    public CompletableFuture<String> orchestrate(String userRequest) {### Demo 2: MeshChat (Conversational AI)

        // 1. Use LLM to analyze intent

        return ai.analyzeIntent(userRequest)**What it shows:** LLM integration, context management, multi-turn conversations# Run MeshChat demo (LLM-powered conversational AI)- ⏰ **Advanced Session Management** - Configurable timeouts, concurrent session limits

            // 2. Find capable agents

            .thenCompose(intent -> registry.findAgentsByCapability(intent.getRequiredCapabilities()))

            // 3. Execute tasks in parallel

            .thenCompose(agents -> executeParallelTasks(agents, userRequest))```bash./run-meshchat-demo.sh

            // 4. Synthesize final response

            .thenCompose(results -> ai.synthesizeResponse(results));./run-meshchat-demo.sh

    }

}```java

```

# Try scenarios:

### Supported LLM Providers

> Tell me about Paris# Run orchestrator demo (AI task coordination)// Example enterprise security implementation

- **Ollama** (local, privacy-first) - ✅ Fully supported

- **Spring AI** (OpenAI, Azure OpenAI, etc.) - ✅ Supported> What's the weather like there?

- **Custom LLM providers** - Easy to integrate

> Plan a 3-day itinerary./run-orchestrator-demo.shAdvancedSecurityManager securityManager = new AdvancedSecurityManager(config);

---

```

## 🚀 Deployment

AuthenticationResult result = securityManager.authenticate(username, password, tenantId).get();

### Local Development (In-Memory)

### Demo 3: Multi-Agent Orchestration (Advanced)

Perfect for development and testing:

**What it shows:** LLM orchestration, parallel execution, agent discovery# Run weather demo (multi-agent weather monitoring)SecurityContext context = securityManager.createSecurityContext(result.getUser(), tenantId);

```properties

# application.properties

amcp.event.broker.type=memory

``````bash./run-weather-demo.sh```



```bash./run-orchestrator-demo.sh

mvn clean package

java -jar examples/target/amcp-examples-1.5.0.jar```

```

# Try complex requests:

### Docker Deployment

> Research weather in Tokyo and Paris, then recommend best travel time## 🏗️ Open Source Architecture

```bash

# Build Docker image> Analyze weather patterns and suggest outdoor activities

docker build -t amcp-agent:1.5.0 .

```### Development

# Run with Docker Compose (includes Kafka, Prometheus, Grafana)

cd deploy/docker

docker-compose up -d

```### Demo 4: Interactive CLI (Full Power)AMCP v1.5 Open Source Edition delivers a developer-friendly agent mesh infrastructure:



**Included services:****What it shows:** All features, debugging tools, session management

- AMCP Agent Context

- Apache Kafka + Zookeeper```bash

- Prometheus (metrics)

- Grafana (dashboards)```bash



### Kubernetes Deployment./run-meshchat-cli.sh# Build without tests```



```bash

# Deploy to Kubernetes

kubectl apply -f deploy/k8s/CLI Commands:mvn clean compile -DskipTests┌─────────────────────────────────────────────────────────────────────────────┐



# Verify deployment  agents              - List all available agents

kubectl get pods -n amcp

  activate weather    - Start the weather agent│                    AMCP v1.5 Open Source Architecture                       │

# Access agent context

kubectl port-forward svc/amcp-context 8080:8080 -n amcp  ask weather "Tokyo" - Query an agent

```

  status             - System health check# Run tests├─────────────────────────────────────────────────────────────────────────────┤

**Kubernetes resources:**

- StatefulSet for Kafka cluster  help               - Full command list

- Deployment for AMCP agents

- Services for load balancing```mvn test│  🧠 LLM Orchestration: TinyLlama, Ollama Integration, AI-Powered Routing    │

- ConfigMaps for configuration

- Secrets for credentials



See [DEPLOYMENT.md](DEPLOYMENT.md) for complete deployment guide.---├─────────────────────────────────────────────────────────────────────────────┤



---



## ⚙️ Configuration## 🏗️ Architecture Overview# Run with quality checks│  🤖 Open Agents: MeshChat, Weather, Orchestrator                           │



### Event Broker Configuration



#### In-Memory (Development)```mvn test -P quality├─────────────────────────────────────────────────────────────────────────────┤



```properties┌─────────────────────────────────────────────────────────────────┐

amcp.event.broker.type=memory

```│                  AMCP v1.5 Open Source Architecture             ││  🔗 Protocol Bridges: Google A2A ↔ AMCP, CloudEvents 1.0, OAuth2/JWT       │



#### Apache Kafka (Production)├─────────────────────────────────────────────────────────────────┤



```properties│  🧠 LLM Layer: TinyLlama/Ollama Orchestration                   │# Run integration tests├─────────────────────────────────────────────────────────────────────────────┤

amcp.event.broker.type=kafka

amcp.kafka.bootstrap.servers=kafka-cluster:9092│  🤖 Agent Layer: MobileAgent, RegistryAgent, OrchestratorAgent  │

amcp.kafka.consumer.group.id=amcp-agents

amcp.kafka.topic.prefix=amcp│  📨 Messaging Layer: EventBroker (Memory/Kafka/NATS/Solace)     │mvn test -P integration│  🚀 Strong Mobility: dispatch(), clone(), retract(), migrate(),             │

amcp.kafka.security.protocol=SASL_SSL

amcp.kafka.sasl.mechanism=PLAIN│  🔗 Protocol Layer: A2A Bridge, CloudEvents 1.0, OAuth2/JWT     │

```

│  🔌 Integration Layer: MCP Tools, External APIs, Legacy Systems │```│                     replicate(), federateWith() - IBM Aglet-style          │

#### NATS (High Performance)

│  📊 Observability: Prometheus, Grafana, Distributed Tracing     │

```properties

amcp.event.broker.type=nats└─────────────────────────────────────────────────────────────────┘├─────────────────────────────────────────────────────────────────────────────┤

amcp.nats.servers=nats://nats-cluster:4222

amcp.nats.auth.username=amcp```

amcp.nats.auth.password=${NATS_PASSWORD}

```### Docker Deployment│  📨 Multi-Broker Support: Pluggable Message Brokers                        │



#### Solace PubSub+ (Advanced Messaging)### Project Structure



```properties│            ├─ InMemoryBroker (Development)                                 │

amcp.event.broker.type=solace

amcp.solace.host=solace.company.com```

amcp.solace.vpn=amcp-vpn

amcp.solace.username=amcp-clientamcp-v1.5-opensource/```bash│            ├─ KafkaBroker (Production)                                     │

```

├── 📁 core/                    # Core framework

### Security Configuration

│   ├── Agent.java             # Base agent interface# Launch full stack│            └─ NATSBroker (Lightweight)                                     │

```properties

# Enable TLS│   ├── MobileAgent.java       # Mobility operations

amcp.security.tls.enabled=true

amcp.security.tls.keystore.path=/certs/keystore.jks│   ├── EventBroker.java       # Messaging abstractioncd deploy/docker├─────────────────────────────────────────────────────────────────────────────┤

amcp.security.tls.keystore.password=${KEYSTORE_PASSWORD}

│   └── AgentContext.java      # Lifecycle management

# Enable mTLS

amcp.security.mtls.enabled=true│docker-compose up -d│  🔌 Tool Integration: MCP Protocol, External APIs                          │

amcp.security.mtls.truststore.path=/certs/truststore.jks

├── 📁 connectors/             # External integrations

# OAuth2 authentication

amcp.security.oauth2.enabled=true│   ├── ollama/               # LLM integration│              ├─ Weather APIs (OpenWeatherMap, WeatherAPI)                  │

amcp.security.oauth2.issuer.uri=https://auth.company.com

```│   ├── a2a/                  # Google A2A bridge



### Agent Mobility Configuration│   └── mcp/                  # Tool connectors# Includes:│              └─ AI Services (Ollama, OpenAI, Azure OpenAI)                 │



```properties│

# Enable agent migration

amcp.migration.enabled=true├── 📁 examples/              # Reference implementations# • AMCP agent contexts├─────────────────────────────────────────────────────────────────────────────┤

amcp.migration.timeout=30s

amcp.migration.max-retries=3│   ├── weather/             # WeatherAgent example



# Enable agent replication│   ├── meshchat/            # Conversational AI# • Kafka event broker│  📊 Open Observability: Prometheus, Grafana, Custom Metrics               │

amcp.replication.enabled=true

amcp.replication.factor=3│   └── orchestrator/        # LLM orchestration

```

│# • Prometheus monitoring├─────────────────────────────────────────────────────────────────────────────┤

### LLM Configuration

├── 📁 cli/                   # Interactive CLI

```properties

# Ollama configuration│# • Grafana dashboards│  🧪 Testing Framework: TestContainers, Performance Testing                 │

amcp.ai.ollama.base-url=http://localhost:11434

amcp.ai.ollama.model=tinyllama└── 📁 docs/                  # Documentation

amcp.ai.ollama.timeout=30s

    ├── ARCHITECTURE.md├─────────────────────────────────────────────────────────────────────────────┤

# Spring AI configuration

amcp.ai.spring-ai.enabled=true    ├── DEVELOPER_GUIDE.md

amcp.ai.openai.api-key=${OPENAI_API_KEY}

amcp.ai.openai.model=gpt-4    └── API_REFERENCE.md# Check services│  🏢 Open Platform: Docker, Local Development                               │

```

```

---

curl http://localhost:8080/api/v1.5/agents    # Agent status└─────────────────────────────────────────────────────────────────────────────┘

## 📚 Documentation

---

### Core Documentation

curl http://localhost:8081/metrics           # Prometheus metrics```

- **[Quick Start Guide](QUICK_START.md)** - Get started in 5 minutes

- **[Developer Guide](docs/DEVELOPER_GUIDE.md)** - Comprehensive development guide## 📚 Documentation

- **[API Reference](docs/API_REFERENCE.md)** - Complete API documentation

- **[Architecture Guide](docs/ARCHITECTURE.md)** - System architecture and designcurl http://localhost:3000                   # Grafana (admin/admin)│              ├─ Travel APIs (Amadeus, TripAdvisor)                         │



### Feature Guides### For Developers



- **[LLM Integration Guide](docs/guides/OLLAMA_INTEGRATION_GUIDE.md)** - Ollama and TinyLlama setup- **[Quick Start Guide](QUICK_START.md)** - Get up and running in 5 minutes```│              └─ AI Services (Ollama, OpenAI, Azure OpenAI)                 │

- **[CloudEvents Compliance](docs/guides/CLOUDEVENTS_COMPLIANCE.md)** - CloudEvents 1.0 implementation

- **[Multi-Agent Systems](docs/guides/MULTIAGENT_SYSTEM_GUIDE.md)** - Building multi-agent apps- **[Developer Guide](docs/DEVELOPER_GUIDE.md)** - Build your first agent

- **[API Keys Guide](docs/guides/API_KEYS_GUIDE.md)** - External API configuration

- **[Demo Guide](docs/guides/DEMO_GUIDE.md)** - Comprehensive demo documentation- **[Architecture Guide](docs/ARCHITECTURE.md)** - Deep dive into AMCP design├─────────────────────────────────────────────────────────────────────────────┤



### Deployment & Operations- **[API Reference](docs/API_REFERENCE.md)** - Complete API documentation



- **[Deployment Guide](DEPLOYMENT.md)** - Production deployment (Kubernetes, Docker, Cloud)## 🎯 Core API Examples│  🔒 Advanced Security: MFA, mTLS, RBAC, Audit, Compliance                  │

- **[Security Guide](SECURITY.md)** - Security best practices and compliance

- **[Changelog](CHANGELOG.md)** - Version history and release notes### For Contributors



### Community- **[Contributing Guide](CONTRIBUTING.md)** - How to contribute├─────────────────────────────────────────────────────────────────────────────┤



- **[Contributing Guidelines](CONTRIBUTING.md)** - How to contribute- **[Build Guide](BUILD_TROUBLESHOOTING.md)** - Build and test locally

- **[Code of Conduct](CODE_OF_CONDUCT.md)** - Community guidelines

- **[GitHub Discussions](https://github.com/xaviercallens/amcp-v1.5-opensource/discussions)** - Q&A and community support- **[Code Style Guide](docs/CODE_STYLE.md)** - Coding conventions### Basic Agent Implementation│  📊 Enterprise Observability: Prometheus, Grafana, Jaeger, Custom Metrics  │



---



## 🏗️ Project Structure### Advanced Topics├─────────────────────────────────────────────────────────────────────────────┤



```- **[Multi-Agent Systems](MULTIAGENT_SYSTEM_GUIDE.md)** - Coordination patterns

amcp-v1.5-opensource-edition/

├── core/                          # Core AMCP framework- **[LLM Orchestration](OLLAMA_INTEGRATION_GUIDE.md)** - AI-powered agents```java│  🧪 Testing Framework: TestContainers, Performance, Security, Chaos        │

│   ├── src/main/java/io/amcp/

│   │   ├── core/                  # Agent interfaces and core types- **[Protocol Bridge](docs/A2A_BRIDGE.md)** - Google A2A integration

│   │   ├── messaging/             # EventBroker implementations

│   │   ├── mobility/              # Agent mobility (dispatch, clone, migrate)- **[CloudEvents Guide](CLOUDEVENTS_COMPLIANCE.md)** - Event interoperabilitypublic class MyAgent extends AbstractMobileAgent {├─────────────────────────────────────────────────────────────────────────────┤

│   │   ├── security/              # Security and authentication

│   │   ├── cloudevents/           # CloudEvents 1.0 support

│   │   └── lifecycle/             # Agent lifecycle management

│   └── src/test/java/             # Unit tests---    │  🏢 Production Platform: Kubernetes, Docker, Istio Service Mesh            │

│

├── connectors/                    # External integrations

│   ├── src/main/java/io/amcp/connectors/

│   │   ├── ai/                    # LLM integrations (Ollama, Spring AI)## 🛠️ Use Cases    @Override└─────────────────────────────────────────────────────────────────────────────┘

│   │   ├── mcp/                   # Model Context Protocol

│   │   ├── a2a/                   # Google A2A protocol bridge

│   │   └── tools/                 # Tool connectors

│   └── src/test/java/             # Connector tests### 1. **AI Agent Orchestration**    public void onActivate() {```

│

├── examples/                      # Reference implementationsBuild LangChain/AutoGen-style systems with distributed execution:

│   ├── src/main/java/io/amcp/examples/

│   │   ├── weather/               # Weather monitoring agent- Multi-agent research assistants        super.onActivate();

│   │   ├── travel/                # Travel planning agent

│   │   ├── stock/                 # Stock price agent- Intelligent task decomposition

│   │   ├── meshchat/              # AI chat agent

│   │   └── orchestrator/          # LLM orchestrator- Parallel execution with correlation        subscribe("my.topic.**");  // Subscribe to topics### 🏗️ Open Source Project Structure

│   └── resources/                 # Example configurations

│

├── cli/                           # Interactive command-line interface

│   └── src/main/java/io/amcp/cli/### 2. **Edge Computing**        logMessage("Agent activated");

│

├── scripts/                       # Build and utility scriptsMove agents to where data lives:

│   ├── demos/                     # Demo launch scripts

│   │   ├── run-weather-demo.sh- IoT data processing at edge    }```

│   │   ├── run-meshchat-demo.sh

│   │   └── run-orchestrator-demo.sh- Low-latency decision making

│   └── setup-java21.sh            # Java 21 setup

│- Bandwidth optimization    amcp-v1.5-opensource/

├── deploy/                        # Deployment configurations

│   ├── docker/                    # Docker and Compose files

│   ├── k8s/                       # Kubernetes manifests

│   └── istio/                     # Service mesh configuration### 3. **Event-Driven Microservices**    @Override├── 🧠 core/                          # Open Source Core Framework

│

├── docs/                          # DocumentationReplace traditional RPC with intelligent agents:

│   ├── guides/                    # Feature-specific guides

│   └── internal/                  # Implementation notes- Dynamic service discovery    public CompletableFuture<Void> handleEvent(Event event) {│   ├── src/main/java/io/amcp/

│

├── .github/                       # GitHub configuration- Resilient communication

│   ├── workflows/                 # CI/CD workflows

│   ├── ISSUE_TEMPLATE/            # Issue templates- Built-in observability        return CompletableFuture.runAsync(() -> {│   │   ├── core/                      # Agent interfaces, lifecycle, discovery

│   └── PULL_REQUEST_TEMPLATE.md   # PR template

│

├── pom.xml                        # Maven parent POM

├── README.md                      # This file### 4. **Legacy System Integration**            switch (event.getTopic()) {│   │   ├── mobility/                  # IBM Aglet-style strong mobility

├── CHANGELOG.md                   # Version history

├── DEPLOYMENT.md                  # Deployment guideBridge old and new with protocol adapters:

├── SECURITY.md                    # Security documentation

├── CONTRIBUTING.md                # Contribution guidelines- A2A protocol compatibility                case "my.topic.request":│   │   ├── messaging/                 # Multi-broker EventBroker system

└── LICENSE                        # MIT License

```- CloudEvents for cloud integration



---- Custom MCP tool connectors                    handleRequest(event.getPayload(RequestType.class));│   │   ├── memory/                    # Conversation memory system



## 🤝 Contributing



We welcome contributions from the community! Here's how you can help:---                    publishEvent("my.topic.response", response);│   │   ├── registry/                  # Dynamic agent registry



### Ways to Contribute



- 🐛 **Report bugs** - Found an issue? [Open a bug report](https://github.com/xaviercallens/amcp-v1.5-opensource/issues/new?template=bug_report.md)## 🚦 Getting Started - Next Steps                    break;│   │   └── util/                      # Core utilities

- ✨ **Request features** - Have an idea? [Submit a feature request](https://github.com/xaviercallens/amcp-v1.5-opensource/issues/new?template=feature_request.md)

- 📖 **Improve docs** - Documentation can always be better

- 💻 **Submit PRs** - Code contributions are highly appreciated

- 💬 **Join discussions** - Share your experience and help others### 1. **Learn the Basics** (30 minutes)            }│   └── src/test/java/                 # Comprehensive testing framework



### Development Setup```bash



```bash# Run all demos in sequence        });├── 🔌 connectors/                     # Open Source Tool Connectors

# 1. Fork the repository

git clone https://github.com/YOUR_USERNAME/amcp-v1.5-opensource.git./demo-launcher.sh

cd amcp-v1.5-opensource

    }│   ├── src/main/java/io/amcp/connectors/

# 2. Create a feature branch

git checkout -b feature/my-awesome-feature# Read the Quick Start



# 3. Make your changescat QUICK_START.md    │   │   ├── ai/                        # LLM/AI service connectors

# ... edit code ...

```

# 4. Run tests

mvn clean test    @Override│   │   ├── weather/                   # Weather service APIs



# 5. Commit and push### 2. **Build Your First Agent** (1 hour)

git add .

git commit -m "feat: add awesome feature"Follow our [Developer Guide](docs/DEVELOPER_GUIDE.md) to create:    public void setContext(AgentContext context) {│   │   ├── mcp/                       # Model Context Protocol integration

git push origin feature/my-awesome-feature

- A simple echo agent

# 6. Open a Pull Request

```- An agent that calls external APIs        this.context = context; // CRITICAL: Always set context before activation│   │   ├── ollama/                    # Ollama LLM integration



See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines.- A mobile agent that migrates at runtime



---    }│   │   └── a2a/                       # Google A2A protocol bridge



## 📊 Use Cases### 3. **Explore Advanced Features** (2 hours)



### 🏠 Smart Home Automation- Add LLM orchestration to your agents}│   └── src/test/java/                 # Connector integration tests

Coordinate multiple IoT devices and sensors with intelligent agents that can move closer to data sources.

- Implement agent mobility patterns

### 🤖 AI Chatbot Mesh

Build distributed AI assistants where specialized agents handle different domains (weather, travel, finance).- Bridge to Google A2A or CloudEvents```├── 🎯 examples/                       # Open Source Agent Examples



### 🏭 Industrial IoT

Monitor and control industrial equipment with mobile agents that can migrate to edge devices.

### 4. **Deploy to Production** (variable)│   ├── src/main/java/io/amcp/examples/

### 📈 Financial Trading Systems

Implement distributed trading strategies with agents that can replicate for high availability.- [Docker Deployment](deploy/docker/README.md)



### 🚗 Autonomous Vehicle Coordination- [Kubernetes with Istio](deploy/k8s/README.md)### Strong Mobility Operations│   │   ├── meshchat/                  # Conversational AI system

Coordinate multiple autonomous agents with real-time decision-making capabilities.

- [Monitoring Setup](docs/MONITORING.md)

---

│   │   ├── weather/                   # Weather monitoring agents

## 🎯 Roadmap

---

### v1.5.1 (Current)

- ✅ IBM Aglet-style strong mobility```java│   │   ├── orchestrator/              # LLM orchestration agents

- ✅ LLM integration (Ollama, TinyLlama)

- ✅ Google A2A protocol bridge## 🤝 Community & Support

- ✅ CloudEvents 1.0 compliance

- ✅ Multi-broker support (Kafka, NATS, Solace)// IBM Aglet-style mobility│   │   └── multiagent/                # Multi-agent coordination examples

- ✅ Interactive CLI

- ✅ Production deployment guides### Get Help



### v1.6 (Next Quarter)- **GitHub Issues**: [Report bugs or request features](https://github.com/xaviercallens/amcp-v1.5-opensource/issues)public interface MobileAgent extends Agent {│   └── src/test/java/                 # Example tests and scenarios

- 🔄 Python SDK (official support)

- 🔄 Rust SDK (official support)- **Discussions**: [Ask questions, share ideas](https://github.com/xaviercallens/amcp-v1.5-opensource/discussions)

- 🔄 LangChain integration library

- 🔄 Semantic Kernel adapters- **Documentation**: [Full docs](https://github.com/xaviercallens/amcp-v1.5-opensource/tree/main/docs)    // Move agent to remote context├── 🚀 deploy/                         # Open Source Deployment

- 🔄 Enhanced web dashboard

- 🔄 Plugin marketplace



### v2.0 (Future)### Contribute    CompletableFuture<Void> dispatch(String destinationContext);│   ├── docker/                        # Container orchestration

- 🔮 Multi-language protocol (Protobuf/gRPC)

- 🔮 Advanced federation capabilitiesWe welcome contributions! See [CONTRIBUTING.md](CONTRIBUTING.md) for:

- 🔮 Built-in CRDT for state synchronization

- 🔮 Auto-scaling agents- Code contributions    │   │   ├── Dockerfile                 # Multi-stage build

- 🔮 SaaS platform (AMCP Cloud)

- Documentation improvements

---

- Bug reports and feature requests    // Create copy in remote context│   │   ├── docker-compose.yml         # Local stack with monitoring

## ❓ FAQ

- Example agents and demos

### Q: What's the difference between AMCP and other agent frameworks?

    CompletableFuture<AgentID> clone(String destinationContext);│   │   └── entrypoint.sh              # Development entrypoint

**A:** AMCP uniquely combines:

1. **Strong mobility** (agents can move with full state)### Roadmap

2. **LLM integration** built-in (not an afterthought)

3. **Production-ready** (Kubernetes, Kafka, monitoring out of the box)- **v1.6**: Python and Rust SDKs, LangChain/Semantic Kernel integrations    │   └── monitoring/                   # Open source observability

4. **Protocol interoperability** (A2A, CloudEvents, MCP)

- **v1.7**: Enhanced CLI with GUI dashboard, more LLM providers

### Q: Do I need to use Kafka in production?

- **v1.8**: Advanced security policies, multi-tenancy    // Recall agent from remote context│       ├── prometheus/                # Metrics collection

**A:** No! Start with in-memory broker for development, then choose Kafka, NATS, or Solace based on your needs. The broker is pluggable—change it in configuration without code changes.

- **v2.0**: Breaking changes for cleaner APIs, performance optimizations

### Q: Can agents written in different languages communicate?

    CompletableFuture<Void> retract(String sourceContext);│       └── grafana/                   # Community dashboards

**A:** Currently, Java is the primary language. Python and Rust SDKs are planned for v1.6. All agents communicate via standard CloudEvents, so cross-language support is straightforward.

---

### Q: How does AMCP handle agent failures?

    ├── 📚 docs/                          # Community Documentation

**A:** Agents support replication, health monitoring, and automatic failover. When an agent fails, its replicas can take over seamlessly.

## 📊 Performance & Scale

### Q: Is AMCP suitable for production use?

    // Intelligent migration with options│   ├── QUICK_START.md                 # Getting started guide

**A:** Yes! AMCP v1.5 includes production-ready features: Kubernetes deployment, monitoring, security, high availability, and has been tested in real-world scenarios.

### Benchmarks (Local Development)

### Q: How does LLM integration work offline?

- **Latency**: < 1ms event delivery (in-memory broker)    CompletableFuture<Void> migrate(MigrationOptions options);│   ├── ARCHITECTURE.md               # System architecture

**A:** Use Ollama with TinyLlama for fully offline LLM capabilities. No internet required for basic AI features.

- **Throughput**: 100K+ events/sec (single node)

---

- **Agents**: 1000+ concurrent agents per context    │   ├── API_REFERENCE.md              # Developer API docs

## 📜 License

- **Memory**: ~50MB baseline + ~1MB per agent

AMCP v1.5 Open Source Edition is licensed under the **MIT License**.

    // Replicate for high availability│   └── CONTRIBUTING.md               # Contribution guidelines

```

MIT License### Production Scale (Kafka/NATS)



Copyright (c) 2025 AMCP Contributors- **Latency**: 5-10ms p99 (Kafka cluster)    CompletableFuture<List<AgentID>> replicate(String... contexts);├── 🔧 scripts/                       # Build and deployment scripts



Permission is hereby granted, free of charge, to any person obtaining a copy- **Throughput**: 1M+ events/sec (partitioned)

of this software and associated documentation files (the "Software"), to deal

in the Software without restriction, including without limitation the rights- **Agents**: 10K+ distributed across nodes}├── pom.xml                           # Maven configuration

to use, copy, modify, merge, publish, distribute, sublicense, and/or sell

copies of the Software, and to permit persons to whom the Software is- **Availability**: 99.9%+ with replication

furnished to do so, subject to the following conditions:

```└── LICENSE                           # Apache 2.0 license

The above copyright notice and this permission notice shall be included in all

copies or substantial portions of the Software.*Benchmarks run on: MacBook Pro M1, 16GB RAM, Java 21*



THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR```

IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,

FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE---

AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER

LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,### Event-Driven Messaging│   └── src/test/java/                 # Example tests and scenarios

OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE

SOFTWARE.## 📄 License

```

├── 🚀 deploy/                         # Enterprise Deployment

See [LICENSE](LICENSE) file for details.

AMCP v1.5 Open Source Edition is licensed under the **Apache License 2.0**.

---

```java│   ├── docker/                        # Container orchestration

## 🌐 Community & Support

See [LICENSE](LICENSE) for details.

- **GitHub Issues:** [Report bugs and request features](https://github.com/xaviercallens/amcp-v1.5-opensource/issues)

- **GitHub Discussions:** [Community Q&A and support](https://github.com/xaviercallens/amcp-v1.5-opensource/discussions)// Create and publish events│   │   ├── Dockerfile                 # Multi-stage enterprise build

- **Documentation:** [Complete documentation](docs/)

- **Email:** callensxavier@gmail.com---



---Event event = Event.builder()│   │   ├── docker-compose.yml         # Full stack with monitoring



## 🙏 Acknowledgments## 🌟 Why Open Source?



AMCP v1.5 is inspired by and builds upon:    .topic("weather.request.forecast")│   │   └── entrypoint.sh              # Production entrypoint



- **IBM Aglets** - Strong mobility modelAMCP was born from the belief that **multi-agent systems should be accessible to everyone**. Whether you're:

- **Google A2A Protocol** - Agent interoperability standards

- **CloudEvents** - Event format standardization    .payload(weatherRequest)│   ├── k8s/                          # Kubernetes manifests

- **Apache Kafka** - Distributed streaming platform

- **Ollama** - Local LLM inference- 🎓 A **student** learning distributed systems



Special thanks to all contributors and the open-source community!- 🚀 A **startup** building the next AI unicorn    .correlationId("req-" + UUID.randomUUID())│   │   ├── namespace.yaml             # Multi-tenant namespaces



---- 🏢 An **enterprise** exploring agent-based architecture



## 🌟 Star History- 🔬 A **researcher** pushing the boundaries of AI    .metadata("source", "weather-app")│   │   ├── deployment.yaml            # Production deployment



If you find AMCP useful, please consider giving it a star ⭐️ on GitHub!



[![Star History Chart](https://api.star-history.com/svg?repos=xaviercallens/amcp-v1.5-opensource&type=Date)](https://star-history.com/#xaviercallens/amcp-v1.5-opensource&Date)**AMCP gives you production-grade tools without vendor lock-in.**    .build();│   │   ├── service.yaml               # Service mesh integration



---



**Built with ❤️ by the AMCP Open Source Community**### What Makes AMCP Special?│   │   ├── configmap.yaml             # Configuration management



[⬆ Back to top](#amcp-v15-open-source-edition)


1. **True Mobility**: Only framework with runtime agent migration (IBM Aglet heritage)publishEvent(event);│   │   └── hpa.yaml                   # Horizontal Pod Autoscaling

2. **Protocol Bridge**: Native A2A compatibility - integrate with existing systems

3. **LLM-Native**: Orchestration built-in, not bolted-on│   ├── istio/                        # Service mesh configuration

4. **Zero Infrastructure**: Start with in-memory, scale to enterprise brokers

5. **Production Ready**: Formal verification, 95% coverage, battle-tested// CloudEvents 1.0 compliant│   │   ├── gateway.yaml               # Ingress configuration



---Event cloudEvent = Event.builder()│   │   ├── virtual-service.yaml       # Traffic management



## 🚀 Start Building Now    .topic("travel.request.plan")│   │   ├── destination-rule.yaml      # Load balancing rules



```bash    .payload(planRequest)│   │   └── security/                  # mTLS and security policies

git clone https://github.com/xaviercallens/amcp-v1.5-opensource.git

cd amcp-v1.5-opensource    .correlationId("trip-12345")│   └── monitoring/                   # Enterprise observability

./setup-java21.sh

./run-weather-demo.sh    .metadata("specversion", "1.0")│       ├── prometheus/                # Metrics collection

```

    .metadata("type", "io.amcp.travel.request")│       ├── grafana/                   # Enterprise dashboards

**Welcome to the future of multi-agent systems.** 🤖

    .build();│       └── jaeger/                    # Distributed tracing

---

```├── 📚 docs/                          # Comprehensive Documentation

<div align="center">

  │   ├── MESHCHAT_DOCUMENTATION.md     # Complete user guide

**[⭐ Star us on GitHub](https://github.com/xaviercallens/amcp-v1.5-opensource)** | 

**[📖 Read the Docs](docs/)** | ### LLM-Powered Orchestration│   ├── MESHCHAT_ARCHITECTURE.md      # System architecture

**[💬 Join Discussions](https://github.com/xaviercallens/amcp-v1.5-opensource/discussions)**

│   ├── MESHCHAT_API_REFERENCE.md     # Developer API docs

Made with ❤️ by the AMCP Community

```java│   └── MESHCHAT_QUICK_START.md       # Getting started guide

</div>

public class OrchestratorAgent extends AbstractMobileAgent {├── 🔧 scripts/                       # Build and deployment scripts

    private final OllamaSpringAIConnector aiConnector;├── 📜 formal-verification/           # TLA+ specifications

    ├── 🏷️ Enterprise Documentation/      # Enterprise-specific guides

    @Override│   ├── ENTERPRISE_LEVERAGE_GUIDE.md  # How to leverage enterprise features

    public CompletableFuture<String> handleComplexTask(String userRequest) {│   ├── ADVANCED_SECURITY_SUITE.md    # Security implementation guide

        return aiConnector.generateTaskPlan(userRequest)│   ├── TESTING_FRAMEWORK_GUIDE.md    # Testing strategy and tools

            .thenCompose(plan -> registryAgent.findCapableAgents(plan.getRequiredCapabilities()))│   └── DEPLOYMENT_GUIDE.md           # Production deployment guide

            .thenCompose(agents -> executeParallelTasks(plan, agents))├── pom.xml                           # Enterprise Maven configuration

            .thenCompose(results -> aiConnector.synthesizeResponse(results));└── LICENSE                           # Apache 2.0 license

    }```

}

```## 🚀 Open Source Quick Start



### Multi-Broker Configuration### 🔧 Prerequisites



```java- **Java 21+** (OpenJDK or Oracle JDK)

// Factory pattern for broker creation- **Maven 3.8+** for build management

EventBroker broker = EventBrokerFactory.create("kafka", config);- **Docker 20.10+** (for containerized deployment)

EventBroker broker = EventBrokerFactory.create("memory", config);  // Default for dev- **Ollama** (for LLM integration) - [Install Guide](https://ollama.ai/)

EventBroker broker = EventBrokerFactory.create("nats", config);

```### ⚡ Rapid Open Source Deployment



```properties```bash

# Development: In-memory broker# Clone open source repository

amcp.event.broker.type=memorygit clone https://github.com/xaviercallens/amcp-v1.5-opensource.git

cd amcp-v1.5-opensource

# Production: Kafka

amcp.event.broker.type=kafka# Quick open source demo (core features)

amcp.kafka.bootstrap.servers=kafka-cluster:9092./run-meshchat-demo.sh



# Lightweight: NATS# Interactive MeshChat with LLM orchestration

amcp.event.broker.type=nats./run-orchestrator-demo.sh

amcp.nats.servers=nats://localhost:4222

```# Weather services demo

./run-weather-demo.sh

## 🎯 Example Demos```



### 🧠 LLM-Powered Orchestration### 🏗️ Local Development



```bash```bash

./run-orchestrator-demo.sh# Build enterprise edition

mvn clean compile package -DskipTests

# Key Capabilities:

# • Dynamic task planning with TinyLlama# Alternative enterprise build

# • Intelligent agent discovery./scripts/build.sh --clean --quality --docker

# • Parallel task execution

# • CloudEvents v1.0 messaging# Run with in-memory broker (development)

```java -jar core/target/amcp-core-1.5.0.jar



### 💬 MeshChat - Conversational AI# Run with external Kafka (production)

export AMCP_EVENT_BROKER_TYPE=kafka

```bashexport AMCP_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

./run-meshchat-demo.shjava -jar core/target/amcp-core-1.5.0.jar

```

# Try example queries:

> "What's the weather in Paris?"### 🐳 Enterprise Docker Deployment

> "Tell me about agent mobility"

> "How does AMCP handle events?"```bash

# Launch full enterprise stack

# Features:cd deploy/docker

# • LLM-powered intelligent routingdocker-compose up -d

# • Conversation memory

# • Multi-agent coordination# Includes:

```# • Multi-context AMCP deployment

# • Kafka event broker cluster  

### 🌤️ Weather Monitoring System# • Prometheus monitoring

# • Grafana enterprise dashboards

```bash# • Jaeger distributed tracing

./run-weather-demo.sh

# Check enterprise services

# Features:curl http://localhost:8080/api/v1.5/agents    # Agent status

# • Multi-city weather monitoringcurl http://localhost:8081/metrics           # Prometheus metrics

# • Real-time alertscurl http://localhost:3000                   # Grafana (admin/admin)

# • Event-driven updatescurl http://localhost:16686                  # Jaeger tracing

# • Agent mobility demonstrations```

```

### ☸️ Production Kubernetes Deployment

## 🧪 Testing

```bash

```bash# Deploy enterprise stack to Kubernetes

# Run all testskubectl apply -f deploy/k8s/

mvn clean testkubectl apply -f deploy/istio/



# Unit tests only# Verify enterprise deployment

mvn test -Dtest=*Testkubectl get pods -n amcp-system

kubectl get services -n amcp-system

# Integration tests

mvn test -P integration# Access enterprise services

kubectl port-forward service/amcp-agent-context 8080:8080

# Code quality checkskubectl port-forward service/grafana 3000:3000

mvn test -P qualitykubectl port-forward service/jaeger 16686:16686



# Generate coverage report# Enterprise monitoring

mvn jacoco:reportkubectl port-forward service/prometheus 9090:9090

# View: target/site/jacoco/index.html```

```

### 🧪 Enterprise Testing

### TestContainers Integration

```bash

```java# Run comprehensive enterprise test suite

@Testcontainersmvn clean test -P enterprise-tests

public class IntegrationTest {

    # Quality and security validation

    @Containermvn test -P quality -P integration

    static KafkaContainer kafka = new KafkaContainer(

        DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));# Performance benchmarking

    mvn test -P performance

    @Test

    void testMultiAgentWorkflow() {# Chaos engineering tests

        // Test end-to-end scenariosmvn test -P chaos-engineering

        AgentContext context = AgentContext.create(kafka.getBootstrapServers());

        // ... test logic# Generate coverage reports

    }mvn jacoco:report

}# View: target/site/jacoco/index.html

``````



## 📚 Documentation## 🎯 Enterprise API & Integration Examples



- [Quick Start Guide](QUICK_START.md) - Get started in 5 minutes### 🧠 LLM-Powered Orchestration

- [Demo Guide](DEMO_GUIDE.md) - Comprehensive demo walkthrough

- [API Keys Setup](API_KEYS_GUIDE.md) - Configure external APIs```java

- [Contributing](CONTRIBUTING.md) - Contribution guidelines// Enterprise orchestrator with AI-powered decision making

public class EnhancedOrchestratorAgent extends MobileAgent {

## 🔧 Configuration    private final OllamaSpringAIConnector aiConnector;

    private final RegistryAgent registryAgent;

### Broker Configuration    

    @Override

```properties    public CompletableFuture<String> handleComplexTask(String userRequest) {

# In-Memory Broker (Development)        return aiConnector.generateTaskPlan(userRequest)

amcp.event.broker.type=memory            .thenCompose(plan -> registryAgent.findCapableAgents(plan.getRequiredCapabilities()))

            .thenCompose(agents -> executeParallelTasks(plan, agents))

# Kafka Broker (Production)            .thenCompose(results -> aiConnector.synthesizeResponse(results));

amcp.event.broker.type=kafka    }

amcp.kafka.bootstrap.servers=localhost:9092}

amcp.kafka.consumer.group.id=amcp-agents```



# NATS Broker (Lightweight)### 🚀 Enhanced Strong Mobility (IBM Aglet-Inspired)

amcp.event.broker.type=nats

amcp.nats.servers=nats://localhost:4222```java

```// Enterprise mobility with security and monitoring

public interface EnhancedMobileAgent extends Agent {

### LLM Configuration    // Secure migration with authentication context

    CompletableFuture<Void> dispatch(String destinationContext, SecurityContext securityContext);

```properties    

# Ollama Integration    // Intelligent replication with load balancing

amcp.ai.ollama.enabled=true    CompletableFuture<List<AgentID>> replicate(String... contexts);

amcp.ai.ollama.base-url=http://localhost:11434    

amcp.ai.ollama.model=tinyllama:1.1b    // Agent federation for complex workflows

amcp.ai.ollama.timeout=30s    CompletableFuture<Void> federateWith(List<AgentID> agents, String federationId);

```    

    // High-availability migration with failover

### Mobility Configuration    CompletableFuture<Void> migrate(MigrationOptions options);

    

```properties    // Enterprise monitoring hooks

# Agent Migration    void onMigrationStarted(String destination);

amcp.mobility.enabled=true    void onMigrationCompleted(String source, String destination);

amcp.migration.timeout=30s    void onMigrationFailed(String destination, Exception cause);

amcp.migration.retry.max=3}

``````



### Observability Configuration### 📊 Enterprise EventBroker with Advanced Features



```properties```java

# Prometheus Metrics// Multi-broker configuration with enterprise features

amcp.monitoring.prometheus.enabled=trueEventBroker broker = EventBrokerFactory.create("kafka", EnterpriseConfig.builder()

amcp.monitoring.prometheus.port=8081    .bootstrapServers("kafka-cluster:9092")

amcp.monitoring.prometheus.path=/metrics    .securityProtocol("SASL_SSL")

    .saslMechanism("PLAIN")

# Health Checks    .enableMonitoring(true)

amcp.monitoring.health.enabled=true    .enableTracing(true)

amcp.monitoring.health.check-interval=30s    .retryPolicy(ExponentialBackoff.builder()

```        .maxRetries(5)

        .initialDelay(Duration.ofSeconds(1))

## 🤝 Contributing        .maxDelay(Duration.ofMinutes(5))

        .build())

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.    .build());



### Development Setup// CloudEvents compliance with enterprise metadata

Event event = Event.builder()

1. Fork the repository    .topic("enterprise.financial.stock.analysis")

2. Create a feature branch: `git checkout -b feature/my-feature`    .payload(stockAnalysisRequest)

3. Make changes with tests    .correlationId("analysis-" + UUID.randomUUID())

4. Ensure tests pass: `mvn clean test`    .metadata("tenant", "enterprise-client-001")

5. Submit a pull request    .metadata("priority", "high")

    .metadata("security-classification", "confidential")

### Code Quality Standards    .deliveryOptions(DeliveryOptions.builder()

        .guaranteeOrder(true)

- **95%+ test coverage** with comprehensive scenarios        .enableEncryption(true)

- **All tests must pass** before merging        .compressionEnabled(true)

- **Code quality checks** (SpotBugs, Checkstyle, PMD)        .build())

- **Documentation** for new features    .build();

- **Signed commits** preferred```



## 📄 License### 🔌 Enterprise Tool Integration



This project is licensed under the **Apache License 2.0** - see the [LICENSE](LICENSE) file for details.```java

// Financial data connector with enterprise features

## 📞 Contact & Community@Component

public class EnterpriseFinancialConnector extends AbstractToolConnector {

- **Repository**: https://github.com/xaviercallens/amcp-v1.5-opensource    

- **Issues**: [GitHub Issues](https://github.com/xaviercallens/amcp-v1.5-opensource/issues)    @Override

- **Discussions**: [GitHub Discussions](https://github.com/xaviercallens/amcp-v1.5-opensource/discussions)    public CompletableFuture<StockAnalysis> analyzeStock(String symbol, AuthenticationContext auth) {

        return validatePermissions(auth, "financial.analysis")

## 🗺️ Roadmap            .thenCompose(permissions -> callMCPTool("polygon-api", 

                StockRequest.builder()

### v1.6 - Enhanced AI & Integration (Q2 2025)                    .symbol(symbol)

- Additional LLM provider support (OpenAI, Anthropic)                    .analysisType("comprehensive")

- Enhanced agent discovery and capability matching                    .includeOptions(true)

- Improved CloudEvents compliance                    .includeNews(true)

                    .build(), auth))

### v1.7 - Multi-Cloud Support (Q3 2025)            .thenApply(response -> parseStockAnalysis(response))

- Multi-cloud federation protocol            .thenApply(analysis -> enrichWithAIInsights(analysis, auth));

- Cross-cloud agent migration    }

- Cloud-agnostic deployment    

    private CompletableFuture<StockAnalysis> enrichWithAIInsights(

### v2.0 - Next-Generation Platform (Q4 2025)            StockAnalysis analysis, AuthenticationContext auth) {

- GraphQL query interface        return aiConnector.generateInvestmentInsights(analysis)

- WebAssembly agent runtime            .thenApply(insights -> analysis.withAIInsights(insights));

- Enhanced visual monitoring    }

}

---```



**Built with ❤️ by the AMCP Open Source Community**### 🔒 Enterprise Security Implementation



*Experience the future of distributed multi-agent systems with AMCP v1.5 Open Source Edition*```java

// Advanced security with comprehensive authentication
public class EnterpriseSecurityExample {
    
    public CompletableFuture<SecurityContext> authenticateUser(
            String username, String password, String tenantId) {
        
        return securityManager.authenticate(username, password, tenantId)
            .thenCompose(authResult -> {
                if (authResult.requiresMFA()) {
                    return handleMFAChallenge(authResult);
                }
                return CompletableFuture.completedFuture(authResult);
            })
            .thenCompose(authResult -> createSecurityContext(authResult, tenantId))
            .thenApply(context -> auditSuccessfulLogin(context));
    }
    
    private CompletableFuture<AuthenticationResult> handleMFAChallenge(
            AuthenticationResult authResult) {
        MfaChallenge challenge = generateMFAChallenge(authResult.getUser());
        return sendMFAChallenge(challenge)
            .thenCompose(response -> validateMFAResponse(challenge, response));
    }
}
```

### 🌐 Google A2A Protocol Compatibility

```java
// Bidirectional A2A integration
public class EnterpriseA2ABridge {
    
    public void sendToA2AAgent(String agentEndpoint, Event event, OAuth2Token token) {
        A2AMessage message = convertToA2A(event);
        
        // Add enterprise headers
        message.addHeader("X-AMCP-Version", "1.5");
        message.addHeader("X-Enterprise-Tenant", event.getTenantId());
        message.addHeader("X-Correlation-ID", event.getCorrelationId());
        
        httpClient.postWithAuth(agentEndpoint, message, token)
            .thenRun(() -> auditA2AInteraction(agentEndpoint, event));
    }
    
    public Event receiveFromA2A(A2AMessage message) {
        return Event.builder()
            .fromA2AMessage(message)
            .enrichWithEnterpriseMetadata()
            .validateSecurityContext()
            .build();
    }
}
```

## 📊 Enterprise Production Features

### 🔒 Advanced Enterprise Security
- **Multi-Factor Authentication (MFA)** - TOTP, SMS, Email, Hardware Keys, Backup Codes
- **Certificate-Based mTLS** - X.509 validation, CRL/OCSP, Custom CA, Smart Card integration
- **JWT Token Management** - Standards compliance, per-tenant signing, OAuth2/OIDC integration
- **Role-Based Access Control (RBAC)** - Fine-grained permissions, role hierarchies, dynamic policies
- **Security Audit & Compliance** - Comprehensive logging, SIEM integration, compliance reporting
- **Advanced Session Management** - Configurable timeouts, concurrent session limits, cross-device tracking

### 📈 Comprehensive Enterprise Observability
- **Custom Prometheus Metrics** - Agent performance, migration stats, event throughput, error rates
- **Distributed Tracing with Jaeger** - End-to-end request tracking across agent interactions
- **Enterprise Grafana Dashboards** - Real-time monitoring, alerting, capacity planning
- **Structured JSON Logging** - Correlation context, security events, performance metrics
- **Health Check Endpoints** - Kubernetes readiness/liveness probes, custom health indicators
- **Performance Profiling** - JVM metrics, memory usage, garbage collection analysis

### ☸️ Cloud-Native Enterprise Architecture
- **Kubernetes-Native Deployment** - HPA, pod affinity, resource quotas, network policies
- **Istio Service Mesh Integration** - Traffic management, security policies, observability
- **Multi-Region Support** - Geographic distribution, disaster recovery, data residency
- **Container Optimization** - Multi-stage builds, security scanning, resource efficiency
- **GitOps Ready** - Helm charts, Kustomize overlays, ArgoCD integration
- **Zero-Downtime Deployments** - Rolling updates, blue-green deployments, canary releases

### 🧪 Enterprise Testing & Quality Assurance
- **TestContainers Integration** - Infrastructure testing with real databases and message brokers
- **Performance Benchmarking** - Load testing, stress testing, capacity planning
- **Security Validation** - Vulnerability scanning, penetration testing, compliance checks
- **Chaos Engineering** - Fault injection, resilience testing, disaster recovery validation
- **Code Quality Gates** - 95% test coverage, SpotBugs, Checkstyle, PMD analysis
- **Formal Verification** - TLA+ specifications for migration safety and consistency

## 🧪 Enterprise Testing & Validation

AMCP v1.5 Enterprise Edition includes the most comprehensive testing framework in the industry:

```bash
# Enterprise test suite with all features
mvn clean test -P enterprise-tests

# Individual test categories
mvn test -P unit-tests          # Unit tests (95% coverage)
mvn test -P integration         # Integration tests with TestContainers
mvn test -P performance         # Performance benchmarks and load testing
mvn test -P security           # Security validation and penetration testing
mvn test -P chaos-engineering  # Chaos engineering and fault injection
mvn test -P quality           # Code quality (SpotBugs, Checkstyle, PMD)

# Generate comprehensive reports
mvn site                      # Complete test and quality reports
mvn jacoco:report            # Code coverage analysis
mvn spotbugs:gui             # Security vulnerability analysis
```

### 🏗️ TestContainers Enterprise Integration

```java
// Example enterprise integration test
@Testcontainers
public class EnterpriseIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14")
            .withDatabaseName("amcp_enterprise")
            .withUsername("amcp")
            .withPassword("enterprise");
    
    @Container 
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));
    
    @Container
    static GenericContainer<?> ollama = new GenericContainer<>("ollama/ollama:latest")
            .withExposedPorts(11434)
            .withCommand("serve");
    
    @Test
    void testCompleteEnterpriseWorkflow() {
        // Test end-to-end enterprise scenarios
        EnterpriseTestFramework framework = new EnterpriseTestFramework();
        TestResult result = framework.runCompleteWorkflow(
            postgres.getJdbcUrl(),
            kafka.getBootstrapServers(),
            ollama.getHost() + ":" + ollama.getMappedPort(11434)
        );
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPerformanceMetrics().getAverageResponseTime())
            .isLessThan(Duration.ofMillis(500));
    }
}
```

### 🔍 Formal Verification with TLA+

Enterprise-grade formal specifications ensure system correctness:

- **Migration Protocol Safety** - Ensures agent state consistency during migration
- **Event Ordering Guarantees** - Validates message delivery and ordering properties
- **Security Invariants** - Proves authentication and authorization correctness
- **Fault Tolerance** - Verifies system behavior under various failure scenarios

## 🌟 Enterprise Use Cases & Success Stories

### 🏦 **Financial Services - Real-Time Trading System**
```java
public class HighFrequencyTradingAgent extends EnhancedMobileAgent {
    @Override
    public CompletableFuture<TradeDecision> analyzeMarket() {
        return financialConnector.getRealtimeData()
            .thenCompose(data -> aiConnector.analyzeMarketTrends(data))
            .thenCompose(analysis -> riskManager.assessRisk(analysis))
            .thenApply(risk -> generateTradeDecision(risk));
    }
    
    // Move to edge location near exchange for ultra-low latency
    public void optimizeLatency() {
        dispatch("edge-nyse-datacenter").thenRun(() -> 
            subscribeToMarketData("real-time-feed"));
    }
}
```

### 🌍 **Smart City - Traffic Optimization Network**
```java
public class TrafficOptimizerAgent extends EnhancedMobileAgent {
    @Override
    public CompletableFuture<Void> optimizeTrafficFlow() {
        return weatherAgent.getCurrentConditions()
            .thenCompose(conditions -> eventAgent.getUpcomingEvents())
            .thenCompose(events -> predictTrafficPatterns(conditions, events))
            .thenCompose(patterns -> {
                // Federate with intersection agents
                return federateWith(findIntersectionAgents(), "traffic-optimization-" + UUID.randomUUID());
            })
            .thenCompose(federation -> coordiantedOptimization(federation));
    }
    
    // Clone to handle multiple city districts simultaneously
    public void scaleToDistricts(List<String> districts) {
        districts.forEach(district -> 
            clone("edge-" + district).thenRun(() -> 
                subscribeToTrafficSensors(district)));
    }
}
```

### 🏥 **Healthcare - Distributed Patient Monitoring**
```java
public class PatientMonitoringAgent extends EnhancedMobileAgent {
    @Override
    public CompletableFuture<AlertDecision> monitorVitalSigns() {
        return iotConnector.getVitalSigns(patientId)
            .thenCompose(vitals -> aiConnector.analyzeHealthTrends(vitals))
            .thenCompose(analysis -> {
                if (analysis.isEmergency()) {
                    // Replicate to emergency response centers
                    return replicate("emergency-room", "ambulance-dispatch", "specialist-oncall")
                        .thenRun(() -> triggerEmergencyProtocol(analysis));
                }
                return CompletableFuture.completedFuture(analysis);
            })
            .thenApply(analysis -> generatePatientReport(analysis));
    }
}
```

### 🚀 **Aerospace - Satellite Constellation Management**
```java
public class SatelliteControlAgent extends EnhancedMobileAgent {
    @Override
    public CompletableFuture<OrbitCorrection> manageSatelliteNetwork() {
        return spaceWeatherService.getCurrentConditions()
            .thenCompose(conditions -> satelliteConnector.getTelemetry())
            .thenCompose(telemetry -> calculateOptimalOrbits(conditions, telemetry))
            .thenCompose(orbits -> {
                // Migrate to ground station with best signal quality
                return findOptimalGroundStation()
                    .thenCompose(station -> dispatch(station))
                    .thenRun(() -> executeOrbitCorrections(orbits));
            });
    }
}
```

### 🏭 **Manufacturing - Global Supply Chain Orchestration**
```java
public class SupplyChainOrchestratorAgent extends EnhancedMobileAgent {
    @Override
    public CompletableFuture<Void> optimizeGlobalSupplyChain() {
        return suppliers.parallelStream()
            .map(supplier -> clone("datacenter-" + supplier.getRegion())
                .thenCompose(cloneId -> negotiateWithSupplier(supplier, cloneId)))
            .collect(Collectors.toList())
            .stream()
            .reduce(CompletableFuture.completedFuture(null), 
                (acc, future) -> acc.thenCompose(v -> future));
    }
    
    private CompletableFuture<Void> handleSupplyDisruption(String region) {
        return replicate("backup-suppliers-" + region, "logistics-" + region)
            .thenCompose(replicas -> activateContingencyPlan(replicas));
    }
}
```

## 🔧 Enterprise Configuration

### 🏢 Multi-Broker Enterprise Configuration
```properties
# Development Environment
amcp.event.broker.type=memory
amcp.development.mode=true

# Production Kafka Cluster
amcp.event.broker.type=kafka
amcp.kafka.bootstrap.servers=kafka-cluster:9092
amcp.kafka.consumer.group.id=amcp-enterprise-agents
amcp.kafka.security.protocol=SASL_SSL
amcp.kafka.sasl.mechanism=PLAIN
amcp.kafka.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="amcp-user" password="enterprise-secret";

# Enterprise NATS (High Performance)
amcp.event.broker.type=nats
amcp.nats.servers=nats://nats-cluster-1:4222,nats://nats-cluster-2:4222
amcp.nats.auth.username=amcp-enterprise
amcp.nats.auth.password=${NATS_PASSWORD}
amcp.nats.tls.enabled=true

# Solace PubSub+ (Enterprise Messaging)
amcp.event.broker.type=solace
amcp.solace.host=solace.enterprise.com
amcp.solace.vpn=amcp-production
amcp.solace.username=amcp-service-user
amcp.solace.password=${SOLACE_PASSWORD}
amcp.solace.connection-retries=5
amcp.solace.reconnect-retries=10
```

### 🔒 Enterprise Security Configuration
```properties
# Advanced Security Suite
amcp.security.enabled=true
amcp.security.advanced-suite.enabled=true

# Multi-Factor Authentication
amcp.security.mfa.enabled=true
amcp.security.mfa.providers=totp,sms,email,hardware-key
amcp.security.mfa.backup-codes.enabled=true

# Certificate-Based Authentication
amcp.security.mtls.enabled=true
amcp.security.mtls.keystore.path=/etc/amcp/certs/keystore.p12
amcp.security.mtls.keystore.password=${KEYSTORE_PASSWORD}
amcp.security.mtls.truststore.path=/etc/amcp/certs/truststore.p12
amcp.security.mtls.client-auth=required

# JWT Configuration
amcp.security.jwt.enabled=true
amcp.security.jwt.issuer=https://auth.enterprise.com
amcp.security.jwt.audience=amcp-agents
amcp.security.jwt.signing-key=${JWT_SIGNING_KEY}
amcp.security.jwt.expiration=1h

# RBAC Configuration
amcp.security.rbac.enabled=true
amcp.security.rbac.policy-file=/etc/amcp/security/rbac-policies.yaml
amcp.security.rbac.admin-roles=amcp-admin,system-admin
```

### 🚀 Enterprise Migration & Mobility
```properties
# Enhanced Mobility Configuration
amcp.mobility.enabled=true
amcp.mobility.security.enabled=true
amcp.mobility.encryption.enabled=true
amcp.mobility.compression.enabled=true

# Migration Settings
amcp.migration.timeout=60s
amcp.migration.retry.max=5
amcp.migration.retry.backoff=exponential
amcp.migration.authentication.required=true

# Replication Configuration
amcp.replication.enabled=true
amcp.replication.consistency-level=strong
amcp.replication.sync-timeout=30s
amcp.replication.health-check-interval=10s

# Federation Settings
amcp.federation.enabled=true
amcp.federation.discovery.enabled=true
amcp.federation.load-balancing=round-robin
```

### 🧠 LLM & AI Integration Configuration
```properties
# Ollama Integration
amcp.ai.ollama.enabled=true
amcp.ai.ollama.base-url=http://ollama:11434
amcp.ai.ollama.model=tinyllama:1.1b
amcp.ai.ollama.timeout=30s
amcp.ai.ollama.max-retries=3

# OpenAI Integration (Optional)
amcp.ai.openai.enabled=false
amcp.ai.openai.api-key=${OPENAI_API_KEY}
amcp.ai.openai.model=gpt-4
amcp.ai.openai.organization=${OPENAI_ORG_ID}

# Azure OpenAI (Enterprise)
amcp.ai.azure-openai.enabled=false
amcp.ai.azure-openai.endpoint=${AZURE_OPENAI_ENDPOINT}
amcp.ai.azure-openai.api-key=${AZURE_OPENAI_KEY}
amcp.ai.azure-openai.deployment-id=gpt-4-enterprise
```

### 📊 Enterprise Monitoring Configuration
```properties
# Prometheus Metrics
amcp.monitoring.prometheus.enabled=true
amcp.monitoring.prometheus.port=8081
amcp.monitoring.prometheus.path=/metrics
amcp.monitoring.prometheus.custom-metrics.enabled=true

# Jaeger Tracing
amcp.monitoring.jaeger.enabled=true
amcp.monitoring.jaeger.endpoint=http://jaeger:14268/api/traces
amcp.monitoring.jaeger.sampler.type=probabilistic
amcp.monitoring.jaeger.sampler.param=0.1

# Health Checks
amcp.monitoring.health.enabled=true
amcp.monitoring.health.check-interval=30s
amcp.monitoring.health.external-dependencies=kafka,postgres,ollama

# Audit Logging
amcp.audit.enabled=true
amcp.audit.level=info
amcp.audit.format=json
amcp.audit.destination=file,syslog,kafka
amcp.audit.retention-days=90
```

### 🔌 External API Integration
```properties
# Financial APIs
amcp.connectors.polygon.enabled=true
amcp.connectors.polygon.api-key=${POLYGON_API_KEY}
amcp.connectors.polygon.base-url=https://api.polygon.io
amcp.connectors.polygon.rate-limit=5/second

amcp.connectors.alpha-vantage.enabled=true
amcp.connectors.alpha-vantage.api-key=${ALPHA_VANTAGE_API_KEY}

# Weather APIs
amcp.connectors.openweather.enabled=true
amcp.connectors.openweather.api-key=${OPENWEATHER_API_KEY}
amcp.connectors.openweather.base-url=https://api.openweathermap.org

# Travel APIs
amcp.connectors.amadeus.enabled=false
amcp.connectors.amadeus.client-id=${AMADEUS_CLIENT_ID}
amcp.connectors.amadeus.client-secret=${AMADEUS_CLIENT_SECRET}
```

## 📚 Comprehensive Documentation

### 🎯 **Getting Started**
- [🚀 Enterprise Leverage Guide](ENTERPRISE_LEVERAGE_GUIDE.md) - Complete guide to leveraging enterprise features
- [⚡ Quick Start Guide](docs/MESHCHAT_QUICK_START.md) - Get up and running in 5 minutes
- [🏗️ Architecture Overview](docs/MESHCHAT_ARCHITECTURE.md) - Deep dive into system design

### 🔧 **Developer Resources**
- [📖 Complete Documentation](docs/MESHCHAT_DOCUMENTATION.md) - Comprehensive user and developer guide
- [🔧 API Reference](docs/MESHCHAT_API_REFERENCE.md) - Complete developer API documentation
- [🧪 Testing Framework Guide](TESTING_FRAMEWORK_GUIDE.md) - Comprehensive testing strategies
