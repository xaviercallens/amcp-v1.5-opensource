package io.quarkus.amcp.runtime;

import io.amcp.core.AgentContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Readiness probe for AMCP agent mesh.
 * Kubernetes uses this to determine if the pod is ready to receive traffic.
 * 
 * Endpoint: GET /q/health/ready
 */
@Readiness
@ApplicationScoped
public class AmcpReadinessCheck implements HealthCheck {
    
    private static final Logger logger = LoggerFactory.getLogger(AmcpReadinessCheck.class);
    
    @Inject
    AgentContext agentContext;
    
    @Override
    public HealthCheckResponse call() {
        try {
            // Check if mesh is ready to receive traffic
            // Requires: context started, broker running, agents registered
            int agentCount = agentContext != null ? agentContext.getAgents().size() : 0;
            boolean brokerRunning = agentContext != null && 
                                   agentContext.getEventBroker() != null && 
                                   agentContext.getEventBroker().isRunning();
            
            boolean isReady = agentContext != null && 
                            agentContext.isStarted() &&
                            brokerRunning &&
                            agentCount > 0;
            
            var builder = HealthCheckResponse.named("amcp-agent-mesh-ready")
                    .status(isReady);
            
            if (agentContext != null) {
                builder.withData("agents_registered", agentCount);
                builder.withData("broker_running", brokerRunning);
                builder.withData("context_started", agentContext.isStarted());
                builder.withData("ready_for_traffic", isReady);
                
                if (!isReady) {
                    if (agentCount == 0) {
                        builder.withData("reason", "No agents registered");
                    } else if (!agentContext.isStarted()) {
                        builder.withData("reason", "Context not started");
                    } else if (!brokerRunning) {
                        builder.withData("reason", "Broker not running");
                    }
                }
            } else {
                builder.withData("error", "AgentContext not initialized");
            }
            
            return builder.build();
            
        } catch (Exception e) {
            logger.error("Readiness check failed", e);
            return HealthCheckResponse.named("amcp-agent-mesh-ready")
                    .down()
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}
