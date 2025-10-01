package io.amcp.connectors.ai.orchestration;

import io.amcp.core.AgentID;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an active orchestration session with task tracking and lifecycle management.
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class OrchestrationSession {
    
    private final String sessionId;
    private final String userQuery;
    private final LocalDateTime startTime;
    private final Map<String, Object> context;
    private final Map<String, TaskResult> taskResults;
    private final Set<String> activeTaskIds;
    private final AgentID orchestratorId;
    
    private volatile SessionState state;
    private LocalDateTime lastUpdateTime;
    private String errorMessage;
    
    public enum SessionState {
        INITIALIZING,
        PLANNING,
        EXECUTING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
    
    public OrchestrationSession(String sessionId, String userQuery, AgentID orchestratorId) {
        this.sessionId = sessionId;
        this.userQuery = userQuery;
        this.orchestratorId = orchestratorId;
        this.startTime = LocalDateTime.now();
        this.lastUpdateTime = this.startTime;
        this.context = new ConcurrentHashMap<>();
        this.taskResults = new ConcurrentHashMap<>();
        this.activeTaskIds = ConcurrentHashMap.newKeySet();
        this.state = SessionState.INITIALIZING;
    }
    
    // Getters
    public String getSessionId() { return sessionId; }
    public String getUserQuery() { return userQuery; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getLastUpdateTime() { return lastUpdateTime; }
    public AgentID getOrchestratorId() { return orchestratorId; }
    public SessionState getState() { return state; }
    public String getErrorMessage() { return errorMessage; }
    public Map<String, Object> getContext() { return new HashMap<>(context); }
    public Map<String, TaskResult> getTaskResults() { return new HashMap<>(taskResults); }
    public Set<String> getActiveTaskIds() { return new HashSet<>(activeTaskIds); }
    
    // State management
    public void setState(SessionState newState) {
        this.state = newState;
        this.lastUpdateTime = LocalDateTime.now();
    }
    
    public void setError(String errorMessage) {
        this.errorMessage = errorMessage;
        this.state = SessionState.FAILED;
        this.lastUpdateTime = LocalDateTime.now();
    }
    
    // Context management
    public void putContext(String key, Object value) {
        this.context.put(key, value);
        this.lastUpdateTime = LocalDateTime.now();
    }
    
    public Object getContext(String key) {
        return this.context.get(key);
    }
    
    // Task management
    public void addActiveTask(String taskId) {
        this.activeTaskIds.add(taskId);
        this.lastUpdateTime = LocalDateTime.now();
    }
    
    public void completeTask(String taskId, TaskResult result) {
        this.activeTaskIds.remove(taskId);
        this.taskResults.put(taskId, result);
        this.lastUpdateTime = LocalDateTime.now();
    }
    
    public boolean isCompleted() {
        return state == SessionState.COMPLETED || state == SessionState.FAILED || state == SessionState.CANCELLED;
    }
    
    public boolean hasActiveTasks() {
        return !activeTaskIds.isEmpty();
    }
    
    public long getDurationMs() {
        return java.time.Duration.between(startTime, lastUpdateTime).toMillis();
    }
    
    /**
     * Get the normalized query for this session.
     */
    public String getNormalizedQuery() {
        return userQuery; // In this implementation, the query is already normalized when session is created
    }
    
    /**
     * Get the correlation ID for this session.
     */
    public String getCorrelationId() {
        return sessionId; // Using session ID as correlation ID
    }
    
    /**
     * Set the task plan for this session.
     */
    public void setTaskPlan(Object taskPlan) {
        // Store task plan in context
        context.put("taskPlan", taskPlan);
        this.lastUpdateTime = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return String.format("OrchestrationSession{id='%s', state=%s, activeTasks=%d, completedTasks=%d, duration=%dms}", 
                           sessionId, state, activeTaskIds.size(), taskResults.size(), getDurationMs());
    }
}