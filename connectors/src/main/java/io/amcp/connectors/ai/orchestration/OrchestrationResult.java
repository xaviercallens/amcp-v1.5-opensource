package io.amcp.connectors.ai.orchestration;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents the final result of an orchestration session.
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class OrchestrationResult {
    
    private final String sessionId;
    private final boolean success;
    private final String finalResult;
    private final String errorMessage;
    private final LocalDateTime completionTime;
    private final long totalExecutionTimeMs;
    private final Map<String, TaskResult> taskResults;
    private final Map<String, Object> metadata;
    private final OrchestrationSession.SessionState finalState;
    
    // Constructor for successful orchestration
    public OrchestrationResult(String sessionId, String finalResult, long totalExecutionTimeMs, 
                             Map<String, TaskResult> taskResults) {
        this.sessionId = sessionId;
        this.success = true;
        this.finalResult = finalResult;
        this.errorMessage = null;
        this.completionTime = LocalDateTime.now();
        this.totalExecutionTimeMs = totalExecutionTimeMs;
        this.taskResults = taskResults != null ? new HashMap<>(taskResults) : new HashMap<>();
        this.metadata = new HashMap<>();
        this.finalState = OrchestrationSession.SessionState.COMPLETED;
    }
    
    // Constructor for failed orchestration
    public OrchestrationResult(String sessionId, String errorMessage, long totalExecutionTimeMs, 
                             Map<String, TaskResult> taskResults, OrchestrationSession.SessionState finalState) {
        this.sessionId = sessionId;
        this.success = false;
        this.finalResult = null;
        this.errorMessage = errorMessage;
        this.completionTime = LocalDateTime.now();
        this.totalExecutionTimeMs = totalExecutionTimeMs;
        this.taskResults = taskResults != null ? new HashMap<>(taskResults) : new HashMap<>();
        this.metadata = new HashMap<>();
        this.finalState = finalState != null ? finalState : OrchestrationSession.SessionState.FAILED;
    }
    
    // Full constructor
    public OrchestrationResult(String sessionId, boolean success, String finalResult, String errorMessage,
                             long totalExecutionTimeMs, Map<String, TaskResult> taskResults, 
                             Map<String, Object> metadata, OrchestrationSession.SessionState finalState) {
        this.sessionId = sessionId;
        this.success = success;
        this.finalResult = finalResult;
        this.errorMessage = errorMessage;
        this.completionTime = LocalDateTime.now();
        this.totalExecutionTimeMs = totalExecutionTimeMs;
        this.taskResults = taskResults != null ? new HashMap<>(taskResults) : new HashMap<>();
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        this.finalState = finalState != null ? finalState : 
                         (success ? OrchestrationSession.SessionState.COMPLETED : OrchestrationSession.SessionState.FAILED);
    }
    
    // Getters
    public String getSessionId() { return sessionId; }
    public boolean isSuccess() { return success; }
    public String getFinalResult() { return finalResult; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getCompletionTime() { return completionTime; }
    public long getTotalExecutionTimeMs() { return totalExecutionTimeMs; }
    public Map<String, TaskResult> getTaskResults() { return new HashMap<>(taskResults); }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
    public OrchestrationSession.SessionState getFinalState() { return finalState; }
    
    // Utility methods
    public int getTaskCount() {
        return taskResults.size();
    }
    
    public int getSuccessfulTaskCount() {
        return (int) taskResults.values().stream().filter(TaskResult::isSuccess).count();
    }
    
    public int getFailedTaskCount() {
        return (int) taskResults.values().stream().filter(result -> !result.isSuccess()).count();
    }
    
    public List<TaskResult> getSuccessfulTasks() {
        return taskResults.values().stream()
                .filter(TaskResult::isSuccess)
                .collect(ArrayList::new, (list, item) -> list.add(item), (list1, list2) -> list1.addAll(list2));
    }
    
    public List<TaskResult> getFailedTasks() {
        return taskResults.values().stream()
                .filter(result -> !result.isSuccess())
                .collect(ArrayList::new, (list, item) -> list.add(item), (list1, list2) -> list1.addAll(list2));
    }
    
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
    
    public Object getMetadata(String key) {
        return this.metadata.get(key);
    }
    
    // Static factory methods
    public static OrchestrationResult success(String sessionId, String finalResult, 
                                            long totalExecutionTimeMs, Map<String, TaskResult> taskResults) {
        return new OrchestrationResult(sessionId, finalResult, totalExecutionTimeMs, taskResults);
    }
    
    public static OrchestrationResult failure(String sessionId, String errorMessage, 
                                            long totalExecutionTimeMs, Map<String, TaskResult> taskResults) {
        return new OrchestrationResult(sessionId, errorMessage, totalExecutionTimeMs, taskResults, 
                                     OrchestrationSession.SessionState.FAILED);
    }
    
    public static OrchestrationResult timeout(String sessionId, long totalExecutionTimeMs, 
                                            Map<String, TaskResult> taskResults) {
        return new OrchestrationResult(sessionId, "Orchestration timeout", totalExecutionTimeMs, taskResults, 
                                     OrchestrationSession.SessionState.FAILED);
    }
    
    public static OrchestrationResult cancelled(String sessionId, long totalExecutionTimeMs, 
                                               Map<String, TaskResult> taskResults) {
        return new OrchestrationResult(sessionId, "Orchestration cancelled", totalExecutionTimeMs, taskResults, 
                                     OrchestrationSession.SessionState.CANCELLED);
    }
    
    @Override
    public String toString() {
        return String.format("OrchestrationResult{sessionId='%s', success=%s, tasks=%d/%d successful, totalTime=%dms}", 
                           sessionId, success, getSuccessfulTaskCount(), getTaskCount(), totalExecutionTimeMs);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        OrchestrationResult that = (OrchestrationResult) obj;
        return Objects.equals(sessionId, that.sessionId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }
}