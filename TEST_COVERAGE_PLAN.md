# AMCP v1.5 - Test Coverage Enhancement Plan

## Current Test Status ✅

### Existing Tests:
- **Examples Module**: 14 tests passing
  - `MeshChatBasicTest.java` - 4 tests
  - `MeshChatSimpleTest.java` - 10 tests
  - `MeshChatDemoIntegrationTest.java` - Integration tests

### Test Coverage:
- **Core Module**: Minimal coverage (testing framework demo only)
- **Connectors Module**: No unit tests
- **CLI Module**: No unit tests
- **Examples Module**: Good coverage for MeshChat agents

## New Unit Tests Created 📝

### Core Module Tests:
1. **`AgentIDTest.java`** ✅ Created
   - Tests AgentID creation, uniqueness, equality
   - Edge cases: null, empty, special characters
   - 9 test methods

2. **`EventTest.java`** ⚠️ Needs API fixes
   - Tests Event builder pattern
   - Payload handling, correlation IDs
   - Timestamp management
   - 10 test methods
   - **Status**: Compilation errors - needs API alignment

3. **`InMemoryEventBrokerTest.java`** ⚠️ Needs API fixes
   - Tests publish/subscribe functionality
   - Pattern matching, multiple subscribers
   - Error handling, event ordering
   - 10 test methods
   - **Status**: Compilation errors - needs EventSubscriber interface

### Connectors Module Tests:
4. **`OrchestratorAgentTest.java`** ✅ Created
   - Tests simulation mode functionality
   - Query routing (weather, travel, finance)
   - Lifecycle management
   - Concurrent request handling
   - 12 test methods

5. **`AgentRegistryTest.java`** ✅ Created
   - Tests capability registration
   - Agent discovery
   - Multiple capabilities per agent
   - 5 test methods

### CLI Module Tests:
6. **`AgentRegistryTest.java`** ✅ Created
   - Tests agent registration
   - Instance creation
   - Registry queries
   - 9 test methods

7. **`CommandProcessorTest.java`** ✅ Created
   - Tests command parsing
   - Command execution
   - Edge cases (null, empty, whitespace)
   - 14 test methods

## Required Fixes 🔧

### API Alignment Issues:

#### Event Class:
```java
// Current API:
event.getId()          // NOT getEventId()
event.getTimestamp()   // Returns LocalDateTime, not long
event.getPayload()     // Returns Object
```

#### EventBroker Interface:
```java
// Current API:
subscribe(EventSubscriber subscriber, String topicPattern)
// NOT subscribe(String topic, Consumer<Event> handler, AgentID agentId)
```

### Required Changes:

1. **EventTest.java** - Fix method calls:
   - Change `getEventId()` → `getId()`
   - Fix timestamp comparison (use LocalDateTime methods)
   - Fix payload type casting

2. **InMemoryEventBrokerTest.java** - Implement EventSubscriber:
   - Create EventSubscriber implementations
   - Update subscribe() calls to match API
   - Update unsubscribe() calls

## Test Coverage Goals 🎯

### Target Coverage by Module:

| Module | Current | Target | Priority |
|--------|---------|--------|----------|
| **Core** | ~10% | 70%+ | HIGH |
| **Connectors** | ~5% | 60%+ | HIGH |
| **Examples** | ~40% | 70%+ | MEDIUM |
| **CLI** | 0% | 50%+ | MEDIUM |

### Additional Tests Needed:

#### Core Module:
- ✅ AgentID - Complete
- ⚠️ Event - Needs fixes
- ⚠️ EventBroker - Needs fixes
- ❌ AgentContext - Not started
- ❌ AgentLifecycle - Not started
- ❌ DeliveryOptions - Not started
- ❌ MobilityManager - Not started

#### Connectors Module:
- ✅ OrchestratorAgent - Complete (simulation mode)
- ✅ AgentRegistry - Complete
- ❌ OllamaSpringAIConnector - Not started
- ❌ EnhancedChatAgent - Not started
- ❌ ToolRequest/ToolResponse - Not started

#### Examples Module:
- ✅ MeshChatAgent - Good coverage
- ❌ WeatherAgent - Not started
- ❌ TravelPlannerAgent - Not started
- ❌ StockAgent - Not started

#### CLI Module:
- ✅ AgentRegistry - Complete
- ✅ CommandProcessor - Complete
- ❌ AMCPInteractiveCLI - Not started
- ❌ StatusMonitor - Not started

## Mock Objects Needed 🎭

### Core Mocks:
```java
// MockAgent - Simple agent for testing
// MockEventBroker - In-memory broker for testing
// MockAgentContext - Minimal context implementation
```

### Connector Mocks:
```java
// MockOllamaConnector - Simulated AI responses
// MockToolConnector - Tool execution simulation
// MockAgentRegistry - Capability registry mock
```

### Integration Test Helpers:
```java
// TestEventBuilder - Fluent event creation
// TestAgentFactory - Agent instance creation
// TestScenarios - Common test scenarios
```

## Implementation Strategy 📋

### Phase 1: Fix Existing Tests (Immediate)
1. ✅ Fix EventTest.java API calls
2. ✅ Fix InMemoryEventBrokerTest.java EventSubscriber usage
3. ✅ Run `mvn test` to verify all tests pass

### Phase 2: Core Module Coverage (High Priority)
1. ✅ Complete AgentContext tests
2. ✅ Complete DeliveryOptions tests
3. ✅ Add MobilityManager tests
4. ✅ Target: 70%+ line coverage

### Phase 3: Connectors Module Coverage (High Priority)
1. ✅ Add OllamaSpringAIConnector tests (with mocks)
2. ✅ Add EnhancedChatAgent tests
3. ✅ Add ToolRequest/Response tests
4. ✅ Target: 60%+ line coverage

### Phase 4: Examples & CLI Coverage (Medium Priority)
1. ✅ Add WeatherAgent tests
2. ✅ Add TravelPlannerAgent tests
3. ✅ Add StockAgent tests
4. ✅ Add CLI integration tests
5. ✅ Target: 50%+ line coverage

### Phase 5: Integration Tests (Future)
1. ✅ Multi-agent workflow tests
2. ✅ End-to-end scenario tests
3. ✅ Performance benchmarks
4. ✅ Stress tests

## Test Utilities Created 🛠️

### Mock Implementations:
```java
// MockAgent - Basic agent for testing
class MockAgent implements Agent {
    private AgentLifecycle state = AgentLifecycle.INACTIVE;
    private final AgentID agentId;
    // ... implementation
}
```

### Test Helpers:
- Event builders for common scenarios
- Agent factory methods
- Assertion utilities for async operations

## Running Tests 🧪

### Commands:
```bash
# Run all tests
mvn test

# Run tests with coverage
mvn test jacoco:report

# Run specific module tests
mvn test -pl core
mvn test -pl connectors
mvn test -pl examples
mvn test -pl cli

# View coverage report
open core/target/site/jacoco/index.html
open connectors/target/site/jacoco/index.html
```

## Success Criteria ✅

### Minimum Requirements:
- ✅ All existing tests pass
- ✅ Core module: 70%+ line coverage
- ✅ Connectors module: 60%+ line coverage
- ✅ No compilation errors
- ✅ No test failures

### Stretch Goals:
- 🎯 Examples module: 70%+ coverage
- 🎯 CLI module: 50%+ coverage
- 🎯 Integration tests for key workflows
- 🎯 Performance benchmarks established

## Current Status Summary 📊

### Tests Created: 7 files
- ✅ 3 Core module tests (1 needs fixes)
- ✅ 2 Connectors module tests
- ✅ 2 CLI module tests

### Tests Passing: 14/14 (existing)
### Tests Failing: 0
### Compilation Errors: 2 files (fixable)

### Estimated Coverage Improvement:
- **Before**: ~15% overall
- **After fixes**: ~35% overall
- **After full implementation**: ~65% overall

---

**Next Steps**: Fix API alignment issues in EventTest and InMemoryEventBrokerTest, then proceed with Phase 2-5 implementation.
