package io.amcp.testing;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test Suite Result container for AMCP Testing Framework
 * 
 * Aggregates results from multiple test executions including:
 * - Test execution summary
 * - Performance metrics
 * - Security validation results
 * - Chaos testing outcomes
 */
public class TestSuiteResult {
    
    private final String suiteName;
    private final Instant startTime;
    private final Instant endTime;
    private final Duration totalDuration;
    
    private final int totalTests;
    private final int passedTests;
    private final int failedTests;
    private final int skippedTests;
    
    private final List<TestResult> testResults;
    private final Map<String, Object> metrics;
    private final List<String> errors;
    private final boolean successful;
    
    private TestSuiteResult(Builder builder) {
        this.suiteName = builder.suiteName;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.totalDuration = Duration.between(startTime, endTime);
        
        this.totalTests = builder.totalTests;
        this.passedTests = builder.passedTests;
        this.failedTests = builder.failedTests;
        this.skippedTests = builder.skippedTests;
        
        this.testResults = builder.testResults;
        this.metrics = builder.metrics;
        this.errors = builder.errors;
        this.successful = failedTests == 0;
    }
    
    // Getters
    public String getSuiteName() { return suiteName; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public Duration getTotalDuration() { return totalDuration; }
    public int getTotalTests() { return totalTests; }
    public int getPassedTests() { return passedTests; }
    public int getFailedTests() { return failedTests; }
    public int getSkippedTests() { return skippedTests; }
    public List<TestResult> getTestResults() { return testResults; }
    public Map<String, Object> getMetrics() { return metrics; }
    public List<String> getErrors() { return errors; }
    public boolean isSuccessful() { return successful; }
    public double getSuccessRate() { 
        return totalTests > 0 ? (double) passedTests / totalTests : 0.0; 
    }
    
    /**
     * Add a test result to this suite result
     */
    public void addResult(String testName, TestResult result) {
        // This is immutable, so we'd need to track this during building
        throw new UnsupportedOperationException("TestSuiteResult is immutable. Use Builder.addResult() instead.");
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String suiteName;
        private Instant startTime;
        private Instant endTime;
        private int totalTests;
        private int passedTests;
        private int failedTests;
        private int skippedTests;
        private List<TestResult> testResults = new ArrayList<>();
        private Map<String, Object> metrics = new HashMap<>();
        private List<String> errors = new ArrayList<>();
        
        public Builder suiteName(String suiteName) {
            this.suiteName = suiteName;
            return this;
        }
        
        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }
        
        public Builder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }
        
        public Builder totalTests(int totalTests) {
            this.totalTests = totalTests;
            return this;
        }
        
        public Builder passedTests(int passedTests) {
            this.passedTests = passedTests;
            return this;
        }
        
        public Builder failedTests(int failedTests) {
            this.failedTests = failedTests;
            return this;
        }
        
        public Builder skippedTests(int skippedTests) {
            this.skippedTests = skippedTests;
            return this;
        }
        
        public Builder testResults(List<TestResult> testResults) {
            this.testResults = testResults;
            return this;
        }
        
        public Builder metrics(Map<String, Object> metrics) {
            this.metrics = metrics;
            return this;
        }
        
        public Builder errors(List<String> errors) {
            this.errors = errors;
            return this;
        }
        
        /**
         * Add a test result and update counters
         */
        public Builder addResult(String testName, TestResult result) {
            this.testResults.add(result);
            this.totalTests++;
            
            switch (result.getStatus()) {
                case PASSED:
                    this.passedTests++;
                    break;
                case FAILED:
                case ERROR:
                    this.failedTests++;
                    break;
                case SKIPPED:
                    this.skippedTests++;
                    break;
            }
            
            return this;
        }
        
        public TestSuiteResult build() {
            return new TestSuiteResult(this);
        }
    }
}