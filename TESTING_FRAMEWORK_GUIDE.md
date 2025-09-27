# AMCP v1.5 Enterprise Edition - Testing Framework

## Overview

The AMCP Testing Framework is a comprehensive enterprise-grade testing infrastructure that provides automated testing capabilities for multi-agent communication platforms. It integrates TestContainers for infrastructure testing, performance benchmarking, security validation, and chaos engineering to ensure system reliability and resilience.

## Features

### 🚀 Core Testing Infrastructure
- **TestContainers Integration**: Automated provisioning of Kafka, PostgreSQL, Redis, Elasticsearch containers
- **Multi-Environment Support**: Development, staging, and production testing environments
- **Parallel Test Execution**: Concurrent test execution with configurable thread pools
- **Test Result Aggregation**: Comprehensive reporting and metrics collection

### 📊 Performance Testing
- **Throughput Benchmarks**: Events per second measurement with configurable load
- **Latency Analysis**: P50, P95, P99 latency measurements with detailed percentile analysis
- **Scalability Testing**: Multi-agent and multi-topic scalability validation
- **Load Testing**: Sustained load and spike testing scenarios
- **Resource Monitoring**: Memory, CPU, and system resource utilization tracking

### 🔒 Security Validation
- **Authentication Testing**: Basic auth, JWT, certificate-based, and multi-factor authentication
- **Authorization Validation**: Role-based access control (RBAC) and permission testing
- **Encryption Verification**: Transport (TLS), message-level, and storage encryption
- **Input Validation**: SQL injection, XSS, and other injection attack prevention
- **Security Policy Compliance**: Password policies, session management, audit logging
- **Vulnerability Scanning**: Automated security vulnerability detection

### 🌪️ Chaos Engineering
- **Network Chaos**: Partition simulation, delay injection, packet loss testing
- **Service Chaos**: Failure simulation, slowdown testing, recovery validation
- **Resource Chaos**: Memory/CPU exhaustion, disk full scenarios
- **Byzantine Fault Tolerance**: Split-brain scenarios and consensus testing
- **Disaster Recovery**: System resilience and recovery capability validation

## Architecture

```
AMCP Testing Framework
├── AmcpTestingFramework (Main orchestrator)
├── TestMetricsCollector (Metrics and reporting)
├── PerformanceBenchmark (Performance testing)
├── SecurityTestValidator (Security validation)
├── ChaosTestEngine (Chaos engineering)
├── TestResult (Result aggregation)
└── TestConfiguration (Configuration management)
```

## Quick Start

### 1. Basic Usage

```java
import io.amcp.testing.*;

// Create testing framework with default configuration
AmcpTestingFramework framework = new AmcpTestingFramework();

// Run all tests
TestResult result = framework.runAllTests();

// Check results
if (result.isPassed()) {
    System.out.println("All tests passed! ✅");
    System.out.println(result.getSummary());
} else {
    System.out.println("Tests failed! ❌");
    System.out.println(result.getDetailedReport());
}

// Cleanup
framework.shutdown();
```

### 2. Custom Configuration

```java
// Create custom configuration
TestConfiguration config = TestConfiguration.builder()
    .enableTestContainers(true)
    .enablePerformanceTests(true)
    .enableSecurityTests(true)
    .enableChaosTests(false)  // Disable chaos tests
    .maxConcurrentThreads(10)
    .testTimeout(300)
    .enableContainer("kafka")
    .enableContainer("postgresql")
    .enableContainer("redis")
    .containerImage("kafka", "confluentinc/cp-kafka:7.0.0")
    .kafkaSetting("bootstrap.servers", "localhost:9092")
    .databaseSetting("url", "jdbc:postgresql://localhost:5432/testdb")
    .build();

// Initialize framework with custom configuration
AmcpTestingFramework framework = new AmcpTestingFramework(config);

// Run specific test categories
TestResult performanceResult = framework.runPerformanceTests();
TestResult securityResult = framework.runSecurityTests();
```

### 3. Pre-configured Test Scenarios

```java
// Performance-focused testing
TestConfiguration perfConfig = TestConfiguration.performanceConfiguration();
AmcpTestingFramework perfFramework = new AmcpTestingFramework(perfConfig);
TestResult perfResults = perfFramework.runPerformanceTests();

// Security-focused testing
TestConfiguration secConfig = TestConfiguration.securityConfiguration();
AmcpTestingFramework secFramework = new AmcpTestingFramework(secConfig);
TestResult secResults = secFramework.runSecurityTests();

// Comprehensive testing (all features)
TestConfiguration compConfig = TestConfiguration.comprehensiveConfiguration();
AmcpTestingFramework compFramework = new AmcpTestingFramework(compConfig);
TestResult compResults = compFramework.runAllTests();
```

## Test Categories

### Performance Tests

```java
// Throughput benchmarks
- Low throughput: 100 events/second
- Medium throughput: 1,000 events/second  
- High throughput: 10,000 events/second

// Latency measurements
- Single sender latency
- Concurrent sender latency
- P50, P95, P99 percentiles

// Scalability tests
- Multi-agent scalability (up to 100 agents)
- Multi-topic scalability (up to 50 topics)
- Sustained load testing (configurable duration)
- Spike load testing (burst scenarios)

// Resource monitoring
- Memory usage tracking
- CPU utilization
- Thread pool metrics
- Network I/O monitoring
```

### Security Tests

```java
// Authentication validation
- Basic authentication (username/password)
- JWT token validation (including expiration)
- X.509 certificate authentication
- Multi-factor authentication (MFA)

// Authorization testing
- Role-based access control (RBAC)
- Permission-based authorization
- Resource-level access control
- Time/location/device-based policies

// Encryption verification
- TLS/SSL transport encryption
- Message-level encryption/decryption
- Storage encryption validation
- Key management testing

// Input validation and injection prevention
- SQL injection prevention
- Cross-site scripting (XSS) protection
- Command injection prevention
- Path traversal protection

// Security policy compliance
- Password strength requirements
- Session timeout enforcement
- Account lockout policies
- Audit trail integrity
```

### Chaos Engineering Tests

```java
// Network chaos scenarios
- Network partition simulation
- Network delay injection (configurable latency)
- Packet loss simulation (configurable loss rate)
- Network jitter and instability

// Service chaos scenarios
- Service failure simulation
- Service slowdown testing
- Graceful degradation validation
- Recovery time measurement

// Resource chaos scenarios  
- Memory exhaustion testing
- CPU starvation simulation
- Disk space exhaustion
- Connection pool exhaustion

// Byzantine fault scenarios
- Split-brain condition testing
- Consensus mechanism validation
- Fault detection and recovery
- Data consistency verification
```

## Configuration Options

### TestContainers Configuration

```java
TestConfiguration config = TestConfiguration.builder()
    // Enable/disable TestContainers
    .enableTestContainers(true)
    
    // Container selection
    .enableContainer("kafka")
    .enableContainer("postgresql")  
    .enableContainer("redis")
    .enableContainer("elasticsearch")
    
    // Custom container images
    .containerImage("kafka", "confluentinc/cp-kafka:7.0.0")
    .containerImage("postgresql", "postgres:14")
    .containerImage("redis", "redis:7-alpine")
    
    // Port configuration
    .containerPort("kafka", 9092)
    .containerPort("postgresql", 5432)
    
    // Environment variables
    .containerEnv("kafka", "KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
    .containerEnv("postgresql", "POSTGRES_DB", "amcp_test")
    .build();
```

### Performance Configuration

```java
TestConfiguration config = TestConfiguration.builder()
    // Performance test settings
    .enablePerformanceTests(true)
    .maxConcurrentThreads(20)
    .performanceTestDuration(120) // seconds
    .maxEventsPerSecond(50000)
    .latencyTestSamples(5000)
    
    // Infrastructure settings
    .kafkaSetting("bootstrap.servers", "localhost:9092")
    .kafkaSetting("batch.size", "32768")
    .kafkaSetting("linger.ms", "10")
    .build();
```

### Security Configuration

```java
TestConfiguration config = TestConfiguration.builder()
    // Security test settings
    .enableSecurityTests(true)
    .enableAuthenticationTests(true)
    .enableAuthorizationTests(true)
    .enableEncryptionTests(true)
    .enableVulnerabilityTests(true)
    
    // Test credentials
    .securityCredential("test.username", "testuser")
    .securityCredential("test.password", "securepass123")
    .securityCredential("admin.username", "admin")
    .securityCredential("admin.password", "adminpass456")
    .build();
```

### Chaos Engineering Configuration

```java
TestConfiguration config = TestConfiguration.builder()
    // Chaos test settings
    .enableChaosTests(true)
    .enableNetworkChaos(true)
    .enableServiceChaos(true)
    .enableResourceChaos(true)
    .chaosInjectionRate(0.15) // 15% chaos injection rate
    .chaosTestDuration(240) // seconds
    .build();
```

## Metrics and Reporting

### Available Metrics

```java
// Performance metrics
- Throughput (events/second)
- Latency percentiles (P50, P95, P99)
- Resource utilization (CPU, memory)
- Error rates and success rates

// Security metrics
- Authentication success/failure rates
- Authorization violation attempts
- Encryption verification results
- Vulnerability scan results

// Chaos engineering metrics
- System recovery times
- Failure detection accuracy
- Data consistency verification
- Service availability during chaos
```

### Test Reports

```java
// Summary report
TestResult result = framework.runAllTests();
System.out.println(result.getSummary());
/* Output:
Test: comprehensive-test
Status: PASSED
Duration: 245678ms
Checks: 47/52 passed
Success Rate: 90.4%
*/

// Detailed report
System.out.println(result.getDetailedReport());
/* Output:
=== TEST RESULT REPORT ===
Test Name: comprehensive-test
Status: PASSED
Start Time: 2024-01-15T10:30:00Z
End Time: 2024-01-15T10:34:05Z
Duration: 245678ms
Success Rate: 90.4%

=== CHECK RESULTS ===
✅ throughput_low
✅ throughput_medium  
✅ throughput_high
✅ latency_single
❌ latency_concurrent
✅ authentication_basic
...
*/

// Metrics access
TestMetricsCollector metrics = framework.getMetrics();
System.out.println("Average throughput: " + metrics.getAverageThroughput());
System.out.println("Security violations: " + metrics.getSecurityViolations());
```

## Integration Examples

### CI/CD Pipeline Integration

```yaml
# GitHub Actions example
name: AMCP Testing Framework
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    - uses: actions/setup-java@v3
      with:
        java-version: '17'
    - name: Run AMCP Tests
      run: |
        mvn test -Dtest=AmcpTestingFrameworkIntegrationTest
        mvn test -Dtest=PerformanceBenchmarkTest
        mvn test -Dtest=SecurityValidationTest
```

### Docker Compose Integration

```yaml
# docker-compose.test.yml
version: '3.8'
services:
  amcp-test:
    build: .
    depends_on:
      - kafka
      - postgresql
      - redis
    environment:
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - DATABASE_URL=postgresql://postgres:password@postgresql:5432/testdb
      - REDIS_URL=redis:6379
    command: java -jar amcp-testing-framework.jar --config=integration
  
  kafka:
    image: confluentinc/cp-kafka:latest
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
    depends_on:
      - zookeeper
      
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      
  postgresql:
    image: postgres:13
    environment:
      POSTGRES_PASSWORD: password
      POSTGRES_DB: testdb
      
  redis:
    image: redis:6-alpine
```

## Best Practices

### 1. Test Organization

```java
// Organize tests by category
public class AmcpTestSuite {
    @Test
    public void runInfrastructureTests() {
        // TestContainers integration tests
    }
    
    @Test 
    public void runPerformanceTests() {
        // Throughput and latency tests
    }
    
    @Test
    public void runSecurityTests() {
        // Authentication and authorization tests
    }
    
    @Test
    public void runChaosTests() {
        // Resilience and fault tolerance tests
    }
}
```

### 2. Configuration Management

```java
// Environment-specific configurations
public class TestConfigurations {
    public static TestConfiguration development() {
        return TestConfiguration.builder()
            .enableChaosTests(false) // Disable chaos in dev
            .testTimeout(60)
            .maxConcurrentThreads(2)
            .build();
    }
    
    public static TestConfiguration staging() {
        return TestConfiguration.performanceConfiguration();
    }
    
    public static TestConfiguration production() {
        return TestConfiguration.comprehensiveConfiguration();
    }
}
```

### 3. Error Handling

```java
try {
    AmcpTestingFramework framework = new AmcpTestingFramework(config);
    TestResult result = framework.runAllTests();
    
    if (!result.isPassed()) {
        // Log failures for investigation
        logger.error("Tests failed: {}", result.getDetailedReport());
        
        // Send notifications for critical failures
        if (result.getFailedChecks() > 10) {
            notificationService.sendAlert("Critical test failures detected");
        }
    }
    
} catch (Exception e) {
    logger.error("Testing framework error", e);
    // Implement fallback testing strategy
} finally {
    framework.shutdown();
}
```

### 4. Resource Management

```java
// Use try-with-resources for automatic cleanup
try (AmcpTestingFramework framework = new AmcpTestingFramework(config)) {
    
    // Set resource limits
    framework.setMemoryLimit("2g");
    framework.setCpuLimit(4);
    framework.setTestTimeout(Duration.ofMinutes(10));
    
    // Run tests
    TestResult result = framework.runAllTests();
    
    // Framework automatically cleaned up when try block exits
}
```

## Troubleshooting

### Common Issues

1. **TestContainers startup failures**
   ```
   Solution: Ensure Docker daemon is running and accessible
   Check Docker permissions and resource availability
   ```

2. **Performance test timeouts**
   ```
   Solution: Increase test timeout or reduce load parameters
   Check system resources and network connectivity
   ```

3. **Security test failures**
   ```
   Solution: Verify test credentials and security configurations
   Check certificate validity and encryption settings
   ```

4. **Chaos test instability**
   ```
   Solution: Reduce chaos injection rate
   Increase recovery timeout settings
   ```

### Debug Mode

```java
// Enable debug logging
TestConfiguration config = TestConfiguration.builder()
    .enableDebugLogging(true)
    .logLevel("DEBUG")
    .generateReports(true)
    .reportOutputPath("./debug-reports")
    .build();

AmcpTestingFramework framework = new AmcpTestingFramework(config);
```

## Performance Benchmarks

Expected performance characteristics on modern hardware:

| Test Category | Throughput | Latency (P95) | Duration |
|---------------|------------|---------------|----------|
| Basic Infrastructure | N/A | <50ms | 30s |
| Performance Suite | 25,000 events/sec | <100ms | 120s |
| Security Suite | N/A | <200ms | 180s |
| Chaos Engineering | Variable | Variable | 240s |
| Comprehensive | 15,000 events/sec | <150ms | 300s |

## Dependencies

```xml
<!-- Required dependencies -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.0</version>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>kafka</artifactId>
    <version>1.19.0</version>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.0</version>
</dependency>
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>3.5.0</version>
</dependency>
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.0</version>
</dependency>
```

## Support and Contributing

For issues, feature requests, or contributions:
- Create an issue in the project repository
- Follow the contribution guidelines
- Ensure all tests pass before submitting pull requests
- Include comprehensive test coverage for new features

The AMCP Testing Framework is designed to evolve with your testing needs, providing a robust foundation for enterprise-grade multi-agent system validation.