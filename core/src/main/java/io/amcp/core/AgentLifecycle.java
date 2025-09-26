package io.amcp.core;

/**
 * Enumeration of agent lifecycle states in the AMCP v1.4 framework.
 * 
 * <p>This enum defines the possible states an agent can be in during its lifecycle,
 * from creation through destruction. State transitions are managed by the
 * {@link AgentContext} and follow well-defined rules to ensure system consistency.</p>
 * 
 * <p>State transition diagram:
 * <pre>
 * CREATED → INACTIVE → ACTIVE → MIGRATING → ACTIVE
 *    ↓         ↓         ↓         ↓         ↓
 *    DESTROYED DESTROYED INACTIVE  FAILED    DESTROYED
 * </pre>
 * </p>
 * 
 * @author AMCP Development Team
 * @version 1.4.0
 * @since 1.0.0
 */
public enum AgentLifecycle {

    /**
     * Agent has been instantiated but not yet initialized.
     * 
     * <p>In this state, the agent object exists but has not been registered
     * with an AgentContext. No event processing or mobility operations are
     * possible in this state.</p>
     */
    CREATED("Agent created but not initialized"),

    /**
     * Agent is initialized but not actively processing events.
     * 
     * <p>The agent has been registered with an AgentContext and basic
     * initialization has completed. The agent is ready to be activated
     * but is not yet processing events or executing business logic.</p>
     */
    INACTIVE("Agent initialized but inactive"),

    /**
     * Agent is fully active and processing events.
     * 
     * <p>This is the normal operating state. The agent is processing
     * events, can initiate mobility operations, and is fully participating
     * in the agent mesh communication protocol.</p>
     */
    ACTIVE("Agent is active and processing events"),

    /**
     * Agent is in the process of migrating to another context.
     * 
     * <p>During migration, the agent temporarily suspends normal event
     * processing while its state is being transferred to the destination
     * context. Incoming events may be queued or rejected depending on
     * the migration strategy.</p>
     */
    MIGRATING("Agent is migrating to another context"),

    /**
     * Agent migration or operation has failed.
     * 
     * <p>This state indicates that a mobility operation failed and the
     * agent could not complete the requested transition. Manual intervention
     * may be required to recover the agent to a stable state.</p>
     */
    FAILED("Agent operation failed, manual intervention may be required"),

    /**
     * Agent has been permanently destroyed.
     * 
     * <p>This is the final state. The agent has been cleaned up and
     * removed from the system. No further operations are possible
     * on a destroyed agent.</p>
     */
    DESTROYED("Agent has been permanently destroyed");

    private final String description;

    AgentLifecycle(String description) {
        this.description = description;
    }

    /**
     * Gets a human-readable description of this lifecycle state.
     * 
     * @return description of the state
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this state allows event processing.
     * 
     * @return true if the agent can process events in this state
     */
    public boolean canProcessEvents() {
        return this == ACTIVE;
    }

    /**
     * Checks if this state allows mobility operations.
     * 
     * @return true if the agent can perform mobility operations in this state
     */
    public boolean canPerformMobility() {
        return this == ACTIVE;
    }

    /**
     * Checks if this state is a terminal state.
     * 
     * @return true if this is a final state with no further transitions possible
     */
    public boolean isTerminal() {
        return this == DESTROYED || this == FAILED;
    }

    /**
     * Checks if transition to the target state is allowed.
     * 
     * @param targetState the desired target state
     * @return true if the transition is allowed
     */
    public boolean canTransitionTo(AgentLifecycle targetState) {
        if (targetState == null || this == targetState) {
            return false;
        }

        // Terminal states cannot transition
        if (isTerminal()) {
            return false;
        }

        switch (this) {
            case CREATED:
                return targetState == INACTIVE || targetState == DESTROYED;
                
            case INACTIVE:
                return targetState == ACTIVE || targetState == DESTROYED;
                
            case ACTIVE:
                return targetState == INACTIVE || targetState == MIGRATING || targetState == DESTROYED;
                
            case MIGRATING:
                return targetState == ACTIVE || targetState == FAILED || targetState == DESTROYED;
                
            case FAILED:
                return targetState == INACTIVE || targetState == DESTROYED;
                
            default:
                return false;
        }
    }

    /**
     * Gets all valid target states from this state.
     * 
     * @return array of valid target states
     */
    public AgentLifecycle[] getValidTransitions() {
        switch (this) {
            case CREATED:
                return new AgentLifecycle[]{INACTIVE, DESTROYED};
                
            case INACTIVE:
                return new AgentLifecycle[]{ACTIVE, DESTROYED};
                
            case ACTIVE:
                return new AgentLifecycle[]{INACTIVE, MIGRATING, DESTROYED};
                
            case MIGRATING:
                return new AgentLifecycle[]{ACTIVE, FAILED, DESTROYED};
                
            case FAILED:
                return new AgentLifecycle[]{INACTIVE, DESTROYED};
                
            default:
                return new AgentLifecycle[0];
        }
    }

}