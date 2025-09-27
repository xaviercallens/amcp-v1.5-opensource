package io.amcp.messaging.impl;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Enhanced metrics collection for Kafka EventBroker.
 * 
 * <p>Provides comprehensive monitoring and observability for production deployments.</p>
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class EnhancedKafkaMetrics {
    
    // Global counters
    private final AtomicLong totalPublishedEvents = new AtomicLong(0);
    private final AtomicLong totalConsumedEvents = new AtomicLong(0);
    private final AtomicLong totalPublishErrors = new AtomicLong(0);
    private final AtomicLong totalConsumerErrors = new AtomicLong(0);
    private final AtomicLong totalDeliveryErrors = new AtomicLong(0);
    
    // Topic-specific metrics
    private final Map<String, AtomicLong> topicPublishCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> topicConsumeCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> topicErrorCounts = new ConcurrentHashMap<>();
    
    // Partition metrics
    private final Map<Integer, AtomicLong> partitionCounts = new ConcurrentHashMap<>();
    
    // Timing metrics
    private volatile long lastPublishTime = System.currentTimeMillis();
    private volatile long lastConsumeTime = System.currentTimeMillis();
    
    // Health metrics
    private final AtomicLong healthDegradationCount = new AtomicLong(0);
    private final AtomicLong healthRecoveryCount = new AtomicLong(0);
    private final AtomicLong circuitBreakerOpenCount = new AtomicLong(0);
    
    // Subscription metrics
    private final AtomicLong activeSubscriptions = new AtomicLong(0);
    private final AtomicLong totalSubscriptions = new AtomicLong(0);
    
    // System metrics
    private volatile double cpuUsage = 0.0;
    private volatile long memoryUsed = 0L;
    private volatile int activeThreads = 0;
    
    // Broker lifecycle
    private LocalDateTime brokerStartTime;
    private LocalDateTime lastBrokerRestart;
    
    public void recordPublishSuccess(String topic, int partition) {
        totalPublishedEvents.incrementAndGet();
        topicPublishCounts.computeIfAbsent(topic, k -> new AtomicLong(0)).incrementAndGet();
        partitionCounts.computeIfAbsent(partition, k -> new AtomicLong(0)).incrementAndGet();
        lastPublishTime = System.currentTimeMillis();
    }
    
    public void recordPublishError(String topic) {
        totalPublishErrors.incrementAndGet();
        topicErrorCounts.computeIfAbsent(topic, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    public void recordEventDelivery(String topicPattern, String kafkaTopic) {
        totalConsumedEvents.incrementAndGet();
        topicConsumeCounts.computeIfAbsent(kafkaTopic, k -> new AtomicLong(0)).incrementAndGet();
        lastConsumeTime = System.currentTimeMillis();
    }
    
    public void recordConsumerError(String topicPattern) {
        totalConsumerErrors.incrementAndGet();
    }
    
    public void recordDeliveryError(String topicPattern) {
        totalDeliveryErrors.incrementAndGet();
    }
    
    public void recordProcessingError(String kafkaTopic) {
        topicErrorCounts.computeIfAbsent(kafkaTopic, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    public void recordSubscription(String topicPattern) {
        activeSubscriptions.incrementAndGet();
        totalSubscriptions.incrementAndGet();
    }
    
    public void recordUnsubscription(String topicPattern) {
        activeSubscriptions.decrementAndGet();
    }
    
    public void recordHealthDegradation() {
        healthDegradationCount.incrementAndGet();
    }
    
    public void recordHealthRecovery() {
        healthRecoveryCount.incrementAndGet();
    }
    
    public void recordCircuitBreakerOpen() {
        circuitBreakerOpenCount.incrementAndGet();
    }
    
    public void recordBrokerStart() {
        brokerStartTime = LocalDateTime.now();
    }
    
    public void recordBrokerStop() {
        // Could record stop metrics if needed
    }
    
    public void recordBrokerRestart() {
        lastBrokerRestart = LocalDateTime.now();
    }
    
    public void updateSystemMetrics() {
        // Update system-level metrics
        Runtime runtime = Runtime.getRuntime();
        memoryUsed = runtime.totalMemory() - runtime.freeMemory();
        activeThreads = Thread.activeCount();
        
        // CPU usage would require more sophisticated monitoring
        // This is a simplified implementation
        cpuUsage = getCurrentCpuUsage();
    }
    
    private double getCurrentCpuUsage() {
        // Simplified CPU usage estimation
        // In production, use proper system monitoring tools
        return Math.random() * 100; // Placeholder
    }
    
    // Getter methods for metrics
    
    public long getTotalPublishedEvents() { return totalPublishedEvents.get(); }
    public long getTotalConsumedEvents() { return totalConsumedEvents.get(); }
    public long getTotalPublishErrors() { return totalPublishErrors.get(); }
    public long getTotalConsumerErrors() { return totalConsumerErrors.get(); }
    public long getTotalDeliveryErrors() { return totalDeliveryErrors.get(); }
    
    public Map<String, Long> getTopicPublishCounts() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        topicPublishCounts.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }
    
    public Map<String, Long> getTopicConsumeCounts() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        topicConsumeCounts.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }
    
    public Map<String, Long> getTopicErrorCounts() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        topicErrorCounts.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }
    
    public Map<Integer, Long> getPartitionCounts() {
        Map<Integer, Long> result = new ConcurrentHashMap<>();
        partitionCounts.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }
    
    public long getLastPublishTime() { return lastPublishTime; }
    public long getLastConsumeTime() { return lastConsumeTime; }
    public long getActiveSubscriptions() { return activeSubscriptions.get(); }
    public long getTotalSubscriptions() { return totalSubscriptions.get(); }
    
    public double getCpuUsage() { return cpuUsage; }
    public long getMemoryUsed() { return memoryUsed; }
    public int getActiveThreads() { return activeThreads; }
    
    public LocalDateTime getBrokerStartTime() { return brokerStartTime; }
    public LocalDateTime getLastBrokerRestart() { return lastBrokerRestart; }
    
    public long getHealthDegradationCount() { return healthDegradationCount.get(); }
    public long getHealthRecoveryCount() { return healthRecoveryCount.get(); }
    public long getCircuitBreakerOpenCount() { return circuitBreakerOpenCount.get(); }
    
    /**
     * Get comprehensive metrics summary.
     */
    public String getMetricsSummary() {
        return String.format(
            "EnhancedKafkaMetrics Summary:\n" +
            "  Published Events: %d\n" +
            "  Consumed Events: %d\n" +
            "  Publish Errors: %d\n" +
            "  Consumer Errors: %d\n" +
            "  Delivery Errors: %d\n" +
            "  Active Subscriptions: %d\n" +
            "  Total Subscriptions: %d\n" +
            "  CPU Usage: %.2f%%\n" +
            "  Memory Used: %d bytes\n" +
            "  Active Threads: %d\n" +
            "  Health Issues: %d\n" +
            "  Health Recoveries: %d\n",
            getTotalPublishedEvents(),
            getTotalConsumedEvents(),
            getTotalPublishErrors(),
            getTotalConsumerErrors(),
            getTotalDeliveryErrors(),
            getActiveSubscriptions(),
            getTotalSubscriptions(),
            getCpuUsage(),
            getMemoryUsed(),
            getActiveThreads(),
            getHealthDegradationCount(),
            getHealthRecoveryCount()
        );
    }
    
    /**
     * Reset all metrics (useful for testing).
     */
    public void reset() {
        totalPublishedEvents.set(0);
        totalConsumedEvents.set(0);
        totalPublishErrors.set(0);
        totalConsumerErrors.set(0);
        totalDeliveryErrors.set(0);
        
        topicPublishCounts.clear();
        topicConsumeCounts.clear();
        topicErrorCounts.clear();
        partitionCounts.clear();
        
        activeSubscriptions.set(0);
        totalSubscriptions.set(0);
        
        healthDegradationCount.set(0);
        healthRecoveryCount.set(0);
        circuitBreakerOpenCount.set(0);
        
        cpuUsage = 0.0;
        memoryUsed = 0L;
        activeThreads = 0;
        
        brokerStartTime = null;
        lastBrokerRestart = null;
    }
}