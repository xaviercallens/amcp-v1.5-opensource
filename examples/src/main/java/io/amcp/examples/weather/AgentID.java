package io.amcp.examples.weather;

/**
 * Simple AgentID interface for the Weather Collection System
 */
public interface AgentID {
    
    /**
     * Get the unique identifier for this agent
     */
    String getId();
    
    /**
     * Get the namespace/context for this agent
     */
    String getNamespace();
    
}