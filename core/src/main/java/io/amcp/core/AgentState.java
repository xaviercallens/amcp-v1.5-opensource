package io.amcp.core;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the serializable state of an agent for persistence and migration.
 * Contains all information needed to recreate the agent in another context.
 * 
 * @since AMCP 1.0
 */
public final class AgentState implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final AgentID agentId;
    private final String agentType;
    private final AgentLifecycle lifecycleState;
    private final long captureTimestamp;
    private final Object agentData;
    private final Map<String, Object> metadata;
    
    /**
     * Basic constructor with minimal required information.
     */
    public AgentState(AgentID agentId, String agentType, AgentLifecycle lifecycleState, 
                     long captureTimestamp) {
        this(agentId, agentType, lifecycleState, captureTimestamp, null, new HashMap<>());
    }
    
    /**
     * Full constructor with agent data and metadata.
     */
    public AgentState(AgentID agentId, String agentType, AgentLifecycle lifecycleState,
                     long captureTimestamp, Object agentData, Map<String, Object> metadata) {
        if (agentId == null) {
            throw new IllegalArgumentException("AgentID cannot be null");
        }
        if (agentType == null || agentType.trim().isEmpty()) {
            throw new IllegalArgumentException("AgentType cannot be null or empty");
        }
        
        this.agentId = agentId;
        this.agentType = agentType.trim();
        this.lifecycleState = lifecycleState != null ? lifecycleState : AgentLifecycle.INACTIVE;
        this.captureTimestamp = captureTimestamp;
        this.agentData = agentData;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }
    
    /**
     * Builder for creating AgentState instances.
     */
    public static class Builder {
        private AgentID agentId;
        private String agentType;
        private AgentLifecycle lifecycleState = AgentLifecycle.INACTIVE;
        private long captureTimestamp = System.currentTimeMillis();
        private Object agentData;
        private Map<String, Object> metadata = new HashMap<>();
        
        public Builder agentId(AgentID agentId) {
            this.agentId = agentId;
            return this;
        }
        
        public Builder agentType(String agentType) {
            this.agentType = agentType;
            return this;
        }
        
        public Builder lifecycleState(AgentLifecycle lifecycleState) {
            this.lifecycleState = lifecycleState;
            return this;
        }
        
        public Builder captureTimestamp(long timestamp) {
            this.captureTimestamp = timestamp;
            return this;
        }
        
        public Builder agentData(Object agentData) {
            this.agentData = agentData;
            return this;
        }
        
        public Builder metadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            if (metadata != null) {
                this.metadata.putAll(metadata);
            }
            return this;
        }
        
        public AgentState build() {
            return new AgentState(agentId, agentType, lifecycleState, 
                                captureTimestamp, agentData, metadata);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Factory method to capture state from an existing agent.
     */
    public static AgentState fromAgent(Agent agent) {
        return builder()
                .agentId(agent.getAgentId())
                .agentType(agent.getAgentType())
                .lifecycleState(agent.getLifecycleState())
                .captureTimestamp(System.currentTimeMillis())
                .agentData(agent.captureState())
                .metadata("sourceContext", agent.getAgentContext().getContextId())
                .build();
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
    
    public long getCaptureTimestamp() {
        return captureTimestamp;
    }
    
    public Instant getCaptureTimestampAsInstant() {
        return Instant.ofEpochMilli(captureTimestamp);
    }
    
    public Object getAgentData() {
        return agentData;
    }
    
    /**
     * Type-safe accessor for agent data.
     */
    @SuppressWarnings("unchecked")
    public <T> T getAgentData(Class<T> type) {
        if (agentData == null) {
            return null;
        }
        
        if (type.isAssignableFrom(agentData.getClass())) {
            return (T) agentData;
        }
        
        throw new ClassCastException("Agent data is not of type " + type.getName() + 
                                     ", actual type: " + agentData.getClass().getName());
    }
    
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (value == null) {
            return null;
        }
        
        if (type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        
        throw new ClassCastException("Metadata value for key '" + key + 
                                     "' is not of type " + type.getName());
    }
    
    /**
     * Checks if this state is stale (captured too long ago).
     */
    public boolean isStale(long maxAgeMillis) {
        return System.currentTimeMillis() - captureTimestamp > maxAgeMillis;
    }
    
    /**
     * Creates a new AgentState with updated metadata.
     */
    public AgentState withMetadata(String key, Object value) {
        Map<String, Object> newMetadata = new HashMap<>(metadata);
        newMetadata.put(key, value);
        return new AgentState(agentId, agentType, lifecycleState, 
                            captureTimestamp, agentData, newMetadata);
    }
    
    /**
     * Creates a new AgentState with updated lifecycle state.
     */
    public AgentState withLifecycleState(AgentLifecycle newState) {
        return new AgentState(agentId, agentType, newState, 
                            System.currentTimeMillis(), agentData, metadata);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AgentState that = (AgentState) obj;
        return captureTimestamp == that.captureTimestamp &&
               Objects.equals(agentId, that.agentId) &&
               Objects.equals(agentType, that.agentType) &&
               lifecycleState == that.lifecycleState;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(agentId, agentType, lifecycleState, captureTimestamp);
    }
    
    @Override
    public String toString() {
        return "AgentState{" +
                "agentId=" + agentId +
                ", agentType='" + agentType + '\'' +
                ", lifecycleState=" + lifecycleState +
                ", captureTimestamp=" + getCaptureTimestampAsInstant() +
                ", hasAgentData=" + (agentData != null) +
                ", metadataKeys=" + metadata.keySet() +
                '}';
    }
}