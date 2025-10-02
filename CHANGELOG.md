# Changelog

All notable changes to AMCP (Agent Mesh Communication Protocol) will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Comprehensive developer documentation suite
- Interactive demo launcher (`amcp-demos.sh`)
- Automated getting started script (`get-started.sh`)
- GitHub issue and PR templates
- CI/CD workflows for automated testing
- API reference documentation
- Architecture guide with diagrams

### Changed
- Reorganized project structure (scripts to `scripts/demos/`, guides to `docs/guides/`)
- Improved README with comparison table and quick start
- Enhanced developer onboarding experience

## [1.5.0] - 2025-10-02

### Added
- **IBM Aglet-style Strong Mobility**: Complete implementation of agent mobility operations
  - `dispatch()` - Move agent to remote context
  - `clone()` - Create agent copy in remote context
  - `retract()` - Recall agent from remote context
  - `migrate()` - Intelligent migration with load balancing
  - `replicate()` - High-availability replication across contexts
  - `federateWith()` - Form collaborative agent federations
- **LLM Integration**: Native support for Ollama and TinyLlama
  - OrchestratorAgent for multi-agent AI coordination
  - MeshChat conversational AI agent
  - Context-aware dialogue management
- **Google A2A Protocol Bridge**: Bidirectional interoperability
  - A2A message format compatibility
  - Correlation ID mapping
  - Authentication context propagation
- **CloudEvents 1.0 Compliance**: Standard event format
  - CloudEvents metadata support
  - Integration with Azure Event Grid, AWS EventBridge
- **Enhanced EventBroker System**: Pluggable broker architecture
  - InMemoryEventBroker for development
  - KafkaEventBroker for production scale
  - NATSEventBroker for edge deployments
  - SolaceEventBroker for enterprise messaging
- **Interactive CLI**: Full-featured command-line interface
  - Real-time agent monitoring
  - Event inspection and replay
  - Session management with history
  - Multiple agent control
- **Security Enhancements**: Multi-layer security model
  - Authentication context for agents
  - Topic-level authorization
  - TLS/mTLS support for brokers
  - Audit logging with correlation tracking
- **Tool Connector Framework**: MCP protocol integration
  - AbstractToolConnector base class
  - OAuth2 authentication support
  - Request/response schema validation
- **Example Agents**: Production-ready reference implementations
  - WeatherAgent with OpenWeatherMap integration
  - TravelPlannerAgent for multi-step workflows
  - StockPriceAgent with Polygon.io API
  - MeshChatAgent for conversational AI

### Changed
- **Agent Lifecycle**: Strict state machine enforcement
  - Guaranteed callback execution order
  - Timeout handling for long-running operations
  - Migration state tracking
- **Event System**: Enhanced topic pattern matching
  - Hierarchical topic namespaces (`domain.action.detail`)
  - Single-level wildcard (`*`) and multi-level wildcard (`**`)
  - Correlation ID support for request-response patterns
- **Serialization**: Improved agent state transfer
  - Custom serialization hooks
  - Non-serializable resource handling
  - State verification during migration

### Fixed
- Agent lifecycle callbacks not called consistently
- Event subscription race conditions during migration
- Memory leaks in EventBroker implementations
- Topic pattern matching edge cases
- Authentication context not propagated to tool calls

### Security
- Added authentication context for all agent operations
- Implemented topic-level authorization checks
- Enhanced audit logging with event correlation
- TLS/mTLS support for inter-context communication

## [1.4.0] - 2025-08-15

### Added
- Core agent framework with event-driven architecture
- Basic mobility operations (dispatch, clone)
- Multi-broker support (Kafka, NATS)
- Initial documentation

### Changed
- Refactored EventBroker interface for extensibility
- Improved error handling in agent lifecycle

### Fixed
- Agent registration race conditions
- Event ordering issues in distributed scenarios

## [1.3.0] - 2025-06-10

### Added
- Prototype mobile agent implementation
- Basic tool connector support
- Example agents

### Changed
- Enhanced agent context API
- Improved event serialization

## [1.2.0] - 2025-04-05

### Added
- Topic-based pub/sub messaging
- Agent lifecycle management
- Basic security model

## [1.1.0] - 2025-02-20

### Added
- Initial EventBroker implementation
- Agent registration and discovery
- Simple event routing

## [1.0.0] - 2025-01-15

### Added
- Initial release of AMCP framework
- Core agent interface
- Basic event system
- In-memory message broker
- Documentation and examples

---

## Release Notes Format

### Categories
- **Added** for new features
- **Changed** for changes in existing functionality
- **Deprecated** for soon-to-be removed features
- **Removed** for now removed features
- **Fixed** for any bug fixes
- **Security** for vulnerability fixes

### Version Format
- **Major.Minor.Patch** following Semantic Versioning
- Major: Breaking changes
- Minor: New features, backward compatible
- Patch: Bug fixes, backward compatible

[Unreleased]: https://github.com/xaviercallens/amcp-v1.5-opensource/compare/v1.5.0...HEAD
[1.5.0]: https://github.com/xaviercallens/amcp-v1.5-opensource/compare/v1.4.0...v1.5.0
[1.4.0]: https://github.com/xaviercallens/amcp-v1.5-opensource/compare/v1.3.0...v1.4.0
[1.3.0]: https://github.com/xaviercallens/amcp-v1.5-opensource/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/xaviercallens/amcp-v1.5-opensource/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/xaviercallens/amcp-v1.5-opensource/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/xaviercallens/amcp-v1.5-opensource/releases/tag/v1.0.0
