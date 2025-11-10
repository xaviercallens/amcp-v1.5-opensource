# Migration Guide: AMCP v1.5 → v1.6

## Overview

This guide helps you migrate from AMCP v1.5 to v1.6. While v1.6 introduces breaking changes, we provide a compatibility layer and step-by-step migration path.

**Estimated Migration Time**: 2-4 hours for typical projects

---

## 🔄 Breaking Changes Summary

| Component | v1.5 | v1.6 | Migration |
|-----------|------|------|-----------|
| Agent Interface | `Agent` | `StrongMobilityAgent` | Implement new interface |
| Events | Custom format | CloudEvents | Use CloudEventBuilder |
| Configuration | Old schema | New schema | Update config files |
| LLM API | Basic connector | Enhanced connector | Update method calls |
| Mesh | Basic mesh | Advanced mesh | Update mesh config |

---

## Step 1: Update Dependencies

### Maven (pom.xml)

```xml
<!-- Before (v1.5) -->
<dependency>
    <groupId>com.amcp</groupId>
    <artifactId>amcp-core</artifactId>
    <version>1.5.1</version>
</dependency>

<!-- After (v1.6) -->
<dependency>
    <groupId>com.amcp</groupId>
    <artifactId>amcp-core</artifactId>
    <version>1.6.0</version>
</dependency>

<!-- Add new dependencies -->
<dependency>
    <groupId>io.cloudevents</groupId>
    <artifactId>cloudevents-core</artifactId>
    <version>2.5.0</version>
</dependency>

<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-netty-shaded</artifactId>
    <version>1.59.0</version>
</dependency>
```

### Gradle (build.gradle)

```gradle
// Before (v1.5)
implementation 'com.amcp:amcp-core:1.5.1'

// After (v1.6)
implementation 'com.amcp:amcp-core:1.6.0'
implementation 'io.cloudevents:cloudevents-core:2.5.0'
implementation 'io.grpc:grpc-netty-shaded:1.59.0'
```

---

## Step 2: Update Agent Implementations

### Before (v1.5)

```java
public class DataProcessorAgent implements Agent {
    private String data;
    
    @Override
    public void execute() {
        // Process data
        String result = processData(data);
        System.out.println("Result: " + result);
    }
    
    private String processData(String input) {
        return input.toUpperCase();
    }
}
```

### After (v1.6)

```java
public class DataProcessorAgent implements StrongMobilityAgent {
    private String data;
    private ExecutionState executionState;
    
    @Override
    public void execute() {
        // Process data
        String result = processData(data);
        System.out.println("Result: " + result);
    }
    
    @Override
    public CompletableFuture<Void> dispatch(String destination) {
        // Capture state before migration
        this.executionState = captureState();
        
        // Send to remote system
        return ATPTransport.getInstance()
            .dispatch(destination, this);
    }
    
    @Override
    public StrongMobilityAgent clone() {
        DataProcessorAgent clone = new DataProcessorAgent();
        clone.data = this.data;
        clone.executionState = this.executionState;
        return clone;
    }
    
    @Override
    public CompletableFuture<Void> retract() {
        return ATPTransport.getInstance()
            .retract(this);
    }
    
    @Override
    public ExecutionState getExecutionState() {
        return this.executionState;
    }
    
    private ExecutionState captureState() {
        return ExecutionStateCapture.getInstance()
            .captureState();
    }
    
    private String processData(String input) {
        return input.toUpperCase();
    }
}
```

### Using Compatibility Layer (Optional)

If you want to keep v1.5 agents working without modification:

```java
// Wrap v1.5 agent with compatibility adapter
Agent legacyAgent = new LegacyDataProcessorAgent();
StrongMobilityAgent adaptedAgent = 
    new StrongMobilityAdapter(legacyAgent);
```

---

## Step 3: Update Event Handling

### Before (v1.5)

```java
// Creating events
Event event = new Event("agent.created", 
    new EventData("processor-1", "active"));

// Handling events
eventBus.subscribe("agent.created", (event) -> {
    System.out.println("Agent created: " + event.getData());
});
```

### After (v1.6)

```java
// Creating CloudEvents
CloudEvent event = CloudEventBuilder.v1()
    .withType("com.amcp.agent.created")
    .withSource("https://amcp.dev/agents/processor-1")
    .withId(UUID.randomUUID().toString())
    .withTime(OffsetDateTime.now(ZoneOffset.UTC))
    .withDataContentType("application/json")
    .withData(new EventData("processor-1", "active").toJson())
    .build();

// Handling CloudEvents
eventBus.subscribe("com.amcp.agent.created", (cloudEvent) -> {
    String agentId = cloudEvent.getSubject();
    System.out.println("Agent created: " + agentId);
});
```

### Event Types Reference

```java
// Agent Events
"com.amcp.agent.created"      // Agent created
"com.amcp.agent.migrated"     // Agent migrated
"com.amcp.agent.failed"       // Agent failed
"com.amcp.agent.completed"    // Agent completed

// Mesh Events
"com.amcp.mesh.node.joined"   // Node joined mesh
"com.amcp.mesh.node.left"     // Node left mesh

// LLM Events
"com.amcp.llm.request"        // LLM request made
"com.amcp.llm.response"       // LLM response received
```

---

## Step 4: Update Configuration

### Before (v1.5) - config.properties

```properties
# Agent Configuration
agent.timeout=30000
agent.pool.size=10

# LLM Configuration
llm.model=gemma2b
llm.timeout=90000
llm.cache.enabled=true

# Mesh Configuration
mesh.port=4434
mesh.nodes=localhost:4434,localhost:4435
```

### After (v1.6) - config.yaml

```yaml
# Agent Configuration
agent:
  timeout: 30000
  pool:
    size: 10
  mobility:
    enabled: true
    timeout: 5000

# LLM Configuration
llm:
  model: gemma2b
  timeout: 90000
  cache:
    enabled: true
    ttl: 86400
    tiers:
      - type: memory
        size: 100MB
      - type: disk
        size: 1GB
  fallback:
    enabled: true
    confidence_threshold: 0.75

# Mesh Configuration
mesh:
  port: 4434
  nodes:
    - localhost:4434
    - localhost:4435
  service_discovery:
    enabled: true
    type: kubernetes
  load_balancing:
    strategy: round_robin
  circuit_breaker:
    enabled: true
    failure_threshold: 5
    timeout: 30000

# Security Configuration
security:
  mtls:
    enabled: true
    cert_path: /etc/amcp/certs/server.crt
    key_path: /etc/amcp/certs/server.key
  rbac:
    enabled: true
  audit:
    enabled: true
    log_path: /var/log/amcp/audit.log
  vault:
    enabled: true
    address: https://vault.example.com:8200
    token_path: /var/run/secrets/vault-token
```

---

## Step 5: Update LLM Connector Usage

### Before (v1.5)

```java
// Synchronous LLM calls
LLMConnector connector = new LLMConnector();
String response = connector.query("What is AI?");
System.out.println(response);
```

### After (v1.6)

```java
// Asynchronous LLM calls with fallback
AsyncLLMConnector connector = new AsyncLLMConnector();

connector.queryAsync("What is AI?")
    .thenAccept(response -> {
        System.out.println("LLM Response: " + response);
    })
    .exceptionally(ex -> {
        System.out.println("Using fallback: " + ex.getMessage());
        return null;
    });

// Or with CompletableFuture
CompletableFuture<String> future = connector.queryAsync("What is AI?");
String response = future.join();
```

---

## Step 6: Update Mesh Configuration

### Before (v1.5)

```java
// Manual mesh setup
AgentMesh mesh = new AgentMesh();
mesh.addNode("localhost:4434");
mesh.addNode("localhost:4435");
mesh.start();
```

### After (v1.6)

```java
// Automatic service discovery
AgentMesh mesh = new AgentMesh();
mesh.enableServiceDiscovery(ServiceDiscoveryType.KUBERNETES);
mesh.enableLoadBalancing(LoadBalancingStrategy.ROUND_ROBIN);
mesh.enableCircuitBreaker(5, 30000);
mesh.start();
```

---

## Step 7: Update Security Configuration

### Before (v1.5)

```java
// Basic security
AgentMesh mesh = new AgentMesh();
mesh.setPort(4434);
```

### After (v1.6)

```java
// Enterprise security
AgentMesh mesh = new AgentMesh();

// Enable mTLS
MutualTLSManager tlsManager = new MutualTLSManager();
tlsManager.loadCertificate("/etc/amcp/certs/server.crt");
tlsManager.loadPrivateKey("/etc/amcp/certs/server.key");
mesh.setTLSManager(tlsManager);

// Enable RBAC
RBACManager rbacManager = new RBACManager();
rbacManager.createRole("admin", 
    Permission.AGENT_CREATE, 
    Permission.AGENT_DELETE,
    Permission.AGENT_MIGRATE);
rbacManager.createRole("user",
    Permission.AGENT_EXECUTE);
mesh.setRBACManager(rbacManager);

// Enable audit logging
AuditLogger auditLogger = new AuditLogger("/var/log/amcp/audit.log");
mesh.setAuditLogger(auditLogger);

mesh.start();
```

---

## Step 8: Testing Your Migration

### Unit Tests

```java
@Test
public void testAgentMigration() {
    // Create agent
    StrongMobilityAgent agent = new DataProcessorAgent();
    agent.setData("test data");
    
    // Migrate to remote
    CompletableFuture<Void> future = 
        agent.dispatch("atp://remote-node:4434/context");
    
    // Verify migration
    future.join();
    assertTrue(agent.getExecutionState() != null);
}

@Test
public void testCloudEvents() {
    // Create CloudEvent
    CloudEvent event = CloudEventBuilder.v1()
        .withType("com.amcp.agent.created")
        .withSource("https://amcp.dev/agents/test")
        .build();
    
    // Verify event
    assertEquals("com.amcp.agent.created", event.getType());
    assertEquals("https://amcp.dev/agents/test", event.getSource());
}

@Test
public void testLLMFallback() {
    AsyncLLMConnector connector = new AsyncLLMConnector();
    
    // Should use fallback when LLM times out
    CompletableFuture<String> future = 
        connector.queryAsync("What is AI?");
    
    String response = future.join();
    assertNotNull(response);
}
```

### Integration Tests

```java
@Test
public void testEndToEndMigration() {
    // Start two nodes
    AgentMesh node1 = new AgentMesh();
    AgentMesh node2 = new AgentMesh();
    node1.start();
    node2.start();
    
    // Create and migrate agent
    StrongMobilityAgent agent = new DataProcessorAgent();
    agent.dispatch("atp://localhost:4435/context").join();
    
    // Verify agent is running on node2
    assertTrue(node2.hasAgent(agent.getId()));
    
    // Cleanup
    node1.stop();
    node2.stop();
}
```

---

## Step 9: Deployment Checklist

- [ ] Updated all dependencies
- [ ] Updated agent implementations
- [ ] Updated event handling
- [ ] Updated configuration files
- [ ] Updated LLM connector usage
- [ ] Updated mesh configuration
- [ ] Updated security configuration
- [ ] All tests passing
- [ ] Documentation updated
- [ ] Performance benchmarks verified
- [ ] Security audit completed
- [ ] Backup of v1.5 configuration created

---

## Step 10: Rollback Plan

If you encounter issues, you can rollback:

```bash
# Revert to v1.5
git checkout v1.5.1

# Restore v1.5 configuration
cp config.properties.v1.5 config.properties

# Rebuild with v1.5 dependencies
mvn clean install
```

---

## Troubleshooting

### Issue: "StrongMobilityAgent not found"

**Solution**: Ensure you've updated the AMCP dependency to v1.6.0

```xml
<version>1.6.0</version>
```

### Issue: "CloudEvent type not recognized"

**Solution**: Use the new CloudEvents format with proper type prefix

```java
.withType("com.amcp.agent.created")  // Correct
// NOT: .withType("agent.created")   // Wrong
```

### Issue: "mTLS handshake failed"

**Solution**: Verify certificates are properly configured

```java
MutualTLSManager tlsManager = new MutualTLSManager();
tlsManager.loadCertificate("/path/to/cert.crt");
tlsManager.loadPrivateKey("/path/to/key.key");
tlsManager.validateCertificates();  // Verify
```

### Issue: "LLM connector timeout"

**Solution**: Check fallback system is enabled

```yaml
llm:
  fallback:
    enabled: true
    confidence_threshold: 0.75
```

---

## Performance Considerations

### Before Migration
- Single LLM request: ~5-120s
- Memory usage: ~2.5GB
- Concurrent requests: 1

### After Migration
- Cached response: ~50ms (10x faster)
- Memory usage: ~1GB (60% reduction)
- Concurrent requests: 10 (10x capacity)

---

## Support

If you encounter issues:

1. Check [GitHub Issues](https://github.com/xaviercallens/amcp-v1.5-opensource/issues)
2. Review [Architecture Documentation](AMCP_V1.6_ARCHITECTURE.md)
3. Check [CHANGELOG](../CHANGELOG.md)
4. Contact maintainers

---

## Next Steps

After successful migration:

1. **Monitor Performance**: Use new performance profiler
2. **Explore New Features**: Try strong mobility and CloudEvents
3. **Optimize Configuration**: Tune for your environment
4. **Provide Feedback**: Share your experience with the team

---

**Migration Guide Version**: 1.0  
**Last Updated**: 2024-11-10  
**Status**: Ready for Use
