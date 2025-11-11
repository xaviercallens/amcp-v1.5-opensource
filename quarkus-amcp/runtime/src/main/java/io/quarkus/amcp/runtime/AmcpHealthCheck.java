package io.quarkus.amcp.runtime;

import io.amcp.core.AgentContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Liveness probe for AMCP agent mesh.
 * Kubernetes uses this to determine if the pod should be restarted.
 * 
 * Endpoint: GET /q/health/live
 */
@Liveness
@ApplicationScoped
public class AmcpHealthCheck implements HealthCheck {
    
    private static final Logger logger = LoggerFactory.getLogger(AmcpHealthCheck.class);
    
    @Inject
    AgentContext agentContext;
    
    @Override
    public HealthCheckResponse call() {
        try {
            // Check if context is started and broker is running
            boolean isHealthy = agentContext != null && 
                              agentContext.isStarted() &&
                              agentContext.getEventBroker() != null &&
                              agentContext.getEventBroker().isRunning();
            
            var builder = HealthCheckResponse.named("amcp-agent-mesh")
                    .status(isHealthy);
            
            if (agentContext != null) {
                builder.withData("context_id", agentContext.getContextId());
                builder.withData("agents_active", agentContext.getAgents().size());
                builder.withData("context_started", agentContext.isStarted());
                if (agentContext.getEventBroker() != null) {
                    builder.withData("broker_running", agentContext.getEventBroker().isRunning());
                }
            } else {
                builder.withData("error", "AgentContext not initialized");
            }
            
            return builder.build();
            
        } catch (Exception e) {
            logger.error("Health check failed", e);
            return HealthCheckResponse.named("amcp-agent-mesh")
                    .down()
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}
