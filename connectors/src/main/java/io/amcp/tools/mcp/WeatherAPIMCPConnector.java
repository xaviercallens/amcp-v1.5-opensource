package io.amcp.tools.mcp;

import io.amcp.tools.ToolConnector;
import io.amcp.tools.ToolRequest;
import io.amcp.tools.ToolResponse;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * AMCP v1.4 Weather API MCP Connector.
 * Provides weather data capabilities using OpenWeatherMap API
 * following the Model Context Protocol (MCP) specification.
 */
public class WeatherAPIMCPConnector implements ToolConnector {
    
    private static final String TOOL_ID = "weather-api-mcp";
    private static final String TOOL_NAME = "Weather API";
    private static final String VERSION = "1.4.0";
    private static final String API_BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private static final String GEO_API_URL = "https://api.openweathermap.org/geo/1.0/";
    
    private volatile boolean initialized = false;
    private String apiKey = "demo_key_replace_with_real_key";
    private int timeoutMs = 30000;
    private String units = "metric"; // metric, imperial, kelvin
    
    @Override
    public String getToolId() {
        return TOOL_ID;
    }
    
    @Override
    public String getToolName() {
        return TOOL_NAME;
    }
    
    @Override
    public String getVersion() {
        return VERSION;
    }
    
    @Override
    public CompletableFuture<Boolean> isHealthy() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Test with a simple current weather request for London
                String urlStr = API_BASE_URL + "weather?q=London&appid=" + apiKey + "&units=" + units;
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                connection.disconnect();
                return responseCode == 200;
            } catch (Exception e) {
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> initialize(Map<String, Object> config) {
        return CompletableFuture.runAsync(() -> {
            if (config != null) {
                String key = (String) config.get("apiKey");
                if (key != null && !key.trim().isEmpty()) {
                    this.apiKey = key;
                }
                
                String envApiKey = System.getenv("OPENWEATHER_API_KEY");
                if (envApiKey != null && !envApiKey.trim().isEmpty()) {
                    this.apiKey = envApiKey;
                }
                
                Integer timeout = (Integer) config.get("timeoutMs");
                if (timeout != null && timeout > 0) {
                    this.timeoutMs = timeout;
                }
                
                String unitsConfig = (String) config.get("units");
                if (unitsConfig != null && (unitsConfig.equals("metric") || 
                    unitsConfig.equals("imperial") || unitsConfig.equals("kelvin"))) {
                    this.units = unitsConfig;
                }
            }
            this.initialized = true;
        });
    }
    
    @Override
    public CompletableFuture<ToolResponse> invoke(ToolRequest request) {
        if (!initialized) {
            return CompletableFuture.completedFuture(
                ToolResponse.error("Connector not initialized", request.getRequestId(), 0)
            );
        }
        
        long startTime = System.currentTimeMillis();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String operation = request.getOperation();
                
                switch (operation) {
                    case "current_weather":
                        return getCurrentWeather(request, startTime);
                    case "forecast":
                        return getForecast(request, startTime);
                    case "weather_alerts":
                        return getWeatherAlerts(request, startTime);
                    case "geocoding":
                        return getGeocoding(request, startTime);
                    case "air_quality":
                        return getAirQuality(request, startTime);
                    default:
                        long duration = System.currentTimeMillis() - startTime;
                        return ToolResponse.error("Unsupported operation: " + operation, 
                                                request.getRequestId(), duration);
                }
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                return ToolResponse.error("Weather API request failed: " + e.getMessage(), 
                                        request.getRequestId(), duration);
            }
        });
    }
    
    private ToolResponse getCurrentWeather(ToolRequest request, long startTime) throws Exception {
        String location = request.getStringParameter("location");
        String lat = request.getStringParameter("lat");
        String lon = request.getStringParameter("lon");
        
        if ((location == null || location.trim().isEmpty()) && (lat == null || lon == null)) {
            long duration = System.currentTimeMillis() - startTime;
            return ToolResponse.error("Either location or lat/lon parameters are required", 
                                    request.getRequestId(), duration);
        }
        
        String urlStr;
        if (lat != null && lon != null) {
            urlStr = API_BASE_URL + "weather?lat=" + lat + "&lon=" + lon + 
                    "&appid=" + apiKey + "&units=" + units;
        } else {
            String encodedLocation = URLEncoder.encode(location.trim(), "UTF-8");
            urlStr = API_BASE_URL + "weather?q=" + encodedLocation + 
                    "&appid=" + apiKey + "&units=" + units;
        }
        
        String jsonResponse = makeHttpRequest(urlStr);
        Map<String, Object> weatherData = parseWeatherResponse(jsonResponse, "current");
        
        long duration = System.currentTimeMillis() - startTime;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("location", location != null ? location : lat + "," + lon);
        metadata.put("units", units);
        metadata.put("api_endpoint", "current_weather");
        
        return ToolResponse.success(weatherData, request.getRequestId(), duration, metadata);
    }
    
    private ToolResponse getForecast(ToolRequest request, long startTime) throws Exception {
        String location = request.getStringParameter("location");
        String lat = request.getStringParameter("lat");
        String lon = request.getStringParameter("lon");
        Integer days = request.getIntegerParameter("days");
        
        if (days == null) days = 5; // Default 5-day forecast
        if (days > 5) days = 5; // OpenWeatherMap free tier limit
        
        if ((location == null || location.trim().isEmpty()) && (lat == null || lon == null)) {
            long duration = System.currentTimeMillis() - startTime;
            return ToolResponse.error("Either location or lat/lon parameters are required", 
                                    request.getRequestId(), duration);
        }
        
        String urlStr;
        if (lat != null && lon != null) {
            urlStr = API_BASE_URL + "forecast?lat=" + lat + "&lon=" + lon + 
                    "&appid=" + apiKey + "&units=" + units + "&cnt=" + (days * 8); // 8 forecasts per day (3h intervals)
        } else {
            String encodedLocation = URLEncoder.encode(location.trim(), "UTF-8");
            urlStr = API_BASE_URL + "forecast?q=" + encodedLocation + 
                    "&appid=" + apiKey + "&units=" + units + "&cnt=" + (days * 8);
        }
        
        String jsonResponse = makeHttpRequest(urlStr);
        Map<String, Object> forecastData = parseForecastResponse(jsonResponse, days);
        
        long duration = System.currentTimeMillis() - startTime;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("location", location != null ? location : lat + "," + lon);
        metadata.put("days", days);
        metadata.put("units", units);
        metadata.put("api_endpoint", "forecast");
        
        return ToolResponse.success(forecastData, request.getRequestId(), duration, metadata);
    }
    
    private ToolResponse getWeatherAlerts(ToolRequest request, long startTime) throws Exception {
        String lat = request.getStringParameter("lat");
        String lon = request.getStringParameter("lon");
        
        if (lat == null || lon == null) {
            long duration = System.currentTimeMillis() - startTime;
            return ToolResponse.error("lat and lon parameters are required for weather alerts", 
                                    request.getRequestId(), duration);
        }
        
        String urlStr = API_BASE_URL + "onecall?lat=" + lat + "&lon=" + lon + 
                       "&appid=" + apiKey + "&exclude=minutely,hourly,daily";
        
        String jsonResponse = makeHttpRequest(urlStr);
        Map<String, Object> alertData = parseAlertsResponse(jsonResponse);
        
        long duration = System.currentTimeMillis() - startTime;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("location", lat + "," + lon);
        metadata.put("api_endpoint", "weather_alerts");
        
        return ToolResponse.success(alertData, request.getRequestId(), duration, metadata);
    }
    
    private ToolResponse getGeocoding(ToolRequest request, long startTime) throws Exception {
        String location = request.getStringParameter("location");
        if (location == null || location.trim().isEmpty()) {
            long duration = System.currentTimeMillis() - startTime;
            return ToolResponse.error("location parameter is required", request.getRequestId(), duration);
        }
        
        String encodedLocation = URLEncoder.encode(location.trim(), "UTF-8");
        String urlStr = GEO_API_URL + "direct?q=" + encodedLocation + "&limit=5&appid=" + apiKey;
        
        String jsonResponse = makeHttpRequest(urlStr);
        Map<String, Object> geoData = parseGeocodingResponse(jsonResponse, location);
        
        long duration = System.currentTimeMillis() - startTime;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("location", location);
        metadata.put("api_endpoint", "geocoding");
        
        return ToolResponse.success(geoData, request.getRequestId(), duration, metadata);
    }
    
    private ToolResponse getAirQuality(ToolRequest request, long startTime) throws Exception {
        String lat = request.getStringParameter("lat");
        String lon = request.getStringParameter("lon");
        
        if (lat == null || lon == null) {
            long duration = System.currentTimeMillis() - startTime;
            return ToolResponse.error("lat and lon parameters are required for air quality", 
                                    request.getRequestId(), duration);
        }
        
        String urlStr = API_BASE_URL + "air_pollution?lat=" + lat + "&lon=" + lon + "&appid=" + apiKey;
        
        String jsonResponse = makeHttpRequest(urlStr);
        Map<String, Object> airQualityData = parseAirQualityResponse(jsonResponse);
        
        long duration = System.currentTimeMillis() - startTime;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("location", lat + "," + lon);
        metadata.put("api_endpoint", "air_quality");
        
        return ToolResponse.success(airQualityData, request.getRequestId(), duration, metadata);
    }
    
    private String makeHttpRequest(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(timeoutMs);
            connection.setRequestProperty("User-Agent", "AMCP-v1.4-Weather-Agent/1.0");
            connection.setRequestProperty("Accept", "application/json");
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("HTTP " + responseCode + " response from Weather API");
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            
            return response.toString();
        } finally {
            connection.disconnect();
        }
    }
    
    private Map<String, Object> parseWeatherResponse(String jsonResponse, String type) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", type);
        
        // Extract basic weather info
        result.put("temperature", extractJsonNumber(jsonResponse, "temp"));
        result.put("feels_like", extractJsonNumber(jsonResponse, "feels_like"));
        result.put("humidity", extractJsonNumber(jsonResponse, "humidity"));
        result.put("pressure", extractJsonNumber(jsonResponse, "pressure"));
        result.put("visibility", extractJsonNumber(jsonResponse, "visibility"));
        
        // Weather conditions
        result.put("description", extractNestedJsonField(jsonResponse, "weather", "description"));
        result.put("main", extractNestedJsonField(jsonResponse, "weather", "main"));
        result.put("icon", extractNestedJsonField(jsonResponse, "weather", "icon"));
        
        // Wind data
        result.put("wind_speed", extractNestedJsonNumber(jsonResponse, "wind", "speed"));
        result.put("wind_direction", extractNestedJsonNumber(jsonResponse, "wind", "deg"));
        
        // Location info
        result.put("city_name", extractJsonField(jsonResponse, "name"));
        result.put("country", extractNestedJsonField(jsonResponse, "sys", "country"));
        
        // Timestamps
        result.put("dt", extractJsonNumber(jsonResponse, "dt"));
        result.put("sunrise", extractNestedJsonNumber(jsonResponse, "sys", "sunrise"));
        result.put("sunset", extractNestedJsonNumber(jsonResponse, "sys", "sunset"));
        
        return result;
    }
    
    private Map<String, Object> parseForecastResponse(String jsonResponse, int days) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "forecast");
        result.put("days", days);
        
        // This is a simplified parser - in production you'd want a proper JSON library
        result.put("city_name", extractNestedJsonField(jsonResponse, "city", "name"));
        result.put("country", extractNestedJsonField(jsonResponse, "city", "country"));
        result.put("forecasts_count", extractJsonNumber(jsonResponse, "cnt"));
        
        // For simplicity, we'll include the raw response for now
        // In production, you'd parse the "list" array properly
        result.put("raw_forecast_data", jsonResponse);
        
        return result;
    }
    
    private Map<String, Object> parseAlertsResponse(String jsonResponse) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "weather_alerts");
        
        // Check if alerts exist in the response
        if (jsonResponse.contains("\"alerts\"")) {
            result.put("has_alerts", true);
            result.put("raw_alerts", jsonResponse);
        } else {
            result.put("has_alerts", false);
            result.put("message", "No active weather alerts");
        }
        
        return result;
    }
    
    private Map<String, Object> parseGeocodingResponse(String jsonResponse, String location) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "geocoding");
        result.put("query", location);
        result.put("raw_response", jsonResponse);
        
        // Simple check if we got results
        if (jsonResponse.startsWith("[") && jsonResponse.length() > 10) {
            result.put("has_results", true);
        } else {
            result.put("has_results", false);
        }
        
        return result;
    }
    
    private Map<String, Object> parseAirQualityResponse(String jsonResponse) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "air_quality");
        
        // Extract AQI and components
        result.put("aqi", extractNestedJsonNumber(jsonResponse, "list[0].main", "aqi"));
        result.put("co", extractNestedJsonNumber(jsonResponse, "list[0].components", "co"));
        result.put("no2", extractNestedJsonNumber(jsonResponse, "list[0].components", "no2"));
        result.put("o3", extractNestedJsonNumber(jsonResponse, "list[0].components", "o3"));
        result.put("pm2_5", extractNestedJsonNumber(jsonResponse, "list[0].components", "pm2_5"));
        result.put("pm10", extractNestedJsonNumber(jsonResponse, "list[0].components", "pm10"));
        
        return result;
    }
    
    private String extractJsonField(String json, String fieldName) {
        String pattern = "\"" + fieldName + "\"\\s*:\\s*\"([^\"]*?)\"";
        Pattern regex = Pattern.compile(pattern, Pattern.DOTALL);
        Matcher matcher = regex.matcher(json);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    private Double extractJsonNumber(String json, String fieldName) {
        String pattern = "\"" + fieldName + "\"\\s*:\\s*([\\d\\.\\-]+)";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(json);
        
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private String extractNestedJsonField(String json, String parentField, String childField) {
        // Simple nested field extraction for weather[0].description format
        String pattern = "\"" + parentField + "\"\\s*:\\s*\\[\\s*\\{[^}]*\"" + childField + "\"\\s*:\\s*\"([^\"]*?)\"";
        Pattern regex = Pattern.compile(pattern, Pattern.DOTALL);
        Matcher matcher = regex.matcher(json);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Try direct nested object format
        pattern = "\"" + parentField + "\"\\s*:\\s*\\{[^}]*\"" + childField + "\"\\s*:\\s*\"([^\"]*?)\"";
        regex = Pattern.compile(pattern, Pattern.DOTALL);
        matcher = regex.matcher(json);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    private Double extractNestedJsonNumber(String json, String parentField, String childField) {
        String pattern = "\"" + parentField + "\"\\s*:\\s*\\{[^}]*\"" + childField + "\"\\s*:\\s*([\\d\\.\\-]+)";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(json);
        
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    @Override
    public Map<String, Object> getRequestSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        // Location parameter (for current_weather, forecast)
        Map<String, Object> locationParam = new HashMap<>();
        locationParam.put("type", "string");
        locationParam.put("description", "City name, state code, and country code divided by comma");
        properties.put("location", locationParam);
        
        // Coordinates
        Map<String, Object> latParam = new HashMap<>();
        latParam.put("type", "string");
        latParam.put("description", "Latitude coordinate");
        properties.put("lat", latParam);
        
        Map<String, Object> lonParam = new HashMap<>();
        lonParam.put("type", "string");
        lonParam.put("description", "Longitude coordinate");
        properties.put("lon", lonParam);
        
        // Days for forecast
        Map<String, Object> daysParam = new HashMap<>();
        daysParam.put("type", "integer");
        daysParam.put("description", "Number of forecast days (max 5)");
        daysParam.put("minimum", 1);
        daysParam.put("maximum", 5);
        properties.put("days", daysParam);
        
        schema.put("properties", properties);
        
        return schema;
    }
    
    @Override
    public String[] getSupportedOperations() {
        return new String[]{"current_weather", "forecast", "weather_alerts", "geocoding", "air_quality"};
    }
    
    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            this.initialized = false;
        });
    }
}