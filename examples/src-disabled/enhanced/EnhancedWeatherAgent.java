package io.amcp.examples.enhanced;

import io.amcp.connectors.ai.enhanced.EnhancedAgentBase;
import io.amcp.core.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Enhanced Weather Agent demonstrating AMCP v1.5 agent-side improvements
 * 
 * Features:
 * - Structured JSON payload parsing with validation
 * - Standardized response formatting with CloudEvents compliance
 * - Multi-turn conversation support with context retention
 * - Enhanced parameter extraction from natural language
 * - Comprehensive error handling and recovery
 * - Performance metrics and observability
 * 
 * This agent showcases the enhanced agent processing capabilities
 * built on top of the EnhancedAgentBase framework.
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 * @since 1.5.0
 */
public class EnhancedWeatherAgent extends EnhancedAgentBase {
    
    // Location patterns for parameter extraction
    private static final Pattern LOCATION_PATTERN = Pattern.compile(
        "(?i)(?:in|for|at|weather|temperature)\\s+([a-zA-Z\\s]+?)(?:\\s|$|,|\\?|!)"
    );
    
    private static final Pattern CELSIUS_PATTERN = Pattern.compile("(?i)celsius|°c|c\\b");
    private static final Pattern FAHRENHEIT_PATTERN = Pattern.compile("(?i)fahrenheit|°f|f\\b");
    
    // Weather data simulation
    private final Map<String, WeatherInfo> weatherDatabase = new HashMap<>();
    
    public EnhancedWeatherAgent() {
        super(AgentID.named("EnhancedWeatherAgent"));
        initializeWeatherData();
    }
    
    @Override
    protected void initializeCapabilities() {
        addCapability("weather.get");
        addCapability("weather.forecast");
        addCapability("weather.alerts");
        addCapability("general.query");
    }
    
    @Override
    protected void initializeParameterSchemas() {
        // Weather get schema
        ParameterSchema weatherGetSchema = new ParameterSchema()
            .addRequired("location", "string", "City or location name")
            .addOptional("units", "string", "Temperature units: celsius or fahrenheit")
            .addOptional("includeDetails", "boolean", "Include detailed weather information");
        addParameterSchema("weather.get", weatherGetSchema);
        
        // Weather forecast schema
        ParameterSchema forecastSchema = new ParameterSchema()
            .addRequired("location", "string", "City or location name")
            .addOptional("days", "number", "Number of forecast days (1-7)")
            .addOptional("units", "string", "Temperature units: celsius or fahrenheit");
        addParameterSchema("weather.forecast", forecastSchema);
    }
    
    @Override
    public void onActivate() {
        super.onActivate();
        
        // Subscribe to weather-related topics
        subscribe("weather.request.**");
        subscribe("weather.query.**");
        subscribe("agent.request.weather");
        subscribe("chat.weather.**");
        
        logMessage("Enhanced Weather Agent activated with " + weatherDatabase.size() + " cities");
    }
    
    @Override
    protected void handleEnhancedEvent(Event event, EnhancedEventPayload payload) {
        String capability = payload.getCapability();
        Map<String, Object> parameters = payload.getParameters();
        
        logMessage("Handling enhanced event - capability: " + capability);
        
        switch (capability) {
            case "weather.get":
                handleWeatherGet(event, payload, parameters);
                break;
            case "weather.forecast":
                handleWeatherForecast(event, payload, parameters);
                break;
            case "weather.alerts":
                handleWeatherAlerts(event, payload, parameters);
                break;
            case "general.query":
                handleGeneralQuery(event, payload, parameters);
                break;
            default:
                sendEnhancedResponse(event, 
                    "I don't understand that weather request. I can help with current weather, forecasts, and alerts.",
                    ResponseType.WARNING, null);
        }
    }
    
    private void handleWeatherGet(Event event, EnhancedEventPayload payload, Map<String, Object> parameters) {
        String location = (String) parameters.get("location");
        String units = (String) parameters.getOrDefault("units", "celsius");
        boolean includeDetails = (boolean) parameters.getOrDefault("includeDetails", false);
        
        if (location == null || location.trim().isEmpty()) {
            sendEnhancedResponse(event, 
                "Please specify a location for the weather information.",
                ResponseType.ERROR, null);
            return;
        }
        
        WeatherInfo weather = getWeatherInfo(location.trim());
        if (weather != null) {
            String response = formatWeatherResponse(weather, units, includeDetails);
            Map<String, Object> weatherData = createWeatherDataMap(weather);
            
            sendEnhancedResponse(event, response, ResponseType.SUCCESS, weatherData);
            
        } else {
            sendEnhancedResponse(event, 
                "Sorry, I don't have weather information for " + location + 
                ". Available locations: " + String.join(", ", weatherDatabase.keySet()),
                ResponseType.WARNING, null);
        }
    }
    
    private void handleWeatherForecast(Event event, EnhancedEventPayload payload, Map<String, Object> parameters) {
        String location = (String) parameters.get("location");
        int days = ((Number) parameters.getOrDefault("days", 3)).intValue();
        String units = (String) parameters.getOrDefault("units", "celsius");
        
        if (location == null || location.trim().isEmpty()) {
            sendEnhancedResponse(event, 
                "Please specify a location for the weather forecast.",
                ResponseType.ERROR, null);
            return;
        }
        
        // Generate forecast (simplified for demo)
        String forecast = generateWeatherForecast(location.trim(), days, units);
        Map<String, Object> forecastData = Map.of(
            "location", location,
            "days", days,
            "units", units,
            "generated", LocalDateTime.now().toString()
        );
        
        sendEnhancedResponse(event, forecast, ResponseType.SUCCESS, forecastData);
    }
    
    private void handleWeatherAlerts(Event event, EnhancedEventPayload payload, Map<String, Object> parameters) {
        String location = (String) parameters.get("location");
        
        // Generate weather alerts (simplified for demo)
        List<String> alerts = generateWeatherAlerts(location);
        String response = alerts.isEmpty() 
            ? "No active weather alerts for " + (location != null ? location : "your area") + "."
            : "🚨 Active weather alerts:\n" + String.join("\n", alerts);
        
        Map<String, Object> alertData = Map.of(
            "location", location != null ? location : "general",
            "alertCount", alerts.size(),
            "alerts", alerts
        );
        
        sendEnhancedResponse(event, response, ResponseType.INFO, alertData);
    }
    
    private void handleGeneralQuery(Event event, EnhancedEventPayload payload, Map<String, Object> parameters) {
        String query = payload.getQuery();
        
        // Analyze query for weather-related intent
        if (containsWeatherKeywords(query)) {
            // Extract location and redirect to weather.get
            String location = extractLocationFromQuery(query);
            if (location != null) {
                Map<String, Object> weatherParams = new HashMap<>(parameters);
                weatherParams.put("location", location);
                weatherParams.put("includeDetails", true);
                
                // Create enhanced payload for weather request
                EnhancedEventPayload weatherPayload = new EnhancedEventPayload(true, new ArrayList<>());
                weatherPayload.setCapability("weather.get");
                weatherPayload.setParameters(weatherParams);
                weatherPayload.setQuery(query);
                
                handleWeatherGet(event, weatherPayload, weatherParams);
            } else {
                sendEnhancedResponse(event, 
                    "I understand you're asking about weather, but could you please specify a location?",
                    ResponseType.WARNING, null);
            }
        } else {
            sendEnhancedResponse(event, 
                "I'm a weather specialist. Please ask me about weather conditions, forecasts, or alerts for specific locations.",
                ResponseType.INFO, null);
        }
    }
    
    @Override
    protected Map<String, Object> extractParametersFromQuery(String query) {
        Map<String, Object> parameters = new HashMap<>();
        
        // Extract location
        String location = extractLocationFromQuery(query);
        if (location != null) {
            parameters.put("location", location);
        }
        
        // Extract temperature units
        if (CELSIUS_PATTERN.matcher(query).find()) {
            parameters.put("units", "celsius");
        } else if (FAHRENHEIT_PATTERN.matcher(query).find()) {
            parameters.put("units", "fahrenheit");
        }
        
        // Check for detail requests
        if (query.toLowerCase().contains("detail") || query.toLowerCase().contains("more information")) {
            parameters.put("includeDetails", true);
        }
        
        // Check for forecast requests
        if (query.toLowerCase().contains("forecast") || query.toLowerCase().contains("tomorrow") || 
            query.toLowerCase().contains("next week")) {
            parameters.put("capability", "weather.forecast");
            
            // Extract number of days
            Pattern daysPattern = Pattern.compile("(?i)(\\d+)\\s*days?");
            Matcher daysMatcher = daysPattern.matcher(query);
            if (daysMatcher.find()) {
                parameters.put("days", Integer.parseInt(daysMatcher.group(1)));
            }
        }
        
        return parameters;
    }
    
    private String extractLocationFromQuery(String query) {
        Matcher matcher = LOCATION_PATTERN.matcher(query);
        if (matcher.find()) {
            String location = matcher.group(1).trim();
            
            // Clean up location (remove common words)
            location = location.replaceAll("(?i)\\b(the|a|an|is|was|will|be)\\b", "").trim();
            
            // Check if it's a known location
            for (String knownLocation : weatherDatabase.keySet()) {
                if (knownLocation.toLowerCase().contains(location.toLowerCase()) ||
                    location.toLowerCase().contains(knownLocation.toLowerCase())) {
                    return knownLocation;
                }
            }
            
            return location;
        }
        return null;
    }
    
    private boolean containsWeatherKeywords(String query) {
        String[] weatherKeywords = {
            "weather", "temperature", "rain", "snow", "sunny", "cloudy", 
            "hot", "cold", "warm", "cool", "forecast", "climate"
        };
        
        String lowerQuery = query.toLowerCase();
        for (String keyword : weatherKeywords) {
            if (lowerQuery.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    private WeatherInfo getWeatherInfo(String location) {
        // Try exact match first
        WeatherInfo weather = weatherDatabase.get(location);
        if (weather != null) {
            return weather;
        }
        
        // Try partial match
        for (Map.Entry<String, WeatherInfo> entry : weatherDatabase.entrySet()) {
            if (entry.getKey().toLowerCase().contains(location.toLowerCase()) ||
                location.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    private String formatWeatherResponse(WeatherInfo weather, String units, boolean includeDetails) {
        StringBuilder response = new StringBuilder();
        
        double temp = "fahrenheit".equals(units) ? weather.temperature * 9/5 + 32 : weather.temperature;
        String tempUnit = "fahrenheit".equals(units) ? "°F" : "°C";
        
        response.append("🌤️ **Weather in ").append(weather.location).append("**\n\n");
        response.append("🌡️ **Temperature**: ").append(String.format("%.1f%s", temp, tempUnit)).append("\n");
        response.append("☁️ **Conditions**: ").append(weather.description).append("\n");
        
        if (includeDetails) {
            response.append("💧 **Humidity**: ").append(String.format("%.0f%%", weather.humidity)).append("\n");
            response.append("💨 **Wind**: ").append(String.format("%.1f km/h", weather.windSpeed)).append("\n");
            response.append("📊 **Pressure**: ").append(String.format("%.0f hPa", weather.pressure)).append("\n");
            response.append("👁️ **Visibility**: ").append(String.format("%.1f km", weather.visibility)).append("\n");
        }
        
        response.append("\n🕒 **Updated**: ").append(
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        
        // Add weather advice
        if (temp > 30) {
            response.append("\n\n☀️ *It's quite hot! Stay hydrated and seek shade.*");
        } else if (temp < 5) {
            response.append("\n\n🧥 *It's quite cold! Bundle up and stay warm.*");
        } else if (weather.description.toLowerCase().contains("rain")) {
            response.append("\n\n☔ *Don't forget your umbrella!*");
        }
        
        return response.toString();
    }
    
    private Map<String, Object> createWeatherDataMap(WeatherInfo weather) {
        Map<String, Object> data = new HashMap<>();
        data.put("location", weather.location);
        data.put("temperature", weather.temperature);
        data.put("description", weather.description);
        data.put("humidity", weather.humidity);
        data.put("windSpeed", weather.windSpeed);
        data.put("pressure", weather.pressure);
        data.put("visibility", weather.visibility);
        data.put("timestamp", LocalDateTime.now().toString());
        
        return data;
    }
    
    private String generateWeatherForecast(String location, int days, String units) {
        StringBuilder forecast = new StringBuilder();
        forecast.append("📅 **").append(days).append("-Day Forecast for ").append(location).append("**\n\n");
        
        Random random = new Random();
        String[] conditions = {"Sunny", "Partly Cloudy", "Cloudy", "Light Rain", "Showers"};
        
        for (int i = 1; i <= Math.min(days, 7); i++) {
            LocalDateTime date = LocalDateTime.now().plusDays(i);
            String dayName = date.getDayOfWeek().toString();
            
            double baseTemp = 20 + random.nextGaussian() * 8;
            double temp = "fahrenheit".equals(units) ? baseTemp * 9/5 + 32 : baseTemp;
            String tempUnit = "fahrenheit".equals(units) ? "°F" : "°C";
            String condition = conditions[random.nextInt(conditions.length)];
            
            forecast.append("**").append(dayName.charAt(0)).append(dayName.substring(1).toLowerCase())
                    .append("**: ").append(String.format("%.0f%s", temp, tempUnit))
                    .append(", ").append(condition).append("\n");
        }
        
        return forecast.toString();
    }
    
    private List<String> generateWeatherAlerts(String location) {
        List<String> alerts = new ArrayList<>();
        
        // Simulate random alerts for demo
        Random random = new Random();
        if (random.nextDouble() < 0.3) { // 30% chance of alerts
            if (random.nextBoolean()) {
                alerts.add("⚠️ High Wind Warning: Winds up to 60 km/h expected");
            }
            if (random.nextDouble() < 0.2) {
                alerts.add("🌧️ Heavy Rain Advisory: 25-50mm rainfall expected");
            }
            if (random.nextDouble() < 0.1) {
                alerts.add("🌡️ Heat Warning: Temperatures may exceed 35°C");
            }
        }
        
        return alerts;
    }
    
    private void initializeWeatherData() {
        weatherDatabase.put("Paris", new WeatherInfo("Paris", 18.5, "Partly cloudy", 65, 12.5, 1013, 10.0));
        weatherDatabase.put("London", new WeatherInfo("London", 15.2, "Overcast", 78, 8.3, 1019, 8.5));
        weatherDatabase.put("New York", new WeatherInfo("New York", 22.1, "Sunny", 55, 15.2, 1025, 12.0));
        weatherDatabase.put("Tokyo", new WeatherInfo("Tokyo", 26.3, "Light rain", 82, 6.7, 1008, 6.5));
        weatherDatabase.put("Sydney", new WeatherInfo("Sydney", 21.8, "Clear", 60, 18.9, 1021, 15.0));
        weatherDatabase.put("Berlin", new WeatherInfo("Berlin", 16.9, "Foggy", 88, 5.4, 1015, 4.0));
        weatherDatabase.put("Madrid", new WeatherInfo("Madrid", 25.7, "Sunny", 45, 11.2, 1018, 20.0));
        weatherDatabase.put("Rome", new WeatherInfo("Rome", 24.3, "Partly cloudy", 58, 9.8, 1016, 12.5));
    }
    
    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [EnhancedWeatherAgent] " + message);
    }
    
    // Weather data structure
    private static class WeatherInfo {
        final String location;
        final double temperature; // Celsius
        final String description;
        final double humidity; // Percentage
        final double windSpeed; // m/s
        final double pressure; // hPa
        final double visibility; // km
        
        WeatherInfo(String location, double temperature, String description, 
                   double humidity, double windSpeed, double pressure, double visibility) {
            this.location = location;
            this.temperature = temperature;
            this.description = description;
            this.humidity = humidity;
            this.windSpeed = windSpeed;
            this.pressure = pressure;
            this.visibility = visibility;
        }
    }
}