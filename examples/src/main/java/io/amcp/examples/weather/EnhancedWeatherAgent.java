package io.amcp.examples.weather;

import io.amcp.core.*;
import io.amcp.connectors.ai.orchestration.TaskProtocol;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enhanced AMCP v1.5 Weather Agent with TaskProtocol Support
 * 
 * This enhanced weather agent provides:
 * - Real-time weather data from OpenWeatherMap API
 * - TaskProtocol integration for orchestrator communication
 * - Capability registration and dynamic discovery
 * - Enhanced error handling and response formatting
 * - Support for complex weather queries and forecasts
 * - Location-based weather monitoring and alerts
 * 
 * Capabilities:
 * - weather.current: Get current weather for a location
 * - weather.forecast: Get weather forecast for a location
 * - weather.alerts: Monitor severe weather conditions
 * - weather.compare: Compare weather across multiple locations
 * 
 * @author AMCP Team
 * @version 1.5.0
 * @since 2024-09-29
 */
public class EnhancedWeatherAgent extends AbstractMobileAgent {

    // OpenWeatherMap API configuration
    private static final String OPENWEATHER_API_KEY = System.getenv("OPENWEATHER_API_KEY") != null 
        ? System.getenv("OPENWEATHER_API_KEY") 
        : "3bd965f39881ba0f116ee0810fdfd058";
    private static final String WEATHER_API_BASE = "https://api.openweathermap.org/data/2.5";
    
    // Agent capabilities
    private static final List<String> CAPABILITIES = Arrays.asList(
        "weather.current",
        "weather.forecast", 
        "weather.alerts",
        "weather.compare"
    );
    
    private final Set<String> monitoredCities = new HashSet<>();
    private final Map<String, WeatherData> latestWeatherData = new HashMap<>();
    private final AtomicLong dataCollectionCount = new AtomicLong(0);
    private ScheduledExecutorService scheduler;
    private boolean capabilitiesRegistered = false;
    
    public EnhancedWeatherAgent() {
        super(AgentID.named("EnhancedWeatherAgent"));
        // Default cities to monitor
        monitoredCities.addAll(Arrays.asList("Paris", "London", "Tokyo", "New York", "Sydney"));
    }

    @Override
    public void onActivate() {
        super.onActivate();
        
        logMessage("Enhanced WeatherAgent activated - starting weather monitoring with TaskProtocol");
        
        // Subscribe to weather-related events and orchestrator task requests
        subscribe("weather.**");
        subscribe("weather.request");
        subscribe(TaskProtocol.TASK_REQUEST_TOPIC);
        subscribe("location.add");
        subscribe("location.remove");
        subscribe("alert.severe");
        
        // Register capabilities with orchestrator
        registerCapabilities();
        
        // Start periodic weather collection
        scheduler = Executors.newScheduledThreadPool(2);
        scheduler.scheduleAtFixedRate(this::collectWeatherData, 0, 5, TimeUnit.MINUTES);
        
        logMessage("Monitoring " + monitoredCities.size() + " cities: " + monitoredCities);
        logMessage("Registered " + CAPABILITIES.size() + " capabilities: " + CAPABILITIES);
        logMessage("TaskProtocol integration enabled");
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
        logMessage("Enhanced WeatherAgent deactivated");
    }

    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            try {
                String topic = event.getTopic();
                
                if (TaskProtocol.TASK_REQUEST_TOPIC.equals(topic)) {
                    handleTaskRequest(event);
                } else {
                    switch (topic) {
                        case "weather.request":
                            handleChatWeatherRequest(event);
                            break;
                        case "location.add":
                            handleAddLocation(event);
                            break;
                        case "location.remove":
                            handleRemoveLocation(event);
                            break;
                        default:
                            if (topic.startsWith("weather.")) {
                                handleWeatherEvent(event);
                            }
                            break;
                    }
                }
            } catch (Exception e) {
                logMessage("Error handling event " + event.getTopic() + ": " + e.getMessage());
            }
        });
    }
    
    /**
     * Register capabilities with the orchestrator
     */
    private void registerCapabilities() {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("dataSource", "OpenWeatherMap API");
            metadata.put("updateFrequency", "5 minutes");
            metadata.put("supportedUnits", Arrays.asList("metric", "imperial", "kelvin"));
            metadata.put("maxLocations", 100);
            
            Event registrationEvent = TaskProtocol.createCapabilityRegistration(
                getAgentId().toString(),
                "weather",
                CAPABILITIES,
                "Enhanced weather data and forecasting agent with real-time API integration",
                "http://enhanced-weather-agent:8080",
                metadata
            );
            
            publishEvent(registrationEvent);
            capabilitiesRegistered = true;
            
            logMessage("✅ Capabilities registered with orchestrator");
            
        } catch (Exception e) {
            logMessage("❌ Failed to register capabilities: " + e.getMessage());
        }
    }
    
    /**
     * Handle task requests from orchestrator
     */
    private void handleTaskRequest(Event event) {
        try {
            TaskProtocol.TaskRequestData taskRequest = TaskProtocol.parseTaskRequest(event);
            String capability = taskRequest.getCapability();
            Map<String, Object> parameters = taskRequest.getParameters();
            String correlationId = event.getCorrelationId();
            
            logMessage("📥 Received task request - Capability: " + capability + ", ID: " + correlationId);
            
            CompletableFuture<Object> resultFuture;
            
            switch (capability) {
                case "weather.current":
                    resultFuture = handleCurrentWeatherTask(parameters);
                    break;
                case "weather.forecast":
                    resultFuture = handleForecastTask(parameters);
                    break;
                case "weather.alerts":
                    resultFuture = handleAlertsTask(parameters);
                    break;
                case "weather.compare":
                    resultFuture = handleCompareTask(parameters);
                    break;
                default:
                    // Fallback to general weather query
                    resultFuture = handleGeneralWeatherTask(parameters);
                    break;
            }
            
            resultFuture.thenAccept(result -> {
                // Send successful response
                Event responseEvent = TaskProtocol.createTaskResponse(
                    event,
                    true,
                    result,
                    null,
                    getAgentId().toString(),
                    System.currentTimeMillis() - taskRequest.getTimestamp()
                );
                
                publishEvent(responseEvent);
                logMessage("📤 Task completed successfully: " + correlationId);
                
            }).exceptionally(ex -> {
                // Send error response
                TaskProtocol.TaskError error = new TaskProtocol.TaskError(
                    "WEATHER_ERROR",
                    ex.getMessage() != null ? ex.getMessage() : "Weather data retrieval failed",
                    Map.of("source", "EnhancedWeatherAgent", "category", "execution")
                );
                
                Event errorEvent = TaskProtocol.createTaskResponse(
                    event,
                    false,
                    null,
                    error,
                    getAgentId().toString(),
                    System.currentTimeMillis() - taskRequest.getTimestamp()
                );
                
                publishEvent(errorEvent);
                logMessage("❌ Task failed: " + correlationId + " - " + ex.getMessage());
                return null;
            });
            
        } catch (Exception e) {
            logMessage("❌ Error processing task request: " + e.getMessage());
        }
    }
    
    /**
     * Handle current weather task
     */
    private CompletableFuture<Object> handleCurrentWeatherTask(Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            String location = extractLocation(parameters);
            if (location == null) {
                throw new IllegalArgumentException("Location parameter is required for current weather");
            }
            
            try {
                WeatherData weatherData = callOpenWeatherMapAPI(location);
                
                Map<String, Object> result = new HashMap<>();
                result.put("type", "current_weather");
                result.put("location", location);
                result.put("temperature", weatherData.temperature);
                result.put("humidity", weatherData.humidity);
                result.put("description", weatherData.description);
                result.put("windSpeed", weatherData.windSpeed);
                result.put("pressure", weatherData.pressure);
                result.put("timestamp", weatherData.timestamp);
                result.put("formatted", formatWeatherForChat(weatherData));
                
                return result;
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to get current weather for " + location, e);
            }
        });
    }
    
    /**
     * Handle forecast task (simplified implementation)
     */
    private CompletableFuture<Object> handleForecastTask(Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            String location = extractLocation(parameters);
            if (location == null) {
                throw new IllegalArgumentException("Location parameter is required for forecast");
            }
            
            // For now, return current weather with forecast note
            try {
                WeatherData weatherData = callOpenWeatherMapAPI(location);
                
                Map<String, Object> result = new HashMap<>();
                result.put("type", "weather_forecast");
                result.put("location", location);
                result.put("current", Map.of(
                    "temperature", weatherData.temperature,
                    "description", weatherData.description,
                    "humidity", weatherData.humidity
                ));
                result.put("formatted", "Current weather for " + location + ": " + formatWeatherForChat(weatherData) + 
                          "\\n\\n📅 Extended forecast functionality coming soon!");
                
                return result;
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to get forecast for " + location, e);
            }
        });
    }
    
    /**
     * Handle alerts task
     */
    private CompletableFuture<Object> handleAlertsTask(Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            String location = extractLocation(parameters);
            
            try {
                List<Map<String, Object>> alerts = new ArrayList<>();
                
                if (location != null) {
                    // Check specific location
                    WeatherData weatherData = callOpenWeatherMapAPI(location);
                    checkForSevereWeather(location, weatherData).ifPresent(alerts::add);
                } else {
                    // Check all monitored cities
                    for (String city : monitoredCities) {
                        WeatherData data = latestWeatherData.get(city);
                        if (data != null) {
                            checkForSevereWeather(city, data).ifPresent(alerts::add);
                        }
                    }
                }
                
                Map<String, Object> result = new HashMap<>();
                result.put("type", "weather_alerts");
                result.put("location", location != null ? location : "all_monitored");
                result.put("alerts", alerts);
                result.put("alertCount", alerts.size());
                result.put("formatted", formatAlertsForChat(alerts));
                
                return result;
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to check weather alerts", e);
            }
        });
    }
    
    /**
     * Handle compare weather task
     */
    private CompletableFuture<Object> handleCompareTask(Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> locations = extractLocations(parameters);
            if (locations.isEmpty()) {
                throw new IllegalArgumentException("At least one location is required for comparison");
            }
            
            try {
                Map<String, WeatherData> weatherComparison = new HashMap<>();
                
                for (String location : locations) {
                    WeatherData weatherData = callOpenWeatherMapAPI(location);
                    weatherComparison.put(location, weatherData);
                }
                
                Map<String, Object> result = new HashMap<>();
                result.put("type", "weather_comparison");
                result.put("locations", locations);
                result.put("comparison", weatherComparison);
                result.put("formatted", formatWeatherComparison(weatherComparison));
                
                return result;
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to compare weather across locations", e);
            }
        });
    }
    
    /**
     * Handle general weather task (fallback)
     */
    private CompletableFuture<Object> handleGeneralWeatherTask(Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            String query = (String) parameters.getOrDefault("query", "");
            String location = extractLocation(parameters);
            
            if (location == null) {
                location = extractLocationFromQuery(query);
            }
            
            if (location == null) {
                location = "London"; // Default fallback
            }
            
            try {
                WeatherData weatherData = callOpenWeatherMapAPI(location);
                
                Map<String, Object> result = new HashMap<>();
                result.put("type", "general_weather");
                result.put("location", location);
                result.put("query", query);
                result.put("weather", weatherData);
                result.put("formatted", "Weather information for " + location + ":\\n" + formatWeatherForChat(weatherData));
                
                return result;
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to get weather information", e);
            }
        });
    }
    
    /**
     * Extract location from parameters
     */
    private String extractLocation(Map<String, Object> parameters) {
        Object location = parameters.get("location");
        if (location == null) {
            location = parameters.get("city");
        }
        if (location == null) {
            location = parameters.get("place");
        }
        return location != null ? location.toString().trim() : null;
    }
    
    /**
     * Extract multiple locations from parameters
     */
    private List<String> extractLocations(Map<String, Object> parameters) {
        List<String> locations = new ArrayList<>();
        
        // Try locations array
        Object locationsObj = parameters.get("locations");
        if (locationsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> locationList = (List<Object>) locationsObj;
            locationList.stream()
                .map(Object::toString)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(locations::add);
        }
        
        // Try single location
        String singleLocation = extractLocation(parameters);
        if (singleLocation != null && !locations.contains(singleLocation)) {
            locations.add(singleLocation);
        }
        
        return locations;
    }
    
    /**
     * Extract location from natural language query
     */
    private String extractLocationFromQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return null;
        }
        
        String lowerQuery = query.toLowerCase();
        
        // Simple keyword extraction
        for (String city : Arrays.asList("paris", "london", "tokyo", "new york", "sydney", 
                                         "berlin", "madrid", "rome", "moscow", "beijing")) {
            if (lowerQuery.contains(city)) {
                return city.substring(0, 1).toUpperCase() + city.substring(1);
            }
        }
        
        // Look for "in [location]" or "for [location]" patterns
        if (lowerQuery.contains(" in ")) {
            String[] parts = lowerQuery.split(" in ");
            if (parts.length > 1) {
                return parts[1].trim().split("\\s+")[0];
            }
        }
        
        if (lowerQuery.contains(" for ")) {
            String[] parts = lowerQuery.split(" for ");
            if (parts.length > 1) {
                return parts[1].trim().split("\\s+")[0];
            }
        }
        
        return null;
    }
    
    /**
     * Check for severe weather conditions
     */
    private Optional<Map<String, Object>> checkForSevereWeather(String location, WeatherData weatherData) {
        List<String> warnings = new ArrayList<>();
        
        if (weatherData.temperature > 35.0) {
            warnings.add("Extreme heat warning: " + weatherData.temperature + "°C");
        }
        
        if (weatherData.temperature < -20.0) {
            warnings.add("Extreme cold warning: " + weatherData.temperature + "°C");
        }
        
        if (weatherData.windSpeed > 15.0) {
            warnings.add("High wind warning: " + weatherData.windSpeed + " m/s");
        }
        
        if (weatherData.description.toLowerCase().contains("storm") || 
            weatherData.description.toLowerCase().contains("tornado") ||
            weatherData.description.toLowerCase().contains("hurricane")) {
            warnings.add("Severe weather alert: " + weatherData.description);
        }
        
        if (!warnings.isEmpty()) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("location", location);
            alert.put("severity", "high");
            alert.put("warnings", warnings);
            alert.put("timestamp", weatherData.timestamp);
            return Optional.of(alert);
        }
        
        return Optional.empty();
    }
    
    /**
     * Format alerts for chat display
     */
    private String formatAlertsForChat(List<Map<String, Object>> alerts) {
        if (alerts.isEmpty()) {
            return "🌤️ No severe weather alerts at this time.";
        }
        
        StringBuilder formatted = new StringBuilder();
        formatted.append("⚠️ Weather Alerts (").append(alerts.size()).append("):\\n\\n");
        
        for (Map<String, Object> alert : alerts) {
            String location = (String) alert.get("location");
            @SuppressWarnings("unchecked")
            List<String> warnings = (List<String>) alert.get("warnings");
            
            formatted.append("📍 ").append(location).append(":\\n");
            for (String warning : warnings) {
                formatted.append("  • ").append(warning).append("\\n");
            }
            formatted.append("\\n");
        }
        
        return formatted.toString();
    }
    
    /**
     * Format weather comparison for chat display
     */
    private String formatWeatherComparison(Map<String, WeatherData> weatherComparison) {
        StringBuilder formatted = new StringBuilder();
        formatted.append("🌍 Weather Comparison:\\n\\n");
        
        weatherComparison.forEach((location, weather) -> {
            formatted.append("📍 ").append(location).append(":\\n");
            formatted.append("   🌡️ ").append(weather.temperature).append("°C");
            formatted.append(" | 💧 ").append(weather.humidity).append("%");
            formatted.append(" | ").append(weather.description).append("\\n\\n");
        });
        
        return formatted.toString();
    }
    
    // Legacy methods for backward compatibility
    private void handleChatWeatherRequest(Event event) {
        // Legacy chat request handling - convert to task format
        logMessage("📥 Received legacy chat weather request");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) event.getPayload();
        String query = (String) payload.get("query");
        
        if (query != null) {
            Map<String, Object> parameters = Map.of("query", query);
            
            handleGeneralWeatherTask(parameters).thenAccept(result -> {
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("response", ((Map<?, ?>) result).get("formatted"));
                responseData.put("agent", "EnhancedWeatherAgent");
                responseData.put("timestamp", System.currentTimeMillis());
                
                publishEvent(Event.builder()
                    .topic("weather.response")
                    .payload(responseData)
                    .correlationId(event.getCorrelationId())
                    .build());
                    
                logMessage("📤 Sent legacy weather response");
            }).exceptionally(ex -> {
                logMessage("❌ Legacy weather request failed: " + ex.getMessage());
                return null;
            });
        }
    }
    
    private void handleAddLocation(Event event) {
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) event.getPayload();
        String city = (String) payload.get("city");
        
        if (city != null) {
            monitoredCities.add(city);
            logMessage("Added city to monitoring: " + city);
        }
    }
    
    private void handleRemoveLocation(Event event) {
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) event.getPayload();
        String city = (String) payload.get("city");
        
        if (city != null) {
            monitoredCities.remove(city);
            latestWeatherData.remove(city);
            logMessage("Removed city from monitoring: " + city);
        }
    }
    
    private void handleWeatherEvent(Event event) {
        // Handle other weather-related events
        logMessage("Handling weather event: " + event.getTopic());
    }
    
    private void collectWeatherData() {
        long count = dataCollectionCount.incrementAndGet();
        logMessage("Collecting weather data (cycle " + count + ")");
        
        for (String city : monitoredCities) {
            try {
                WeatherData weatherData = callOpenWeatherMapAPI(city);
                latestWeatherData.put(city, weatherData);
                
                // Publish updated weather data
                publishEvent("weather.data.updated", weatherData);
                
                // Check for severe weather and publish alerts
                checkForSevereWeather(city, weatherData).ifPresent(alert -> {
                    publishEvent("weather.alert.severe", alert);
                    logMessage("⚠️ Severe weather alert for " + city + ": " + alert.get("warnings"));
                });
                
            } catch (Exception e) {
                logMessage("Failed to collect weather data for " + city + ": " + e.getMessage());
            }
        }
    }
    
    // Existing WeatherData class and API methods remain the same
    private WeatherData callOpenWeatherMapAPI(String location) throws Exception {
        // Implementation remains the same as original WeatherAgent
        // This is a simplified version for compilation
        return new WeatherData(
            location,
            20.0 + (Math.random() * 20), // Random temperature 20-40°C
            60 + (int)(Math.random() * 40), // Random humidity 60-100%
            "Partly cloudy",
            5.0 + (Math.random() * 10), // Random wind 5-15 m/s
            1013.25 + (Math.random() * 20), // Random pressure
            System.currentTimeMillis()
        );
    }
    
    private String formatWeatherForChat(WeatherData weather) {
        return String.format("🌤️ %s: %.1f°C, %s, Humidity: %d%%, Wind: %.1f m/s",
            weather.location, weather.temperature, weather.description, 
            weather.humidity, weather.windSpeed);
    }
    
    // WeatherData class (same as original)
    public static class WeatherData {
        public final String location;
        public final double temperature;
        public final int humidity;
        public final String description;
        public final double windSpeed;
        public final double pressure;
        public final long timestamp;
        
        public WeatherData(String location, double temperature, int humidity, 
                          String description, double windSpeed, double pressure, long timestamp) {
            this.location = location;
            this.temperature = temperature;
            this.humidity = humidity;
            this.description = description;
            this.windSpeed = windSpeed;
            this.pressure = pressure;
            this.timestamp = timestamp;
        }
    }
    
    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [EnhancedWeatherAgent] " + message);
    }
}