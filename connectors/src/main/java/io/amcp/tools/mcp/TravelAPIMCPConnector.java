package io.amcp.tools.mcp;

import io.amcp.tools.ToolConnector;
import io.amcp.tools.ToolRequest;
import io.amcp.tools.ToolResponse;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;

/**
 * AMCP v1.5 Travel API MCP Connector.
 * Provides real-time travel data using Amadeus Developer API
 * following the Model Context Protocol (MCP) specification.
 * 
 * Supported operations:
 * - search_flights: Search for flights between destinations
 * - search_hotels: Search for hotels in a city
 * - get_location: Get airport or city information
 * - flight_inspiration: Get flight inspiration for travel planning
 * - hotel_offers: Get detailed hotel offers
 * 
 * @author AMCP Team
 * @version 1.5.0
 * @since 2024-12-18
 */
public class TravelAPIMCPConnector implements ToolConnector {
    
    private final HttpClient httpClient;
    private final String clientId;
    private final String clientSecret;
    private final String baseUrl = "https://api.amadeus.com";
    private volatile boolean initialized = false;
    private volatile String accessToken = null;
    private volatile long tokenExpirationTime = 0;
    
    /**
     * Constructor with API credentials configuration.
     * 
     * @param clientId The Amadeus API client ID
     * @param clientSecret The Amadeus API client secret
     */
    public TravelAPIMCPConnector(String clientId, String clientSecret) {
        this.clientId = clientId != null ? clientId : System.getenv("AMADEUS_API_KEY");
        this.clientSecret = clientSecret != null ? clientSecret : System.getenv("AMADEUS_API_SECRET");
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        
        if (this.clientId != null && !this.clientId.trim().isEmpty() && 
            this.clientSecret != null && !this.clientSecret.trim().isEmpty()) {
            this.initialized = true;
        }
    }
    
    /**
     * Default constructor that reads API credentials from environment.
     */
    public TravelAPIMCPConnector() {
        this(null, null);
    }
    
    @Override
    public String getToolId() {
        return "travel-api-mcp";
    }
    
    @Override
    public String getToolName() {
        return "Travel API";
    }
    
    @Override
    public String getVersion() {
        return "1.5.0";
    }
    
    @Override
    public CompletableFuture<Boolean> isHealthy() {
        return CompletableFuture.completedFuture(initialized);
    }
    
    @Override
    public CompletableFuture<Void> initialize(Map<String, Object> config) {
        return CompletableFuture.runAsync(() -> {
            if (clientId == null || clientId.trim().isEmpty() || 
                clientSecret == null || clientSecret.trim().isEmpty()) {
                System.err.println("Warning: AMADEUS_API_KEY or AMADEUS_API_SECRET not found. Travel API connector will not work.");
                initialized = false;
            } else {
                initialized = true;
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            // HTTP client cleanup is automatic
            accessToken = null;
            tokenExpirationTime = 0;
            initialized = false;
        });
    }
    
    @Override
    public CompletableFuture<ToolResponse> invoke(ToolRequest request) {
        long startTime = System.currentTimeMillis();
        return CompletableFuture.supplyAsync(() -> {
            if (!initialized) {
                return ToolResponse.error("Travel API connector not initialized", 
                                        request.getRequestId(), 
                                        System.currentTimeMillis() - startTime);
            }
            
            try {
                String operation = request.getOperation();
                Map<String, Object> params = request.getParameters();
                
                switch (operation) {
                    case "search_flights":
                        return searchFlights(params, request.getRequestId(), startTime);
                    case "search_hotels":
                        return searchHotels(params, request.getRequestId(), startTime);
                    case "get_location":
                        return getLocation(params, request.getRequestId(), startTime);
                    case "flight_inspiration":
                        return getFlightInspiration(params, request.getRequestId(), startTime);
                    case "hotel_offers":
                        return getHotelOffers(params, request.getRequestId(), startTime);
                    default:
                        return ToolResponse.error("Unknown operation: " + operation, 
                                                request.getRequestId(), 
                                                System.currentTimeMillis() - startTime);
                }
            } catch (Exception e) {
                return ToolResponse.error("Travel API error: " + e.getMessage(), 
                                        request.getRequestId(), 
                                        System.currentTimeMillis() - startTime);
            }
        });
    }

    @Override
    public String[] getSupportedOperations() {
        return new String[]{
            "search_flights",
            "search_hotels", 
            "get_location",
            "flight_inspiration",
            "hotel_offers"
        };
    }
    
    @Override
    public Map<String, Object> getRequestSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", createSchemaProperties());
        return schema;
    }
    
    private Map<String, Object> createSchemaProperties() {
        Map<String, Object> properties = new HashMap<>();
        
        // Flight search operation
        Map<String, Object> flightSearch = new HashMap<>();
        flightSearch.put("type", "object");
        flightSearch.put("description", "Search for flights between destinations");
        flightSearch.put("required", new String[]{"origin", "destination", "departureDate"});
        Map<String, Object> flightProps = new HashMap<>();
        flightProps.put("origin", Map.of("type", "string", "description", "Origin airport code"));
        flightProps.put("destination", Map.of("type", "string", "description", "Destination airport code"));
        flightProps.put("departureDate", Map.of("type", "string", "description", "Departure date (YYYY-MM-DD)"));
        flightSearch.put("properties", flightProps);
        properties.put("search_flights", flightSearch);
        
        return properties;
    }
    
    private ToolResponse searchFlights(Map<String, Object> params, String requestId, long startTime) {
        try {
            String token = getValidAccessToken();
            if (token == null) {
                return ToolResponse.error("Failed to obtain access token", 
                                        requestId, 
                                        System.currentTimeMillis() - startTime);
            }
            
            Object originObj = params.get("origin");
            Object destinationObj = params.get("destination");
            Object departureDateObj = params.get("departureDate");
            
            if (originObj == null || destinationObj == null || departureDateObj == null) {
                return ToolResponse.error("Origin, destination, and departureDate parameters are required", 
                                        requestId, 
                                        System.currentTimeMillis() - startTime);
            }
            
            String origin = originObj.toString().toUpperCase();
            String destination = destinationObj.toString().toUpperCase();
            String departureDate = departureDateObj.toString();
            
            String url = baseUrl + "/v2/shopping/flight-offers?originLocationCode=" + origin + 
                        "&destinationLocationCode=" + destination + 
                        "&departureDate=" + departureDate + 
                        "&adults=1&max=10";
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                Map<String, Object> result = parseFlightSearchResponse(response.body());
                return ToolResponse.success(result, requestId, System.currentTimeMillis() - startTime);
            } else {
                return ToolResponse.error("API request failed: " + response.statusCode() + " - " + response.body(), 
                                        requestId, 
                                        System.currentTimeMillis() - startTime);
            }
        } catch (Exception e) {
            return ToolResponse.error("Failed to search flights: " + e.getMessage(), 
                                    requestId, 
                                    System.currentTimeMillis() - startTime);
        }
    }
    
    private ToolResponse searchHotels(Map<String, Object> params, String requestId, long startTime) {
        try {
            String token = getValidAccessToken();
            if (token == null) {
                return ToolResponse.error("Failed to obtain access token", 
                                        requestId, 
                                        System.currentTimeMillis() - startTime);
            }
            
            Object cityCodeObj = params.get("cityCode");
            if (cityCodeObj == null) {
                return ToolResponse.error("cityCode parameter is required", 
                                        requestId, 
                                        System.currentTimeMillis() - startTime);
            }
            
            String cityCode = cityCodeObj.toString().toUpperCase();
            // Note: checkIn/checkOut dates would be used in hotel offers API, not location search
            String url = baseUrl + "/v1/reference-data/locations/hotels/by-city?cityCode=" + cityCode;
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                Map<String, Object> result = parseHotelSearchResponse(response.body());
                return ToolResponse.success(result, requestId, System.currentTimeMillis() - startTime);
            } else {
                return ToolResponse.error("API request failed: " + response.statusCode() + " - " + response.body(), 
                                        requestId, 
                                        System.currentTimeMillis() - startTime);
            }
        } catch (Exception e) {
            return ToolResponse.error("Failed to search hotels: " + e.getMessage(), 
                                    requestId, 
                                    System.currentTimeMillis() - startTime);
        }
    }
    
    private ToolResponse getLocation(Map<String, Object> params, String requestId, long startTime) {
        try {
            String token = getValidAccessToken();
            if (token == null) {
                return ToolResponse.error("Failed to obtain access token", 
                                        requestId, 
                                        System.currentTimeMillis() - startTime);
            }
            
            Object keywordObj = params.get("keyword");
            if (keywordObj == null) {
                return ToolResponse.error("keyword parameter is required", 
                                        requestId, 
                                        System.currentTimeMillis() - startTime);
            }
            
            String keyword = keywordObj.toString();
            String url = baseUrl + "/v1/reference-data/locations?keyword=" + keyword + "&subType=AIRPORT,CITY";
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                Map<String, Object> result = parseLocationResponse(response.body());
                return ToolResponse.success(result, requestId, System.currentTimeMillis() - startTime);
            } else {
                return ToolResponse.error("API request failed: " + response.statusCode() + " - " + response.body(), 
                                        requestId, 
                                        System.currentTimeMillis() - startTime);
            }
        } catch (Exception e) {
            return ToolResponse.error("Failed to get location: " + e.getMessage(), 
                                    requestId, 
                                    System.currentTimeMillis() - startTime);
        }
    }
    
    private ToolResponse getFlightInspiration(Map<String, Object> params, String requestId, long startTime) {
        try {
            String token = getValidAccessToken();
            if (token == null) {
                return ToolResponse.error("Failed to obtain access token", 
                                        requestId, 
                                        System.currentTimeMillis() - startTime);
            }
            
            Object originObj = params.get("origin");
            if (originObj == null) {
                return ToolResponse.error("origin parameter is required", 
                                        requestId, 
                                        System.currentTimeMillis() - startTime);
            }
            
            String origin = originObj.toString().toUpperCase();
            String url = baseUrl + "/v1/shopping/flight-destinations?origin=" + origin + "&maxPrice=1000";
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                Map<String, Object> result = parseFlightInspirationResponse(response.body());
                return ToolResponse.success(result, requestId, System.currentTimeMillis() - startTime);
            } else {
                return ToolResponse.error("API request failed: " + response.statusCode() + " - " + response.body(), 
                                        requestId, 
                                        System.currentTimeMillis() - startTime);
            }
        } catch (Exception e) {
            return ToolResponse.error("Failed to get flight inspiration: " + e.getMessage(), 
                                    requestId, 
                                    System.currentTimeMillis() - startTime);
        }
    }
    
    private ToolResponse getHotelOffers(Map<String, Object> params, String requestId, long startTime) {
        try {
            String token = getValidAccessToken();
            if (token == null) {
                return ToolResponse.error("Failed to obtain access token", 
                                        requestId, 
                                        System.currentTimeMillis() - startTime);
            }
            
            Object hotelIdsObj = params.get("hotelIds");
            if (hotelIdsObj == null) {
                return ToolResponse.error("hotelIds parameter is required", 
                                        requestId, 
                                        System.currentTimeMillis() - startTime);
            }
            
            String hotelIds = hotelIdsObj.toString();
            String checkInDate = params.getOrDefault("checkInDate", "2024-12-25").toString();
            String checkOutDate = params.getOrDefault("checkOutDate", "2024-12-27").toString();
            
            String url = baseUrl + "/v3/shopping/hotel-offers?hotelIds=" + hotelIds + 
                        "&checkInDate=" + checkInDate + "&checkOutDate=" + checkOutDate;
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                Map<String, Object> result = parseHotelOffersResponse(response.body());
                return ToolResponse.success(result, requestId, System.currentTimeMillis() - startTime);
            } else {
                return ToolResponse.error("API request failed: " + response.statusCode() + " - " + response.body(), 
                                        requestId, 
                                        System.currentTimeMillis() - startTime);
            }
        } catch (Exception e) {
            return ToolResponse.error("Failed to get hotel offers: " + e.getMessage(), 
                                    requestId, 
                                    System.currentTimeMillis() - startTime);
        }
    }
    
    private String getValidAccessToken() {
        try {
            // Check if current token is still valid (with 5 minutes buffer)
            if (accessToken != null && System.currentTimeMillis() < (tokenExpirationTime - 300000)) {
                return accessToken;
            }
            
            // Get new access token
            String url = baseUrl + "/v1/security/oauth2/token";
            String body = "grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret;
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                
                // Simple JSON parsing for access token
                String token = extractJsonString(responseBody, "\"access_token\":");
                if (token != null) {
                    accessToken = token;
                    // Set expiration time (typically 30 minutes, we'll use 25 to be safe)
                    tokenExpirationTime = System.currentTimeMillis() + (25 * 60 * 1000);
                    return accessToken;
                }
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("Failed to get access token: " + e.getMessage());
            return null;
        }
    }
    
    // Response parsing methods (simplified JSON parsing)
    
    private Map<String, Object> parseFlightSearchResponse(String json) {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("source", "amadeus");
        result.put("operation", "flight_search");
        
        // Count offers by counting occurrences of "price"
        int offerCount = json.split("\"price\"").length - 1;
        result.put("offers_found", offerCount);
        result.put("raw_response", json.length() > 1500 ? json.substring(0, 1500) + "..." : json);
        
        return result;
    }
    
    private Map<String, Object> parseHotelSearchResponse(String json) {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("source", "amadeus");
        result.put("operation", "hotel_search");
        
        // Count hotels by counting occurrences of "hotelId"
        int hotelCount = json.split("\"hotelId\"").length - 1;
        result.put("hotels_found", hotelCount);
        result.put("raw_response", json.length() > 1000 ? json.substring(0, 1000) + "..." : json);
        
        return result;
    }
    
    private Map<String, Object> parseLocationResponse(String json) {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("source", "amadeus");
        result.put("operation", "location_search");
        
        // Count locations by counting occurrences of "iataCode"
        int locationCount = json.split("\"iataCode\"").length - 1;
        result.put("locations_found", locationCount);
        result.put("raw_response", json.length() > 800 ? json.substring(0, 800) + "..." : json);
        
        return result;
    }
    
    private Map<String, Object> parseFlightInspirationResponse(String json) {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("source", "amadeus");
        result.put("operation", "flight_inspiration");
        
        // Count destinations by counting occurrences of "destination"
        int destinationCount = json.split("\"destination\"").length - 1;
        result.put("destinations_found", destinationCount);
        result.put("raw_response", json.length() > 1200 ? json.substring(0, 1200) + "..." : json);
        
        return result;
    }
    
    private Map<String, Object> parseHotelOffersResponse(String json) {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("source", "amadeus");
        result.put("operation", "hotel_offers");
        
        // Count offers by counting occurrences of "offers"
        int offerCount = json.split("\"offers\"").length - 1;
        result.put("offers_found", offerCount);
        result.put("raw_response", json.length() > 1200 ? json.substring(0, 1200) + "..." : json);
        
        return result;
    }
    
    private String extractJsonString(String json, String key) {
        try {
            int startIndex = json.indexOf(key);
            if (startIndex == -1) return null;
            
            startIndex += key.length();
            
            // Skip whitespace and opening quote
            while (startIndex < json.length() && (Character.isWhitespace(json.charAt(startIndex)) || json.charAt(startIndex) == '"')) {
                startIndex++;
            }
            
            // Find closing quote
            int endIndex = startIndex;
            while (endIndex < json.length() && json.charAt(endIndex) != '"') {
                endIndex++;
            }
            
            return json.substring(startIndex, endIndex);
        } catch (Exception e) {
            return null;
        }
    }
}