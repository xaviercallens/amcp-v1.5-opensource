---
name: Performance Issue
about: Report a performance problem or regression
title: '[PERF] '
labels: ['performance', 'needs-triage']
assignees: ''
---

## Performance Issue Description
Describe the performance problem you're experiencing.

## Environment
- **AMCP Version**: [e.g., v1.5.0]
- **Java Version**: [e.g., OpenJDK 8]
- **Operating System**: [e.g., macOS 13.0]
- **Hardware**: [e.g., CPU, RAM, Storage]
- **JVM Settings**: [e.g., -Xmx4g -Xms2g]

## Workload Details
Describe your workload:
- **Number of agents**: [e.g., 100 agents]
- **Event throughput**: [e.g., 1000 events/second]
- **Message size**: [e.g., average 1KB payloads]
- **Topic patterns**: [e.g., hierarchical topics with wildcards]
- **Event broker**: [e.g., InMemoryEventBroker, Kafka]

## Performance Metrics
### Current Performance
- **Response time**: [e.g., 95th percentile: 500ms]
- **Throughput**: [e.g., 100 events/second]
- **CPU usage**: [e.g., 80% average]
- **Memory usage**: [e.g., 2GB heap, frequent GC]
- **Error rate**: [e.g., 0.1%]

### Expected Performance
- **Response time**: [e.g., 95th percentile: <100ms]
- **Throughput**: [e.g., 1000 events/second]
- **CPU usage**: [e.g., <50% average]
- **Memory usage**: [e.g., stable heap, minimal GC]

## Code Example
```java
// Code that exhibits the performance issue
public class PerformanceIssueAgent implements Agent {
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        // Code that's slow
        return CompletableFuture.runAsync(() -> {
            // Your code here
        });
    }
}
```

## Benchmark Results (if available)
```
Benchmark                    Mode  Cnt   Score   Error  Units
EventProcessingBenchmark    thrpt   10  100.5 ± 5.2   ops/s
```

## Profiling Data
If you have profiling data (JProfiler, async-profiler, etc.), please attach:
- CPU profiling results
- Memory allocation profiling
- GC analysis
- Thread dumps (if relevant)

## Configuration
```properties
# Relevant configuration that might affect performance
amcp.broker.type=inmemory
amcp.thread.pool.size=10
amcp.event.queue.size=1000
```

## Steps to Reproduce
1. Set up environment with [specific configuration]
2. Create [number] agents
3. Send [number] events at [rate]
4. Observe performance degradation after [time/events]

## Comparison (if applicable)
- **Previous version performance**: [e.g., v1.4 handled this workload fine]
- **Similar system performance**: [e.g., compared to other frameworks]
- **Performance regression timeline**: [e.g., started after commit XYZ]

## Impact
How is this performance issue affecting your application?
- [ ] Blocking production deployment
- [ ] Causing user-visible delays
- [ ] Requiring additional hardware resources
- [ ] Preventing scaling to required load

## Additional Context
- Is this consistently reproducible?
- Does it happen under specific conditions?
- Any workarounds you've found?
- Network latency considerations?
- Database/external service dependencies?