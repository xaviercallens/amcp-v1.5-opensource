package io.amcp.mobility;

import io.amcp.core.AgentID;

/**
 * Mobility manager interface.
 */
public interface MobilityManager {
    /**
     * Check if migration is possible.
     */
    boolean canMigrate(AgentID agentId, String targetContext);
    
    /**
     * Execute migration.
     */
    boolean migrate(AgentID agentId, String targetContext, MigrationOptions options);
}