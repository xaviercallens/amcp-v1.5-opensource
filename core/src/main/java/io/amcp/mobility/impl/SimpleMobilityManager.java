package io.amcp.mobility.impl;

import io.amcp.core.AgentID;
import io.amcp.mobility.MobilityManager;
import io.amcp.mobility.MigrationOptions;

/**
 * Simple implementation of MobilityManager for AMCP v1.5 Enterprise Edition.
 * 
 * <p>This implementation provides basic mobility functionality suitable for
 * single-node deployments and testing scenarios. In this simple implementation,
 * mobility operations always return false since no actual migration is performed.</p>
 */
public class SimpleMobilityManager implements MobilityManager {
    
    @Override
    public boolean canMigrate(AgentID agentId, String targetContext) {
        // Simple implementation - no migration support
        return false;
    }
    
    @Override
    public boolean migrate(AgentID agentId, String targetContext, MigrationOptions options) {
        // Simple implementation - no migration support
        return false;
    }
}