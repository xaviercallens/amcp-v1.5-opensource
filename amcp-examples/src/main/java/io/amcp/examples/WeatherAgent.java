package io.amcp.examples;

import io.amcp.core.AbstractMobileAgent;
import io.amcp.core.Event;
import jakarta.enterprise.context.ApplicationScoped;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Weather Agent - Demonstrates v1.6 features with Quarkus integration.
 * 
 * Features:
 * - CloudEvents v1.0 compliance
 * - Quarkus CDI integration (@ApplicationScoped)
 * - Async event handling with CompletableFuture
 * - JSON payload processing
 * - Multi-instance support via Kafka
 * 
 * Spec Reference: AMCP v1.6 Architecture Evolution
 */
@ApplicationScoped
public class WeatherAgent extends AbstractMobileAgent {
    
    private static final ObjectMapper mapper = new ObjectMapper();
    
    // Simulated weather data
    private static final Map<String, Map<String, Object>> WEATHER_DATA = new HashMap<>();
    
    static {
        // Initialize sample weather data
        WEATHER_DATA.put("paris", Map.of(
            "city", "Paris",
            "temperature", 15,
            "condition", "Cloudy",
            "humidity", 65,
            "windSpeed", 12
        ));
        WEATHER_DATA.put("london", Map.of(
            "city", "London",
            "temperature", 12,
            "condition", "Rainy",
            "humidity", 75,
            "windSpeed", 18
        ));
        WEATHER_DATA.put("tokyo", Map.of(
            "city", "Tokyo",
            "temperature", 22,
            "condition", "Sunny",
            "humidity", 55,
            "windSpeed", 8
        ));
        WEATHER_DATA.put("new york", Map.of(
            "city", "New York",
            "temperature", 18,
            "condition", "Partly Cloudy",
            "humidity", 60,
            "windSpeed", 15
        ));
    }

    @Override
    public void onActivate() {
        super.onActivate();
        
        // Subscribe to weather request topics
        subscribe("weather.**");
        subscribe("weather.request");
        
        logMessage("🌤️  Weather Agent activated - Ready for v1.6 distributed mesh");
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

    /**
     * Handles weather request for a specific city.
     * Demonstrates CloudEvents payload handling.
     */
    private void handleWeatherRequest(Event event) {
        try {
            // Extract city from payload
            Map<String, Object> request = event.getPayload(Map.class);
            String city = (String) request.getOrDefault("city", "paris");
            
            logMessage("📍 Weather request for: " + city);
            
            // Get weather data
            Map<String, Object> weather = WEATHER_DATA.getOrDefault(
                city.toLowerCase(), 
                WEATHER_DATA.get("paris")
            );
            
            // Publish response event (CloudEvents compliant)
            Map<String, Object> response = new HashMap<>(weather);
            response.put("timestamp", System.currentTimeMillis());
            response.put("source", "weather-agent");
            
            publishEvent("weather.response", response);
            logMessage("✅ Weather response sent for: " + city);
            
        } catch (Exception e) {
            logger.error("Error handling weather request: {}", e.getMessage(), e);
        }
    }

    /**
     * Handles forecast request.
     * Demonstrates async processing.
     */
    private void handleForecastRequest(Event event) {
        try {
            Map<String, Object> request = event.getPayload(Map.class);
            String city = (String) request.getOrDefault("city", "paris");
            
            logMessage("📊 Forecast request for: " + city);
            
            // Simulate forecast data
            Map<String, Object> forecast = new HashMap<>();
            forecast.put("city", city);
            forecast.put("today", WEATHER_DATA.getOrDefault(city.toLowerCase(), WEATHER_DATA.get("paris")));
            forecast.put("tomorrow", Map.of(
                "temperature", 16,
                "condition", "Sunny",
                "humidity", 60
            ));
            forecast.put("dayAfter", Map.of(
                "temperature", 14,
                "condition", "Rainy",
                "humidity", 70
            ));
            
            publishEvent("weather.forecast.response", forecast);
            logMessage("✅ Forecast response sent for: " + city);
            
        } catch (Exception e) {
            logger.error("Error handling forecast request: {}", e.getMessage(), e);
        }
    }

    /**
     * Handles status request.
     * Demonstrates agent health check.
     */
    private void handleStatusRequest(Event event) {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("agent", "WeatherAgent");
            status.put("version", "1.6.0");
            status.put("status", "active");
            status.put("timestamp", System.currentTimeMillis());
            status.put("citiesSupported", WEATHER_DATA.keySet());
            
            publishEvent("weather.status.response", status);
            logMessage("✅ Status response sent");
            
        } catch (Exception e) {
            logger.error("Error handling status request: {}", e.getMessage(), e);
        }
    }

    @Override
    public void onDeactivate() {
        logMessage("👋 Weather Agent shutting down");
        super.onDeactivate();
    }
}
