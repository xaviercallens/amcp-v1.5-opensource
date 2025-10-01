package io.amcp.connectors.ai.planning;

import java.util.*;

/**
 * Represents a task definition in the orchestration planning engine.
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class TaskDefinition {
    
    private final String taskId;
    private final String name;
    private final String description;
    private final String agentType;
    private final Map<String, Object> parameters;
    private final Set<String> dependencies;
    private final int priority;
    private final long timeoutMs;
    private final boolean optional;
    
    public TaskDefinition(String taskId, String name, String description, String agentType) {
        this.taskId = taskId;
        this.name = name;
        this.description = description;
        this.agentType = agentType;
        this.parameters = new HashMap<>();
        this.dependencies = new HashSet<>();
        this.priority = 0;
        this.timeoutMs = 30000; // 30 seconds default
        this.optional = false;
    }
    
    public TaskDefinition(String taskId, String name, String description, String agentType,
                         Map<String, Object> parameters, Set<String> dependencies, 
                         int priority, long timeoutMs, boolean optional) {
        this.taskId = taskId;
        this.name = name;
        this.description = description;
        this.agentType = agentType;
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
        this.dependencies = dependencies != null ? new HashSet<>(dependencies) : new HashSet<>();
        this.priority = priority;
        this.timeoutMs = timeoutMs;
        this.optional = optional;
    }
    
    // Getters
    public String getTaskId() { return taskId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getAgentType() { return agentType; }
    public Map<String, Object> getParameters() { return new HashMap<>(parameters); }
    public Set<String> getDependencies() { return new HashSet<>(dependencies); }
    public int getPriority() { return priority; }
    public long getTimeoutMs() { return timeoutMs; }
    public boolean isOptional() { return optional; }
    
    // Compatibility methods for legacy code
    public String getCapability() { return agentType; }
    public String getAgent() { return agentType; }
    public Map<String, Object> getParams() { return getParameters(); }
    
    // Utility methods
    public void addParameter(String key, Object value) {
        this.parameters.put(key, value);
    }
    
    public Object getParameter(String key) {
        return this.parameters.get(key);
    }
    
    public void addDependency(String taskId) {
        this.dependencies.add(taskId);
    }
    
    public boolean hasDependencies() {
        return !dependencies.isEmpty();
    }
    
    public boolean dependsOn(String taskId) {
        return dependencies.contains(taskId);
    }
    
    @Override
    public String toString() {
        return String.format("TaskDefinition{id='%s', name='%s', agentType='%s', priority=%d}", 
                           taskId, name, agentType, priority);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TaskDefinition that = (TaskDefinition) obj;
        return Objects.equals(taskId, that.taskId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }
    
    // Builder pattern
    public static class Builder {
        private String taskId;
        private String name;
        private String description;
        private String agentType;
        private Map<String, Object> parameters = new HashMap<>();
        private Set<String> dependencies = new HashSet<>();
        private int priority = 0;
        private long timeoutMs = 30000;
        private boolean optional = false;
        
        public Builder(String taskId, String name, String agentType) {
            this.taskId = taskId;
            this.name = name;
            this.agentType = agentType;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder addParameter(String key, Object value) {
            this.parameters.put(key, value);
            return this;
        }
        
        public Builder parameters(Map<String, Object> parameters) {
            this.parameters = new HashMap<>(parameters);
            return this;
        }
        
        public Builder addDependency(String taskId) {
            this.dependencies.add(taskId);
            return this;
        }
        
        public Builder dependencies(Set<String> dependencies) {
            this.dependencies = new HashSet<>(dependencies);
            return this;
        }
        
        public Builder priority(int priority) {
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
        
        public TaskDefinition build() {
            return new TaskDefinition(taskId, name, description, agentType, 
                                     parameters, dependencies, priority, timeoutMs, optional);
        }
    }
}