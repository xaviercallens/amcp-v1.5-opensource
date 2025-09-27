package io.amcp.testing;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.time.Instant;

/**
 * Test Metrics Collector for AMCP Testing Framework
 * 
 * Collects and aggregates test execution metrics including:
 * - Test execution counts and success rates
 * - Performance metrics (latency, throughput)
 * - Resource utilization metrics
 * - Error tracking and categorization
 * - Historical trend data
 * 
 * Thread-safe implementation for concurrent test execution.
 */
public class TestMetricsCollector {
    
    private final AtomicLong totalTestsExecuted = new AtomicLong(0);
    private final AtomicLong totalTestsPassed = new AtomicLong(0);
    private final AtomicLong totalTestsFailed = new AtomicLong(0);
    private final AtomicLong totalExecutionTime = new AtomicLong(0); // milliseconds
    
    private final Map<String, TestCategoryMetrics> categoryMetrics = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> performanceMetrics = new ConcurrentHashMap<>(); 
    private final List<TestExecutionRecord> executionHistory = new ArrayList<>();
    
    private final Instant startTime = Instant.now();
    
    /**
     * Record test execution result
     */
    public void recordTestExecution(String testName, boolean success, long executionTimeMs) {
        totalTestsExecuted.incrementAndGet();
        if (success) {
            totalTestsPassed.incrementAndGet();
        } else {
            totalTestsFailed.incrementAndGet();
        }
        totalExecutionTime.addAndGet(executionTimeMs);
        
        // Record in history
        TestExecutionRecord record = new TestExecutionRecord(
            testName, success, executionTimeMs, Instant.now());
        synchronized (executionHistory) {
            executionHistory.add(record);
            // Keep only last 1000 records
            if (executionHistory.size() > 1000) {
                executionHistory.remove(0);
            }
        }
        
        // Update category metrics
        String category = extractCategory(testName);
        categoryMetrics.computeIfAbsent(category, k -> new TestCategoryMetrics(k))
                     .recordExecution(success, executionTimeMs);
    }
    
    /**
     * Record performance metric
     */
    public void recordPerformanceMetric(String metricName, long value) {
        performanceMetrics.computeIfAbsent(metricName, k -> new AtomicLong(0))
                          .addAndGet(value);
    }
    
    /**
     * Increment performance counter
     */
    public void incrementCounter(String counterName) {
        recordPerformanceMetric(counterName, 1);
    }
    
    /**
     * Record latency measurement
     */
    public void recordLatency(String operation, long latencyMs) {
        recordPerformanceMetric(operation + "_latency", latencyMs);
        recordPerformanceMetric(operation + "_count", 1);
    }
    
    /**
     * Record throughput measurement
     */
    public void recordThroughput(String operation, long throughput) {
        recordPerformanceMetric(operation + "_throughput", throughput);
    }
    
    /**
     * Get current test metrics
     */
    public TestMetrics getMetrics() {
        return TestMetrics.builder()
            .totalTests(totalTestsExecuted.get())
            .passedTests(totalTestsPassed.get())
            .failedTests(totalTestsFailed.get())
            .totalExecutionTime(Duration.ofMillis(totalExecutionTime.get()))
            .categoryMetrics(new ConcurrentHashMap<>(categoryMetrics))
            .performanceMetrics(new ConcurrentHashMap<>(performanceMetrics))
            .uptime(Duration.between(startTime, Instant.now()))
            .build();
    }
    
    /**
     * Get success rate percentage
     */
    public double getSuccessRate() {
        long total = totalTestsExecuted.get();
        if (total == 0) return 100.0;
        return (double) totalTestsPassed.get() / total * 100.0;
    }
    
    /**
     * Get average execution time
     */
    public double getAverageExecutionTime() {
        long total = totalTestsExecuted.get();
        if (total == 0) return 0.0;
        return (double) totalExecutionTime.get() / total;
    }
    
    /**
     * Get test execution history
     */
    public List<TestExecutionRecord> getExecutionHistory() {
        synchronized (executionHistory) {
            return new ArrayList<>(executionHistory);
        }
    }
    
    /**
     * Get metrics for specific category
     */
    public TestCategoryMetrics getCategoryMetrics(String category) {
        return categoryMetrics.get(category);
    }
    
    /**
     * Reset all metrics
     */
    public void reset() {
        totalTestsExecuted.set(0);
        totalTestsPassed.set(0);
        totalTestsFailed.set(0);
        totalExecutionTime.set(0);
        categoryMetrics.clear();
        performanceMetrics.clear();
        synchronized (executionHistory) {
            executionHistory.clear();
        }
    }
    
    /**
     * Generate metrics summary report
     */
    public String generateSummaryReport() {
        TestMetrics metrics = getMetrics();
        StringBuilder report = new StringBuilder();
        
        report.append("\n=============== Test Metrics Summary ===============\n");
        report.append(String.format("Total Tests: %d\n", metrics.getTotalTests()));
        report.append(String.format("Passed: %d (%.1f%%)\n", 
            metrics.getPassedTests(), 
            metrics.getTotalTests() > 0 ? (double) metrics.getPassedTests() / metrics.getTotalTests() * 100.0 : 0.0));
        report.append(String.format("Failed: %d (%.1f%%)\n", 
            metrics.getFailedTests(),
            metrics.getTotalTests() > 0 ? (double) metrics.getFailedTests() / metrics.getTotalTests() * 100.0 : 0.0));
        report.append(String.format("Total Execution Time: %ds\n", metrics.getTotalExecutionTime().getSeconds()));
        report.append(String.format("Average Execution Time: %.2fms\n", getAverageExecutionTime()));
        report.append(String.format("Uptime: %ds\n", metrics.getUptime().getSeconds()));
        
        report.append("\nCategory Breakdown:\n");
        metrics.getCategoryMetrics().forEach((category, catMetrics) -> {
            report.append(String.format("  %s: %d tests, %.1f%% success rate\n",
                category, catMetrics.getTotalTests(), catMetrics.getSuccessRate()));
        });
        
        report.append("\nPerformance Metrics:\n");
        metrics.getPerformanceMetrics().forEach((name, value) -> {
            report.append(String.format("  %s: %d\n", name, value.get()));
        });
        
        report.append("=============================================\n");
        
        return report.toString();
    }
    
    private String extractCategory(String testName) {
        if (testName.contains("_")) {
            return testName.split("_")[0];
        }
        return "general";
    }
    
    /**
     * Additional methods for demo and comprehensive reporting
     */
    
    public void recordSecurityMetric(String metricName, long value) {
        recordPerformanceMetric("security_" + metricName, value);
    }
    
    public void recordChaosMetric(String metricName, long value) {
        recordPerformanceMetric("chaos_" + metricName, value);
    }
    
    public long getSecurityMetric(String metricName) {
        AtomicLong metric = performanceMetrics.get("security_" + metricName);
        return metric != null ? metric.get() : 0L;
    }
    
    public long getChaosMetric(String metricName) {
        AtomicLong metric = performanceMetrics.get("chaos_" + metricName);
        return metric != null ? metric.get() : 0L;
    }
    
    public long getAverageThroughput() {
        return performanceMetrics.entrySet().stream()
            .filter(entry -> entry.getKey().contains("throughput"))
            .mapToLong(entry -> entry.getValue().get())
            .sum() / Math.max(1, performanceMetrics.entrySet().stream()
                .filter(entry -> entry.getKey().contains("throughput"))
                .mapToInt(entry -> 1).sum());
    }
    
    public long getPeakThroughput() {
        return performanceMetrics.entrySet().stream()
            .filter(entry -> entry.getKey().contains("throughput"))
            .mapToLong(entry -> entry.getValue().get())
            .max().orElse(0L);
    }
    
    public long getP50Latency() {
        return performanceMetrics.entrySet().stream()
            .filter(entry -> entry.getKey().contains("p50_latency"))
            .mapToLong(entry -> entry.getValue().get())
            .findFirst().orElse(0L);
    }
    
    public long getP95Latency() {
        return performanceMetrics.entrySet().stream()
            .filter(entry -> entry.getKey().contains("p95_latency"))
            .mapToLong(entry -> entry.getValue().get())
            .findFirst().orElse(0L);
    }
    
    public long getP99Latency() {
        return performanceMetrics.entrySet().stream()
            .filter(entry -> entry.getKey().contains("p99_latency"))
            .mapToLong(entry -> entry.getValue().get())
            .findFirst().orElse(0L);
    }
    
    public long getSecurityViolations() {
        return performanceMetrics.entrySet().stream()
            .filter(entry -> entry.getKey().contains("security_") && entry.getKey().contains("violation"))
            .mapToLong(entry -> entry.getValue().get())
            .sum();
    }
    
    public int getInfrastructureTestCount() {
        TestCategoryMetrics infra = categoryMetrics.get("infrastructure");
        return infra != null ? (int) infra.getTotalTests() : 0;
    }
    
    public int getPerformanceTestCount() {
        TestCategoryMetrics perf = categoryMetrics.get("performance");
        return perf != null ? (int) perf.getTotalTests() : 0;
    }
    
    public int getSecurityTestCount() {
        TestCategoryMetrics sec = categoryMetrics.get("security");
        return sec != null ? (int) sec.getTotalTests() : 0;
    }
    
    public int getChaosTestCount() {
        TestCategoryMetrics chaos = categoryMetrics.get("chaos");
        return chaos != null ? (int) chaos.getTotalTests() : 0;
    }
    
    public long getPeakMemoryUsage() {
        return performanceMetrics.entrySet().stream()
            .filter(entry -> entry.getKey().contains("memory") && entry.getKey().contains("mb"))
            .mapToLong(entry -> entry.getValue().get())
            .max().orElse(0L);
    }
    
    public double getAverageCpuUsage() {
        return performanceMetrics.entrySet().stream()
            .filter(entry -> entry.getKey().contains("cpu") && entry.getKey().contains("usage"))
            .mapToLong(entry -> entry.getValue().get())
            .average().orElse(0.0);
    }
    
    /**
     * Test category metrics for grouping related tests
     */
    public static class TestCategoryMetrics {
        private final String category;
        private final AtomicLong totalTests = new AtomicLong(0);
        private final AtomicLong passedTests = new AtomicLong(0);
        private final AtomicLong failedTests = new AtomicLong(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        
        public TestCategoryMetrics(String category) {
            this.category = category;
        }
        
        public void recordExecution(boolean success, long executionTimeMs) {
            totalTests.incrementAndGet();
            if (success) {
                passedTests.incrementAndGet();
            } else {
                failedTests.incrementAndGet();
            }
            totalExecutionTime.addAndGet(executionTimeMs);
        }
        
        public String getCategory() { return category; }
        public long getTotalTests() { return totalTests.get(); }
        public long getPassedTests() { return passedTests.get(); }
        public long getFailedTests() { return failedTests.get(); }
        public long getTotalExecutionTime() { return totalExecutionTime.get(); }
        
        public double getSuccessRate() {
            long total = totalTests.get();
            return total > 0 ? (double) passedTests.get() / total * 100.0 : 100.0;
        }
        
        public double getAverageExecutionTime() {
            long total = totalTests.get();
            return total > 0 ? (double) totalExecutionTime.get() / total : 0.0;
        }
    }
    
    /**
     * Individual test execution record
     */
    public static class TestExecutionRecord {
        private final String testName;
        private final boolean success;
        private final long executionTimeMs;
        private final Instant timestamp;
        
        public TestExecutionRecord(String testName, boolean success, 
                                 long executionTimeMs, Instant timestamp) {
            this.testName = testName;
            this.success = success;
            this.executionTimeMs = executionTimeMs;
            this.timestamp = timestamp;
        }
        
        public String getTestName() { return testName; }
        public boolean isSuccess() { return success; }
        public long getExecutionTimeMs() { return executionTimeMs; }
        public Instant getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("%s: %s (%dms) at %s", 
                testName, success ? "PASS" : "FAIL", executionTimeMs, timestamp);
        }
    }
}