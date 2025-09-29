package io.amcp.cli;

import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;

/**
 * Real-time Status Monitor for AMCP CLI
 * 
 * Monitors:
 * - Agent health and activity
 * - API connectivity and response times
 * - System resources and performance
 * - Event broker status
 */
public class StatusMonitor {
    
    private final Map<String, String> apiStatus = new ConcurrentHashMap<>();
    private final Map<String, ApiMetrics> apiMetrics = new ConcurrentHashMap<>();
    private final Map<String, AgentMetrics> agentMetrics = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    private volatile boolean running = false;
    private HttpClient httpClient;
    
    public StatusMonitor() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
            
        initializeApiEndpoints();
    }
    
    private void initializeApiEndpoints() {
        // Initialize known API endpoints
        apiStatus.put("polygon.io", "UNKNOWN");
        apiStatus.put("openweather", "UNKNOWN");
        apiStatus.put("eventbroker", "ACTIVE");
        
        // Initialize metrics
        apiMetrics.put("polygon.io", new ApiMetrics("polygon.io"));
        apiMetrics.put("openweather", new ApiMetrics("openweather"));
        apiMetrics.put("eventbroker", new ApiMetrics("eventbroker"));
    }
    
    /**
     * Start monitoring
     */
    public void start() {
        if (running) return;
        
        running = true;
        
        // Monitor API health every 30 seconds
        scheduler.scheduleWithFixedDelay(this::checkApiHealth, 0, 30, TimeUnit.SECONDS);
        
        // Update metrics every 10 seconds
        scheduler.scheduleWithFixedDelay(this::updateMetrics, 5, 10, TimeUnit.SECONDS);
        
        System.out.println("📊 Status monitor started");
    }
    
    /**
     * Stop monitoring
     */
    public void stop() {
        if (!running) return;
        
        running = false;
        scheduler.shutdown();
        
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("📊 Status monitor stopped");
    }
    
    /**
     * Check API health status
     */
    private void checkApiHealth() {
        // Check Polygon.io API
        checkPolygonApi();
        
        // Check OpenWeatherMap API
        checkOpenWeatherApi();
        
        // Event broker is always active in this implementation
        apiStatus.put("eventbroker", "ACTIVE");
        apiMetrics.get("eventbroker").recordSuccess(0); // No latency for in-memory
    }
    
    private void checkPolygonApi() {
        try {
            String apiKey = System.getProperty("POLYGON_API_KEY", 
                System.getenv("POLYGON_API_KEY"));
            
            if (apiKey == null) {
                apiStatus.put("polygon.io", "NO_KEY");
                return;
            }
            
            String url = "https://api.polygon.io/v1/meta/symbols/AAPL/company?apikey=" + apiKey;
            
            long startTime = System.currentTimeMillis();
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            long latency = System.currentTimeMillis() - startTime;
            
            if (response.statusCode() == 200) {
                apiStatus.put("polygon.io", "ACTIVE");
                apiMetrics.get("polygon.io").recordSuccess(latency);
            } else if (response.statusCode() == 401) {
                apiStatus.put("polygon.io", "AUTH_ERROR");
                apiMetrics.get("polygon.io").recordError();
            } else {
                apiStatus.put("polygon.io", "ERROR_" + response.statusCode());
                apiMetrics.get("polygon.io").recordError();
            }
            
        } catch (Exception e) {
            apiStatus.put("polygon.io", "UNREACHABLE");
            apiMetrics.get("polygon.io").recordError();
        }
    }
    
    private void checkOpenWeatherApi() {
        try {
            String apiKey = System.getProperty("OPENWEATHER_API_KEY", 
                System.getenv("OPENWEATHER_API_KEY"));
            
            if (apiKey == null) {
                apiStatus.put("openweather", "NO_KEY");
                return;
            }
            
            String url = "https://api.openweathermap.org/data/2.5/weather?q=London&appid=" + apiKey;
            
            long startTime = System.currentTimeMillis();
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            long latency = System.currentTimeMillis() - startTime;
            
            if (response.statusCode() == 200) {
                apiStatus.put("openweather", "ACTIVE");
                apiMetrics.get("openweather").recordSuccess(latency);
            } else if (response.statusCode() == 401) {
                apiStatus.put("openweather", "AUTH_ERROR");
                apiMetrics.get("openweather").recordError();
            } else {
                apiStatus.put("openweather", "ERROR_" + response.statusCode());
                apiMetrics.get("openweather").recordError();
            }
            
        } catch (Exception e) {
            apiStatus.put("openweather", "UNREACHABLE");
            apiMetrics.get("openweather").recordError();
        }
    }
    
    /**
     * Update system metrics
     */
    private void updateMetrics() {
        // Update system metrics
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        // Store system metrics (could be expanded)
        // For now, just maintain API metrics
    }
    
    /**
     * Record agent activity
     */
    public void recordAgentActivity(String agentName, String activity) {
        agentMetrics.computeIfAbsent(agentName, AgentMetrics::new)
            .recordActivity(activity);
    }
    
    /**
     * Record API call
     */
    public void recordApiCall(String apiName, boolean success, long latency) {
        ApiMetrics metrics = apiMetrics.get(apiName);
        if (metrics != null) {
            if (success) {
                metrics.recordSuccess(latency);
            } else {
                metrics.recordError();
            }
        }
    }
    
    /**
     * Get API status map
     */
    public Map<String, String> getApiStatus() {
        return new HashMap<>(apiStatus);
    }
    
    /**
     * Get detailed API metrics
     */
    public Map<String, ApiMetrics> getApiMetrics() {
        return new HashMap<>(apiMetrics);
    }
    
    /**
     * Get agent metrics
     */
    public Map<String, AgentMetrics> getAgentMetrics() {
        return new HashMap<>(agentMetrics);
    }
    
    /**
     * Get system status summary
     */
    public SystemStatus getSystemStatus() {
        Runtime runtime = Runtime.getRuntime();
        
        return new SystemStatus(
            runtime.availableProcessors(),
            runtime.totalMemory(),
            runtime.freeMemory(),
            runtime.maxMemory(),
            apiStatus.size(),
            (int) apiStatus.values().stream().filter("ACTIVE"::equals).count(),
            agentMetrics.size()
        );
    }
    
    /**
     * API Metrics tracking
     */
    public static class ApiMetrics {
        private final String apiName;
        private long totalCalls = 0;
        private long successfulCalls = 0;
        private long errorCalls = 0;
        private long totalLatency = 0;
        private long maxLatency = 0;
        private long minLatency = Long.MAX_VALUE;
        private LocalDateTime lastCall;
        
        public ApiMetrics(String apiName) {
            this.apiName = apiName;
        }
        
        public synchronized void recordSuccess(long latency) {
            totalCalls++;
            successfulCalls++;
            totalLatency += latency;
            maxLatency = Math.max(maxLatency, latency);
            minLatency = Math.min(minLatency, latency);
            lastCall = LocalDateTime.now();
        }
        
        public synchronized void recordError() {
            totalCalls++;
            errorCalls++;
            lastCall = LocalDateTime.now();
        }
        
        public String getApiName() { return apiName; }
        public long getTotalCalls() { return totalCalls; }
        public long getSuccessfulCalls() { return successfulCalls; }
        public long getErrorCalls() { return errorCalls; }
        public double getSuccessRate() { 
            return totalCalls > 0 ? (double) successfulCalls / totalCalls * 100 : 0; 
        }
        public double getAverageLatency() { 
            return successfulCalls > 0 ? (double) totalLatency / successfulCalls : 0; 
        }
        public long getMaxLatency() { return maxLatency; }
        public long getMinLatency() { return minLatency == Long.MAX_VALUE ? 0 : minLatency; }
        public LocalDateTime getLastCall() { return lastCall; }
    }
    
    /**
     * Agent Metrics tracking
     */
    public static class AgentMetrics {
        private final String agentName;
        private final List<String> recentActivities = new ArrayList<>();
        private long totalActivities = 0;
        private LocalDateTime lastActivity;
        private final int maxRecentActivities = 50;
        
        public AgentMetrics(String agentName) {
            this.agentName = agentName;
        }
        
        public synchronized void recordActivity(String activity) {
            totalActivities++;
            lastActivity = LocalDateTime.now();
            
            String timestampedActivity = lastActivity.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + 
                " " + activity;
            
            recentActivities.add(timestampedActivity);
            
            // Keep only recent activities
            while (recentActivities.size() > maxRecentActivities) {
                recentActivities.remove(0);
            }
        }
        
        public String getAgentName() { return agentName; }
        public long getTotalActivities() { return totalActivities; }
        public LocalDateTime getLastActivity() { return lastActivity; }
        public List<String> getRecentActivities() { return new ArrayList<>(recentActivities); }
    }
    
    /**
     * System Status summary
     */
    public static class SystemStatus {
        private final int availableProcessors;
        private final long totalMemory;
        private final long freeMemory;
        private final long maxMemory;
        private final int totalApis;
        private final int activeApis;
        private final int activeAgents;
        
        public SystemStatus(int availableProcessors, long totalMemory, long freeMemory, 
                          long maxMemory, int totalApis, int activeApis, int activeAgents) {
            this.availableProcessors = availableProcessors;
            this.totalMemory = totalMemory;
            this.freeMemory = freeMemory;
            this.maxMemory = maxMemory;
            this.totalApis = totalApis;
            this.activeApis = activeApis;
            this.activeAgents = activeAgents;
        }
        
        public int getAvailableProcessors() { return availableProcessors; }
        public long getTotalMemory() { return totalMemory; }
        public long getFreeMemory() { return freeMemory; }
        public long getUsedMemory() { return totalMemory - freeMemory; }
        public long getMaxMemory() { return maxMemory; }
        public int getTotalApis() { return totalApis; }
        public int getActiveApis() { return activeApis; }
        public int getActiveAgents() { return activeAgents; }
        
        public double getMemoryUsagePercentage() {
            return (double) getUsedMemory() / totalMemory * 100;
        }
    }
}