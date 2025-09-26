package io.amcp.mobility;

/**
 * Enumeration of mobility states for agents in the AMCP v1.4 framework.
 * 
 * @author AMCP Development Team
 * @version 1.4.0
 * @since 1.4.0
 */
public enum MobilityState {
    STATIONARY,
    PREPARING_MIGRATION,
    MIGRATING,
    CLONING,
    REPLICATING
}