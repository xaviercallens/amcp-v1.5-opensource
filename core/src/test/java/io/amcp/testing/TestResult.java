package io.amcp.testing;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test Result representation for AMCP Testing Framework
 * 
 * Encapsulates test execution results including:
 * - Test status and outcome
 * - Individual check results
 * - Performance metrics
 * - Error information
 * - Execution timestamps
 * - Duration tracking
 */
public class TestResult {
    
    private final String testName;
    private final TestStatus status;
    private final Instant startTime;
    private final Instant endTime;
    private final Duration duration;
    private final Map<String, Boolean> checks;
    private final List<String> errors;
    private final Map<String, Object> metadata;
    private final String errorMessage;
    
    private TestResult(Builder builder) {
        this.testName = builder.testName;
        this.status = builder.status;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime != null ? builder.endTime : Instant.now();
        this.duration = Duration.between(this.startTime, this.endTime);
        this.checks = new HashMap<>(builder.checks);
        this.errors = new ArrayList<>(builder.errors);
        this.metadata = new HashMap<>(builder.metadata);
        this.errorMessage = builder.errorMessage;
    }
    
    // Getters
    public String getTestName() { return testName; }
    public TestStatus getStatus() { return status; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public Duration getDuration() { return duration; }
    public Map<String, Boolean> getChecks() { return new HashMap<>(checks); }
    public List<String> getErrors() { return new ArrayList<>(errors); }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
    public String getErrorMessage() { return errorMessage; }
    
    /**
     * Check if test passed (all checks successful)
     */
    public boolean isPassed() {
        return status == TestStatus.PASSED && 
               checks.values().stream().allMatch(Boolean::booleanValue);
    }
    
    /**
     * Check if test failed
     */
    public boolean isFailed() {
        return status == TestStatus.FAILED || 
               checks.values().stream().anyMatch(check -> !check);
    }
    
    /**
     * Get success rate of individual checks
     */
    public double getSuccessRate() {
        if (checks.isEmpty()) return 0.0;
        long successful = checks.values().stream().mapToLong(check -> check ? 1 : 0).sum();
        return (double) successful / checks.size();
    }
    
    /**
     * Get number of successful checks
     */
    public int getSuccessfulChecks() {
        return (int) checks.values().stream().mapToLong(check -> check ? 1 : 0).sum();
    }
    
    /**
     * Get number of failed checks
     */
    public int getFailedChecks() {
        return (int) checks.values().stream().mapToLong(check -> check ? 0 : 1).sum();
    }
    
    /**
     * Get total number of checks
     */
    public int getTotalChecks() {
        return checks.size();
    }
    
    /**
     * Create a summary report
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Test: ").append(testName).append("\n");
        summary.append("Status: ").append(status).append("\n");
        summary.append("Duration: ").append(duration.toMillis()).append("ms\n");
        summary.append("Checks: ").append(getSuccessfulChecks()).append("/").append(getTotalChecks()).append(" passed\n");
        summary.append("Success Rate: ").append(String.format("%.1f%%", getSuccessRate() * 100)).append("\n");
        
        if (!errors.isEmpty()) {
            summary.append("Errors: ").append(errors.size()).append("\n");
        }
        
        if (errorMessage != null) {
            summary.append("Error: ").append(errorMessage).append("\n");
        }
        
        return summary.toString();
    }
    
    /**
     * Create detailed report
     */
    public String getDetailedReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== TEST RESULT REPORT ===\n");
        report.append("Test Name: ").append(testName).append("\n");
        report.append("Status: ").append(status).append("\n");
        report.append("Start Time: ").append(startTime).append("\n");
        report.append("End Time: ").append(endTime).append("\n");
        report.append("Duration: ").append(duration.toMillis()).append("ms\n");
        report.append("Success Rate: ").append(String.format("%.1f%%", getSuccessRate() * 100)).append("\n");
        report.append("\n");
        
        if (!checks.isEmpty()) {
            report.append("=== CHECK RESULTS ===\n");
            checks.forEach((name, passed) -> {
                report.append(passed ? "✅ " : "❌ ").append(name).append("\n");
            });
            report.append("\n");
        }
        
        if (!metadata.isEmpty()) {
            report.append("=== METADATA ===\n");
            metadata.forEach((key, value) -> {
                report.append(key).append(": ").append(value).append("\n");
            });
            report.append("\n");
        }
        
        if (!errors.isEmpty()) {
            report.append("=== ERRORS ===\n");
            for (int i = 0; i < errors.size(); i++) {
                report.append((i + 1)).append(". ").append(errors.get(i)).append("\n");
            }
            report.append("\n");
        }
        
        if (errorMessage != null) {
            report.append("=== ERROR MESSAGE ===\n");
            report.append(errorMessage).append("\n");
        }
        
        return report.toString();
    }
    
    @Override
    public String toString() {
        return String.format("TestResult{name='%s', status=%s, duration=%dms, checks=%d/%d passed}", 
            testName, status, duration.toMillis(), getSuccessfulChecks(), getTotalChecks());
    }
    
    /**
     * Test status enumeration
     */
    public enum TestStatus {
        PASSED,
        FAILED,
        SKIPPED,
        ERROR
    }
    
    /**
     * Builder for TestResult
     */
    public static class Builder {
        private final String testName;
        private final Instant startTime;
        private TestStatus status = TestStatus.PASSED;
        private Instant endTime;
        private final Map<String, Boolean> checks = new ConcurrentHashMap<>();
        private final List<String> errors = new ArrayList<>();
        private final Map<String, Object> metadata = new ConcurrentHashMap<>();
        private String errorMessage;
        
        public Builder(String testName) {
            this.testName = testName;
            this.startTime = Instant.now();
        }
        
        public Builder status(TestStatus status) {
            this.status = status;
            return this;
        }
        
        public Builder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }
        
        public Builder addCheck(String name, boolean passed) {
            this.checks.put(name, passed);
            if (!passed && this.status == TestStatus.PASSED) {
                this.status = TestStatus.FAILED;
            }
            return this;
        }
        
        public Builder addError(String error) {
            this.errors.add(error);
            if (this.status == TestStatus.PASSED) {
                this.status = TestStatus.ERROR;
            }
            return this;
        }
        
        public Builder error(String errorMessage) {
            this.errorMessage = errorMessage;
            this.status = TestStatus.ERROR;
            return this;
        }
        
        public Builder metadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public Builder skip() {
            this.status = TestStatus.SKIPPED;
            return this;
        }
        
        public Builder success() {
            this.status = TestStatus.PASSED;
            return this;
        }
        
        public TestResult build() {
            return new TestResult(this);
        }
    }
    
    /**
     * Create a new builder
     */
    public static Builder builder(String testName) {
        return new Builder(testName);
    }
    
    /**
     * Create a successful test result
     */
    public static TestResult success(String testName) {
        return builder(testName).status(TestStatus.PASSED).build();
    }
    
    /**
     * Create a failed test result
     */
    public static TestResult failure(String testName, String errorMessage) {
        return builder(testName).error(errorMessage).build();
    }
    
    /**
     * Create a skipped test result
     */
    public static TestResult skipped(String testName) {
        return builder(testName).skip().build();
    }
    
    /**
     * Combine multiple test results into one
     */
    public static TestResult combine(String testName, List<TestResult> results) {
        Builder combined = builder(testName);
        
        Instant earliestStart = results.stream()
            .map(TestResult::getStartTime)
            .min(Instant::compareTo)
            .orElse(Instant.now());
        
        Instant latestEnd = results.stream()
            .map(TestResult::getEndTime)
            .max(Instant::compareTo)
            .orElse(Instant.now());
        
        // Determine overall status
        TestStatus overallStatus = TestStatus.PASSED;
        if (results.stream().anyMatch(r -> r.getStatus() == TestStatus.ERROR)) {
            overallStatus = TestStatus.ERROR;
        } else if (results.stream().anyMatch(r -> r.getStatus() == TestStatus.FAILED)) {
            overallStatus = TestStatus.FAILED;
        } else if (results.stream().allMatch(r -> r.getStatus() == TestStatus.SKIPPED)) {
            overallStatus = TestStatus.SKIPPED;
        }
        
        combined.status(overallStatus);
        
        // Combine all checks
        results.forEach(result -> {
            result.getChecks().forEach(combined::addCheck);
            result.getErrors().forEach(combined::addError);
            result.getMetadata().forEach(combined::metadata);
        });
        
        // Add summary metadata
        combined.metadata("combined_results_count", results.size());
        combined.metadata("earliest_start", earliestStart.toString());
        combined.metadata("latest_end", latestEnd.toString());
        combined.metadata("total_duration_ms", Duration.between(earliestStart, latestEnd).toMillis());
        
        return combined.build();
    }
}