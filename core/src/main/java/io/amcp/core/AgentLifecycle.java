package io.amcp.core;

/**
 * Enumeration of agent lifecycle states in AMCP.
 * 
 * @since AMCP 1.0
 */
public enum AgentLifecycle {
    /**
     * Agent exists but is not currently running or processing events.
     * Agent state is preserved and can be activated later.
     */
    INACTIVE,
    
    /**
     * Agent is running and actively processing events.
     */
    ACTIVE,
    
    /**
     * Agent is in the process of migrating between contexts.
     * During this state, the agent may be temporarily unavailable.
     */
    MIGRATING,
    
    /**
     * Agent is being cloned. The original agent remains active
     * while a copy is being created.
     */
    CLONING,
    
    /**
     * Agent has been permanently destroyed and cannot be reactivated.
     * Its AgentID is retired and should not be reused.
     */
    TERMINATED;
    
    /**
     * Checks if the agent can receive and process events in this state.
     */
    public boolean canProcessEvents() {
        return this == ACTIVE;
    }
    
    /**
     * Checks if the agent can be migrated from this state.
     */
    public boolean canMigrate() {
        return this == ACTIVE || this == INACTIVE;
    }
    
    /**
     * Checks if the agent can be cloned from this state.
     */
    public boolean canClone() {
        return this == ACTIVE || this == INACTIVE;
    }
    
    /**
     * Checks if this is a terminal state (agent lifecycle has ended).
     */
    public boolean isTerminal() {
        return this == TERMINATED;
    }
    
    /**
     * Checks if this is a transient state (temporary during operations).
     */
    public boolean isTransient() {
        return this == MIGRATING || this == CLONING;
    }
}