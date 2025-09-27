package io.amcp.core;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for an Agent in the AMCP mesh.
 * AgentID persists across agent migrations and cloning operations.
 * 
 * @since AMCP 1.0
 */
public final class AgentID implements Serializable, Comparable<AgentID> {
    private static final long serialVersionUID = 1L;
    
    private final String id;
    private final String namespace;
    
    /**
     * Creates an AgentID with a random UUID in the default namespace.
     */
    public static AgentID random() {
        return new AgentID(UUID.randomUUID().toString(), "default");
    }
    
    /**
     * Creates an AgentID with a random UUID in the specified namespace.
     */
    public static AgentID random(String namespace) {
        return new AgentID(UUID.randomUUID().toString(), namespace);
    }
    
    /**
     * Creates an AgentID from a string representation.
     * Format: "namespace:id" or just "id" (uses default namespace)
     */
    public static AgentID fromString(String agentIdStr) {
        if (agentIdStr == null || agentIdStr.trim().isEmpty()) {
            throw new IllegalArgumentException("AgentID string cannot be null or empty");
        }
        
        String[] parts = agentIdStr.split(":", 2);
        if (parts.length == 2) {
            return new AgentID(parts[1], parts[0]);
        } else {
            return new AgentID(agentIdStr, "default");
        }
    }
    
    /**
     * Constructs an AgentID with specified id and namespace.
     */
    public AgentID(String id, String namespace) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }
        if (namespace == null || namespace.trim().isEmpty()) {
            throw new IllegalArgumentException("Namespace cannot be null or empty");
        }
        
        this.id = id.trim();
        this.namespace = namespace.trim();
    }
    
    /**
     * Gets the unique identifier portion.
     */
    public String getId() {
        return id;
    }
    
    /**
     * Gets the namespace for this agent.
     */
    public String getNamespace() {
        return namespace;
    }
    
    /**
     * Returns the fully qualified agent ID as "namespace:id".
     */
    public String getFullyQualifiedId() {
        return namespace + ":" + id;
    }
    
    /**
     * Creates a control topic name for this agent: "agent.{fullyQualifiedId}.control"
     */
    public String getControlTopic() {
        return "agent." + getFullyQualifiedId() + ".control";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AgentID agentID = (AgentID) obj;
        return Objects.equals(id, agentID.id) && 
               Objects.equals(namespace, agentID.namespace);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, namespace);
    }
    
    @Override
    public int compareTo(AgentID other) {
        if (other == null) return 1;
        
        int namespaceCompare = namespace.compareTo(other.namespace);
        if (namespaceCompare != 0) {
            return namespaceCompare;
        }
        
        return id.compareTo(other.id);
    }
    
    @Override
    public String toString() {
        return getFullyQualifiedId();
    }
}