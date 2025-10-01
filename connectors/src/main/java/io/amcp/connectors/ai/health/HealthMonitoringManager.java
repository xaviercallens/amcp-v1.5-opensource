package io.amcp.connectors.ai.health;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Health Monitoring Manager for AMCP v1.5 Enhanced Orchestrator
 * 
 * Provides comprehensive health monitoring capabilities including:
 * - Agent health status tracking
 * - Performance metrics collection
 * - Resource utilization monitoring
 * - System status aggregation and reporting
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 * @since 1.5.0
 */
public class HealthMonitoringManager {
    
    public enum HealthStatus {
        HEALTHY,
        WARNING,
        CRITICAL,
        UNKNOWN
    }
    
    public enum MetricType {
        RESPONSE_TIME,
        ERROR_RATE,
        THROUGHPUT,
        RESOURCE_USAGE,
        AVAILABILITY
    }
    
    // Health tracking
    private final Map<String, AgentHealthStatus> agentHealthMap = new ConcurrentHashMap<>();
    private final Map<String, List<HealthMetric>> metricsHistory = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> errorCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> requestCounts = new ConcurrentHashMap<>();
    
    // Configuration
    private static final int MAX_METRICS_HISTORY = 100;
    private static final long HEALTH_CHECK_INTERVAL_MS = 30000; // 30 seconds
    private static final double ERROR_RATE_WARNING_THRESHOLD = 0.1; // 10%
    private static final double ERROR_RATE_CRITICAL_THRESHOLD = 0.3; // 30%
    private static final long RESPONSE_TIME_WARNING_THRESHOLD = 5000; // 5 seconds
    private static final long RESPONSE_TIME_CRITICAL_THRESHOLD = 10000; // 10 seconds
    
    // Scheduled health checks
    private final ScheduledExecutorService healthCheckExecutor = Executors.newScheduledThreadPool(2);
    
    public HealthMonitoringManager() {
        startHealthCheckScheduler();
    }
    
    /**
     * Record a health metric for an agent
     */
    public void recordMetric(String agentId, MetricType type, double value) {
        HealthMetric metric = new HealthMetric(type, value, LocalDateTime.now());
        
        metricsHistory.computeIfAbsent(agentId, k -> new ArrayList<>()).add(metric);
        
        // Trim history if needed
        List<HealthMetric> metrics = metricsHistory.get(agentId);
        if (metrics.size() > MAX_METRICS_HISTORY) {
            metrics.remove(0);
        }
        
        // Update health status based on metric
        updateHealthStatus(agentId, type, value);
    }
    
    /**
     * Record a successful request for an agent
     */
    public void recordSuccess(String agentId) {
        requestCounts.computeIfAbsent(agentId, k -> new AtomicLong(0)).incrementAndGet();
        recordMetric(agentId, MetricType.AVAILABILITY, 1.0);
    }
    
    /**
     * Record an error for an agent
     */
    public void recordError(String agentId, Throwable error) {
        errorCounts.computeIfAbsent(agentId, k -> new AtomicInteger(0)).incrementAndGet();
        requestCounts.computeIfAbsent(agentId, k -> new AtomicLong(0)).incrementAndGet();
        
        recordMetric(agentId, MetricType.AVAILABILITY, 0.0);
        recordMetric(agentId, MetricType.ERROR_RATE, calculateErrorRate(agentId));
        
        logMessage("Recorded error for agent " + agentId + ": " + error.getMessage());
    }
    
    /**
     * Record response time for an agent
     */
    public void recordResponseTime(String agentId, long responseTimeMs) {
        recordMetric(agentId, MetricType.RESPONSE_TIME, responseTimeMs);
    }
    
    /**
     * Get current health status for an agent
     */
    public HealthStatus getAgentHealth(String agentId) {
        AgentHealthStatus status = agentHealthMap.get(agentId);
        return status != null ? status.getOverallHealth() : HealthStatus.UNKNOWN;
    }
    
    /**
     * Get detailed health information for an agent
     */
    public AgentHealthStatus getAgentHealthDetails(String agentId) {
        return agentHealthMap.get(agentId);
    }
    
    /**
     * Get system-wide health summary
     */
    public SystemHealthSummary getSystemHealth() {
        Map<HealthStatus, Integer> statusCounts = new HashMap<>();
        statusCounts.put(HealthStatus.HEALTHY, 0);
        statusCounts.put(HealthStatus.WARNING, 0);
        statusCounts.put(HealthStatus.CRITICAL, 0);
        statusCounts.put(HealthStatus.UNKNOWN, 0);
        
        List<String> unhealthyAgents = new ArrayList<>();
        
        for (Map.Entry<String, AgentHealthStatus> entry : agentHealthMap.entrySet()) {
            HealthStatus status = entry.getValue().getOverallHealth();
            statusCounts.put(status, statusCounts.get(status) + 1);
            
            if (status == HealthStatus.WARNING || status == HealthStatus.CRITICAL) {
                unhealthyAgents.add(entry.getKey());
            }
        }
        
        return new SystemHealthSummary(statusCounts, unhealthyAgents, calculateSystemAvailability());
    }
    
    /**
     * Get recent metrics for an agent
     */
    public List<HealthMetric> getRecentMetrics(String agentId, MetricType type, int count) {
        List<HealthMetric> allMetrics = metricsHistory.get(agentId);
        if (allMetrics == null) {
            return new ArrayList<>();
        }
        
        return allMetrics.stream()
                        .filter(metric -> metric.getType() == type)
                        .sorted((m1, m2) -> m2.getTimestamp().compareTo(m1.getTimestamp()))
                        .limit(count)
                        .toList();
    }
    
    /**
     * Perform health check for specific agent
     */
    public CompletableFuture<HealthStatus> performHealthCheck(String agentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Mock health check - would integrate with actual agent ping
                boolean isResponding = pingAgent(agentId);
                
                if (isResponding) {
                    recordSuccess(agentId);
                    return HealthStatus.HEALTHY;
                } else {
                    recordError(agentId, new RuntimeException("Agent not responding"));
                    return HealthStatus.CRITICAL;
                }
                
            } catch (Exception e) {
                recordError(agentId, e);
                return HealthStatus.CRITICAL;
            }
        });
    }
    
    /**
     * Register an agent for health monitoring
     */
    public void registerAgent(String agentId) {
        agentHealthMap.putIfAbsent(agentId, new AgentHealthStatus(agentId));
        logMessage("Registered agent for health monitoring: " + agentId);
    }
    
    /**
     * Unregister an agent from health monitoring
     */
    public void unregisterAgent(String agentId) {
        agentHealthMap.remove(agentId);
        metricsHistory.remove(agentId);
        errorCounts.remove(agentId);
        requestCounts.remove(agentId);
        logMessage("Unregistered agent from health monitoring: " + agentId);
    }
    
    // Private helper methods
    
    private void updateHealthStatus(String agentId, MetricType type, double value) {
        AgentHealthStatus status = agentHealthMap.computeIfAbsent(agentId, k -> new AgentHealthStatus(agentId));
        
        switch (type) {
            case RESPONSE_TIME:
                if (value > RESPONSE_TIME_CRITICAL_THRESHOLD) {
                    status.updateMetricHealth(type, HealthStatus.CRITICAL);
                } else if (value > RESPONSE_TIME_WARNING_THRESHOLD) {
                    status.updateMetricHealth(type, HealthStatus.WARNING);
                } else {
                    status.updateMetricHealth(type, HealthStatus.HEALTHY);
                }
                break;
                
            case ERROR_RATE:
                if (value > ERROR_RATE_CRITICAL_THRESHOLD) {
                    status.updateMetricHealth(type, HealthStatus.CRITICAL);
                } else if (value > ERROR_RATE_WARNING_THRESHOLD) {
                    status.updateMetricHealth(type, HealthStatus.WARNING);
                } else {
                    status.updateMetricHealth(type, HealthStatus.HEALTHY);
                }
                break;
                
            case AVAILABILITY:
                status.updateMetricHealth(type, value > 0.8 ? HealthStatus.HEALTHY : 
                                               value > 0.5 ? HealthStatus.WARNING : HealthStatus.CRITICAL);
                break;
                
            default:
                status.updateMetricHealth(type, HealthStatus.HEALTHY);
        }
    }
    
    private double calculateErrorRate(String agentId) {
        AtomicInteger errors = errorCounts.get(agentId);
        AtomicLong requests = requestCounts.get(agentId);
        
        if (requests == null || requests.get() == 0) {
            return 0.0;
        }
        
        return errors != null ? (double) errors.get() / requests.get() : 0.0;
    }
    
    private double calculateSystemAvailability() {
        if (agentHealthMap.isEmpty()) {
            return 1.0;
        }
        
        long healthyCount = agentHealthMap.values().stream()
                                         .mapToLong(status -> status.getOverallHealth() == HealthStatus.HEALTHY ? 1 : 0)
                                         .sum();
        
        return (double) healthyCount / agentHealthMap.size();
    }
    
    private boolean pingAgent(String agentId) {
        // Mock implementation - would actually ping the agent
        return Math.random() > 0.1; // 90% success rate
    }
    
    private void startHealthCheckScheduler() {
        healthCheckExecutor.scheduleAtFixedRate(
            this::performScheduledHealthChecks,
            HEALTH_CHECK_INTERVAL_MS,
            HEALTH_CHECK_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );
    }
    
    private void performScheduledHealthChecks() {
        List<CompletableFuture<Void>> healthCheckFutures = new ArrayList<>();
        
        for (String agentId : agentHealthMap.keySet()) {
            CompletableFuture<Void> future = performHealthCheck(agentId)
                .thenAccept(status -> {
                    if (status == HealthStatus.CRITICAL) {
                        logMessage("Health check failed for agent: " + agentId);
                    }
                })
                .exceptionally(throwable -> {
                    logMessage("Health check error for agent " + agentId + ": " + throwable.getMessage());
                    return null;
                });
            
            healthCheckFutures.add(future);
        }
        
        // Wait for all health checks to complete
        CompletableFuture.allOf(healthCheckFutures.toArray(new CompletableFuture[0]))
                        .orTimeout(10, TimeUnit.SECONDS)
                        .join();
    }
    
    private void logMessage(String message) {
        System.out.println("[HealthMonitoringManager] " + message);
    }
    
    /**
     * Shutdown the health monitoring manager
     */
    public void shutdown() {
        healthCheckExecutor.shutdown();
        try {
            if (!healthCheckExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                healthCheckExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            healthCheckExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // Inner classes
    
    /**
     * Individual health metric
     */
    public static class HealthMetric {
        private final MetricType type;
        private final double value;
        private final LocalDateTime timestamp;
        
        public HealthMetric(MetricType type, double value, LocalDateTime timestamp) {
            this.type = type;
            this.value = value;
            this.timestamp = timestamp;
        }
        
        public MetricType getType() { return type; }
        public double getValue() { return value; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("HealthMetric{type=%s, value=%.2f, timestamp=%s}", type, value, timestamp);
        }
    }
    
    /**
     * Agent health status
     */
    public static class AgentHealthStatus {
        private final String agentId;
        private final Map<MetricType, HealthStatus> metricHealthStatus = new ConcurrentHashMap<>();
        private final LocalDateTime registeredAt;
        private volatile LocalDateTime lastUpdateAt;
        
        public AgentHealthStatus(String agentId) {
            this.agentId = agentId;
            this.registeredAt = LocalDateTime.now();
            this.lastUpdateAt = LocalDateTime.now();
        }
        
        public void updateMetricHealth(MetricType type, HealthStatus status) {
            metricHealthStatus.put(type, status);
            lastUpdateAt = LocalDateTime.now();
        }
        
        public HealthStatus getOverallHealth() {
            if (metricHealthStatus.isEmpty()) {
                return HealthStatus.UNKNOWN;
            }
            
            boolean hasCritical = metricHealthStatus.values().contains(HealthStatus.CRITICAL);
            boolean hasWarning = metricHealthStatus.values().contains(HealthStatus.WARNING);
            
            if (hasCritical) return HealthStatus.CRITICAL;
            if (hasWarning) return HealthStatus.WARNING;
            return HealthStatus.HEALTHY;
        }
        
        public String getAgentId() { return agentId; }
        public Map<MetricType, HealthStatus> getMetricHealthStatus() { return new HashMap<>(metricHealthStatus); }
        public LocalDateTime getRegisteredAt() { return registeredAt; }
        public LocalDateTime getLastUpdateAt() { return lastUpdateAt; }
        
        @Override
        public String toString() {
            return String.format("AgentHealthStatus{agentId='%s', overall=%s, metrics=%s}", 
                                agentId, getOverallHealth(), metricHealthStatus);
        }
    }
    
    /**
     * System-wide health summary
     */
    public static class SystemHealthSummary {
        private final Map<HealthStatus, Integer> statusCounts;
        private final List<String> unhealthyAgents;
        private final double systemAvailability;
        private final LocalDateTime timestamp;
        
        public SystemHealthSummary(Map<HealthStatus, Integer> statusCounts, 
                                 List<String> unhealthyAgents, 
                                 double systemAvailability) {
            this.statusCounts = new HashMap<>(statusCounts);
            this.unhealthyAgents = new ArrayList<>(unhealthyAgents);
            this.systemAvailability = systemAvailability;
            this.timestamp = LocalDateTime.now();
        }
        
        public Map<HealthStatus, Integer> getStatusCounts() { return new HashMap<>(statusCounts); }
        public List<String> getUnhealthyAgents() { return new ArrayList<>(unhealthyAgents); }
        public double getSystemAvailability() { return systemAvailability; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        public boolean isSystemHealthy() {
            return systemAvailability > 0.8 && statusCounts.get(HealthStatus.CRITICAL) == 0;
        }
        
        @Override
        public String toString() {
            return String.format("SystemHealthSummary{availability=%.2f%%, healthy=%d, warning=%d, critical=%d, unknown=%d}",
                systemAvailability * 100,
                statusCounts.get(HealthStatus.HEALTHY),
                statusCounts.get(HealthStatus.WARNING),
                statusCounts.get(HealthStatus.CRITICAL),
                statusCounts.get(HealthStatus.UNKNOWN));
        }
    }
    
    /**
     * Start health monitoring for a specific agent or component.
     */
    public void startHealthMonitoring(Object component) {
        if (component == null) return;
        
        String componentId = component.getClass().getSimpleName() + "@" + System.identityHashCode(component);
        
        // Register the component for monitoring
        AgentHealthStatus healthStatus = new AgentHealthStatus(componentId);
        agentHealthMap.put(componentId, healthStatus);
        
        System.out.println("Started health monitoring for component: " + componentId);
    }
    
    /**
     * Stop health monitoring.
     */
    public void stopHealthMonitoring() {
        if (healthCheckExecutor != null && !healthCheckExecutor.isShutdown()) {
            healthCheckExecutor.shutdown();
            try {
                if (!healthCheckExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    healthCheckExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                healthCheckExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("Health monitoring stopped");
    }
}