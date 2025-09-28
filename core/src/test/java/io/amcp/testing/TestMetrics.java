package io.amcp.testing;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive test metrics for AMCP v1.5 Enterprise Edition testing framework.
 * 
 * This class encapsulates all test execution metrics including performance data,
 * success/failure rates, execution times, and category-specific metrics.
 */
public class TestMetrics {
    private final long totalTests;
    private final long passedTests;
    private final long failedTests;
    private final Duration totalExecutionTime;
    private final Map<String, Long> categoryMetrics;
    private final Map<String, Double> performanceMetrics;
    private final Duration uptime;

    private TestMetrics(Builder builder) {
        this.totalTests = builder.totalTests;
        this.passedTests = builder.passedTests;
        this.failedTests = builder.failedTests;
        this.totalExecutionTime = builder.totalExecutionTime;
        this.categoryMetrics = new ConcurrentHashMap<>(builder.categoryMetrics);
        this.performanceMetrics = new ConcurrentHashMap<>(builder.performanceMetrics);
        this.uptime = builder.uptime;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public long getTotalTests() {
        return totalTests;
    }

    public long getPassedTests() {
        return passedTests;
    }

    public long getFailedTests() {
        return failedTests;
    }

    public Duration getTotalExecutionTime() {
        return totalExecutionTime;
    }

    public Map<String, Long> getCategoryMetrics() {
        return new ConcurrentHashMap<>(categoryMetrics);
    }

    public Map<String, Double> getPerformanceMetrics() {
        return new ConcurrentHashMap<>(performanceMetrics);
    }

    public Duration getUptime() {
        return uptime;
    }

    public double getSuccessRate() {
        if (totalTests == 0) return 0.0;
        return (double) passedTests / totalTests * 100.0;
    }

    public double getFailureRate() {
        if (totalTests == 0) return 0.0;
        return (double) failedTests / totalTests * 100.0;
    }

    public long getAverageExecutionTimeMs() {
        if (totalTests == 0) return 0;
        return totalExecutionTime.toMillis() / totalTests;
    }

    @Override
    public String toString() {
        return String.format(
            "TestMetrics{totalTests=%d, passed=%d, failed=%d, successRate=%.2f%%, " +
            "totalTime=%s, avgTime=%dms, uptime=%s}",
            totalTests, passedTests, failedTests, getSuccessRate(),
            formatDuration(totalExecutionTime), getAverageExecutionTimeMs(),
            formatDuration(uptime)
        );
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long absSeconds = Math.abs(seconds);
        String positive = String.format(
            "%d:%02d:%02d",
            absSeconds / 3600,
            (absSeconds % 3600) / 60,
            absSeconds % 60
        );
        return seconds < 0 ? "-" + positive : positive;
    }

    /**
     * Builder pattern for TestMetrics
     */
    public static class Builder {
        private long totalTests;
        private long passedTests;
        private long failedTests;
        private Duration totalExecutionTime = Duration.ZERO;
        private Map<String, Long> categoryMetrics = new ConcurrentHashMap<>();
        private Map<String, Double> performanceMetrics = new ConcurrentHashMap<>();
        private Duration uptime = Duration.ZERO;

        public Builder totalTests(long totalTests) {
            this.totalTests = totalTests;
            return this;
        }

        public Builder passedTests(long passedTests) {
            this.passedTests = passedTests;
            return this;
        }

        public Builder failedTests(long failedTests) {
            this.failedTests = failedTests;
            return this;
        }

        public Builder totalExecutionTime(Duration totalExecutionTime) {
            this.totalExecutionTime = totalExecutionTime;
            return this;
        }

        public Builder categoryMetrics(Map<String, Long> categoryMetrics) {
            this.categoryMetrics = new ConcurrentHashMap<>(categoryMetrics);
            return this;
        }

        public Builder performanceMetrics(Map<String, Double> performanceMetrics) {
            this.performanceMetrics = new ConcurrentHashMap<>(performanceMetrics);
            return this;
        }

        public Builder uptime(Duration uptime) {
            this.uptime = uptime;
            return this;
        }

        public TestMetrics build() {
            return new TestMetrics(this);
        }
    }
}