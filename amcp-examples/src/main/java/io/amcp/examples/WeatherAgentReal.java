package io.amcp.examples;

import io.amcp.core.AbstractMobileAgent;
import io.amcp.core.Event;
import jakarta.enterprise.context.ApplicationScoped;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Weather Agent with Real Data Integration - v1.6 Quarkus
 * 
 * Fetches real weather data from OpenWeatherMap API.
 * Falls back to simulated data if API is unavailable.
 * 
 * Environment Variables:
 * - OPENWEATHER_API_KEY: OpenWeatherMap API key (optional, uses demo if not set)
 */
@ApplicationScoped
public class WeatherAgentReal extends AbstractMobileAgent {
    
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(WeatherAgentReal.class);
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    
    // OpenWeatherMap API configuration
    private static final String OPENWEATHER_API_KEY = System.getenv().getOrDefault("OPENWEATHER_API_KEY", "demo");
    private static final String OPENWEATHER_URL = "https://api.openweathermap.org/data/2.5/weather";
    
    // Fallback simulated weather data
    private static final Map<String, Map<String, Object>> WEATHER_DATA = new HashMap<>();
    
    static {
        WEATHER_DATA.put("paris", Map.of(
            "city", "Paris",
            "country", "FR",
            "temperature", 15,
            "condition", "Cloudy",
            "humidity", 65,
            "windSpeed", 12,
            "lat", 48.8566,
            "lon", 2.3522
        ));
        WEATHER_DATA.put("london", Map.of(
            "city", "London",
            "country", "GB",
            "temperature", 12,
            "condition", "Rainy",
            "humidity", 75,
            "windSpeed", 18,
            "lat", 51.5074,
            "lon", -0.1278
        ));
        WEATHER_DATA.put("tokyo", Map.of(
            "city", "Tokyo",
            "country", "JP",
            "temperature", 22,
            "condition", "Sunny",
            "humidity", 55,
            "windSpeed", 8,
            "lat", 35.6762,
            "lon", 139.6503
        ));
        WEATHER_DATA.put("new york", Map.of(
            "city", "New York",
            "country", "US",
            "temperature", 18,
            "condition", "Partly Cloudy",
            "humidity", 60,
            "windSpeed", 15,
            "lat", 40.7128,
            "lon", -74.0060
        ));
    }

    @Override
    public void onActivate() {
        super.onActivate();
        subscribe("weather.**");
        subscribe("weather.request");
        logMessage("🌤️  Weather Agent (Real Data) activated");
    }

    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            try {
                String topic = event.getTopic();
                logger.debug("Weather Agent handling event: {}", topic);
                
                if (topic.equals("weather.request")) {
                    handleWeatherRequest(event);
                } else if (topic.equals("weather.forecast")) {
                    handleForecastRequest(event);
                } else if (topic.equals("weather.status")) {
                    handleStatusRequest(event);
                }
            } catch (Exception e) {
                logger.error("Error handling weather event: {}", e.getMessage(), e);
            }
        });
    }

    private void handleWeatherRequest(Event event) {
        try {
            Map<String, Object> request = event.getPayload(Map.class);
            String city = (String) request.getOrDefault("city", "paris");
            
            logMessage("📍 Weather request for: " + city);
            
            // Try to fetch real data from OpenWeatherMap
            Map<String, Object> weather = fetchRealWeatherData(city);
            
            if (weather == null) {
                weather = WEATHER_DATA.getOrDefault(city.toLowerCase(), WEATHER_DATA.get("paris"));
                logMessage("⚠️  Using fallback data for: " + city);
            }
            
            Map<String, Object> response = new HashMap<>(weather);
            response.put("timestamp", System.currentTimeMillis());
            response.put("source", "weather-agent-real");
            response.put("dataSource", weather.containsKey("real") ? "openweathermap" : "simulated");
            
            publishEvent("weather.response", response);
            logMessage("✅ Weather response sent for: " + city);
            
        } catch (Exception e) {
            logger.error("Error handling weather request: {}", e.getMessage(), e);
        }
    }

    private Map<String, Object> fetchRealWeatherData(String city) {
        try {
            String url = OPENWEATHER_URL + "?q=" + city + "&appid=" + OPENWEATHER_API_KEY + "&units=metric";
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(java.time.Duration.ofSeconds(5))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                
                Map<String, Object> weather = new HashMap<>();
                weather.put("city", root.get("name").asText());
                weather.put("country", root.get("sys").get("country").asText());
                weather.put("temperature", root.get("main").get("temp").asDouble());
                weather.put("feelsLike", root.get("main").get("feels_like").asDouble());
                weather.put("tempMin", root.get("main").get("temp_min").asDouble());
                weather.put("tempMax", root.get("main").get("temp_max").asDouble());
                weather.put("humidity", root.get("main").get("humidity").asInt());
                weather.put("pressure", root.get("main").get("pressure").asInt());
                weather.put("condition", root.get("weather").get(0).get("main").asText());
                weather.put("description", root.get("weather").get(0).get("description").asText());
                weather.put("windSpeed", root.get("wind").get("speed").asDouble());
                weather.put("windDeg", root.get("wind").get("deg").asInt());
                weather.put("cloudiness", root.get("clouds").get("all").asInt());
                weather.put("visibility", root.get("visibility").asInt());
                weather.put("lat", root.get("coord").get("lat").asDouble());
                weather.put("lon", root.get("coord").get("lon").asDouble());
                weather.put("real", true);
                
                logger.info("✅ Real weather data fetched for {}", city);
                return weather;
            } else {
                logger.warn("OpenWeatherMap API returned status: {}", response.statusCode());
                return null;
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch real weather data: {}", e.getMessage());
            return null;
        }
    }

    private void handleForecastRequest(Event event) {
        try {
            Map<String, Object> request = event.getPayload(Map.class);
            String city = (String) request.getOrDefault("city", "paris");
            
            logMessage("📊 Forecast request for: " + city);
            
            Map<String, Object> weather = fetchRealWeatherData(city);
            if (weather == null) {
                weather = WEATHER_DATA.getOrDefault(city.toLowerCase(), WEATHER_DATA.get("paris"));
            }
            
            Map<String, Object> forecast = new HashMap<>();
            forecast.put("city", weather.get("city"));
            forecast.put("today", weather);
            forecast.put("tomorrow", Map.of(
                "temperature", ((Number) weather.get("temperature")).doubleValue() + 1,
                "condition", "Sunny",
                "humidity", 60
            ));
            
            publishEvent("weather.forecast.response", forecast);
            logMessage("✅ Forecast response sent for: " + city);
            
        } catch (Exception e) {
            logger.error("Error handling forecast request: {}", e.getMessage(), e);
        }
    }

    private void handleStatusRequest(Event event) {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("agent", "WeatherAgentReal");
            status.put("version", "1.6.0");
            status.put("status", "active");
            status.put("timestamp", System.currentTimeMillis());
            status.put("apiKey", OPENWEATHER_API_KEY.equals("demo") ? "demo (limited)" : "configured");
            status.put("citiesSupported", WEATHER_DATA.keySet());
            
            publishEvent("weather.status.response", status);
            logMessage("✅ Status response sent");
            
        } catch (Exception e) {
            logger.error("Error handling status request: {}", e.getMessage(), e);
        }
    }

    @Override
    public void onDeactivate() {
        logMessage("👋 Weather Agent (Real Data) shutting down");
        super.onDeactivate();
    }
}
