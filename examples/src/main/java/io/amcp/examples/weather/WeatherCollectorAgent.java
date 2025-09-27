package io.amcp.examples.weather;

import io.amcp.core.AbstractAgent;
import io.amcp.core.AgentID;
import io.amcp.core.Event;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Weather collector agent that retrieves real-time weather data from OpenWeatherMap API
 * every 5 minutes for multiple locations. Includes API call counting for cost management
 * and comprehensive logging.
 */
public class WeatherCollectorAgent extends AbstractAgent {
    
    // Default OpenWeatherMap API key - can be overridden via environment variable
    private static final String DEFAULT_API_KEY = "3bd965f39881ba0f116ee0810fdfd058";
    private static final String API_KEY_ENV_VAR = "OPENWEATHERMAP_API_KEY";
    private static final String BASE_URL = "http://api.openweathermap.org/data/2.5/weather";
    
    // Collection interval: 5 minutes (300 seconds)
    private static final long COLLECTION_INTERVAL_SECONDS = 300;
    
    // API call management
    private final AtomicLong apiCallCounter = new AtomicLong(0);
    private final AtomicLong totalApiCalls = new AtomicLong(0);
    
    // Configuration
    private final String apiKey;
    private final Map<String, String> locations = new ConcurrentHashMap<String, String>();
    
    // Scheduling and lifecycle
    private ScheduledExecutorService scheduler;
    private final Map<String, ScheduledFuture<?>> collectionTasks = new ConcurrentHashMap<String, ScheduledFuture<?>>();
    private volatile boolean isCollecting = false;
    
    public WeatherCollectorAgent(AgentID agentID) {
        super(agentID, "WeatherCollector");
        
        // Initialize API key (environment variable overrides default)
        this.apiKey = System.getenv(API_KEY_ENV_VAR) != null ? 
                      System.getenv(API_KEY_ENV_VAR) : DEFAULT_API_KEY;
        
        // Setup default locations for demonstration
        initializeDefaultLocations();
        
        log("WeatherCollectorAgent initialized with API key: " + 
            (apiKey.equals(DEFAULT_API_KEY) ? "default" : "custom") + 
            " (" + locations.size() + " locations configured)");
    }
    
    private void initializeDefaultLocations() {
        locations.put("toronto", "Toronto,CA");
        locations.put("vancouver", "Vancouver,CA");
        locations.put("montreal", "Montreal,CA");
        locations.put("calgary", "Calgary,CA");
        locations.put("ottawa", "Ottawa,CA");
    }
    
    @Override
    public void onActivate() {
        super.onActivate();
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        log("WeatherCollectorAgent activated - starting weather data collection");
        startCollection();
        
        // Subscribe to system control events
        if (getAgentContext() != null) {
            subscribe("weather.control.*");
            log("Subscribed to weather control events");
        }
    }
    
    @Override
    public void onDeactivate() {
        log("Stopping WeatherCollectorAgent...");
        stopCollection();
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
        
        logApiCallStatistics();
        super.onDeactivate();
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    String topic = event.getTopic();
                    log("Received event: " + topic);
                    
                    if (topic.equals("weather.control.start")) {
                        startCollection();
                    } else if (topic.equals("weather.control.stop")) {
                        stopCollection();
                    } else if (topic.equals("weather.control.status")) {
                        publishStatus();
                    } else {
                        log("Unhandled event topic: " + topic);
                    }
                } catch (Exception e) {
                    log("Error handling event: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }
    
    private synchronized void startCollection() {
        if (isCollecting) {
            log("Weather collection already running");
            return;
        }
        
        log("Starting weather data collection for " + locations.size() + " locations");
        log("Collection interval: " + COLLECTION_INTERVAL_SECONDS + " seconds (5 minutes)");
        
        isCollecting = true;
        
        // Start collection tasks for each location
        for (Map.Entry<String, String> location : locations.entrySet()) {
            final String locationId = location.getKey();
            final String locationQuery = location.getValue();
            
            ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    collectWeatherData(locationId, locationQuery);
                }
            }, 0, COLLECTION_INTERVAL_SECONDS, TimeUnit.SECONDS);
            
            collectionTasks.put(locationId, task);
            log("Started collection task for " + locationId + " (" + locationQuery + ")");
        }
        
        publishSystemEvent("weather.system.collection.started");
    }
    
    private synchronized void stopCollection() {
        if (!isCollecting) {
            log("Weather collection not running");
            return;
        }
        
        log("Stopping weather data collection...");
        isCollecting = false;
        
        // Cancel all collection tasks
        for (Map.Entry<String, ScheduledFuture<?>> entry : collectionTasks.entrySet()) {
            entry.getValue().cancel(false);
            log("Stopped collection task for " + entry.getKey());
        }
        collectionTasks.clear();
        
        publishSystemEvent("weather.system.collection.stopped");
    }
    
    private void collectWeatherData(String locationId, String locationQuery) {
        long startTime = System.currentTimeMillis();
        
        try {
            log("Collecting weather data for " + locationId + " (" + locationQuery + ")");
            
            WeatherData weatherData = fetchWeatherFromApi(locationQuery);
            if (weatherData != null) {
                // Update location information
                weatherData = createUpdatedWeatherData(weatherData, locationId);
                
                // Publish weather data event
                publishWeatherData(weatherData);
                
                // Check for severe weather and publish alert if needed
                if (weatherData.isSevereWeather()) {
                    publishSevereWeatherAlert(weatherData);
                }
                
                long duration = System.currentTimeMillis() - startTime;
                log("Successfully collected and published weather data for " + locationId + 
                    " (took " + duration + "ms)");
                    
            } else {
                log("Failed to collect weather data for " + locationId);
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log("Error collecting weather data for " + locationId + ": " + e.getMessage() + 
                " (took " + duration + "ms)");
            e.printStackTrace();
        }
    }
    
    private WeatherData createUpdatedWeatherData(WeatherData original, String locationId) {
        return WeatherData.builder()
                .locationId(locationId)
                .locationName(original.getLocationName())
                .temperature(original.getTemperature())
                .humidity(original.getHumidity())
                .pressure(original.getPressure())
                .conditions(original.getConditions())
                .windSpeed(original.getWindSpeed())
                .windDirection(original.getWindDirection())
                .timestamp(original.getTimestamp())
                .source("WeatherCollectorAgent-OpenWeatherMap-v1.0")
                .build();
    }
    
    private WeatherData fetchWeatherFromApi(String locationQuery) throws IOException {
        // Increment API call counter
        long callNumber = apiCallCounter.incrementAndGet();
        totalApiCalls.incrementAndGet();
        
        String urlString = BASE_URL + "?q=" + locationQuery + "&appid=" + apiKey + "&units=metric";
        
        log("API Call #" + callNumber + " to OpenWeatherMap: " + locationQuery);
        
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000); // 10 seconds
        connection.setReadTimeout(15000);    // 15 seconds
        
        int responseCode = connection.getResponseCode();
        
        if (responseCode == 200) {
            // Read response
            StringBuilder response = new StringBuilder();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            // Parse JSON response (simple parsing without external dependencies)
            WeatherData weatherData = parseWeatherResponse(response.toString(), locationQuery);
            log("API Call #" + callNumber + " successful - parsed weather data");
            return weatherData;
            
        } else {
            // Read error response
            StringBuilder errorResponse = new StringBuilder();
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    errorResponse.append(line);
                }
                reader.close();
            } catch (Exception e) {
                // Ignore error stream reading issues
            }
            
            String error = "API Call #" + callNumber + " failed - HTTP " + responseCode + ": " + errorResponse.toString();
            log(error);
            return null;
        }
    }
    
    private WeatherData parseWeatherResponse(String jsonResponse, String locationQuery) {
        try {
            // Simple JSON parsing for the required fields
            String locationName = extractJsonValue(jsonResponse, "\"name\":");
            if (locationName.isEmpty()) locationName = "Unknown";
            
            String tempStr = extractJsonValue(jsonResponse, "\"temp\":");
            double temperature = tempStr.isEmpty() ? 0.0 : Double.parseDouble(tempStr);
            
            String humidityStr = extractJsonValue(jsonResponse, "\"humidity\":");
            double humidity = humidityStr.isEmpty() ? 0.0 : Double.parseDouble(humidityStr);
            
            String pressureStr = extractJsonValue(jsonResponse, "\"pressure\":");
            double pressure = pressureStr.isEmpty() ? 0.0 : Double.parseDouble(pressureStr);
            
            String conditions = extractJsonValue(jsonResponse, "\"main\":");
            if (conditions.isEmpty()) conditions = "Unknown";
            
            String speedStr = extractJsonValue(jsonResponse, "\"speed\":");
            double windSpeedMs = speedStr.isEmpty() ? 0.0 : Double.parseDouble(speedStr);
            double windSpeed = windSpeedMs * 3.6; // Convert m/s to km/h
            
            String degStr = extractJsonValue(jsonResponse, "\"deg\":");
            double windDirection = degStr.isEmpty() ? 0.0 : Double.parseDouble(degStr);
            String windDir = convertWindDirection(windDirection);
            
            return WeatherData.builder()
                    .locationId(locationQuery) // Will be updated by caller
                    .locationName(locationName)
                    .temperature(temperature)
                    .humidity(humidity)
                    .pressure(pressure)
                    .conditions(conditions)
                    .windSpeed(windSpeed)
                    .windDirection(windDir)
                    .timestamp(Instant.now())
                    .source("OpenWeatherMap-API")
                    .build();
                    
        } catch (Exception e) {
            log("Error parsing weather response: " + e.getMessage());
            return null;
        }
    }
    
    private String extractJsonValue(String json, String key) {
        try {
            int keyIndex = json.indexOf(key);
            if (keyIndex == -1) return "";
            
            int startIndex = json.indexOf(':', keyIndex) + 1;
            if (startIndex <= keyIndex) return "";
            
            // Skip whitespace
            while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
                startIndex++;
            }
            
            if (startIndex >= json.length()) return "";
            
            int endIndex;
            if (json.charAt(startIndex) == '"') {
                // String value
                startIndex++; // Skip opening quote
                endIndex = json.indexOf('"', startIndex);
                if (endIndex == -1) return "";
            } else {
                // Numeric value
                endIndex = startIndex;
                while (endIndex < json.length() && 
                       (Character.isDigit(json.charAt(endIndex)) || 
                        json.charAt(endIndex) == '.' || 
                        json.charAt(endIndex) == '-')) {
                    endIndex++;
                }
            }
            
            return json.substring(startIndex, endIndex);
        } catch (Exception e) {
            return "";
        }
    }
    
    private String convertWindDirection(double degrees) {
        String[] directions = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                              "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};
        int index = (int) Math.round(degrees / 22.5) % 16;
        return directions[index];
    }
    
    private void publishWeatherData(WeatherData weatherData) {
        if (getAgentContext() == null) return;
        
        try {
            // Publish general weather update
            publishEvent("weather.data.updated", weatherData);
            
            // Publish location-specific event  
            publishEvent("weather.location." + weatherData.getLocationId(), weatherData);
            
            log("Published weather events for " + weatherData.getLocationName() + 
                ": " + weatherData.getTemperature() + "°C, " + weatherData.getConditions());
                
        } catch (Exception e) {
            log("Error publishing weather data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void publishSevereWeatherAlert(WeatherData weatherData) {
        if (getAgentContext() == null) return;
        
        try {
            Map<String, Object> alertData = new HashMap<String, Object>();
            alertData.put("locationId", weatherData.getLocationId());
            alertData.put("locationName", weatherData.getLocationName());
            alertData.put("alertLevel", weatherData.getAlertLevel().toString());
            alertData.put("temperature", Double.valueOf(weatherData.getTemperature()));
            alertData.put("windSpeed", Double.valueOf(weatherData.getWindSpeed()));
            alertData.put("conditions", weatherData.getConditions());
            alertData.put("timestamp", weatherData.getTimestamp().toString());
            alertData.put("source", getAgentId().toString());
            
            publishEvent("weather.alert.severe", alertData);
            
            log("SEVERE WEATHER ALERT [" + weatherData.getAlertLevel() + "] for " + 
                weatherData.getLocationName() + ": " + weatherData.getConditions());
            
        } catch (Exception e) {
            log("Error publishing severe weather alert: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void publishSystemEvent(String topic) {
        if (getAgentContext() == null) return;
        
        try {
            Map<String, Object> eventData = new HashMap<String, Object>();
            eventData.put("agentId", getAgentId().toString());
            eventData.put("timestamp", Instant.now().toString());
            eventData.put("isCollecting", Boolean.valueOf(isCollecting));
            eventData.put("apiCallsThisSession", Long.valueOf(apiCallCounter.get()));
            eventData.put("totalApiCalls", Long.valueOf(totalApiCalls.get()));
            
            publishEvent(topic, eventData);
            log("Published system event: " + topic);
        } catch (Exception e) {
            log("Error publishing system event: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void publishStatus() {
        publishSystemEvent("weather.system.collector.status");
        logApiCallStatistics();
    }
    
    private void logApiCallStatistics() {
        long sessionCalls = apiCallCounter.get();
        long totalCalls = totalApiCalls.get();
        
        String stats = String.format(
            "API Call Statistics - Session: %d calls, Total: %d calls", 
            sessionCalls, totalCalls
        );
        
        log(stats);
    }
    
    // Public API methods
    
    public void addLocation(String locationId, String locationQuery) {
        locations.put(locationId, locationQuery);
        log("Added location: " + locationId + " (" + locationQuery + ")");
        
        // If currently collecting, start task for new location
        if (isCollecting && scheduler != null) {
            final String finalLocationId = locationId;
            final String finalLocationQuery = locationQuery;
            ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    collectWeatherData(finalLocationId, finalLocationQuery);
                }
            }, 0, COLLECTION_INTERVAL_SECONDS, TimeUnit.SECONDS);
            
            collectionTasks.put(locationId, task);
            log("Started collection task for new location: " + locationId);
        }
    }
    
    public void removeLocation(String locationId) {
        String removed = locations.remove(locationId);
        if (removed != null) {
            log("Removed location: " + locationId + " (" + removed + ")");
            
            // Cancel collection task if running
            ScheduledFuture<?> task = collectionTasks.remove(locationId);
            if (task != null) {
                task.cancel(false);
                log("Cancelled collection task for removed location: " + locationId);
            }
        }
    }
    
    public Map<String, String> getLocations() {
        return new HashMap<String, String>(locations);
    }
    
    public boolean isCollecting() {
        return isCollecting;
    }
    
    public long getApiCallCount() {
        return apiCallCounter.get();
    }
    
    public long getTotalApiCalls() {
        return totalApiCalls.get();
    }
    
    public String getApiKeyType() {
        return apiKey.equals(DEFAULT_API_KEY) ? "default" : "custom";
    }
    
    public long getCollectionIntervalSeconds() {
        return COLLECTION_INTERVAL_SECONDS;
    }
    
    private void log(String message) {
        String logMessage = "[" + getAgentId() + "] " + message;
        System.out.println(logMessage);
    }
}