package io.amcp.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract base implementation of the Agent interface.
 * Provides common functionality and state management for all agents.
 * 
 * @since AMCP 1.0
 */
public abstract class AbstractAgent implements Agent {
    private static final long serialVersionUID = 1L;
    
    private final AgentID agentId;
    private final String agentType;
    private final AtomicReference<AgentLifecycle> lifecycleState;
    private transient AgentContext agentContext;
    
    /**
     * Constructor for creating an agent with a specific ID and type.
     */
    protected AbstractAgent(AgentID agentId, String agentType) {
        if (agentId == null) {
            throw new IllegalArgumentException("AgentID cannot be null");
        }
        if (agentType == null || agentType.trim().isEmpty()) {
            throw new IllegalArgumentException("AgentType cannot be null or empty");
        }
        
        this.agentId = agentId;
        this.agentType = agentType.trim();
        this.lifecycleState = new AtomicReference<>(AgentLifecycle.INACTIVE);
    }
    
    /**
     * Constructor for creating an agent with a specific type (generates random ID).
     */
    protected AbstractAgent(String agentType) {
        this(AgentID.random(), agentType);
    }
    
    @Override
    public final AgentID getAgentId() {
        return agentId;
    }
    
    @Override
    public final String getAgentType() {
        return agentType;
    }
    
    @Override
    public final AgentLifecycle getLifecycleState() {
        return lifecycleState.get();
    }
    
    @Override
    public final AgentContext getAgentContext() {
        return agentContext;
    }
    
    /**
     * Sets the agent context. Called by the runtime when the agent is
     * added to or moved between contexts.
     */
    public final void setAgentContext(AgentContext context) {
        this.agentContext = context;
    }
    
    /**
     * Sets the lifecycle state. Called by the runtime during state transitions.
     */
    public final void setLifecycleState(AgentLifecycle newState) {
        AgentLifecycle oldState = lifecycleState.get();
        
        if (isValidTransition(oldState, newState)) {
            lifecycleState.set(newState);
            onLifecycleStateChanged(oldState, newState);
        } else {
            throw new IllegalStateException(
                "Invalid lifecycle transition from " + oldState + " to " + newState
            );
        }
    }
    
    /**
     * Called when the lifecycle state changes. Subclasses can override
     * to react to state transitions.
     */
    protected void onLifecycleStateChanged(AgentLifecycle oldState, AgentLifecycle newState) {
        // Default implementation - subclasses can override
    }
    
    /**
     * Validates if a lifecycle state transition is allowed.
     */
    private boolean isValidTransition(AgentLifecycle from, AgentLifecycle to) {
        if (from == to) {
            return true; // Same state is always valid
        }
        
        switch (from) {
            case INACTIVE:
                return to == AgentLifecycle.ACTIVE || 
                       to == AgentLifecycle.MIGRATING || 
                       to == AgentLifecycle.CLONING ||
                       to == AgentLifecycle.TERMINATED;
                       
            case ACTIVE:
                return to == AgentLifecycle.INACTIVE || 
                       to == AgentLifecycle.MIGRATING || 
                       to == AgentLifecycle.CLONING ||
                       to == AgentLifecycle.TERMINATED;
                       
            case MIGRATING:
                return to == AgentLifecycle.ACTIVE || 
                       to == AgentLifecycle.INACTIVE ||
                       to == AgentLifecycle.TERMINATED;
                       
            case CLONING:
                return to == AgentLifecycle.ACTIVE || 
                       to == AgentLifecycle.INACTIVE ||
                       to == AgentLifecycle.TERMINATED;
                       
            case TERMINATED:
                return false; // Terminal state - no transitions allowed
                
            default:
                return false;
        }
    }
    
    // Default implementations of lifecycle callbacks
    
    @Override
    public void onCreate(Object initData) {
        // Default implementation logs the creation
        System.out.println("Agent " + agentId + " created with type: " + agentType);
    }
    
    @Override
    public void onActivate() {
        System.out.println("Agent " + agentId + " activated");
    }
    
    @Override
    public void onDeactivate() {
        System.out.println("Agent " + agentId + " deactivated");
    }
    
    @Override
    public void onBeforeClone(AgentID cloneId) {
        System.out.println("Agent " + agentId + " preparing to be cloned as " + cloneId);
    }
    
    @Override
    public void onAfterClone(Agent clone) {
        System.out.println("Agent " + agentId + " cloned successfully, clone ID: " + clone.getAgentId());
    }
    
    @Override
    public void onBeforeDispatch(String destinationContext) {
        System.out.println("Agent " + agentId + " preparing to migrate to " + destinationContext);
    }
    
    @Override
    public void onArrival(String previousContext) {
        System.out.println("Agent " + agentId + " arrived from " + previousContext);
    }
    
    @Override
    public void onDestroy() {
        System.out.println("Agent " + agentId + " being destroyed");
    }
    
    // Abstract method that subclasses must implement
    
    /**
     * Main event handling logic. Subclasses must implement this method
     * to define how the agent processes incoming events.
     */
    @Override
    public abstract CompletableFuture<Void> handleEvent(Event event);
    
    // Default control event handling
    
    @Override
    public CompletableFuture<Void> handleControlEvent(ControlEvent controlEvent) {
        return CompletableFuture.runAsync(() -> {
            try {
                switch (controlEvent.getCommand()) {
                    case PING:
                        handlePingCommand(controlEvent);
                        break;
                    case SHUTDOWN:
                        handleShutdownCommand(controlEvent);
                        break;
                    case STATUS_REQUEST:
                        handleStatusCommand(controlEvent);
                        break;
                    default:
                        System.err.println("Agent " + agentId + 
                                         " received unhandled control event: " + controlEvent.getCommand());
                }
            } catch (Exception e) {
                System.err.println("Agent " + agentId + 
                                 " error handling control event: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Handles PING control events by responding with PONG.
     */
    protected void handlePingCommand(ControlEvent controlEvent) {
        if (agentContext != null) {
            Event pongEvent = Event.create(
                "agent." + agentId + ".pong", 
                "pong", 
                agentId
            );
            agentContext.publishEvent(pongEvent);
        }
    }
    
    /**
     * Handles SHUTDOWN control events by cleaning up resources.
     */
    protected void handleShutdownCommand(ControlEvent controlEvent) {
        onDestroy();
        // The actual destruction is handled by the context
    }
    
    /**
     * Handles STATUS_REQUEST control events by publishing status information.
     */
    protected void handleStatusCommand(ControlEvent controlEvent) {
        if (agentContext != null) {
            AgentStatus status = new AgentStatus(
                agentId,
                agentType,
                getLifecycleState(),
                System.currentTimeMillis(),
                agentContext.getContextId()
            );
            
            Event statusEvent = Event.create(
                "agent." + agentId + ".status",
                status,
                agentId
            );
            agentContext.publishEvent(statusEvent);
        }
    }
    
    // State management for serialization
    
    @Override
    public Object captureState() {
        // Default implementation returns the agent itself
        // Subclasses with complex state should override this
        return new AgentState(agentId, agentType, getLifecycleState(), System.currentTimeMillis());
    }
    
    @Override
    public void restoreState(Object state) {
        if (state instanceof AgentState) {
            AgentState agentState = (AgentState) state;
            // Validate that this is the correct agent
            if (!agentId.equals(agentState.getAgentId())) {
                throw new IllegalArgumentException(
                    "State belongs to different agent: " + agentState.getAgentId() + 
                    " vs " + agentId
                );
            }
            // Restore lifecycle state if needed
            // (Context will handle the actual state transition)
        }
        // Default implementation does nothing else
        // Subclasses with complex state should override this
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "agentId=" + agentId +
                ", agentType='" + agentType + '\'' +
                ", lifecycleState=" + lifecycleState.get() +
                ", context=" + (agentContext != null ? agentContext.getContextId() : "null") +
                '}';
    }
}