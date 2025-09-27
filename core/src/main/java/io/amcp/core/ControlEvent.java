package io.amcp.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a control event used for managing agent lifecycle and system operations.
 * Control events are system-level messages distinct from application events.
 * 
 * @since AMCP 1.0
 */
public final class ControlEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * Control command types for agent management.
     */
    public enum Command {
        // Agent lifecycle commands
        ACTIVATE,
        DEACTIVATE,
        CLONE,
        MIGRATE,
        DESTROY,
        
        // System commands
        PING,
        SHUTDOWN,
        STATUS_REQUEST,
        
        // Migration-specific commands
        PREPARE_MIGRATION,
        COMPLETE_MIGRATION,
        CANCEL_MIGRATION,
        
        // Directory/discovery commands
        ANNOUNCE,
        DISCOVER,
        HEARTBEAT
    }
    
    private final Command command;
    private final AgentID targetAgent;
    private final AgentID sourceAgent;
    private final Map<String, Object> parameters;
    private final String requestId;
    
    /**
     * Builder for creating ControlEvents.
     */
    public static class Builder {
        private Command command;
        private AgentID targetAgent;
        private AgentID sourceAgent;
        private Map<String, Object> parameters = new HashMap<>();
        private String requestId;
        
        public Builder command(Command command) {
            this.command = command;
            return this;
        }
        
        public Builder targetAgent(AgentID targetAgent) {
            this.targetAgent = targetAgent;
            return this;
        }
        
        public Builder sourceAgent(AgentID sourceAgent) {
            this.sourceAgent = sourceAgent;
            return this;
        }
        
        public Builder parameter(String key, Object value) {
            this.parameters.put(key, value);
            return this;
        }
        
        public Builder parameters(Map<String, Object> parameters) {
            if (parameters != null) {
                this.parameters.putAll(parameters);
            }
            return this;
        }
        
        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }
        
        public ControlEvent build() {
            if (command == null) {
                throw new IllegalArgumentException("Command cannot be null");
            }
            
            return new ControlEvent(command, targetAgent, sourceAgent, 
                                  new HashMap<>(parameters), requestId);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Factory method for simple control events.
     */
    public static ControlEvent create(Command command, AgentID targetAgent) {
        return builder()
                .command(command)
                .targetAgent(targetAgent)
                .build();
    }
    
    /**
     * Factory method for control events with source.
     */
    public static ControlEvent create(Command command, AgentID targetAgent, AgentID sourceAgent) {
        return builder()
                .command(command)
                .targetAgent(targetAgent)
                .sourceAgent(sourceAgent)
                .build();
    }
    
    private ControlEvent(Command command, AgentID targetAgent, AgentID sourceAgent,
                        Map<String, Object> parameters, String requestId) {
        this.command = command;
        this.targetAgent = targetAgent;
        this.sourceAgent = sourceAgent;
        this.parameters = parameters;
        this.requestId = requestId;
    }
    
    public Command getCommand() {
        return command;
    }
    
    public AgentID getTargetAgent() {
        return targetAgent;
    }
    
    public AgentID getSourceAgent() {
        return sourceAgent;
    }
    
    public Map<String, Object> getParameters() {
        return new HashMap<>(parameters);
    }
    
    public Object getParameter(String key) {
        return parameters.get(key);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, Class<T> type) {
        Object value = parameters.get(key);
        if (value == null) {
            return null;
        }
        
        if (type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        
        throw new ClassCastException("Parameter value for key '" + key + 
                                     "' is not of type " + type.getName());
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    /**
     * Creates an Event representation of this control event for publishing.
     */
    public Event toEvent() {
        String topic = targetAgent != null ? 
                       targetAgent.getControlTopic() : 
                       "system.control";
        
        return Event.builder()
                .topic(topic)
                .payload(this)
                .sender(sourceAgent)
                .deliveryOptions(DeliveryOptions.RELIABLE)
                .build();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ControlEvent that = (ControlEvent) obj;
        return Objects.equals(requestId, that.requestId) &&
               command == that.command &&
               Objects.equals(targetAgent, that.targetAgent) &&
               Objects.equals(sourceAgent, that.sourceAgent);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(command, targetAgent, sourceAgent, requestId);
    }
    
    @Override
    public String toString() {
        return "ControlEvent{" +
                "command=" + command +
                ", targetAgent=" + targetAgent +
                ", sourceAgent=" + sourceAgent +
                ", requestId='" + requestId + '\'' +
                ", parameters=" + parameters.keySet() +
                '}';
    }
}