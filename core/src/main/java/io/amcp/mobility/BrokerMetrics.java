package io.amcp.mobility;

/**
 * Metrics and statistics for broker operations.
 * 
 * @author AMCP Development Team
 * @version 1.4.0
 */
public interface BrokerMetrics {
    
    /**
     * Get the total number of events published.
     * 
     * @return the total events published
     */
    long getTotalEventsPublished();
    
    /**
     * Get the total number of events delivered.
     * 
     * @return the total events delivered
     */
    long getTotalEventsDelivered();
    
    /**
     * Get the current number of active subscriptions.
     * 
     * @return the active subscription count
     */
    int getActiveSubscriptions();
    
    /**
     * Get the current number of connected agents.
     * 
     * @return the connected agent count
     */
    int getConnectedAgents();
    
    /**
     * Get the average event processing time in milliseconds.
     * 
     * @return the average processing time
     */
    double getAverageProcessingTime();
    
    /**
     * Get the number of failed event deliveries.
     * 
     * @return the failed delivery count
     */
    long getFailedDeliveries();
}