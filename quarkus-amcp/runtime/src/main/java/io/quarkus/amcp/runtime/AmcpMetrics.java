package io.quarkus.amcp.runtime;

import io.amcp.core.AgentContext;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prometheus metrics for AMCP agent mesh.
 * Exposes metrics via GET /q/metrics endpoint.
 * 
 * Metrics:
 * - amcp_agents_total: Total number of registered agents
 * - amcp_broker_connected: Broker connection status (1=connected, 0=disconnected)
 * - amcp_mesh_running: Mesh running status (1=running, 0=stopped)
 */
@ApplicationScoped
public class AmcpMetrics {
    
    private static final Logger logger = LoggerFactory.getLogger(AmcpMetrics.class);
    
    @Inject
    AgentContext agentContext;
    
    @Inject
    MeterRegistry meterRegistry;
    
    public void init() {
        // Register gauges programmatically
        Gauge.builder("amcp_agents_total", this::getAgentCount)
                .description("Total number of registered agents in the AMCP mesh")
                .baseUnit("agents")
                .register(meterRegistry);
        
        Gauge.builder("amcp_broker_connected", this::getBrokerStatus)
                .description("AMCP broker connection status (1 = connected, 0 = disconnected)")
                .baseUnit("status")
                .register(meterRegistry);
        
        Gauge.builder("amcp_mesh_running", this::getMeshStatus)
                .description("AMCP mesh running status (1 = running, 0 = stopped)")
                .baseUnit("status")
                .register(meterRegistry);
    }
    
    /**
     * Total number of registered agents in the AMCP mesh.
     */
    public long getAgentCount() {
        try {
            return agentContext != null ? agentContext.getAgents().size() : 0;
        } catch (Exception e) {
            logger.warn("Error getting agent count", e);
            return 0;
        }
    }
    
    /**
     * AMCP broker connection status.
     * 1 = connected, 0 = disconnected
     */
    public long getBrokerStatus() {
        try {
            if (agentContext != null && 
                agentContext.getEventBroker() != null && 
                agentContext.getEventBroker().isRunning()) {
                return 1;
            }
            return 0;
        } catch (Exception e) {
            logger.warn("Error getting broker status", e);
            return 0;
        }
    }
    
    /**
     * AMCP mesh running status.
     * 1 = running, 0 = stopped
     */
    public long getMeshStatus() {
        try {
            if (agentContext != null && agentContext.isStarted()) {
                return agentContext.getEventBroker() != null && agentContext.getEventBroker().isRunning() ? 1 : 0;
            }
            return 0;
        } catch (Exception e) {
            logger.warn("Error getting mesh status", e);
            return 0;
        }
    }
}
