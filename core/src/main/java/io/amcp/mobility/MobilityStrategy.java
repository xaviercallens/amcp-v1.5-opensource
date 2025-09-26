package io.amcp.mobility;

import java.io.Serializable;

/**
 * Strategy for mobility operations.
 */
public interface MobilityStrategy extends Serializable {
    /**
     * Determine if migration is recommended.
     */
    boolean shouldMigrate(String targetContext);
    
    /**
     * Get strategy name.
     */
    String getStrategyName();
}