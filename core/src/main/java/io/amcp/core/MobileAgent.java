package io.amcp.core;

import io.amcp.mobility.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Extended agent interface supporting IBM Aglet-style strong mobility operations.
 * 
 * <p>This interface extends the base {@link Agent} interface to provide sophisticated
 * mobility operations inspired by IBM's Aglet technology. MobileAgents can move
 * between execution contexts while preserving their state, code, and identity.</p>
 * 
 * <p>Strong mobility operations supported:
 * <ul>
 *   <li><strong>dispatch</strong> - Move agent to remote context</li>
 *   <li><strong>clone</strong> - Create copy in remote context</li>
 *   <li><strong>retract</strong> - Recall agent from remote context</li>
 *   <li><strong>migrate</strong> - Intelligent migration with load balancing</li>
 *   <li><strong>replicate</strong> - High-availability replication across contexts</li>
 *   <li><strong>federate</strong> - Form collaborative agent federations</li>
 * </ul>
 * </p>
 * 
 * <p>All mobility operations are asynchronous and include comprehensive error handling,
 * authentication context propagation, and state consistency guarantees.</p>
 * 
 * @author AMCP Development Team
 * @version 1.4.0
 * @since 1.4.0
 */
public interface MobileAgent extends Agent {

    /**
     * Dispatches this agent to a remote execution context.
     * 
     * <p>The agent is moved from its current context to the specified destination context,
     * carrying both its code and current state. The original agent instance is destroyed
     * after successful migration.</p>
     * 
     * <p>This operation is atomic - either the agent is successfully moved to the
     * destination context, or it remains in the original context unchanged.</p>
     * 
     * <p>Example usage:
     * <pre>{@code
     * // Move to edge device for local processing
     * dispatch("edge-device-paris")
     *     .thenRun(() -> log.info("Successfully moved to edge device"))
     *     .exceptionally(throwable -> {
     *         log.error("Failed to dispatch: {}", throwable.getMessage());
     *         return null;
     *     });
     * }</pre>
     * 
     * @param destinationContext the target context identifier
     * @return CompletableFuture that completes when dispatch is successful
     * @throws MobilityException if the destination context is unreachable or invalid
     * @throws SecurityException if authentication fails for the destination context
     */
    CompletableFuture<Void> dispatch(String destinationContext);

    /**
     * Creates a clone of this agent in a remote execution context.
     * 
     * <p>A new agent instance is created in the destination context with the same
     * code, state, and configuration as this agent. Both the original and cloned
     * agents continue to execute independently.</p>
     * 
     * <p>The cloned agent receives a new unique AgentID but maintains a reference
     * to the original agent for coordination purposes.</p>
     * 
     * @param destinationContext the target context for the clone
     * @return CompletableFuture containing the AgentID of the created clone
     * @throws MobilityException if cloning fails due to context or serialization issues
     */
    CompletableFuture<AgentID> clone(String destinationContext);

    /**
     * Retracts (recalls) this agent from a remote context back to the local context.
     * 
     * <p>This operation is typically used to bring back agents that were previously
     * dispatched to remote contexts. The agent is moved from the specified source
     * context to the current context.</p>
     * 
     * @param sourceContext the context from which to retract the agent
     * @return CompletableFuture that completes when retraction is successful
     * @throws MobilityException if the agent is not found in the source context
     */
    CompletableFuture<Void> retract(String sourceContext);

    /**
     * Performs intelligent migration based on specified options.
     * 
     * <p>This is a more sophisticated version of dispatch() that considers factors
     * like load balancing, resource availability, network proximity, and cost
     * optimization when selecting the optimal destination context.</p>
     * 
     * @param options migration preferences and constraints
     * @return CompletableFuture that completes when migration is successful
     */
    CompletableFuture<Void> migrate(MigrationOptions options);

    /**
     * Creates replicas of this agent across multiple contexts for high availability.
     * 
     * <p>Multiple identical copies of the agent are created across the specified
     * contexts. These replicas can be used for:
     * <ul>
     *   <li>Fault tolerance - if one replica fails, others continue operation</li>
     *   <li>Load distribution - requests can be distributed across replicas</li>
     *   <li>Geographic distribution - replicas closer to clients for better performance</li>
     * </ul>
     * </p>
     * 
     * @param contexts the target contexts for replication
     * @return CompletableFuture containing the AgentIDs of all created replicas
     */
    CompletableFuture<List<AgentID>> replicate(String... contexts);

    /**
     * Forms a federation with other agents for collaborative processing.
     * 
     * <p>Agent federations enable coordinated behavior across multiple agent instances.
     * Federated agents can share state, coordinate actions, and distribute workload
     * while maintaining their individual identities.</p>
     * 
     * <p>Example use cases:
     * <ul>
     *   <li>Distributed computation across geographic regions</li>
     *   <li>Collaborative planning with specialized agents</li>
     *   <li>Redundant processing with consensus mechanisms</li>
     * </ul>
     * </p>
     * 
     * @param agents the list of agents to federate with
     * @param federationId unique identifier for this federation
     * @return CompletableFuture that completes when federation is established
     */
    CompletableFuture<Void> federateWith(List<AgentID> agents, String federationId);

    /**
     * Gets the current mobility state of this agent.
     * 
     * @return the current mobility state
     */
    MobilityState getMobilityState();

    /**
     * Gets the migration history for this agent.
     * 
     * @return list of migration events in chronological order
     */
    List<MigrationEvent> getMigrationHistory();

    /**
     * Checks if this agent can migrate to the specified context.
     * 
     * <p>Performs pre-flight checks including:
     * <ul>
     *   <li>Context availability and compatibility</li>
     *   <li>Authentication and authorization</li>
     *   <li>Resource requirements</li>
     *   <li>Network connectivity</li>
     * </ul>
     * </p>
     * 
     * @param targetContext the context to check migration feasibility
     * @return CompletableFuture containing the mobility assessment result
     */
    CompletableFuture<MobilityAssessment> assessMobility(String targetContext);

    /**
     * Sets the mobility strategy for this agent.
     * 
     * <p>The mobility strategy defines the agent's behavior regarding:
     * <ul>
     *   <li>When to migrate (triggers and conditions)</li>
     *   <li>Where to migrate (context selection algorithms)</li>
     *   <li>How to migrate (serialization, security, error handling)</li>
     * </ul>
     * </p>
     * 
     * @param strategy the mobility strategy to apply
     */
    void setMobilityStrategy(MobilityStrategy strategy);

    /**
     * Gets the current mobility strategy for this agent.
     * 
     * @return the current mobility strategy
     */
    MobilityStrategy getMobilityStrategy();

}