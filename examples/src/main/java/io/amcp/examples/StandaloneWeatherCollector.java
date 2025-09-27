package io.amcp.examples.weather;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Standalone Weather Collector Agent that fetches weather data from OpenWeatherMap API.
 * Designed to run every 5 minutes and log weather information for configured locations.
 */
public class StandaloneWeatherCollector {
    
    // Default API key - can be overridden with environment variable
    private static final String DEFAULT_API_KEY = "3bd965f39881ba0f116ee0810fdfd058";
    private static final String API_BASE_URL = "http://api.openweathermap.org/data/2.5/weather";
    private static final long COLLECTION_INTERVAL_MINUTES = 5;
    
    private final String apiKey;
    private final Map<String, String> locations = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final AtomicLong apiCallCount = new AtomicLong(0);
    private final String agentId;
    
    private boolean isCollecting = false;
    
    /**
     * Constructor
     */
    public StandaloneWeatherCollector(String agentId) {
        this.agentId = agentId;
        this.apiKey = System.getenv("OPENWEATHER_API_KEY") != null ? 
            System.getenv("OPENWEATHER_API_KEY") : DEFAULT_API_KEY;
        
        logMessage("Standalone Weather Collector initialized");
        logMessage("API Key type: " + getApiKeyType());
        logMessage("Collection interval: " + COLLECTION_INTERVAL_MINUTES + " minutes");
    }
    
    /**
     * Start the weather collection system
     */
    public void start() {
        if (isCollecting) {
            logMessage("Weather collection is already running");
            return;
        }
        
        logMessage("Starting weather collection...");
        isCollecting = true;
        
        // Schedule weather collection every 5 minutes
        scheduler.scheduleAtFixedRate(
            this::collectWeatherForAllLocations,
            0, // Initial delay
            COLLECTION_INTERVAL_MINUTES, 
            TimeUnit.MINUTES
        );
        
        logMessage("Weather collection started - running every " + COLLECTION_INTERVAL_MINUTES + " minutes");
    }
    
    /**
     * Stop the weather collection system
     */
    public void stop() {
        if (!isCollecting) {
            logMessage("Weather collection is not running");
            return;
        }
        
        logMessage("Stopping weather collection...");
        isCollecting = false;
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logMessage("Weather collection stopped");
    }
    
    /**
     * Add a location for weather monitoring
     */
    public void addLocation(String locationId, String locationQuery) {
        locations.put(locationId, locationQuery);
        logMessage("Added location: " + locationQuery + " [" + locationId + "]");
        
        // If collecting is active, fetch weather immediately for this location
        if (isCollecting) {
            scheduler.execute(() -> collectWeatherForLocation(locationId, locationQuery));
        }
    }
    
    /**
     * Remove a location from monitoring
     */
    public void removeLocation(String locationId) {
        String locationQuery = locations.remove(locationId);
        if (locationQuery != null) {
            logMessage("Removed location: " + locationQuery + " [" + locationId + "]");
        } else {
            logMessage("Location not found: " + locationId);
        }
    }
    
    /**
     * Get all configured locations
     */
    public Map<String, String> getLocations() {
        return new HashMap<>(locations);
    }
    
    /**
     * Check if collection is active
     */
    public boolean isCollecting() {
        return isCollecting;
    }
    
    /**
     * Get API key type (default or custom)
     */
    public String getApiKeyType() {
        return apiKey.equals(DEFAULT_API_KEY) ? "Default" : "Custom";
    }
    
    /**
     * Get collection interval in seconds
     */
    public long getCollectionIntervalSeconds() {
        return COLLECTION_INTERVAL_MINUTES * 60;
    }
    
    /**
     * Get total API call count
     */
    public long getApiCallCount() {
        return apiCallCount.get();
    }
    
    /**
     * Get agent ID
     */
    public String getAgentId() {
        return agentId;
    }
    
    /**
     * Collect weather for all configured locations
     */
    private void collectWeatherForAllLocations() {
        if (locations.isEmpty()) {
            logMessage("No locations configured for weather collection");
            return;
        }
        
        logMessage("=== Starting weather collection cycle for " + locations.size() + " locations ===");
        
        for (Map.Entry<String, String> location : locations.entrySet()) {
            try {
                collectWeatherForLocation(location.getKey(), location.getValue());
                
                // Add small delay between API calls to be respectful to the service
                Thread.sleep(1000);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logMessage("Error collecting weather for " + location.getKey() + ": " + e.getMessage());
            }
        }
        
        logMessage("=== Weather collection cycle completed ===");
    }
    
    /**
     * Collect weather for a specific location
     */
    private void collectWeatherForLocation(String locationId, String locationQuery) {
        try {
            logMessage("Fetching weather for " + locationQuery + " [" + locationId + "]");
            
            String weatherData = fetchWeatherFromApi(locationQuery);
            processWeatherData(locationId, locationQuery, weatherData);
            
        } catch (Exception e) {
            logMessage("Failed to collect weather for " + locationId + ": " + e.getMessage());
        }
    }
    
    /**
     * Fetch weather data from OpenWeatherMap API
     */
    private String fetchWeatherFromApi(String locationQuery) throws IOException {
        apiCallCount.incrementAndGet();
        
        String urlString = API_BASE_URL + "?q=" + URLEncoder.encode(locationQuery, "UTF-8") + 
                          "&appid=" + apiKey + "&units=metric";
        
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000); // 10 seconds
            connection.setReadTimeout(10000);   // 10 seconds
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()))) {
                    
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    
                    return response.toString();
                }
            } else {
                throw new IOException("HTTP error code: " + responseCode);
            }
            
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * Process and log weather data
     */
    private void processWeatherData(String locationId, String locationQuery, String jsonData) {
        try {
            // Parse key weather information from JSON response
            // This is a simple parser - in production, you'd use a JSON library like Jackson or Gson
            WeatherInfo weather = parseWeatherData(jsonData);
            
            // Log the weather information
            logWeatherInfo(locationId, locationQuery, weather);
            
            // Check for severe weather conditions
            checkSevereWeather(locationId, locationQuery, weather);
            
        } catch (Exception e) {
            logMessage("Error processing weather data for " + locationId + ": " + e.getMessage());
        }
    }
    
    /**
     * Simple weather data parser (basic JSON parsing without external libraries)
     */
    private WeatherInfo parseWeatherData(String jsonData) {
        WeatherInfo weather = new WeatherInfo();
        
        try {
            // Extract temperature
            String tempPattern = "\"temp\":";
            int tempIndex = jsonData.indexOf(tempPattern);
            if (tempIndex != -1) {
                int startIndex = tempIndex + tempPattern.length();
                int endIndex = jsonData.indexOf(",", startIndex);
                if (endIndex == -1) endIndex = jsonData.indexOf("}", startIndex);
                weather.temperature = Double.parseDouble(jsonData.substring(startIndex, endIndex).trim());
            }
            
            // Extract humidity
            String humidityPattern = "\"humidity\":";
            int humidityIndex = jsonData.indexOf(humidityPattern);
            if (humidityIndex != -1) {
                int startIndex = humidityIndex + humidityPattern.length();
                int endIndex = jsonData.indexOf(",", startIndex);
                if (endIndex == -1) endIndex = jsonData.indexOf("}", startIndex);
                weather.humidity = Integer.parseInt(jsonData.substring(startIndex, endIndex).trim());
            }
            
            // Extract pressure
            String pressurePattern = "\"pressure\":";
            int pressureIndex = jsonData.indexOf(pressurePattern);
            if (pressureIndex != -1) {
                int startIndex = pressureIndex + pressurePattern.length();
                int endIndex = jsonData.indexOf(",", startIndex);
                if (endIndex == -1) endIndex = jsonData.indexOf("}", startIndex);
                weather.pressure = Double.parseDouble(jsonData.substring(startIndex, endIndex).trim());
            }
            
            // Extract wind speed
            String windSpeedPattern = "\"speed\":";
            int windIndex = jsonData.indexOf(windSpeedPattern);
            if (windIndex != -1) {
                int startIndex = windIndex + windSpeedPattern.length();
                int endIndex = jsonData.indexOf(",", startIndex);
                if (endIndex == -1) endIndex = jsonData.indexOf("}", startIndex);
                weather.windSpeed = Double.parseDouble(jsonData.substring(startIndex, endIndex).trim());
            }
            
            // Extract description
            String descPattern = "\"description\":\"";
            int descIndex = jsonData.indexOf(descPattern);
            if (descIndex != -1) {
                int startIndex = descIndex + descPattern.length();
                int endIndex = jsonData.indexOf("\"", startIndex);
                weather.description = jsonData.substring(startIndex, endIndex);
            }
            
            // Extract city name
            String namePattern = "\"name\":\"";
            int nameIndex = jsonData.indexOf(namePattern);
            if (nameIndex != -1) {
                int startIndex = nameIndex + namePattern.length();
                int endIndex = jsonData.indexOf("\"", startIndex);
                weather.cityName = jsonData.substring(startIndex, endIndex);
            }
            
        } catch (Exception e) {
            logMessage("Error parsing weather data: " + e.getMessage());
        }
        
        return weather;
    }
    
    /**
     * Log weather information in a nice format
     */
    private void logWeatherInfo(String locationId, String locationQuery, WeatherInfo weather) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        logMessage(">>> WEATHER UPDATE for " + locationQuery + " [" + locationId + "] <<<");
        logMessage("    Timestamp: " + timestamp);
        logMessage("    City: " + weather.cityName);
        logMessage("    Description: " + weather.description);
        logMessage("    Temperature: " + String.format("%.1f°C", weather.temperature));
        logMessage("    Humidity: " + weather.humidity + "%");
        logMessage("    Pressure: " + String.format("%.1f hPa", weather.pressure));
        logMessage("    Wind Speed: " + String.format("%.1f m/s", weather.windSpeed));
        logMessage("    API Call Count: " + apiCallCount.get());
        logMessage(">>> END WEATHER UPDATE <<<");
    }
    
    /**
     * Check for severe weather conditions and log alerts
     */
    private void checkSevereWeather(String locationId, String locationQuery, WeatherInfo weather) {
        List<String> alerts = new ArrayList<>();
        
        // Temperature thresholds
        if (weather.temperature > 35.0) {
            alerts.add("EXTREME HEAT: " + String.format("%.1f°C", weather.temperature));
        } else if (weather.temperature < -20.0) {
            alerts.add("EXTREME COLD: " + String.format("%.1f°C", weather.temperature));
        }
        
        // Wind speed threshold (convert m/s to km/h for readability)
        double windKmh = weather.windSpeed * 3.6;
        if (windKmh > 50.0) {
            alerts.add("HIGH WINDS: " + String.format("%.1f km/h", windKmh));
        }
        
        // Pressure threshold
        if (weather.pressure < 980.0) {
            alerts.add("LOW PRESSURE: " + String.format("%.1f hPa", weather.pressure));
        }
        
        // Log any alerts
        if (!alerts.isEmpty()) {
            logMessage("*** SEVERE WEATHER ALERT for " + locationQuery + " [" + locationId + "] ***");
            for (String alert : alerts) {
                logMessage("    ⚠️  " + alert);
            }
            logMessage("*** END ALERT ***");
        }
    }
    
    /**
     * Log a message with timestamp and agent ID
     */
    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [" + agentId + "] " + message);
    }
    
    /**
     * Simple weather information container
     */
    private static class WeatherInfo {
        double temperature = 0.0;
        int humidity = 0;
        double pressure = 0.0;
        double windSpeed = 0.0;
        String description = "Unknown";
        String cityName = "Unknown";
    }
}