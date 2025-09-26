package io.amcp.mobility;

/**
 * Simple mobility strategy implementation.
 */
public class SimpleMobilityStrategy implements MobilityStrategy {
    private final String name;
    
    public SimpleMobilityStrategy(String name) {
        this.name = name;
    }
    
    @Override
    public boolean shouldMigrate(String targetContext) {
        return true; // Simple strategy always allows migration
    }
    
    @Override
    public String getStrategyName() {
        return name;
    }
}