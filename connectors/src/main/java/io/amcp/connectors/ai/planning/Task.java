package io.amcp.connectors.ai.planning;

import io.amcp.connectors.ai.orchestration.TaskResult;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Represents an individual task within an orchestration plan.
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class Task {
    
    public enum TaskStatus {
        PENDING,
        RUNNING, 
        COMPLETED,
        FAILED,
        CANCELLED,
        TIMEOUT
    }
    
    public enum TaskPriority {
        LOW(1),
        NORMAL(2),
        HIGH(3),
        CRITICAL(4);
        
        private final int value;
        
        TaskPriority(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    private final String taskId;
    private final String sessionId;
    private final String agentType;
    private final String description;
    private final Map<String, Object> parameters;
    private final Set<String> dependencies;
    private final TaskPriority priority;
    private final long timeoutMs;
    private final boolean optional;
    private final LocalDateTime createdAt;
    
    private TaskStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Object result;
    private String errorMessage;
    private Exception exception;
    private Map<String, Object> metadata;
    private CompletableFuture<Object> executionFuture;
    
    // Constructor
    public Task(String taskId, String sessionId, String agentType, String description,
                Map<String, Object> parameters, Set<String> dependencies,
                TaskPriority priority, long timeoutMs, boolean optional) {
        this.taskId = taskId;
        this.sessionId = sessionId;
        this.agentType = agentType;
        this.description = description;
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
        this.dependencies = dependencies != null ? new HashSet<>(dependencies) : new HashSet<>();
        this.priority = priority;
        this.timeoutMs = timeoutMs;
        this.optional = optional;
        this.createdAt = LocalDateTime.now();
        this.status = TaskStatus.PENDING;
        this.metadata = new HashMap<>();
    }
    
    // Builder for easier construction
    public static class Builder {
        private String taskId;
        private String sessionId;
        private String agentType;
        private String description;
        private Map<String, Object> parameters = new HashMap<>();
        private Set<String> dependencies = new HashSet<>();
        private TaskPriority priority = TaskPriority.NORMAL;
        private long timeoutMs = 30000; // 30 seconds default
        private boolean optional = false;
        
        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }
        
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public Builder agentType(String agentType) {
            this.agentType = agentType;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder parameter(String key, Object value) {
            this.parameters.put(key, value);
            return this;
        }
        
        public Builder parameters(Map<String, Object> parameters) {
            this.parameters.putAll(parameters);
            return this;
        }
        
        public Builder dependency(String taskId) {
            this.dependencies.add(taskId);
            return this;
        }
        
        public Builder dependencies(Set<String> dependencies) {
            this.dependencies.addAll(dependencies);
            return this;
        }
        
        public Builder priority(TaskPriority priority) {
            this.priority = priority;
            return this;
        }
        
        public Builder timeout(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }
        
        public Builder optional(boolean optional) {
            this.optional = optional;
            return this;
        }
        
        public Task build() {
            Objects.requireNonNull(taskId, "Task ID is required");
            Objects.requireNonNull(sessionId, "Session ID is required");
            Objects.requireNonNull(agentType, "Agent type is required");
            Objects.requireNonNull(description, "Description is required");
            
            return new Task(taskId, sessionId, agentType, description,
                          parameters, dependencies, priority, timeoutMs, optional);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public String getTaskId() { return taskId; }
    public String getSessionId() { return sessionId; }
    public String getAgentType() { return agentType; }
    public String getDescription() { return description; }
    public Map<String, Object> getParameters() { return new HashMap<>(parameters); }
    public Set<String> getDependencies() { return new HashSet<>(dependencies); }
    public TaskPriority getPriority() { return priority; }
    public long getTimeoutMs() { return timeoutMs; }
    public boolean isOptional() { return optional; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public TaskStatus getStatus() { return status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public Object getResult() { return result; }
    public String getErrorMessage() { return errorMessage; }
    public Exception getException() { return exception; }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
    public CompletableFuture<Object> getExecutionFuture() { return executionFuture; }
    
    // Parameter access methods
    public Object getParameter(String key) {
        return parameters.get(key);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, Class<T> type) {
        Object value = parameters.get(key);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return null;
    }
    
    public String getParameterAsString(String key) {
        Object value = parameters.get(key);
        return value != null ? value.toString() : null;
    }
    
    public boolean hasParameter(String key) {
        return parameters.containsKey(key);
    }
    
    // Status management
    public void setStatus(TaskStatus status) {
        this.status = status;
        
        if (status == TaskStatus.RUNNING && startedAt == null) {
            startedAt = LocalDateTime.now();
        } else if ((status == TaskStatus.COMPLETED || status == TaskStatus.FAILED ||
                   status == TaskStatus.CANCELLED || status == TaskStatus.TIMEOUT) 
                   && completedAt == null) {
            completedAt = LocalDateTime.now();
        }
    }
    
    public void setResult(Object result) {
        this.result = result;
        if (status == TaskStatus.RUNNING) {
            setStatus(TaskStatus.COMPLETED);
        }
    }
    
    public void setError(String errorMessage, Exception exception) {
        this.errorMessage = errorMessage;
        this.exception = exception;
        if (status == TaskStatus.RUNNING) {
            setStatus(TaskStatus.FAILED);
        }
    }
    
    public void setExecutionFuture(CompletableFuture<Object> future) {
        this.executionFuture = future;
    }
    
    // Metadata management
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
    
    public Object getMetadata(String key) {
        return this.metadata.get(key);
    }
    
    // Utility methods
    public boolean isDependencyMet(Set<String> completedTaskIds) {
        return completedTaskIds.containsAll(dependencies);
    }
    
    public boolean isRunnable(Set<String> completedTaskIds) {
        return status == TaskStatus.PENDING && isDependencyMet(completedTaskIds);
    }
    
    public boolean isCompleted() {
        return status == TaskStatus.COMPLETED || status == TaskStatus.FAILED ||
               status == TaskStatus.CANCELLED || status == TaskStatus.TIMEOUT;
    }
    
    public boolean isSuccessful() {
        return status == TaskStatus.COMPLETED;
    }
    
    public boolean hasFailed() {
        return status == TaskStatus.FAILED || status == TaskStatus.TIMEOUT;
    }
    
    public long getExecutionTimeMs() {
        if (startedAt != null && completedAt != null) {
            return java.time.Duration.between(startedAt, completedAt).toMillis();
        }
        return 0;
    }
    
    public boolean isTimedOut() {
        if (startedAt != null && status == TaskStatus.RUNNING) {
            long elapsed = java.time.Duration.between(startedAt, LocalDateTime.now()).toMillis();
            return elapsed > timeoutMs;
        }
        return false;
    }
    
    // Create TaskResult from this Task
    public TaskResult toTaskResult() {
        return toTaskResult("unknown-agent");
    }
    
    public TaskResult toTaskResult(String agentId) {
        if (isSuccessful()) {
            return TaskResult.success(taskId, agentId, result, getExecutionTimeMs());
        } else if (hasFailed()) {
            return TaskResult.failure(taskId, agentId, errorMessage, getExecutionTimeMs());
        } else {
            throw new IllegalStateException("Cannot create TaskResult for task in status: " + status);
        }
    }
    
    @Override
    public String toString() {
        return String.format("Task{id='%s', agentType='%s', status=%s, priority=%s, dependencies=%d}", 
                           taskId, agentType, status, priority, dependencies.size());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Task task = (Task) obj;
        return Objects.equals(taskId, task.taskId) && Objects.equals(sessionId, task.sessionId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(taskId, sessionId);
    }
}