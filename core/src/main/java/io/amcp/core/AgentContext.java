package io.amcp.core;

import io.amcp.messaging.EventBroker;
import io.amcp.mobility.MobilityManager;
import io.amcp.mobility.MigrationOptions;
import java.util.concurrent.CompletableFuture;

/**
 * Agent execution context providing access to platform services and lifecycle management.
 * 
 * <p>The AgentContext serves as the primary interface between agents and the AMCP platform,
 * providing access to messaging, mobility, security, and other core services. Each agent
 * is associated with exactly one context at any given time.</p>
 * 
 * <p>Key responsibilities:
 * <ul>
 *   <li>Agent lifecycle management (activation, deactivation, destruction)</li>
 *   <li>Event routing and subscription management</li>
 *   <li>Mobility operation coordination</li>
 *   <li>Security context propagation</li>
 *   <li>Resource management and cleanup</li>
 * </ul>
 * </p>
 * 
 * @author AMCP Development Team
 * @version 1.4.0
 * @since 1.0.0
 */
public interface AgentContext {

    /**
     * Gets the unique identifier for this context.
     * 
     * @return the context identifier
     */
    String getContextId();

    /**
     * Gets the EventBroker instance for this context.
     * 
     * @return the messaging EventBroker
     */
    EventBroker getEventBroker();

    /**
     * Gets the MobilityManager for agent migration operations.
     * 
     * @return the mobility manager
     */
    MobilityManager getMobilityManager();

    /**
     * Gets the AdvancedSecurityManager for authentication and authorization.
     * 
     * @return the advanced security manager
     */
    io.amcp.security.AdvancedSecurityManager getAdvancedSecurityManager();

    /**
     * Registers an agent with this context.
     * 
     * <p>This method initializes the agent, assigns it to this context,
     * and transitions it to the INACTIVE state. The agent becomes eligible
     * for activation after successful registration.</p>
     * 
     * @param agent the agent to register
     * @return CompletableFuture that completes when registration is successful
     * @throws IllegalStateException if the agent is already registered
     */
    CompletableFuture<Void> registerAgent(Agent agent);

    /**
     * Unregisters an agent from this context.
     * 
     * <p>This method removes the agent from this context, performing cleanup
     * and transitioning the agent to the DESTROYED state.</p>
     * 
     * @param agentId the ID of the agent to unregister
     * @return CompletableFuture that completes when unregistration is successful
     */
    CompletableFuture<Void> unregisterAgent(AgentID agentId);

    /**
     * Activates an agent, transitioning it to the ACTIVE state.
     * 
     * <p>Activated agents begin processing events and can perform mobility
     * operations. The agent's {@link Agent#onActivate()} method is called
     * during this transition.</p>
     * 
     * @param agentId the ID of the agent to activate
     * @return CompletableFuture that completes when activation is successful
     */
    CompletableFuture<Void> activateAgent(AgentID agentId);

    /**
     * Deactivates an agent, transitioning it to the INACTIVE state.
     * 
     * <p>Deactivated agents stop processing events but remain registered
     * with the context. The agent's {@link Agent#onDeactivate()} method
     * is called during this transition.</p>
     * 
     * @param agentId the ID of the agent to deactivate
     * @return CompletableFuture that completes when deactivation is successful
     */
    CompletableFuture<Void> deactivateAgent(AgentID agentId);

    /**
     * Gets an agent by its ID.
     * 
     * @param agentId the agent ID
     * @return the agent instance, or null if not found in this context
     */
    Agent getAgent(AgentID agentId);

    /**
     * Checks if an agent is registered in this context.
     * 
     * @param agentId the agent ID to check
     * @return true if the agent is registered in this context
     */
    boolean hasAgent(AgentID agentId);

    /**
     * Gets the current lifecycle state of an agent.
     * 
     * @param agentId the agent ID
     * @return the current lifecycle state, or null if agent not found
     */
    AgentLifecycle getAgentState(AgentID agentId);

    /**
     * Routes an event to the appropriate agents.
     * 
     * <p>This method is typically called by the EventBroker to deliver
     * events to agents based on their subscriptions.</p>
     * 
     * @param event the event to route
     * @return CompletableFuture that completes when routing is finished
     */
    CompletableFuture<Void> routeEvent(Event event);

    /**
     * Publishes an event through the EventBroker.
     * 
     * @param event the event to publish
     * @return CompletableFuture that completes when publishing is finished
     */
    CompletableFuture<Void> publishEvent(Event event);

    /**
     * Subscribes an agent to events matching the given topic pattern.
     * 
     * @param agentId the subscribing agent
     * @param topicPattern the topic pattern to subscribe to
     * @return CompletableFuture that completes when subscription is established
     */
    CompletableFuture<Void> subscribe(AgentID agentId, String topicPattern);

    /**
     * Unsubscribes an agent from events matching the given topic pattern.
     * 
     * @param agentId the unsubscribing agent
     * @param topicPattern the topic pattern to unsubscribe from
     * @return CompletableFuture that completes when unsubscription is finished
     */
    CompletableFuture<Void> unsubscribe(AgentID agentId, String topicPattern);

    /**
     * Initiates agent migration to another context.
     * 
     * @param agentId the agent to migrate
     * @param destinationContext the target context
     * @param options migration options and preferences
     * @return CompletableFuture that completes when migration is successful
     */
    CompletableFuture<Void> migrateAgent(AgentID agentId, String destinationContext, MigrationOptions options);

    /**
     * Receives a migrating agent from another context.
     * 
     * @param serializedAgent the serialized agent state
     * @param sourceContext the origin context
     * @return CompletableFuture that completes when agent is successfully received
     */
    CompletableFuture<AgentID> receiveAgent(byte[] serializedAgent, String sourceContext);

    /**
     * Gets context-level metrics and statistics.
     * 
     * @return context metrics including agent counts, event rates, etc.
     */
    ContextMetrics getMetrics();

    /**
     * Performs graceful shutdown of this context.
     * 
     * <p>This method deactivates all agents, processes any pending events,
     * and releases resources associated with this context.</p>
     * 
     * @return CompletableFuture that completes when shutdown is finished
     */
    CompletableFuture<Void> shutdown();

}