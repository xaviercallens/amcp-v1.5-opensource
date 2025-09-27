package io.amcp.examples.weather;

import io.amcp.core.AbstractAgent;
import io.amcp.core.AgentID;
import io.amcp.core.Event;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Configurable weather consumer agent that monitors weather data and generates alerts
 * based on configurable thresholds. Supports data aggregation and trend analysis.
 */
public class WeatherConsumerAgent extends AbstractAgent {
    
    private final String consumerType;
    private final Map<String, Double> temperatureThresholds = new ConcurrentHashMap<>();
    private final Map<String, Double> windSpeedThresholds = new ConcurrentHashMap<>();
    private final Map<String, Double> pressureThresholds = new ConcurrentHashMap<>();
    
    // Data aggregation storage
    private final Map<String, List<WeatherData>> weatherHistory = new ConcurrentHashMap<>();
    private final Map<String, WeatherData> latestData = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock dataLock = new ReentrantReadWriteLock();
    
    // Configuration
    private final int maxHistorySize;
    private final boolean enableAggregation;
    private final boolean enableAlerts;
    
    public WeatherConsumerAgent(AgentID agentID, String consumerType) {
        this(agentID, consumerType, true, true, 1000);
    }
    
    public WeatherConsumerAgent(AgentID agentID, String consumerType, 
                               boolean enableAlerts, boolean enableAggregation, 
                               int maxHistorySize) {
        super(agentID, "WeatherConsumer");
        this.consumerType = consumerType;
        this.enableAlerts = enableAlerts;
        this.enableAggregation = enableAggregation;
        this.maxHistorySize = maxHistorySize;
        
        // Set default thresholds
        setDefaultThresholds();
        
        log("WeatherConsumerAgent initialized: " + consumerType + 
            " (Alerts: " + enableAlerts + ", Aggregation: " + enableAggregation + ")");
    }
    
    private void setDefaultThresholds() {
        // Default temperature thresholds (Celsius)
        temperatureThresholds.put("default", 35.0);
        
        // Default wind speed thresholds (km/h)
        windSpeedThresholds.put("default", 20.0);
        
        // Default pressure thresholds (hPa)
        pressureThresholds.put("default", 1000.0);
    }
    
    @Override
    public void onActivate() {
        super.onActivate();
        log("Starting WeatherConsumerAgent: " + consumerType);
        
        // Subscribe to weather data events using AgentContext
        if (getAgentContext() != null) {
            getAgentContext().subscribe(getAgentId(), "weather.data.*");
            getAgentContext().subscribe(getAgentId(), "weather.alert.*");
            getAgentContext().subscribe(getAgentId(), "weather.system.*");
            log("Subscribed to weather events");
        }
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            try {
                String topic = event.getTopic();
                
                if (topic.startsWith("weather.data.")) {
                    handleWeatherData(event);
                } else if (topic.startsWith("weather.alert.")) {
                    handleWeatherAlert(event);
                } else if (topic.startsWith("weather.system.")) {
                    handleSystemCommand(event);
                } else {
                    log("Received unhandled event: " + topic);
                }
            } catch (Exception e) {
                log("Error processing event: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    private void handleWeatherData(Event event) {
        try {
            // Extract weather data from event payload
            WeatherData weatherData = extractWeatherData(event);
            if (weatherData == null) {
                log("Warning: Received null weather data");
                return;
            }
            
            String locationId = weatherData.getLocationId();
            log("Processing weather data for " + locationId + ": " + 
                weatherData.getTemperature() + "°C, Wind: " + weatherData.getWindSpeed() + " km/h");
            
            // Store latest data
            dataLock.writeLock().lock();
            try {
                latestData.put(locationId, weatherData);
                
                // Add to history if aggregation is enabled
                if (enableAggregation) {
                    weatherHistory.computeIfAbsent(locationId, k -> new ArrayList<>()).add(weatherData);
                    
                    // Trim history to max size
                    List<WeatherData> history = weatherHistory.get(locationId);
                    if (history.size() > maxHistorySize) {
                        history.subList(0, history.size() - maxHistorySize).clear();
                    }
                }
            } finally {
                dataLock.writeLock().unlock();
            }
            
            // Check thresholds and generate alerts if enabled
            if (enableAlerts) {
                checkThresholdsAndAlert(weatherData);
            }
            
            // Perform aggregation analysis if enabled
            if (enableAggregation) {
                performAggregationAnalysis(locationId, weatherData);
            }
            
        } catch (Exception e) {
            log("Error processing weather data: " + e.getMessage());
        }
    }
    
    private WeatherData extractWeatherData(Event event) {
        // Try to extract weather data from event payload
        Object payload = event.getPayload();
        
        if (payload instanceof WeatherData) {
            return (WeatherData) payload;
        }
        
        // If payload is a Map, try to construct WeatherData
        if (payload instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payload;
            
            if (!data.containsKey("locationId")) {
                return null;
            }
            
            try {
                return WeatherData.builder()
                        .locationId((String) data.get("locationId"))
                        .locationName((String) data.getOrDefault("locationName", "Unknown"))
                        .temperature(((Number) data.getOrDefault("temperature", 0.0)).doubleValue())
                        .humidity(((Number) data.getOrDefault("humidity", 0.0)).doubleValue())
                        .pressure(((Number) data.getOrDefault("pressure", 0.0)).doubleValue())
                        .conditions((String) data.getOrDefault("conditions", "Unknown"))
                        .windSpeed(((Number) data.getOrDefault("windSpeed", 0.0)).doubleValue())
                        .windDirection((String) data.getOrDefault("windDirection", "Unknown"))
                        .source((String) data.getOrDefault("source", "Unknown"))
                        .build();
            } catch (Exception e) {
                log("Error extracting weather data: " + e.getMessage());
                return null;
            }
        }
        
        return null;
    }
    
    private void checkThresholdsAndAlert(WeatherData weatherData) {
        String locationId = weatherData.getLocationId();
        List<String> violations = new ArrayList<>();
        
        // Check temperature thresholds
        double tempThreshold = temperatureThresholds.getOrDefault(locationId, 
                                 temperatureThresholds.get("default"));
        if (weatherData.getTemperature() > tempThreshold) {
            violations.add(String.format("Temperature %.1f°C exceeds threshold %.1f°C", 
                          weatherData.getTemperature(), tempThreshold));
        }
        
        // Check wind speed thresholds  
        double windThreshold = windSpeedThresholds.getOrDefault(locationId,
                                 windSpeedThresholds.get("default"));
        if (weatherData.getWindSpeed() > windThreshold) {
            violations.add(String.format("Wind speed %.1f km/h exceeds threshold %.1f km/h",
                          weatherData.getWindSpeed(), windThreshold));
        }
        
        // Check pressure thresholds (low pressure alert)
        double pressureThreshold = pressureThresholds.getOrDefault(locationId,
                                     pressureThresholds.get("default"));
        if (weatherData.getPressure() < pressureThreshold) {
            violations.add(String.format("Pressure %.1f hPa below threshold %.1f hPa",
                          weatherData.getPressure(), pressureThreshold));
        }
        
        // Generate alerts for violations
        if (!violations.isEmpty()) {
            generateAlert(weatherData, violations);
        }
    }
    
    private void generateAlert(WeatherData weatherData, List<String> violations) {
        WeatherData.AlertLevel alertLevel = weatherData.getAlertLevel();
        
        Map<String, Object> alertData = new HashMap<>();
        alertData.put("locationId", weatherData.getLocationId());
        alertData.put("locationName", weatherData.getLocationName());
        alertData.put("alertLevel", alertLevel.toString());
        alertData.put("violations", violations);
        alertData.put("weatherData", weatherData);
        alertData.put("consumerType", consumerType);
        alertData.put("timestamp", Instant.now().toString());
        
        if (getAgentContext() != null) {
            Event alertEvent = Event.create("weather.alert.threshold", alertData, getAgentId());
            getAgentContext().publishEvent(alertEvent);
        }
        
        log("ALERT [" + alertLevel + "] for " + weatherData.getLocationName() + 
            ": " + String.join(", ", violations));
    }
    
    private void performAggregationAnalysis(String locationId, WeatherData current) {
        dataLock.readLock().lock();
        try {
            List<WeatherData> history = weatherHistory.get(locationId);
            if (history == null || history.size() < 2) {
                return; // Not enough data for analysis
            }
            
            // Perform trend analysis on recent data
            int analysisWindow = Math.min(10, history.size());
            List<WeatherData> recentData = history.subList(history.size() - analysisWindow, history.size());
            
            // Calculate temperature trend
            double tempTrend = calculateTemperatureTrend(recentData);
            if (Math.abs(tempTrend) > 5.0) { // Significant temperature change
                publishTrendAlert(locationId, "temperature", tempTrend, current);
            }
            
            // Calculate pressure trend
            double pressureTrend = calculatePressureTrend(recentData);
            if (Math.abs(pressureTrend) > 10.0) { // Significant pressure change
                publishTrendAlert(locationId, "pressure", pressureTrend, current);
            }
            
        } finally {
            dataLock.readLock().unlock();
        }
    }
    
    private double calculateTemperatureTrend(List<WeatherData> data) {
        if (data.size() < 2) return 0.0;
        
        double first = data.get(0).getTemperature();
        double last = data.get(data.size() - 1).getTemperature();
        return last - first;
    }
    
    private double calculatePressureTrend(List<WeatherData> data) {
        if (data.size() < 2) return 0.0;
        
        double first = data.get(0).getPressure();
        double last = data.get(data.size() - 1).getPressure();
        return last - first;
    }
    
    private void publishTrendAlert(String locationId, String metric, double trend, WeatherData current) {
        Map<String, Object> trendData = new HashMap<>();
        trendData.put("locationId", locationId);
        trendData.put("locationName", current.getLocationName());
        trendData.put("metric", metric);
        trendData.put("trend", trend);
        trendData.put("consumerType", consumerType);
        trendData.put("timestamp", Instant.now().toString());
        
        if (getAgentContext() != null) {
            Event trendEvent = Event.create("weather.trend.detected", trendData, getAgentId());
            getAgentContext().publishEvent(trendEvent);
        }
        
        log("TREND ALERT for " + current.getLocationName() + ": " + metric + 
            " changed by " + String.format("%.1f", trend));
    }
    
    private void handleWeatherAlert(Event event) {
        // Handle alerts from other agents
        if (event.getPayload() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.getPayload();
            String alertLevel = (String) data.get("alertLevel");
            String locationName = (String) data.get("locationName");
            
            log("Received weather alert [" + alertLevel + "] for " + locationName);
            
            // Could implement alert aggregation, filtering, or forwarding logic here
        }
    }
    
    private void handleSystemCommand(Event event) {
        if (event.getPayload() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.getPayload();
            String command = (String) data.get("command");
            
            switch (command != null ? command : "") {
                case "getStatus":
                    publishStatus();
                    break;
                case "getLatestData":
                    publishLatestData();
                    break;
                case "clearHistory":
                    clearHistory();
                    break;
                default:
                    log("Unknown command: " + command);
            }
        }
    }
    
    // Configuration methods
    public void setTemperatureThreshold(String locationId, double threshold) {
        temperatureThresholds.put(locationId, threshold);
        log("Temperature threshold for " + locationId + " set to " + threshold + "°C");
    }
    
    public void setWindSpeedThreshold(String locationId, double threshold) {
        windSpeedThresholds.put(locationId, threshold);
        log("Wind speed threshold for " + locationId + " set to " + threshold + " km/h");
    }
    
    public void setPressureThreshold(String locationId, double threshold) {
        pressureThresholds.put(locationId, threshold);
        log("Pressure threshold for " + locationId + " set to " + threshold + " hPa");
    }
    
    // Status and reporting methods
    private void publishStatus() {
        dataLock.readLock().lock();
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("consumerType", consumerType);
            status.put("agentId", getAgentId().toString());
            status.put("enableAlerts", enableAlerts);
            status.put("enableAggregation", enableAggregation);
            status.put("locationsMonitored", latestData.keySet());
            status.put("totalDataPoints", weatherHistory.values().stream().mapToInt(List::size).sum());
            status.put("timestamp", Instant.now().toString());
            
            if (getAgentContext() != null) {
                Event statusEvent = Event.create("weather.consumer.status", status, getAgentId());
                getAgentContext().publishEvent(statusEvent);
            }
            
        } finally {
            dataLock.readLock().unlock();
        }
    }
    
    private void publishLatestData() {
        dataLock.readLock().lock();
        try {
            for (Map.Entry<String, WeatherData> entry : latestData.entrySet()) {
                Map<String, Object> dataEvent = new HashMap<>();
                dataEvent.put("consumerType", consumerType);
                dataEvent.put("weatherData", entry.getValue());
                
                if (getAgentContext() != null) {
                    Event event = Event.create("weather.consumer.data", dataEvent, getAgentId());
                    getAgentContext().publishEvent(event);
                }
            }
        } finally {
            dataLock.readLock().unlock();
        }
    }
    
    private void clearHistory() {
        dataLock.writeLock().lock();
        try {
            weatherHistory.clear();
            log("Weather history cleared");
        } finally {
            dataLock.writeLock().unlock();
        }
    }
    
    // Getter methods for current state
    public Map<String, WeatherData> getLatestData() {
        dataLock.readLock().lock();
        try {
            return new HashMap<>(latestData);
        } finally {
            dataLock.readLock().unlock();
        }
    }
    
    public int getHistorySize(String locationId) {
        dataLock.readLock().lock();
        try {
            List<WeatherData> history = weatherHistory.get(locationId);
            return history != null ? history.size() : 0;
        } finally {
            dataLock.readLock().unlock();
        }
    }
    
    public String getConsumerType() {
        return consumerType;
    }
    
    @Override
    public void onDeactivate() {
        log("Stopping WeatherConsumerAgent: " + consumerType);
        super.onDeactivate();
    }
    
    private void log(String message) {
        System.out.println("[" + getAgentId() + "] " + message);
    }
}