package io.amcp.testing.demo;

import io.amcp.testing.*;
import io.amcp.messaging.EventBroker;
import io.amcp.messaging.impl.InMemoryEventBroker;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * AMCP Testing Framework Demonstration
 * 
 * This demo showcases the comprehensive testing capabilities of the AMCP
 * Testing Framework including infrastructure testing, performance benchmarks,
 * security validation, and chaos engineering.
 * 
 * Run this demo to see the testing framework in action with different
 * test configurations and scenarios.
 */
public class TestingFrameworkDemo {
    
    private static final Logger logger = Logger.getLogger(TestingFrameworkDemo.class.getName());
    
    public static void main(String[] args) {
        logger.info("🚀 AMCP Testing Framework Demonstration");
        logger.info("=====================================");
        
        try {
            // Run different demonstration scenarios
            runBasicDemo();
            runPerformanceDemo();
            runSecurityDemo();
            runChaosDemo();
            runComprehensiveDemo();
            
        } catch (Exception e) {
            logger.severe("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        logger.info("✅ All demos completed successfully!");
    }
    
    /**
     * Basic testing framework demonstration
     */
    private static void runBasicDemo() {
        logger.info("\n📋 Running Basic Testing Demo");
        logger.info("==============================");
        
        try {
            // Create minimal configuration
            TestConfiguration config = TestConfiguration.minimalConfiguration();
            
            // Initialize framework
            AmcpTestingFramework framework = new AmcpTestingFramework(config);
            
            logger.info("🔧 Starting infrastructure tests...");
            
            // Run infrastructure tests
            TestResult result = framework.runInfrastructureTests();
            
            // Display results
            displayResults("Basic Infrastructure Tests", result);
            
            // Cleanup
            framework.shutdown();
            
        } catch (Exception e) {
            logger.severe("Basic demo failed: " + e.getMessage());
        }
    }
    
    /**
     * Performance testing demonstration
     */
    private static void runPerformanceDemo() {
        logger.info("\n🚀 Running Performance Testing Demo");
        logger.info("===================================");
        
        try {
            // Create performance-focused configuration
            TestConfiguration config = TestConfiguration.performanceConfiguration();
            
            // Initialize framework
            AmcpTestingFramework framework = new AmcpTestingFramework(config);
            
            logger.info("📊 Starting performance benchmarks...");
            
            // Create event broker for testing
            EventBroker eventBroker = new InMemoryEventBroker();
            eventBroker.start();
            
            // Run performance tests
            TestResult result = framework.runPerformanceBenchmarks(eventBroker);
            
            // Cleanup
            eventBroker.stop();
            
            // Display results with metrics
            displayResults("Performance Benchmarks", result);
            displayPerformanceMetrics(framework.getMetricsCollector());
            
            // Cleanup
            framework.shutdown();
            
        } catch (Exception e) {
            logger.severe("Performance demo failed: " + e.getMessage());
        }
    }
    
    /**
     * Security testing demonstration
     */
    private static void runSecurityDemo() {
        logger.info("\n🔒 Running Security Testing Demo");
        logger.info("================================");
        
        try {
            // Create security-focused configuration
            TestConfiguration config = TestConfiguration.securityConfiguration();
            
            // Initialize framework
            AmcpTestingFramework framework = new AmcpTestingFramework(config);
            
            logger.info("🛡️ Starting security validation tests...");
            
            // Create event broker for testing
            EventBroker eventBroker = new InMemoryEventBroker();
            eventBroker.start();
            
            // Run security tests
            TestResult result = framework.runSecurityValidation(eventBroker);
            
            // Cleanup
            eventBroker.stop();
            
            // Display results with security metrics
            displayResults("Security Validation", result);
            displaySecurityMetrics(framework.getMetricsCollector());
            
            // Cleanup
            framework.shutdown();
            
        } catch (Exception e) {
            logger.severe("Security demo failed: " + e.getMessage());
        }
    }
    
    /**
     * Chaos engineering demonstration
     */
    private static void runChaosDemo() {
        logger.info("\n🌪️ Running Chaos Engineering Demo");
        logger.info("=================================");
        
        try {
            // Create chaos-focused configuration
            TestConfiguration config = TestConfiguration.chaosConfiguration();
            
            // Initialize framework
            AmcpTestingFramework framework = new AmcpTestingFramework(config);
            
            logger.info("⚡ Starting chaos engineering tests...");
            logger.info("   Warning: This may cause temporary system instability");
            
            // Create event broker for testing
            EventBroker eventBroker = new InMemoryEventBroker();
            eventBroker.start();
            
            // Run chaos tests
            TestResult result = framework.runChaosTests(eventBroker);
            
            // Cleanup
            eventBroker.stop();
            
            // Display results with chaos metrics
            displayResults("Chaos Engineering", result);
            displayChaosMetrics(framework.getMetricsCollector());
            
            // Cleanup
            framework.shutdown();
            
        } catch (Exception e) {
            logger.severe("Chaos demo failed: " + e.getMessage());
        }
    }
    
    /**
     * Comprehensive testing demonstration
     */
    private static void runComprehensiveDemo() {
        logger.info("\n🎯 Running Comprehensive Testing Demo");
        logger.info("====================================");
        
        try {
            // Create comprehensive configuration
            TestConfiguration config = TestConfiguration.comprehensiveConfiguration();
            
            // Initialize framework
            AmcpTestingFramework framework = new AmcpTestingFramework(config);
            
            logger.info("🔍 Starting comprehensive test suite...");
            logger.info("   This includes all test categories and may take several minutes");
            
            // Create event broker for testing
            EventBroker eventBroker = new InMemoryEventBroker();
            eventBroker.start();
            
            // Run all tests
            TestSuiteResult result = framework.runComprehensiveTestSuite(eventBroker);
            
            // Cleanup
            eventBroker.stop();
            
            // Display comprehensive results
            displaySuiteResults("Comprehensive Test Suite", result);
            displayComprehensiveMetrics(framework.getMetricsCollector());
            
            // Generate detailed report
            logger.info("\n📊 Generating detailed test report...");
            String detailedReport = result.getDetailedReport();
            logger.info("Full report:\n" + detailedReport);
            
            // Cleanup
            framework.shutdown();
            
        } catch (Exception e) {
            logger.severe("Comprehensive demo failed: " + e.getMessage());
        }
    }
    
    /**
     * Display test results in a formatted way
     */
    private static void displayResults(String testName, TestResult result) {
        logger.info("📋 " + testName + " Results:");
        logger.info("   Status: " + (result.isPassed() ? "✅ PASSED" : "❌ FAILED"));
        logger.info("   Duration: " + result.getDuration().toMillis() + "ms");
        logger.info("   Checks: " + result.getSuccessfulChecks() + "/" + result.getTotalChecks() + " passed");
        logger.info("   Success Rate: " + String.format("%.1f%%", result.getSuccessRate() * 100));
        
        if (!result.getErrors().isEmpty()) {
            logger.info("   Errors: " + result.getErrors().size());
            result.getErrors().forEach(error -> logger.warning("     - " + error));
        }
        
        // Show summary of individual checks
        result.getChecks().forEach((check, passed) -> {
            String status = passed ? "✅" : "❌";
            logger.info("     " + status + " " + check);
        });
        logger.info("");
    }
    
    /**
     * Display test suite results in a formatted way
     */
    private static void displaySuiteResults(String testName, TestSuiteResult result) {
        logger.info("📋 " + testName + " Results:");
        logger.info("   Status: " + (result.isSuccessful() ? "✅ PASSED" : "❌ FAILED"));
        logger.info("   Duration: " + result.getTotalDuration().toMillis() + "ms");
        logger.info("   Tests: " + result.getPassedTests() + "/" + result.getTotalTests() + " passed");
        logger.info("   Success Rate: " + String.format("%.1f%%", result.getSuccessRate()));
        
        if (!result.getErrors().isEmpty()) {
            logger.info("   Errors: " + result.getErrors().size());
            result.getErrors().forEach(error -> logger.warning("     - " + error));
        }
        
        // Show summary of individual test results
        result.getTestResults().forEach(testResult -> {
            String status = testResult.isPassed() ? "✅" : "❌";
            logger.info("     " + status + " " + testResult.getTestName());
        });
        logger.info("");
    }
    
    /**
     * Display performance metrics
{{ ... }}
     */
    private static void displayPerformanceMetrics(TestMetricsCollector metrics) {
        logger.info("📊 Performance Metrics:");
        
        // Display throughput metrics
        logger.info("   Throughput Metrics:");
        logger.info("     - Average: " + metrics.getAverageThroughput() + " events/sec");
        logger.info("     - Peak: " + metrics.getPeakThroughput() + " events/sec");
        
        // Display latency metrics
        logger.info("   Latency Metrics:");
        logger.info("     - P50: " + metrics.getP50Latency() + "ms");
        logger.info("     - P95: " + metrics.getP95Latency() + "ms");
        logger.info("     - P99: " + metrics.getP99Latency() + "ms");
        
        // Display resource metrics
        logger.info("   Resource Utilization:");
        logger.info("     - Peak Memory: " + metrics.getPeakMemoryUsage() + "MB");
        logger.info("     - Avg CPU: " + String.format("%.1f%%", metrics.getAverageCpuUsage()));
    }
    
    /**
     * Display security metrics
     */
    private static void displaySecurityMetrics(TestMetricsCollector metrics) {
        logger.info("🔒 Security Metrics:");
        
        // Authentication metrics
        logger.info("   Authentication:");
        logger.info("     - Successful attempts: " + metrics.getSecurityMetric("auth_successful"));
        logger.info("     - Failed attempts: " + metrics.getSecurityMetric("auth_failed"));
        logger.info("     - Blocked attempts: " + metrics.getSecurityMetric("auth_blocked"));
        
        // Authorization metrics
        logger.info("   Authorization:");
        logger.info("     - Access granted: " + metrics.getSecurityMetric("authz_granted"));
        logger.info("     - Access denied: " + metrics.getSecurityMetric("authz_denied"));
        
        // Security violations
        logger.info("   Security Violations:");
        logger.info("     - Injection attempts: " + metrics.getSecurityMetric("injection_attempts"));
        logger.info("     - Policy violations: " + metrics.getSecurityMetric("policy_violations"));
    }
    
    /**
     * Display chaos engineering metrics
     */
    private static void displayChaosMetrics(TestMetricsCollector metrics) {
        logger.info("🌪️ Chaos Engineering Metrics:");
        
        // Network chaos metrics
        logger.info("   Network Resilience:");
        logger.info("     - Partition recovery time: " + metrics.getChaosMetric("partition_recovery_time") + "ms");
        logger.info("     - Delay tolerance: " + metrics.getChaosMetric("delay_tolerance") + "ms");
        
        // Service chaos metrics
        logger.info("   Service Resilience:");
        logger.info("     - Failure recovery time: " + metrics.getChaosMetric("failure_recovery_time") + "ms");
        logger.info("     - Degradation handling: " + metrics.getChaosMetric("degradation_score") + "/10");
        
        // System stability
        logger.info("   System Stability:");
        logger.info("     - Availability during chaos: " + String.format("%.2f%%", metrics.getChaosMetric("availability_score")));
        logger.info("     - Data consistency score: " + String.format("%.2f%%", metrics.getChaosMetric("consistency_score")));
    }
    
    /**
     * Display comprehensive metrics summary
     */
    private static void displayComprehensiveMetrics(TestMetricsCollector metrics) {
        logger.info("🎯 Comprehensive Test Metrics Summary:");
        
        // Overall system health
        logger.info("   System Health Score: " + String.format("%.1f/10", calculateHealthScore(metrics)));
        
        // Test coverage
        logger.info("   Test Coverage:");
        logger.info("     - Infrastructure: " + (metrics.getInfrastructureTestCount() > 0 ? "✅" : "❌"));
        logger.info("     - Performance: " + (metrics.getPerformanceTestCount() > 0 ? "✅" : "❌"));
        logger.info("     - Security: " + (metrics.getSecurityTestCount() > 0 ? "✅" : "❌"));
        logger.info("     - Chaos: " + (metrics.getChaosTestCount() > 0 ? "✅" : "❌"));
        
        // Quality gates
        logger.info("   Quality Gates:");
        boolean performanceGate = metrics.getAverageThroughput() > 1000;
        boolean securityGate = metrics.getSecurityViolations() == 0;
        boolean reliabilityGate = metrics.getChaosMetric("availability_score") > 95.0;
        
        logger.info("     - Performance Gate: " + (performanceGate ? "✅ PASSED" : "❌ FAILED"));
        logger.info("     - Security Gate: " + (securityGate ? "✅ PASSED" : "❌ FAILED"));
        logger.info("     - Reliability Gate: " + (reliabilityGate ? "✅ PASSED" : "❌ FAILED"));
        
        // Recommendations
        logger.info("   Recommendations:");
        if (!performanceGate) {
            logger.info("     📈 Consider performance tuning and optimization");
        }
        if (!securityGate) {
            logger.info("     🔒 Address security vulnerabilities before production");
        }
        if (!reliabilityGate) {
            logger.info("     🛠️ Improve system resilience and fault tolerance");
        }
        if (performanceGate && securityGate && reliabilityGate) {
            logger.info("     ✅ System meets all quality gates - ready for production!");
        }
    }
    
    /**
     * Calculate overall system health score based on metrics
     */
    private static double calculateHealthScore(TestMetricsCollector metrics) {
        double performanceScore = Math.min(10.0, metrics.getAverageThroughput() / 1000.0);
        double securityScore = metrics.getSecurityViolations() == 0 ? 10.0 : Math.max(0.0, 10.0 - metrics.getSecurityViolations());
        double reliabilityScore = metrics.getChaosMetric("availability_score") / 10.0;
        
        return (performanceScore + securityScore + reliabilityScore) / 3.0;
    }
    
    /**
     * Custom demo with specific configuration
     */
    public static void runCustomDemo() {
        logger.info("\n🎨 Running Custom Configuration Demo");
        logger.info("===================================");
        
        try {
            // Create custom configuration
            TestConfiguration config = TestConfiguration.builder()
                .enableTestContainers(true)
                .enablePerformanceTests(true)
                .enableSecurityTests(true)
                .enableChaosTests(false) // Skip chaos for faster demo
                .maxConcurrentThreads(5)
                .performanceTestDuration(30) // Shorter duration
                .testTimeout(120)
                .enableContainer("kafka")
                .enableContainer("postgresql")
                .kafkaSetting("bootstrap.servers", "localhost:9092")
                .databaseSetting("url", "jdbc:postgresql://localhost:5432/amcp_test")
                .generateReports(true)
                .reportOutputPath("./demo-reports")
                .build();
            
            // Initialize framework with custom config
            AmcpTestingFramework framework = new AmcpTestingFramework(config);
            
            logger.info("🔧 Starting custom test configuration...");
            
            // Create event broker for testing
            EventBroker eventBroker = new InMemoryEventBroker();
            eventBroker.start();
            
            // Run selected tests
            TestResult infraResult = framework.runInfrastructureTests();
            TestResult perfResult = framework.runPerformanceBenchmarks(eventBroker);
            TestResult secResult = framework.runSecurityValidation(eventBroker);
            
            // Cleanup
            eventBroker.stop();
            
            // Display individual results
            displayResults("Infrastructure Tests", infraResult);
            displayResults("Performance Tests", perfResult);
            displayResults("Security Tests", secResult);
            
            // Cleanup
            framework.shutdown();
            
            logger.info("✅ Custom demo completed successfully!");
            
        } catch (Exception e) {
            logger.severe("Custom demo failed: " + e.getMessage());
        }
    }
    
    /**
     * Interactive demo mode
     */
    public static void runInteractiveDemo() {
        logger.info("\n🎮 Interactive Testing Framework Demo");
        logger.info("====================================");
        
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        
        try {
            logger.info("Select test configuration:");
            logger.info("1. Minimal (fastest)");
            logger.info("2. Performance-focused");
            logger.info("3. Security-focused");
            logger.info("4. Chaos engineering");
            logger.info("5. Comprehensive (slowest)");
            logger.info("Enter choice (1-5): ");
            
            int choice = scanner.nextInt();
            TestConfiguration config;
            String testName;
            
            switch (choice) {
                case 1:
                    config = TestConfiguration.minimalConfiguration();
                    testName = "Minimal";
                    break;
                case 2:
                    config = TestConfiguration.performanceConfiguration();
                    testName = "Performance";
                    break;
                case 3:
                    config = TestConfiguration.securityConfiguration();
                    testName = "Security";
                    break;
                case 4:
                    config = TestConfiguration.chaosConfiguration();
                    testName = "Chaos Engineering";
                    break;
                case 5:
                    config = TestConfiguration.comprehensiveConfiguration();
                    testName = "Comprehensive";
                    break;
                default:
                    logger.warning("Invalid choice, using minimal configuration");
                    config = TestConfiguration.minimalConfiguration();
                    testName = "Minimal";
                    break;
            }
            
            logger.info("🚀 Starting " + testName + " test configuration...");
            
            // Run selected configuration
            AmcpTestingFramework framework = new AmcpTestingFramework(config);
            
            // Create event broker for testing
            EventBroker eventBroker = new InMemoryEventBroker();
            eventBroker.start();
            
            TestSuiteResult result = framework.runComprehensiveTestSuite(eventBroker);
            
            // Cleanup
            eventBroker.stop();
            
            // Display results
            displaySuiteResults(testName + " Test Suite", result);
            displayComprehensiveMetrics(framework.getMetricsCollector());
            
            // Cleanup
            framework.shutdown();
            
        } catch (Exception e) {
            logger.severe("Interactive demo failed: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }
}