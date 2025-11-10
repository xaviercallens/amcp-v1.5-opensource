package io.amcp.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context for managing agents and their lifecycle.
 * Provides access to the event broker and agent registry.
 */
public class AgentContext {
    private static final Logger logger = LoggerFactory.getLogger(AgentContext.class);
    
    private final EventBroker eventBroker;
    private final Map<String, AbstractMobileAgent> agents = new ConcurrentHashMap<>();
    private final String contextId;
    private boolean started = false;

    /**
     * Creates a new AgentContext with the specified event broker.
     *
     * @param contextId   the context ID
     * @param eventBroker the event broker
     */
    public AgentContext(String contextId, EventBroker eventBroker) {
        this.contextId = contextId;
        this.eventBroker = eventBroker;
    }

    /**
     * Boots a new AgentContext with the specified broker.
     *
     * @param eventBroker the event broker
     * @return a new AgentContext
     */
    public static AgentContext boot(EventBroker eventBroker) {
        String contextId = "context-" + System.currentTimeMillis();
        return new AgentContext(contextId, eventBroker);
    }

    /**
     * Gets the context ID.
     *
     * @return the context ID
     */
    public String getContextId() {
        return contextId;
    }

    /**
     * Gets the event broker.
     *
     * @return the event broker
     */
    public EventBroker getEventBroker() {
        return eventBroker;
    }

    /**
     * Registers an agent with this context.
     *
     * @param agent the agent to register
     * @return the agent ID
     */
    public String registerAgent(AbstractMobileAgent agent) {
        String agentId = agent.getClass().getSimpleName() + "-" + System.currentTimeMillis();
        agent.setAgentId(agentId);
        agent.setContext(this);
        
        agents.put(agentId, agent);
        logger.info("Registered agent: {}", agentId);
        
        return agentId;
    }

    /**
     * Activates an agent by its ID.
     *
     * @param agentId the agent ID
     */
    public void activateAgent(String agentId) {
        AbstractMobileAgent agent = agents.get(agentId);
        if (agent == null) {
            throw new IllegalArgumentException("Agent not found: " + agentId);
        }
        
        agent.onActivate();
        logger.info("Activated agent: {}", agentId);
    }

    /**
     * Deactivates an agent by its ID.
     *
     * @param agentId the agent ID
     */
    public void deactivateAgent(String agentId) {
        AbstractMobileAgent agent = agents.get(agentId);
        if (agent == null) {
            throw new IllegalArgumentException("Agent not found: " + agentId);
        }
        
        agent.onDeactivate();
        logger.info("Deactivated agent: {}", agentId);
    }

    /**
     * Gets an agent by its ID.
     *
     * @param agentId the agent ID
     * @return the agent, or null if not found
     */
    public AbstractMobileAgent getAgent(String agentId) {
        return agents.get(agentId);
    }

    /**
     * Gets all registered agents.
     *
     * @return a map of agent IDs to agents
     */
    public Map<String, AbstractMobileAgent> getAgents() {
        return Map.copyOf(agents);
    }

    /**
     * Starts the agent context (starts the event broker).
     *
     * @return a CompletableFuture that completes when started
     */
    public CompletableFuture<Void> start() {
        if (started) {
            return CompletableFuture.completedFuture(null);
        }
        
        return eventBroker.start().thenRun(() -> {
            started = true;
            logger.info("AgentContext {} started", contextId);
        });
    }

    /**
     * Stops the agent context (deactivates all agents and stops the broker).
     *
     * @return a CompletableFuture that completes when stopped
     */
    public CompletableFuture<Void> stop() {
        if (!started) {
            return CompletableFuture.completedFuture(null);
        }
        
        // Deactivate all agents
        for (String agentId : agents.keySet()) {
            try {
                deactivateAgent(agentId);
            } catch (Exception e) {
                logger.error("Error deactivating agent {}: {}", agentId, e.getMessage(), e);
            }
        }
        
        // Stop the broker
        return eventBroker.stop().thenRun(() -> {
            started = false;
            logger.info("AgentContext {} stopped", contextId);
        });
    }

    /**
     * Checks if the context is started.
     *
     * @return true if started
     */
    public boolean isStarted() {
        return started;
    }

    @Override
    public String toString() {
        return String.format("AgentContext{id=%s, agents=%d, started=%s}",
                contextId, agents.size(), started);
    }
}
