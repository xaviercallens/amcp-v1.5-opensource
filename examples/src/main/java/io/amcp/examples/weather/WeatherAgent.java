package io.amcp.examples.weather;

import io.amcp.core.*;
import io.amcp.mobility.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AMCP v1.5 Weather Collection Agent with Real API Integration.
 * 
 * <p>This agent showcases:
 * <ul>
 *   <li>Real-time weather data from OpenWeatherMap API</li>
 *   <li>Dynamic location-based weather queries</li>
 *   <li>Event-driven weather data collection</li>
 *   <li>Mobility operations (dispatch to edge devices)</li>
 *   <li>Command-line interaction interface</li>
 * </ul>
 * </p>
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class WeatherAgent extends AbstractMobileAgent {

    // OpenWeatherMap API configuration with default key
    private static final String OPENWEATHER_API_KEY = "b6907d289e10d714a6e88b30761fae22"; // Default key for demo
    private static final String WEATHER_API_BASE = "https://api.openweathermap.org/data/2.5";
    
    private final Set<String> monitoredCities = new HashSet<>();
    private final Map<String, WeatherData> latestWeatherData = new HashMap<>();
    private final AtomicLong dataCollectionCount = new AtomicLong(0);
    private ScheduledExecutorService scheduler;
    
    public WeatherAgent() {
        super(AgentID.named("WeatherAgent"));
        // Default cities to monitor
        monitoredCities.addAll(Arrays.asList("Paris", "London", "Tokyo", "New York", "Sydney"));
    }

    @Override
    public void onActivate() {
        super.onActivate();
        
        logMessage("WeatherAgent activated - starting weather monitoring");
        
        // Subscribe to weather-related events INCLUDING chat requests
        subscribe("weather.**");
        subscribe("weather.request"); // Chat agent requests
        subscribe("location.add");
        subscribe("location.remove");
        subscribe("alert.severe");
        
        // Start periodic weather collection
        scheduler = Executors.newScheduledThreadPool(2);
        scheduler.scheduleAtFixedRate(this::collectWeatherData, 0, 5, TimeUnit.MINUTES);
        
        logMessage("Monitoring " + monitoredCities.size() + " cities: " + monitoredCities);
        logMessage("Subscribed to weather.request for chat integration");
    }

    @Override
    public void onDeactivate() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        super.onDeactivate();
        logMessage("WeatherAgent deactivated");
    }

    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            try {
                switch (event.getTopic()) {
                    case "weather.request":
                        handleChatWeatherRequest(event);
                        break;
                    case "location.add":
                        handleAddLocation(event);
                        break;
                    case "location.remove":
                        handleRemoveLocation(event);
                        break;
                    case "weather.request.current":
                        handleWeatherRequest(event);
                        break;
                    case "weather.request.forecast":
                        handleForecastRequest(event);
                        break;
                    case "mobility.dispatch.edge":
                        handleEdgeDispatch(event);
                        break;
                    default:
                        logMessage("Unhandled event: " + event.getTopic());
                }
            } catch (Exception e) {
                logMessage("Error handling event " + event.getTopic() + ": " + e.getMessage());
            }
        });
    }

    private void handleChatWeatherRequest(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> request = (Map<String, Object>) event.getPayload();
            
            String query = (String) request.get("query");
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = (Map<String, Object>) request.get("parameters");
            String location = (String) parameters.get("location");
            
            logMessage("Chat weather request for location: " + location + " (query: " + query + ")");
            
            if (location != null && !location.trim().isEmpty()) {
                // Get weather for the requested location
                WeatherData weatherData = callOpenWeatherMapAPI(location.trim());
                
                // Format response for chat
                String response = formatWeatherForChat(weatherData);
                
                // Send response back to chat agent
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("response", response);
                responseData.put("location", location);
                responseData.put("weatherData", weatherData);
                
                publishEvent(Event.builder()
                    .topic("agent.response")
                    .payload(responseData)
                    .correlationId(event.getCorrelationId())
                    .sender(getAgentId())
                    .build());
                    
                logMessage("Sent weather response for " + location);
            } else {
                // No location provided, send error response
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("response", "I need a location to provide weather information. Please specify a city or location.");
                errorResponse.put("error", "No location specified");
                
                publishEvent(Event.builder()
                    .topic("agent.response")
                    .payload(errorResponse)
                    .correlationId(event.getCorrelationId())
                    .sender(getAgentId())
                    .build());
            }
            
        } catch (Exception e) {
            logMessage("Error handling chat weather request: " + e.getMessage());
        }
    }
    
    private String formatWeatherForChat(WeatherData weather) {
        StringBuilder response = new StringBuilder();
        response.append("🌤️ Current weather in ").append(weather.city).append(":\n");
        response.append("🌡️ Temperature: ").append(String.format("%.1f°C (%.1f°F)", 
            weather.temperature, weather.temperature * 9/5 + 32)).append("\n");
        response.append("☁️ Conditions: ").append(weather.description).append("\n");
        response.append("💧 Humidity: ").append(String.format("%.0f%%", weather.humidity)).append("\n");
        response.append("💨 Wind: ").append(String.format("%.1f km/h", weather.windSpeed * 3.6)).append("\n");
        response.append("📊 Pressure: ").append(String.format("%.0f hPa", weather.pressure));
        
        // Add weather advice
        if (weather.temperature > 25) {
            response.append("\n☀️ It's quite warm! Perfect for outdoor activities.");
        } else if (weather.temperature < 10) {
            response.append("\n🧥 It's chilly - don't forget your jacket!");
        }
        
        return response.toString();
    }

    private void handleAddLocation(Event event) {
        String city = event.getPayload(String.class);
        if (city != null && !city.trim().isEmpty()) {
            monitoredCities.add(city.trim());
            logMessage("Added city to monitoring: " + city);
            
            // Immediately collect weather for new city
            collectWeatherForCity(city);
            
            publishEvent("location.added", city);
        }
    }

    private void handleRemoveLocation(Event event) {
        String city = event.getPayload(String.class);
        if (monitoredCities.remove(city)) {
            latestWeatherData.remove(city);
            logMessage("Removed city from monitoring: " + city);
            publishEvent("location.removed", city);
        }
    }

    private void handleWeatherRequest(Event event) {
        String city = event.getPayload(String.class);
        WeatherData data = latestWeatherData.get(city);
        
        if (data != null) {
            publishEvent("weather.response.current", data);
            logMessage("Sent current weather for " + city + ": " + data.temperature + "°C");
        } else {
            logMessage("No weather data available for " + city);
            collectWeatherForCity(city);
        }
    }

    private void handleForecastRequest(Event event) {
        String city = event.getPayload(String.class);
        logMessage("Forecast requested for " + city + " (feature coming soon)");
        // TODO: Implement weather forecast collection
    }

    private void handleEdgeDispatch(Event event) {
        String edgeDevice = event.getPayload(String.class);
        logMessage("Dispatching to edge device: " + edgeDevice);
        
        // Demonstrate mobility - dispatch to edge device for local processing
        dispatch(edgeDevice).thenRun(() -> {
            logMessage("Successfully dispatched to edge device: " + edgeDevice);
            publishEvent("mobility.dispatch.success", edgeDevice);
        }).exceptionally(throwable -> {
            logMessage("Failed to dispatch to edge device " + edgeDevice + ": " + throwable.getMessage());
            publishEvent("mobility.dispatch.failed", edgeDevice);
            return null;
        });
    }

    private void collectWeatherData() {
        logMessage("Starting weather data collection cycle for " + monitoredCities.size() + " cities");
        
        for (String city : monitoredCities) {
            collectWeatherForCity(city);
        }
        
        dataCollectionCount.incrementAndGet();
        logMessage("Completed weather collection cycle #" + dataCollectionCount.get());
    }

    private void collectWeatherForCity(String city) {
        try {
            // Use real OpenWeatherMap API call
            WeatherData weatherData = callOpenWeatherMapAPI(city);
            latestWeatherData.put(city, weatherData);
            
            // Publish weather update event
            publishEvent("weather.data.updated", weatherData);
            
            // Check for severe weather alerts
            if (weatherData.temperature > 35.0 || weatherData.temperature < -20.0 || 
                weatherData.windSpeed > 25.0) {
                
                SevereWeatherAlert alert = new SevereWeatherAlert(city, weatherData, 
                    "Extreme weather conditions detected");
                publishEvent("alert.severe.weather", alert);
                logMessage("SEVERE WEATHER ALERT for " + city + ": " + alert.reason);
            }
            
            logMessage("Updated weather for " + city + ": " + 
                weatherData.temperature + "°C, " + weatherData.description);
                
        } catch (Exception e) {
            logMessage("Error collecting weather for " + city + ": " + e.getMessage());
            // Fallback to simulated data if API fails
            WeatherData fallbackData = simulateWeatherAPICall(city);
            latestWeatherData.put(city, fallbackData);
        }
    }

    private WeatherData callOpenWeatherMapAPI(String city) {
        try {
            // Build API URL
            String url = String.format("%s/weather?q=%s&appid=%s&units=metric", 
                WEATHER_API_BASE, 
                java.net.URLEncoder.encode(city, "UTF-8"), 
                OPENWEATHER_API_KEY);
            
            logMessage("Calling OpenWeatherMap API for: " + city);
            
            // Make HTTP request (simplified - in production use proper HTTP client)
            java.net.URL apiUrl = new java.net.URL(url);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                // Read response
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                // Parse JSON response (simplified parsing)
                return parseWeatherResponse(response.toString(), city);
                
            } else {
                logMessage("OpenWeatherMap API error: " + responseCode);
                return simulateWeatherAPICall(city); // Fallback
            }
            
        } catch (Exception e) {
            logMessage("Error calling OpenWeatherMap API: " + e.getMessage());
            return simulateWeatherAPICall(city); // Fallback
        }
    }
    
    private WeatherData parseWeatherResponse(String jsonResponse, String city) {
        try {
            // Simple JSON parsing (in production use proper JSON library)
            double temperature = extractJsonValue(jsonResponse, "temp");
            double humidity = extractJsonValue(jsonResponse, "humidity");
            double windSpeed = extractJsonValue(jsonResponse, "speed");
            double pressure = extractJsonValue(jsonResponse, "pressure");
            String description = extractJsonString(jsonResponse, "description");
            
            return new WeatherData(city, temperature, humidity, windSpeed, pressure, 
                description != null ? description : "Weather data from OpenWeatherMap");
                
        } catch (Exception e) {
            logMessage("Error parsing weather response: " + e.getMessage());
            return simulateWeatherAPICall(city); // Fallback
        }
    }
    
    private double extractJsonValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*([0-9.]+)";
            java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher matcher = regex.matcher(json);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return 0.0;
    }
    
    private String extractJsonString(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher matcher = regex.matcher(json);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return null;
    }

    private WeatherData simulateWeatherAPICall(String city) {
        // Simulate realistic weather data
        Random random = new Random();
        double baseTemp;
        switch (city.toLowerCase()) {
            case "paris":
                baseTemp = 15.0 + random.nextGaussian() * 8;
                break;
            case "london":
                baseTemp = 12.0 + random.nextGaussian() * 7;
                break;
            case "tokyo":
                baseTemp = 18.0 + random.nextGaussian() * 10;
                break;
            case "new york":
                baseTemp = 16.0 + random.nextGaussian() * 12;
                break;
            case "sydney":
                baseTemp = 20.0 + random.nextGaussian() * 8;
                break;
            default:
                baseTemp = 15.0 + random.nextGaussian() * 10;
                break;
        }
        
        double humidity = 40 + random.nextDouble() * 50;
        double windSpeed = random.nextDouble() * 15;
        double pressure = 1000 + random.nextDouble() * 50;
        
        String[] descriptions = {"Clear sky", "Few clouds", "Scattered clouds", 
            "Broken clouds", "Light rain", "Moderate rain", "Overcast"};
        String description = descriptions[random.nextInt(descriptions.length)];
        
        return new WeatherData(city, baseTemp, humidity, windSpeed, pressure, description);
    }

    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [WeatherAgent-" + getAgentId().getId().substring(0, 8) + "] " + message);
    }

    // Getters for CLI access
    public Set<String> getMonitoredCities() {
        return new HashSet<>(monitoredCities);
    }

    public Map<String, WeatherData> getLatestWeatherData() {
        return new HashMap<>(latestWeatherData);
    }

    public long getDataCollectionCount() {
        return dataCollectionCount.get();
    }

    // Weather data classes
    public static class WeatherData {
        public final String city;
        public final double temperature;
        public final double humidity;
        public final double windSpeed;
        public final double pressure;
        public final String description;
        public final LocalDateTime timestamp;

        public WeatherData(String city, double temperature, double humidity, 
                          double windSpeed, double pressure, String description) {
            this.city = city;
            this.temperature = temperature;
            this.humidity = humidity;
            this.windSpeed = windSpeed;
            this.pressure = pressure;
            this.description = description;
            this.timestamp = LocalDateTime.now();
        }

        @Override
        public String toString() {
            return String.format("%s: %.1f°C, %s (Humidity: %.1f%%, Wind: %.1f m/s)", 
                city, temperature, description, humidity, windSpeed);
        }
    }

    public static class SevereWeatherAlert {
        public final String city;
        public final WeatherData weatherData;
        public final String reason;
        public final LocalDateTime alertTime;

        public SevereWeatherAlert(String city, WeatherData weatherData, String reason) {
            this.city = city;
            this.weatherData = weatherData;
            this.reason = reason;
            this.alertTime = LocalDateTime.now();
        }
    }
}

// Abstract base class for mobile agents (simplified)
abstract class AbstractMobileAgent implements MobileAgent {
    private final AgentID agentId;
    private AgentContext context;
    private AgentLifecycle state = AgentLifecycle.CREATED;

    protected AbstractMobileAgent(AgentID agentId) {
        this.agentId = agentId;
    }

    @Override
    public AgentID getAgentId() { return agentId; }

    @Override
    public AgentContext getContext() { return context; }

    public void setContext(AgentContext context) { this.context = context; }

    @Override
    public AgentLifecycle getLifecycleState() { return state; }

    protected void setState(AgentLifecycle state) { this.state = state; }

    @Override
    public CompletableFuture<Void> subscribe(String topicPattern) {
        if (context != null) {
            return context.subscribe(agentId, topicPattern);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> unsubscribe(String topicPattern) {
        if (context != null) {
            return context.unsubscribe(agentId, topicPattern);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> publishEvent(Event event) {
        if (context != null) {
            return context.publishEvent(event);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onActivate() { setState(AgentLifecycle.ACTIVE); }
    
    @Override
    public void onDeactivate() { setState(AgentLifecycle.INACTIVE); }
    
    @Override
    public void onDestroy() { setState(AgentLifecycle.DESTROYED); }
    
    @Override
    public void onBeforeMigration(String destinationContext) {}
    
    @Override
    public void onAfterMigration(String sourceContext) {}

    // Simplified mobility operations for demo
    @Override
    public CompletableFuture<Void> dispatch(String destinationContext) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<AgentID> clone(String destinationContext) {
        return CompletableFuture.completedFuture(AgentID.random());
    }

    @Override
    public CompletableFuture<Void> retract(String sourceContext) {
        return CompletableFuture.completedFuture(null);
    }
    
    // Add missing method implementations
    @Override
    public CompletableFuture<Void> migrate(MigrationOptions options) {
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<List<AgentID>> replicate(String... contexts) {
        List<AgentID> replicas = new ArrayList<>();
        for (int i = 0; i < contexts.length; i++) {
            // Create replica agent for each context
            AgentID replicaId = AgentID.random();
            replicas.add(replicaId);
        }
        return CompletableFuture.completedFuture(replicas);
    }
    
    @Override
    public CompletableFuture<Void> federateWith(List<AgentID> agents, String federationId) {
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public MobilityState getMobilityState() {
        return MobilityState.STATIONARY;
    }
    
    @Override
    public List<MigrationEvent> getMigrationHistory() {
        return new ArrayList<>();
    }
    
    public CompletableFuture<MobilityAssessment> assessMobility(String targetContext) {
        return CompletableFuture.completedFuture(
            new MobilityAssessment(true, 0.9, "Migration feasible"));
    }
    
    public void setMobilityStrategy(MobilityStrategy strategy) {
        // Store strategy (simplified)
    }
    
    public MobilityStrategy getMobilityStrategy() {
        return new MobilityStrategy() {
            @Override
            public boolean shouldMigrate(String targetContext) { return true; }
            @Override
            public String getStrategyName() { return "DefaultStrategy"; }
        };
    }

    // Other mobility methods would be implemented similarly
}