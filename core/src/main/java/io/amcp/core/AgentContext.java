package io.amcp.core;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * The runtime environment for agents in the AMCP system.
 * AgentContext provides the execution sandbox and interface to the event mesh.
 * Each context typically runs as a separate microservice or container.
 * 
 * @since AMCP 1.0
 */
public interface AgentContext {
    
    /**
     * Gets the unique identifier for this context.
     */
    String getContextId();
    
    /**
     * Gets the name of this context (human-readable).
     */
    String getContextName();
    
    /**
     * Checks if this context is currently active and accepting agents.
     */
    boolean isActive();
    
    // Agent lifecycle management
    
    /**
     * Creates a new agent of the specified type in this context.
     * 
     * @param agentType the type of agent to create
     * @param initData optional initialization data
     * @return CompletableFuture containing the created agent
     */
    CompletableFuture<Agent> createAgent(String agentType, Object initData);
    
    /**
     * Creates an agent with a specific ID (used during migration/cloning).
     * 
     * @param agentId the specific agent ID to use
     * @param agentType the type of agent to create
     * @param initData optional initialization data
     * @return CompletableFuture containing the created agent
     */
    CompletableFuture<Agent> createAgent(AgentID agentId, String agentType, Object initData);
    
    /**
     * Activates an inactive agent in this context.
     * 
     * @param agentId the ID of the agent to activate
     * @return CompletableFuture that completes when activation is done
     */
    CompletableFuture<Void> activateAgent(AgentID agentId);
    
    /**
     * Deactivates an active agent (but preserves its state).
     * 
     * @param agentId the ID of the agent to deactivate
     * @return CompletableFuture that completes when deactivation is done
     */
    CompletableFuture<Void> deactivateAgent(AgentID agentId);
    
    /**
     * Clones an existing agent, creating a copy with a new ID.
     * 
     * @param sourceAgentId the ID of the agent to clone
     * @return CompletableFuture containing the cloned agent
     */
    CompletableFuture<Agent> cloneAgent(AgentID sourceAgentId);
    
    /**
     * Dispatches (migrates) an agent to another context.
     * 
     * @param agentId the ID of the agent to migrate
     * @param destinationContext the target context
     * @return CompletableFuture that completes when migration starts
     */
    CompletableFuture<Void> dispatchAgent(AgentID agentId, String destinationContext);
    
    /**
     * Receives an agent from another context (completes migration).
     * 
     * @param agentId the ID of the incoming agent
     * @param agentType the type of the agent
     * @param agentState the serialized agent state
     * @param sourceContext the context the agent came from
     * @return CompletableFuture containing the received agent
     */
    CompletableFuture<Agent> receiveAgent(AgentID agentId, String agentType, 
                                        Object agentState, String sourceContext);
    
    /**
     * Destroys an agent permanently.
     * 
     * @param agentId the ID of the agent to destroy
     * @return CompletableFuture that completes when destruction is done
     */
    CompletableFuture<Void> destroyAgent(AgentID agentId);
    
    // Agent access
    
    /**
     * Gets an agent by ID if it exists in this context.
     * 
     * @param agentId the agent ID to look for
     * @return the agent, or null if not found in this context
     */
    Agent getAgent(AgentID agentId);
    
    /**
     * Gets all agents currently hosted in this context.
     * 
     * @return collection of agents in this context
     */
    Collection<Agent> getAllAgents();
    
    /**
     * Gets agents of a specific type in this context.
     * 
     * @param agentType the type to filter by
     * @return collection of matching agents
     */
    Collection<Agent> getAgentsByType(String agentType);
    
    // Event/messaging operations
    
    /**
     * Publishes an event to the event mesh.
     * 
     * @param event the event to publish
     * @return CompletableFuture that completes when the event is sent
     */
    CompletableFuture<Void> publishEvent(Event event);
    
    /**
     * Subscribes an agent to a topic pattern.
     * 
     * @param agentId the agent making the subscription
     * @param topicPattern the topic pattern to subscribe to
     */
    void subscribe(AgentID agentId, String topicPattern);
    
    /**
     * Unsubscribes an agent from a topic pattern.
     * 
     * @param agentId the agent removing the subscription
     * @param topicPattern the topic pattern to unsubscribe from
     */
    void unsubscribe(AgentID agentId, String topicPattern);
    
    /**
     * Gets all topic patterns an agent is subscribed to.
     * 
     * @param agentId the agent to check
     * @return collection of topic patterns
     */
    Collection<String> getSubscriptions(AgentID agentId);
    
    // Control operations
    
    /**
     * Sends a control event to an agent (local or remote).
     * 
     * @param controlEvent the control event to send
     * @return CompletableFuture that completes when the event is sent
     */
    CompletableFuture<Void> sendControlEvent(ControlEvent controlEvent);
    
    /**
     * Starts this context and begins processing events.
     * 
     * @return CompletableFuture that completes when the context is started
     */
    CompletableFuture<Void> start();
    
    /**
     * Stops this context and all its agents gracefully.
     * 
     * @return CompletableFuture that completes when shutdown is complete
     */
    CompletableFuture<Void> shutdown();
    
    // Configuration and metadata
    
    /**
     * Gets a configuration property for this context.
     * 
     * @param key the property key
     * @return the property value, or null if not found
     */
    Object getProperty(String key);
    
    /**
     * Sets a configuration property for this context.
     * 
     * @param key the property key
     * @param value the property value
     */
    void setProperty(String key, Object value);
    
    /**
     * Gets context metadata (read-only information about the context).
     * 
     * @return metadata map
     */
    java.util.Map<String, Object> getMetadata();
}