package io.amcp.core;

/**
 * Context metrics for monitoring agent context performance.
 * 
 * @author AMCP Development Team
 * @version 1.4.0
 */
public interface ContextMetrics {
    
    /**
     * Get the number of active agents in this context.
     * 
     * @return the active agent count
     */
    int getActiveAgentCount();
    
    /**
     * Get the total number of agents created.
     * 
     * @return the total agents created
     */
    long getTotalAgentsCreated();
    
    /**
     * Get the total number of events processed.
     * 
     * @return the total events processed
     */
    long getTotalEventsProcessed();
    
    /**
     * Get the average event processing time in milliseconds.
     * 
     * @return the average processing time
     */
    double getAverageEventProcessingTime();
    
    /**
     * Get the number of failed operations.
     * 
     * @return the failed operation count
     */
    long getFailedOperations();
    
    /**
     * Get the context uptime in milliseconds.
     * 
     * @return the uptime
     */
    long getUptimeMillis();
}

/**
 * Simple implementation of ContextMetrics.
 */
class SimpleContextMetrics implements ContextMetrics {
    private int activeAgentCount;
    private long totalAgentsCreated;
    private long totalEventsProcessed;
    private double averageEventProcessingTime;
    private long failedOperations;
    private final long startTime;
    
    public SimpleContextMetrics() {
        this.startTime = System.currentTimeMillis();
    }
    
    @Override
    public int getActiveAgentCount() {
        return activeAgentCount;
    }
    
    @Override
    public long getTotalAgentsCreated() {
        return totalAgentsCreated;
    }
    
    @Override
    public long getTotalEventsProcessed() {
        return totalEventsProcessed;
    }
    
    @Override
    public double getAverageEventProcessingTime() {
        return averageEventProcessingTime;
    }
    
    @Override
    public long getFailedOperations() {
        return failedOperations;
    }
    
    @Override
    public long getUptimeMillis() {
        return System.currentTimeMillis() - startTime;
    }
    
    // Package-private setters for updating metrics
    void setActiveAgentCount(int count) {
        this.activeAgentCount = count;
    }
    
    void incrementTotalAgentsCreated() {
        this.totalAgentsCreated++;
    }
    
    void incrementTotalEventsProcessed() {
        this.totalEventsProcessed++;
    }
    
    void updateAverageEventProcessingTime(double time) {
        this.averageEventProcessingTime = time;
    }
    
    void incrementFailedOperations() {
        this.failedOperations++;
    }
}