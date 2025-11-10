package io.amcp.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Base class for mobile agents in AMCP.
 * Provides lifecycle management, event subscription, and logging utilities.
 */
public abstract class AbstractMobileAgent {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    private AgentContext context;
    private String agentId;
    private AgentState state = AgentState.CREATED;
    private final List<String> subscriptions = new ArrayList<>();

    /**
     * Agent lifecycle states.
     */
    public enum AgentState {
        CREATED,
        ACTIVATING,
        ACTIVE,
        DEACTIVATING,
        DEACTIVATED
    }

    /**
     * Sets the agent context. Called by the framework.
     *
     * @param context the agent context
     */
    public void setContext(AgentContext context) {
        this.context = context;
    }

    /**
     * Gets the agent context.
     *
     * @return the agent context
     */
    protected AgentContext getContext() {
        return context;
    }

    /**
     * Sets the agent ID. Called by the framework.
     *
     * @param agentId the agent ID
     */
    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    /**
     * Gets the agent ID.
     *
     * @return the agent ID
     */
    public String getAgentId() {
        return agentId;
    }

    /**
     * Gets the current agent state.
     *
     * @return the agent state
     */
    public AgentState getState() {
        return state;
    }

    /**
     * Called when the agent is activated.
     * Subclasses should override this to perform initialization.
     */
    public void onActivate() {
        state = AgentState.ACTIVATING;
        logger.info("Agent {} activating", agentId);
        state = AgentState.ACTIVE;
    }

    /**
     * Called when the agent is deactivated.
     * Subclasses should override this to perform cleanup.
     */
    public void onDeactivate() {
        state = AgentState.DEACTIVATING;
        logger.info("Agent {} deactivating", agentId);
        
        // Unsubscribe from all topics
        synchronized (subscriptions) {
            for (String subId : new ArrayList<>(subscriptions)) {
                context.getEventBroker().unsubscribe(subId);
            }
            subscriptions.clear();
        }
        
        state = AgentState.DEACTIVATED;
    }

    /**
     * Subscribes to events matching the specified topic pattern.
     *
     * @param topicPattern the topic pattern (supports wildcards like "hello.*")
     */
    protected void subscribe(String topicPattern) {
        if (context == null) {
            throw new IllegalStateException("Agent context not set");
        }
        
        String subId = context.getEventBroker().subscribe(topicPattern, event -> {
            handleEventSafely(event);
        });
        
        synchronized (subscriptions) {
            subscriptions.add(subId);
        }
        
        logger.debug("Agent {} subscribed to {}", agentId, topicPattern);
    }

    /**
     * Publishes an event to the specified topic.
     *
     * @param topic   the event topic
     * @param payload the event payload
     */
    protected void publishEvent(String topic, Object payload) {
        if (context == null) {
            throw new IllegalStateException("Agent context not set");
        }
        
        Event event = Event.create(topic, "https://amcp.dev/agents/" + agentId, payload);
        context.getEventBroker().publish(event);
        
        logger.debug("Agent {} published event to {}", agentId, topic);
    }

    /**
     * Logs a message with the agent ID prefix.
     *
     * @param message the message to log
     */
    protected void logMessage(String message) {
        logger.info("[{}] {}", agentId, message);
    }

    /**
     * Handles an incoming event safely, catching any exceptions.
     *
     * @param event the event to handle
     */
    private void handleEventSafely(Event event) {
        try {
            handleEvent(event).exceptionally(ex -> {
                logger.error("Error handling event in agent {}: {}", agentId, ex.getMessage(), ex);
                return null;
            });
        } catch (Exception e) {
            logger.error("Error handling event in agent {}: {}", agentId, e.getMessage(), e);
        }
    }

    /**
     * Handles an incoming event.
     * Subclasses must implement this to process events.
     *
     * @param event the event to handle
     * @return a CompletableFuture that completes when the event is handled
     */
    public abstract CompletableFuture<Void> handleEvent(Event event);

    /**
     * Gets the agent class name.
     *
     * @return the simple class name
     */
    public String getAgentClassName() {
        return getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return String.format("%s{id=%s, state=%s}", 
                getAgentClassName(), agentId, state);
    }
}
