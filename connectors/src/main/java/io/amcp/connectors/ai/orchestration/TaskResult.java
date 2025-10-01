package io.amcp.connectors.ai.orchestration;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents the result of a task execution within an orchestration session.
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class TaskResult {
    
    private final String taskId;
    private final String agentId;
    private final boolean success;
    private final Object result;
    private final String errorMessage;
    private final LocalDateTime completionTime;
    private final long executionTimeMs;
    private final Map<String, Object> metadata;
    
    // Constructor for successful results
    public TaskResult(String taskId, String agentId, Object result, long executionTimeMs) {
        this.taskId = taskId;
        this.agentId = agentId;
        this.success = true;
        this.result = result;
        this.errorMessage = null;
        this.completionTime = LocalDateTime.now();
        this.executionTimeMs = executionTimeMs;
        this.metadata = new HashMap<>();
    }
    
    // Constructor for failed results
    public TaskResult(String taskId, String agentId, String errorMessage, long executionTimeMs) {
        this.taskId = taskId;
        this.agentId = agentId;
        this.success = false;
        this.result = null;
        this.errorMessage = errorMessage;
        this.completionTime = LocalDateTime.now();
        this.executionTimeMs = executionTimeMs;
        this.metadata = new HashMap<>();
    }
    
    // Constructor with metadata
    public TaskResult(String taskId, String agentId, boolean success, Object result, 
                     String errorMessage, long executionTimeMs, Map<String, Object> metadata) {
        this.taskId = taskId;
        this.agentId = agentId;
        this.success = success;
        this.result = result;
        this.errorMessage = errorMessage;
        this.completionTime = LocalDateTime.now();
        this.executionTimeMs = executionTimeMs;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }
    
    // Getters
    public String getTaskId() { return taskId; }
    public String getAgentId() { return agentId; }
    public boolean isSuccess() { return success; }
    public Object getResult() { return result; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getCompletionTime() { return completionTime; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
    
    // Utility methods
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
    
    public Object getMetadata(String key) {
        return this.metadata.get(key);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getResult(Class<T> type) {
        if (result != null && type.isAssignableFrom(result.getClass())) {
            return (T) result;
        }
        return null;
    }
    
    public String getResultAsString() {
        return result != null ? result.toString() : null;
    }
    
    // Static factory methods
    public static TaskResult success(String taskId, String agentId, Object result, long executionTimeMs) {
        return new TaskResult(taskId, agentId, result, executionTimeMs);
    }
    
    public static TaskResult failure(String taskId, String agentId, String errorMessage, long executionTimeMs) {
        return new TaskResult(taskId, agentId, errorMessage, executionTimeMs);
    }
    
    public static TaskResult timeout(String taskId, String agentId, long executionTimeMs) {
        return new TaskResult(taskId, agentId, "Task execution timeout", executionTimeMs);
    }
    
    @Override
    public String toString() {
        return String.format("TaskResult{taskId='%s', agentId='%s', success=%s, executionTime=%dms}", 
                           taskId, agentId, success, executionTimeMs);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TaskResult that = (TaskResult) obj;
        return Objects.equals(taskId, that.taskId) && Objects.equals(agentId, that.agentId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(taskId, agentId);
    }
}