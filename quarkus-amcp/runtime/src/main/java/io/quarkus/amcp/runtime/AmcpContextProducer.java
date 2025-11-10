package io.quarkus.amcp.runtime;

import io.amcp.core.AgentContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * CDI producer for the AgentContext.
 * Allows injection of AgentContext into application code.
 */
@ApplicationScoped
public class AmcpContextProducer {

    private volatile AgentContext context;

    /**
     * Sets the agent context. Called by the framework during initialization.
     *
     * @param context the agent context
     */
    public void setContext(AgentContext context) {
        this.context = context;
    }

    /**
     * Produces the AgentContext for CDI injection.
     *
     * @return the agent context
     */
    @Produces
    @Singleton
    public AgentContext produceAgentContext() {
        if (context == null) {
            throw new IllegalStateException("AgentContext not initialized");
        }
        return context;
    }
}
