package io.amcp.core;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Represents the current status of an agent in the AMCP system.
 * Used for monitoring, health checks, and discovery.
 * 
 * @since AMCP 1.0
 */
public final class AgentStatus implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final AgentID agentId;
    private final String agentType;
    private final AgentLifecycle lifecycleState;
    private final long timestamp;
    private final String contextId;
    private final String hostInfo;
    
    /**
     * Creates an agent status with current timestamp and basic info.
     */
    public AgentStatus(AgentID agentId, String agentType, AgentLifecycle lifecycleState, 
                      long timestamp, String contextId) {
        this(agentId, agentType, lifecycleState, timestamp, contextId, null);
    }
    
    /**
     * Creates a comprehensive agent status.
     */
    public AgentStatus(AgentID agentId, String agentType, AgentLifecycle lifecycleState,
                      long timestamp, String contextId, String hostInfo) {
        this.agentId = agentId;
        this.agentType = agentType;
        this.lifecycleState = lifecycleState;
        this.timestamp = timestamp;
        this.contextId = contextId;
        this.hostInfo = hostInfo;
    }
    
    /**
     * Factory method to create status for the current time.
     */
    public static AgentStatus now(AgentID agentId, String agentType, 
                                 AgentLifecycle lifecycleState, String contextId) {
        return new AgentStatus(agentId, agentType, lifecycleState, 
                             System.currentTimeMillis(), contextId);
    }
    
    public AgentID getAgentId() {
        return agentId;
    }
    
    public String getAgentType() {
        return agentType;
    }
    
    public AgentLifecycle getLifecycleState() {
        return lifecycleState;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public Instant getTimestampAsInstant() {
        return Instant.ofEpochMilli(timestamp);
    }
    
    public String getContextId() {
        return contextId;
    }
    
    public String getHostInfo() {
        return hostInfo;
    }
    
    /**
     * Checks if this status indicates the agent is healthy and active.
     */
    public boolean isHealthy() {
        return lifecycleState != null && 
               lifecycleState.canProcessEvents() &&
               contextId != null;
    }
    
    /**
     * Checks if this status is stale (older than the specified threshold).
     */
    public boolean isStale(long maxAgeMillis) {
        return System.currentTimeMillis() - timestamp > maxAgeMillis;
    }
    
    /**
     * Creates an updated status with a new lifecycle state.
     */
    public AgentStatus withLifecycleState(AgentLifecycle newState) {
        return new AgentStatus(agentId, agentType, newState, 
                             System.currentTimeMillis(), contextId, hostInfo);
    }
    
    /**
     * Creates an updated status with a new context.
     */
    public AgentStatus withContext(String newContextId) {
        return new AgentStatus(agentId, agentType, lifecycleState, 
                             System.currentTimeMillis(), newContextId, hostInfo);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AgentStatus that = (AgentStatus) obj;
        return timestamp == that.timestamp &&
               Objects.equals(agentId, that.agentId) &&
               Objects.equals(agentType, that.agentType) &&
               lifecycleState == that.lifecycleState &&
               Objects.equals(contextId, that.contextId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(agentId, agentType, lifecycleState, timestamp, contextId);
    }
    
    @Override
    public String toString() {
        return "AgentStatus{" +
                "agentId=" + agentId +
                ", agentType='" + agentType + '\'' +
                ", lifecycleState=" + lifecycleState +
                ", timestamp=" + getTimestampAsInstant() +
                ", contextId='" + contextId + '\'' +
                ", hostInfo='" + hostInfo + '\'' +
                '}';
    }
}