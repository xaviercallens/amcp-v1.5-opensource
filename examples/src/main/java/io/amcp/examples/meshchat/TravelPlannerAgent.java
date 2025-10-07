package io.amcp.examples.meshchat;

import io.amcp.core.*;
import io.amcp.mobility.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;

/**
 * TravelPlannerAgent - Specialized agent for travel planning, booking assistance,
 * and destination recommendations. Handles travel-related queries from the orchestrator
 * and provides comprehensive travel services.
 */
public class TravelPlannerAgent implements MobileAgent {
    
    private static final String AGENT_TYPE = "TravelPlannerAgent";
    private static final String VERSION = "1.0.0";
    
    // Topic patterns for travel services
    private static final String TRAVEL_REQUEST_TOPIC = "travel.request.**";
    private static final String TRAVEL_REQUEST_EXACT = "travel.request";
    private static final String ORCHESTRATOR_RESPONSE_TOPIC = "orchestrator.response";
    
    // Agent state
    private final AgentID agentId;
    private AgentContext context;
    private AgentLifecycle lifecycleState = AgentLifecycle.INACTIVE;
    
    // Amadeus API integration
    private static final String AMADEUS_API_BASE_URL = "https://api.amadeus.com";
    private final HttpClient httpClient;
    private final String amadeusApiKey;
    private final String amadeusApiSecret;
    private final boolean useRealTimeData;
    private String amadeusAccessToken;
    private long tokenExpiryTime;
    
    // Travel service capabilities
    private final Map<String, TravelDestination> destinationDatabase = new HashMap<>();
    private final List<String> travelTips = new ArrayList<>();
    
    public TravelPlannerAgent() {
        this.agentId = new AgentID();
        
        // Initialize HTTP client for API calls
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
            
        // Get Amadeus API credentials from environment
        this.amadeusApiKey = System.getenv("AMADEUS_API_KEY");
        this.amadeusApiSecret = System.getenv("AMADEUS_API_SECRET");
        this.useRealTimeData = amadeusApiKey != null && !amadeusApiKey.trim().isEmpty() 
                             && amadeusApiSecret != null && !amadeusApiSecret.trim().isEmpty();
        
        if (useRealTimeData) {
            logMessage("🔑 Amadeus API credentials found - using real-time travel data");
        } else {
            logMessage("⚠️ No Amadeus API credentials found - using static travel data");
        }
        
        initializeTravelData();
    }
    
    @Override
    public AgentID getAgentId() {
        return agentId;
    }
    
    @Override
    public AgentContext getContext() {
        return context;
    }
    
    @Override
    public AgentLifecycle getLifecycleState() {
        return lifecycleState;
    }
    
    public void setContext(AgentContext context) {
        this.context = context;
    }
    
    public void setLifecycleState(AgentLifecycle state) {
        this.lifecycleState = state;
    }
    
    @Override
    public void onActivate() {
        try {
            setLifecycleState(AgentLifecycle.ACTIVE);
            logMessage("🌍 TravelPlannerAgent activating...");
            
            // Subscribe to travel-related topics and wait for completion
            subscribe(TRAVEL_REQUEST_TOPIC).get(); // Wait for subscription
            logMessage("📡 Subscribed to: " + TRAVEL_REQUEST_TOPIC);
            subscribe(TRAVEL_REQUEST_EXACT).get(); // Wait for subscription
            logMessage("📡 Subscribed to: " + TRAVEL_REQUEST_EXACT);
            subscribe("orchestrator.task.travel.**").get(); // Wait for subscription
            logMessage("📡 Subscribed to: orchestrator.task.travel.**");
            subscribe("meshchat.travel.**").get(); // Wait for subscription
            logMessage("📡 Subscribed to: meshchat.travel.**");
            
            // Register capabilities with orchestrator
            registerCapabilities();
            
            logMessage("✅ TravelPlannerAgent activated successfully");
            
        } catch (Exception e) {
            logMessage("❌ Error during activation: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void onDeactivate() {
        logMessage("⏸️ TravelPlannerAgent deactivating...");
        setLifecycleState(AgentLifecycle.INACTIVE);
    }
    
    @Override
    public void onDestroy() {
        logMessage("🔥 TravelPlannerAgent destroyed");
        setLifecycleState(AgentLifecycle.DESTROYED);
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            try {
                String topic = event.getTopic();
                logMessage("📨 Processing event: " + topic + " (correlationId: " + event.getCorrelationId() + ")");
                
                if (topic.startsWith("travel.request") || topic.startsWith("orchestrator.task.travel") 
                    || topic.startsWith("meshchat.travel")) {
                    handleTravelRequest(event);
                } else {
                    logMessage("⚠️ Unhandled topic: " + topic);
                }
                
            } catch (Exception e) {
                logMessage("❌ Error handling event: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    @Override
    public MobilityStrategy getMobilityStrategy() {
        return null;
    }
    
    // Required Agent interface methods
    @Override
    public CompletableFuture<Void> subscribe(String topicPattern) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (getContext() != null) {
                    getContext().subscribe(getAgentId(), topicPattern).get(); // Wait for subscription to complete
                    logMessage("📡 Subscribed to: " + topicPattern);
                }
            } catch (Exception e) {
                logMessage("❌ Failed to subscribe to " + topicPattern + ": " + e.getMessage());
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> unsubscribe(String topicPattern) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (getContext() != null) {
                    getContext().unsubscribe(getAgentId(), topicPattern);
                    logMessage("📡 Unsubscribed from: " + topicPattern);
                }
            } catch (Exception e) {
                logMessage("❌ Failed to unsubscribe from " + topicPattern + ": " + e.getMessage());
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> publishEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (getContext() != null) {
                    getContext().publishEvent(event);
                }
            } catch (Exception e) {
                logMessage("❌ Failed to publish event: " + e.getMessage());
            }
        });
    }
    
    @Override
    public void onBeforeMigration(String destinationContext) {
        logMessage("⚠️ Preparing for migration to: " + destinationContext);
    }
    
    @Override
    public void onAfterMigration(String sourceContext) {
        logMessage("✅ Migration completed from: " + sourceContext);
    }
    
    // Required MobileAgent interface methods
    @Override
    public CompletableFuture<Void> dispatch(String destinationContext) {
        return CompletableFuture.runAsync(() -> {
            logMessage("🚀 Dispatching to: " + destinationContext);
            // Implementation would handle agent dispatch
        });
    }
    
    @Override
    public CompletableFuture<AgentID> clone(String destinationContext) {
        return CompletableFuture.supplyAsync(() -> {
            logMessage("👥 Cloning to: " + destinationContext);
            // Implementation would handle agent cloning
            return new AgentID(); // Return cloned agent ID
        });
    }
    
    @Override
    public CompletableFuture<Void> retract(String sourceContext) {
        return CompletableFuture.runAsync(() -> {
            logMessage("↩️ Retracting from: " + sourceContext);
            // Implementation would handle agent retraction
        });
    }
    
    @Override
    public CompletableFuture<Void> migrate(MigrationOptions options) {
        return CompletableFuture.runAsync(() -> {
            logMessage("🔄 Migrating with options: " + options);
            // Implementation would handle agent migration
        });
    }
    
    @Override
    public CompletableFuture<List<AgentID>> replicate(String... contexts) {
        return CompletableFuture.supplyAsync(() -> {
            logMessage("📋 Replicating to contexts: " + Arrays.toString(contexts));
            // Implementation would handle agent replication
            return Arrays.asList(new AgentID()); // Return replica IDs
        });
    }
    
    @Override
    public CompletableFuture<Void> federateWith(List<AgentID> agents, String federationId) {
        return CompletableFuture.runAsync(() -> {
            logMessage("🤝 Federating with agents: " + agents + " in federation: " + federationId);
            // Implementation would handle agent federation
        });
    }
    
    @Override
    public MobilityState getMobilityState() {
        return MobilityState.STATIONARY;
    }
    
    @Override
    public List<MigrationEvent> getMigrationHistory() {
        // Return empty migration history for now
        return new ArrayList<>();
    }
    
    @Override
    public CompletableFuture<MobilityAssessment> assessMobility(String destinationContext) {
        return CompletableFuture.supplyAsync(() -> {
            // Return basic mobility assessment
            return null;
        });
    }
    
    @Override
    public void setMobilityStrategy(MobilityStrategy strategy) {
        // Implementation would set mobility strategy
    }
    
    /**
     * Handles travel-related requests from various sources
     */
    private void handleTravelRequest(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            
            String query = (String) payload.get("query");
            String taskType = (String) payload.get("taskType");
            String correlationId = event.getCorrelationId();
            
            if (query == null) {
                query = (String) payload.get("message");
            }
            
            if (query == null) {
                logMessage("⚠️ No query found in travel request");
                return;
            }
            
            logMessage("🧳 Processing travel query: " + query);
            
            // Analyze query and determine travel service needed
            TravelResponse response = processTravelQuery(query, taskType);
            
            // Send response back
            sendTravelResponse(response, correlationId, event.getMetadata());
            
        } catch (Exception e) {
            logMessage("❌ Error processing travel request: " + e.getMessage());
            sendErrorResponse(event.getCorrelationId(), "Failed to process travel request: " + e.getMessage());
        }
    }
    
    /**
     * Processes travel query and returns appropriate response
     */
    private TravelResponse processTravelQuery(String query, String taskType) {
        String lowerQuery = query.toLowerCase();
        
        // Destination recommendations
        if (lowerQuery.contains("destination") || lowerQuery.contains("where to go") 
            || lowerQuery.contains("travel to") || lowerQuery.contains("visit")) {
            return generateDestinationRecommendations(query);
        }
        
        // Trip planning
        if (lowerQuery.contains("plan") || lowerQuery.contains("itinerary") 
            || lowerQuery.contains("schedule") || lowerQuery.contains("trip")) {
            return generateTripPlan(query);
        }
        
        // Flight information
        if (lowerQuery.contains("flight") || lowerQuery.contains("airline") 
            || lowerQuery.contains("airport")) {
            return generateFlightInfo(query);
        }
        
        // Hotel information
        if (lowerQuery.contains("hotel") || lowerQuery.contains("accommodation") 
            || lowerQuery.contains("stay") || lowerQuery.contains("lodge")) {
            return generateHotelInfo(query);
        }
        
        // Travel tips
        if (lowerQuery.contains("tip") || lowerQuery.contains("advice") 
            || lowerQuery.contains("recommend") || lowerQuery.contains("suggest")) {
            return generateTravelTips(query);
        }
        
        // Budget planning
        if (lowerQuery.contains("budget") || lowerQuery.contains("cost") 
            || lowerQuery.contains("price") || lowerQuery.contains("expense")) {
            return generateBudgetInfo(query);
        }
        
        // General travel assistance
        return generateGeneralTravelAssistance(query);
    }
    
    /**
     * Generates destination recommendations
     */
    private TravelResponse generateDestinationRecommendations(String query) {
        StringBuilder response = new StringBuilder();
        response.append("🌍 **Destination Recommendations**\n\n");
        
        // Extract any mentioned preferences or season
        String lowerQuery = query.toLowerCase();
        List<TravelDestination> recommendations = new ArrayList<>();
        
        if (lowerQuery.contains("beach") || lowerQuery.contains("sun") || lowerQuery.contains("summer")) {
            recommendations.add(destinationDatabase.get("maldives"));
            recommendations.add(destinationDatabase.get("hawaii"));
            recommendations.add(destinationDatabase.get("santorini"));
        } else if (lowerQuery.contains("mountain") || lowerQuery.contains("ski") || lowerQuery.contains("winter")) {
            recommendations.add(destinationDatabase.get("switzerland"));
            recommendations.add(destinationDatabase.get("aspen"));
            recommendations.add(destinationDatabase.get("banff"));
        } else if (lowerQuery.contains("culture") || lowerQuery.contains("history") || lowerQuery.contains("museum")) {
            recommendations.add(destinationDatabase.get("rome"));
            recommendations.add(destinationDatabase.get("kyoto"));
            recommendations.add(destinationDatabase.get("paris"));
        } else {
            // Default popular destinations
            recommendations.add(destinationDatabase.get("paris"));
            recommendations.add(destinationDatabase.get("tokyo"));
            recommendations.add(destinationDatabase.get("newyork"));
        }
        
        for (TravelDestination dest : recommendations) {
            if (dest != null) {
                response.append("📍 **").append(dest.getName()).append("**\n");
                response.append("   ").append(dest.getDescription()).append("\n");
                response.append("   Best time: ").append(dest.getBestTime()).append("\n");
                response.append("   Highlights: ").append(String.join(", ", dest.getHighlights())).append("\n\n");
            }
        }
        
        return new TravelResponse("destination_recommendations", response.toString());
    }
    
    /**
     * Generates trip planning assistance
     */
    private TravelResponse generateTripPlan(String query) {
        StringBuilder response = new StringBuilder();
        response.append("📅 **Trip Planning Assistance**\n\n");
        
        response.append("Here's a suggested approach for planning your trip:\n\n");
        response.append("**Day 1-2: Arrival & Orientation**\n");
        response.append("• Settle into accommodation\n");
        response.append("• Explore nearby area and get oriented\n");
        response.append("• Visit key landmarks or attractions\n\n");
        
        response.append("**Day 3-5: Main Activities**\n");
        response.append("• Book and enjoy primary activities/tours\n");
        response.append("• Experience local culture and cuisine\n");
        response.append("• Visit museums, markets, or natural sites\n\n");
        
        response.append("**Day 6-7: Relaxation & Departure**\n");
        response.append("• Leisure time and shopping\n");
        response.append("• Final sightseeing or favorite spot revisit\n");
        response.append("• Prepare for departure\n\n");
        
        response.append("💡 **Pro Tips:**\n");
        response.append("• Book accommodations and flights early\n");
        response.append("• Research local customs and basic phrases\n");
        response.append("• Keep copies of important documents\n");
        response.append("• Pack appropriate clothing for the climate\n");
        
        return new TravelResponse("trip_planning", response.toString());
    }
    
    /**
     * Generates flight information and tips
     */
    private TravelResponse generateFlightInfo(String query) {
        StringBuilder response = new StringBuilder();
        response.append("✈️ **Flight Information**\n\n");
        
        // Try to fetch real flight data if available
        if (useRealTimeData) {
            try {
                String flightData = searchFlights(query);
                if (flightData != null) {
                    response.append(flightData);
                    return new TravelResponse("flight_info", response.toString());
                }
            } catch (Exception e) {
                logMessage("⚠️ Failed to fetch real flight data: " + e.getMessage());
            }
        }
        
        // Fallback to general flight tips
        response.append("**General Flight Tips:**\n");
        response.append("• Book flights 6-8 weeks in advance for best prices\n");
        response.append("• Tuesday and Wednesday are typically cheaper travel days\n");
        response.append("• Consider nearby airports for better deals\n");
        response.append("• Clear your browser cookies when searching for flights\n");
        response.append("• Sign up for airline newsletters for exclusive deals\n\n");
        
        response.append("**Airport Security Tips:**\n");
        response.append("• Arrive 2 hours early for domestic, 3 hours for international\n");
        response.append("• Check carry-on restrictions before packing\n");
        response.append("• Have your documents ready before security\n");
        response.append("• Consider TSA PreCheck or Global Entry\n\n");
        
        response.append("**In-Flight Comfort:**\n");
        response.append("• Choose your seat in advance if possible\n");
        response.append("• Bring noise-canceling headphones\n");
        response.append("• Stay hydrated and avoid excessive alcohol\n");
        response.append("• Pack entertainment and snacks\n\n");
        
        if (!useRealTimeData) {
            response.append("💡 **Note:** For real-time flight searches and booking, connect with an Amadeus API key.");
        }
        
        return new TravelResponse("flight_info", response.toString());
    }
    
    /**
     * Gets Amadeus OAuth access token
     */
    private String getAmadeusAccessToken() throws Exception {
        // Check if current token is still valid
        if (amadeusAccessToken != null && System.currentTimeMillis() < tokenExpiryTime) {
            return amadeusAccessToken;
        }
        
        // Get new access token
        String tokenUrl = AMADEUS_API_BASE_URL + "/v1/security/oauth2/token";
        String requestBody = "grant_type=client_credentials&client_id=" + amadeusApiKey + "&client_secret=" + amadeusApiSecret;
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(tokenUrl))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
            
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("OAuth token request failed: " + response.statusCode() + " - " + response.body());
        }
        
        // Parse token from response
        String responseBody = response.body();
        String accessToken = extractJsonString(responseBody, "\"access_token\":");
        String expiresIn = extractJsonString(responseBody, "\"expires_in\":");
        
        if (accessToken == null) {
            throw new RuntimeException("Could not extract access token from response");
        }
        
        this.amadeusAccessToken = accessToken;
        this.tokenExpiryTime = System.currentTimeMillis() + (Long.parseLong(expiresIn != null ? expiresIn : "3600") * 1000);
        
        logMessage("🔐 Amadeus access token refreshed");
        return amadeusAccessToken;
    }
    
    /**
     * Searches for flights using Amadeus API
     */
    private String searchFlights(String query) throws Exception {
        String accessToken = getAmadeusAccessToken();
        
        // Extract origin and destination from query (simplified parsing)
        String[] locations = extractLocationsFromQuery(query);
        String origin = locations[0];
        String destination = locations[1];
        
        if (origin == null || destination == null) {
            return null;
        }
        
        // Search for flights
        String searchUrl = AMADEUS_API_BASE_URL + "/v2/shopping/flight-offers" +
                          "?originLocationCode=" + origin +
                          "&destinationLocationCode=" + destination +
                          "&departureDate=2025-12-01" + // Default date
                          "&adults=1";
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(searchUrl))
            .timeout(Duration.ofSeconds(30))
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();
            
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Flight search failed: " + response.statusCode());
        }
        
        return parseFlightSearchResponse(response.body());
    }
    
    /**
     * Searches for hotels using Amadeus API
     */
    private String searchHotels(String destination) throws Exception {
        String accessToken = getAmadeusAccessToken();
        
        // First get city code
        String cityCode = getCityCode(destination);
        if (cityCode == null) {
            return null;
        }
        
        String searchUrl = AMADEUS_API_BASE_URL + "/v1/reference-data/locations/hotels/by-city" +
                          "?cityCode=" + cityCode;
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(searchUrl))
            .timeout(Duration.ofSeconds(30))
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();
            
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Hotel search failed: " + response.statusCode());
        }
        
        return parseHotelSearchResponse(response.body());
    }
    
    /**
     * Extracts origin and destination from natural language query
     */
    private String[] extractLocationsFromQuery(String query) {
        // Simplified location extraction
        String[] locations = new String[2];
        
        // Common patterns: "from X to Y", "X to Y", "flight to Y from X"
        query = query.toLowerCase();
        
        if (query.contains("from") && query.contains("to")) {
            String[] parts = query.split("from|to");
            if (parts.length >= 3) {
                locations[0] = extractAirportCode(parts[1].trim());
                locations[1] = extractAirportCode(parts[2].trim());
            }
        } else if (query.contains("to")) {
            String[] parts = query.split("to");
            if (parts.length >= 2) {
                locations[1] = extractAirportCode(parts[1].trim());
                locations[0] = "NYC"; // Default origin
            }
        }
        
        return locations;
    }
    
    /**
     * Maps city names to airport codes
     */
    private String extractAirportCode(String location) {
        Map<String, String> cityToCode = Map.of(
            "new york", "NYC",
            "london", "LON", 
            "paris", "PAR",
            "tokyo", "TYO",
            "los angeles", "LAX",
            "chicago", "CHI",
            "miami", "MIA",
            "san francisco", "SFO"
        );
        
        for (Map.Entry<String, String> entry : cityToCode.entrySet()) {
            if (location.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return location.toUpperCase();
    }
    
    /**
     * Gets city code for hotel searches
     */
    private String getCityCode(String destination) {
        Map<String, String> cityToCodes = Map.of(
            "paris", "PAR",
            "london", "LON",
            "tokyo", "TYO",
            "new york", "NYC",
            "rome", "ROM",
            "madrid", "MAD"
        );
        
        destination = destination.toLowerCase();
        for (Map.Entry<String, String> entry : cityToCodes.entrySet()) {
            if (destination.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * Parses flight search response from Amadeus API
     */
    private String parseFlightSearchResponse(String jsonResponse) {
        StringBuilder result = new StringBuilder();
        result.append("**Real-time Flight Search Results:**\n\n");
        
        try {
            // Simple parsing of flight offers
            if (jsonResponse.contains("\"data\":[")) {
                result.append("✈️ **Available Flights Found:**\n");
                result.append("• Multiple flight options available\n");
                result.append("• Prices vary by date and airline\n");
                result.append("• Book directly with airlines for best rates\n\n");
                
                result.append("📡 **Data provided by Amadeus Global Travel Platform**\n");
            } else {
                result.append("No flights found for the specified route.\n");
            }
        } catch (Exception e) {
            result.append("Error processing flight data: ").append(e.getMessage()).append("\n");
        }
        
        return result.toString();
    }
    
    /**
     * Parses hotel search response from Amadeus API
     */
    private String parseHotelSearchResponse(String jsonResponse) {
        StringBuilder result = new StringBuilder();
        result.append("**Real-time Hotel Search Results:**\n\n");
        
        try {
            if (jsonResponse.contains("\"data\":[")) {
                result.append("🏨 **Hotels Available:**\n");
                result.append("• Multiple accommodation options found\n");
                result.append("• Various price ranges and amenities\n");
                result.append("• Check availability for specific dates\n\n");
                
                result.append("📡 **Data provided by Amadeus Global Travel Platform**\n");
            } else {
                result.append("No hotels found for the destination.\n");
            }
        } catch (Exception e) {
            result.append("Error processing hotel data: ").append(e.getMessage()).append("\n");
        }
        
        return result.toString();
    }
    
    /**
     * Extracts string value from JSON
     */
    private String extractJsonString(String json, String key) {
        try {
            int startIndex = json.indexOf(key);
            if (startIndex == -1) return null;
            
            startIndex += key.length();
            if (startIndex >= json.length() || json.charAt(startIndex) != '"') return null;
            
            startIndex++; // Skip opening quote
            int endIndex = json.indexOf('"', startIndex);
            if (endIndex == -1) return null;
            
            return json.substring(startIndex, endIndex);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Generates hotel and accommodation information
     */
    private TravelResponse generateHotelInfo(String query) {
        StringBuilder response = new StringBuilder();
        response.append("🏨 **Hotel & Accommodation Information**\n\n");
        
        // Try to fetch real hotel data if available
        if (useRealTimeData) {
            try {
                String destination = extractDestinationFromQuery(query);
                if (destination != null) {
                    String hotelData = searchHotels(destination);
                    if (hotelData != null) {
                        response.append(hotelData);
                        return new TravelResponse("hotel_info", response.toString());
                    }
                }
            } catch (Exception e) {
                logMessage("⚠️ Failed to fetch real hotel data: " + e.getMessage());
            }
        }
        
        // Fallback to general hotel tips
        response.append("**Hotel Booking Tips:**\n");
        response.append("• Book directly with hotels for potential perks\n");
        response.append("• Read recent reviews from multiple sources\n");
        response.append("• Check cancellation policies carefully\n");
        response.append("• Consider location vs. price trade-offs\n");
        response.append("• Look for hotels with free WiFi and breakfast\n\n");
        
        response.append("**Accommodation Types:**\n");
        response.append("• Luxury Hotels - Full service with premium amenities\n");
        response.append("• Boutique Hotels - Unique character and personalized service\n");
        response.append("• Business Hotels - Convenient for work travelers\n");
        response.append("• Budget Hotels - Basic amenities at lower prices\n");
        response.append("• Vacation Rentals - Apartments, houses, unique stays\n\n");
        
        response.append("**What to Check:**\n");
        response.append("• Location and transportation access\n");
        response.append("• Room amenities and size\n");
        response.append("• Hotel facilities (pool, gym, restaurant)\n");
        response.append("• Parking availability and costs\n");
        response.append("• Pet policies if traveling with animals\n\n");
        
        if (!useRealTimeData) {
            response.append("💡 **Note:** For real-time hotel searches and booking, connect with an Amadeus API key.");
        }
        
        return new TravelResponse("hotel_info", response.toString());
    }
    
    /**
     * Extracts destination from query
     */
    private String extractDestinationFromQuery(String query) {
        String[] commonDestinations = {"paris", "london", "tokyo", "new york", "rome", "madrid", "barcelona"};
        
        query = query.toLowerCase();
        for (String destination : commonDestinations) {
            if (query.contains(destination)) {
                return destination;
            }
        }
        
        return null;
    }
    
    /**
     * Generates travel tips and advice
     */
    private TravelResponse generateTravelTips(String query) {
        StringBuilder response = new StringBuilder();
        response.append("💡 **Travel Tips & Advice**\n\n");
        
        // Get random tips from our database
        Collections.shuffle(travelTips);
        
        response.append("**Essential Travel Tips:**\n");
        for (int i = 0; i < Math.min(8, travelTips.size()); i++) {
            response.append("• ").append(travelTips.get(i)).append("\n");
        }
        
        response.append("\n**Safety Reminders:**\n");
        response.append("• Share your itinerary with someone at home\n");
        response.append("• Keep emergency contacts easily accessible\n");
        response.append("• Research local emergency numbers\n");
        response.append("• Trust your instincts about safety\n");
        
        return new TravelResponse("travel_tips", response.toString());
    }
    
    /**
     * Generates budget and cost information
     */
    private TravelResponse generateBudgetInfo(String query) {
        StringBuilder response = new StringBuilder();
        response.append("💰 **Travel Budget Planning**\n\n");
        
        response.append("**Main Cost Categories:**\n");
        response.append("• **Transportation:** 30-40% of budget\n");
        response.append("• **Accommodation:** 25-35% of budget\n");
        response.append("• **Food & Dining:** 15-25% of budget\n");
        response.append("• **Activities/Tours:** 10-15% of budget\n");
        response.append("• **Miscellaneous:** 5-10% of budget\n\n");
        
        response.append("**Money-Saving Tips:**\n");
        response.append("• Travel during shoulder season\n");
        response.append("• Book flights and hotels in advance\n");
        response.append("• Use public transportation\n");
        response.append("• Eat at local restaurants\n");
        response.append("• Take advantage of free activities\n\n");
        
        response.append("**Budget Tracking:**\n");
        response.append("• Set daily spending limits\n");
        response.append("• Keep track of expenses\n");
        response.append("• Have emergency funds available\n");
        response.append("• Consider travel insurance\n");
        
        return new TravelResponse("budget_info", response.toString());
    }
    
    /**
     * Generates general travel assistance
     */
    private TravelResponse generateGeneralTravelAssistance(String query) {
        StringBuilder response = new StringBuilder();
        response.append("🧳 **General Travel Assistance**\n\n");
        
        response.append("I'm here to help with all your travel needs! I can assist with:\n\n");
        response.append("📍 **Destination Recommendations**\n");
        response.append("• Find perfect destinations based on your preferences\n");
        response.append("• Seasonal travel advice\n");
        response.append("• Hidden gems and local favorites\n\n");
        
        response.append("📅 **Trip Planning**\n");
        response.append("• Create detailed itineraries\n");
        response.append("• Optimize travel routes\n");
        response.append("• Time management for sightseeing\n\n");
        
        response.append("✈️ **Transportation & Accommodation**\n");
        response.append("• Flight booking strategies\n");
        response.append("• Hotel and accommodation advice\n");
        response.append("• Local transportation options\n\n");
        
        response.append("💡 **Travel Tips & Budget Planning**\n");
        response.append("• Safety and security advice\n");
        response.append("• Cost-saving strategies\n");
        response.append("• Cultural etiquette guidance\n\n");
        
        response.append("Just ask me specific questions about your travel plans!");
        
        return new TravelResponse("general_assistance", response.toString());
    }
    
    /**
     * Sends travel response back to the requestor (CloudEvents compliant with structured data)
     */
    private void sendTravelResponse(TravelResponse travelResponse, String correlationId, Map<String, Object> originalMetadata) {
        try {
            // Create structured response payload
            Map<String, Object> responsePayload = new HashMap<>();
            
            // Structured data fields
            responsePayload.put("serviceType", travelResponse.getServiceType());
            responsePayload.put("recommendations", extractRecommendations(travelResponse));
            responsePayload.put("itinerary", extractItinerary(travelResponse));
            
            // Standard CloudEvents fields
            responsePayload.put("correlationId", correlationId);
            responsePayload.put("sourceAgent", AGENT_TYPE);
            responsePayload.put("timestamp", LocalDateTime.now().toString());
            responsePayload.put("success", true);
            
            // Also include formatted text for display
            responsePayload.put("formattedResponse", travelResponse.getContent());
            
            // Determine response topic based on original request
            String responseTopic = ORCHESTRATOR_RESPONSE_TOPIC;
            if (originalMetadata != null && "meshchat".equals(originalMetadata.get("source"))) {
                responseTopic = "meshchat.agent.response";
            }
            
            Event responseEvent = Event.builder()
                .topic(responseTopic)
                .payload(responsePayload)
                .correlationId(correlationId)
                .metadata("source", "travel_planner")
                .metadata("agentId", agentId.toString())
                .build();
            
            publishEvent(responseEvent);
            
            logMessage("📤 Sent structured travel response [" + correlationId + "]");
            
        } catch (Exception e) {
            logMessage("❌ Error sending travel response: " + e.getMessage());
        }
    }
    
    /**
     * Extracts structured recommendations from response
     */
    private List<Map<String, Object>> extractRecommendations(TravelResponse response) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        
        if ("destination_recommendations".equals(response.getServiceType())) {
            // Parse destination recommendations from content
            String content = response.getContent();
            if (content.contains("**") && content.contains("📍")) {
                // Extract structured data (simplified parsing)
                Map<String, Object> rec = new HashMap<>();
                rec.put("type", "destination");
                rec.put("content", content);
                recommendations.add(rec);
            }
        }
        
        return recommendations;
    }
    
    /**
     * Extracts structured itinerary from response
     */
    private Map<String, Object> extractItinerary(TravelResponse response) {
        Map<String, Object> itinerary = new HashMap<>();
        
        if ("trip_planning".equals(response.getServiceType())) {
            itinerary.put("type", "trip_plan");
            itinerary.put("days", extractDays(response.getContent()));
        }
        
        return itinerary;
    }
    
    /**
     * Extracts day-by-day breakdown from itinerary
     */
    private List<Map<String, String>> extractDays(String content) {
        List<Map<String, String>> days = new ArrayList<>();
        
        // Simple extraction of day sections
        String[] lines = content.split("\n");
        Map<String, String> currentDay = null;
        
        for (String line : lines) {
            if (line.contains("**Day")) {
                if (currentDay != null) {
                    days.add(currentDay);
                }
                currentDay = new HashMap<>();
                currentDay.put("title", line.replaceAll("\\*", "").trim());
                currentDay.put("activities", "");
            } else if (currentDay != null && line.startsWith("•")) {
                String activities = currentDay.get("activities");
                currentDay.put("activities", activities + line + "\n");
            }
        }
        
        if (currentDay != null) {
            days.add(currentDay);
        }
        
        return days;
    }
    
    /**
     * Sends error response
     */
    private void sendErrorResponse(String correlationId, String errorMessage) {
        try {
            Map<String, Object> errorPayload = new HashMap<>();
            errorPayload.put("response", "❌ " + errorMessage);
            errorPayload.put("success", false);
            errorPayload.put("error", true);
            errorPayload.put("agentType", AGENT_TYPE);
            
            Event errorEvent = Event.builder()
                .topic(ORCHESTRATOR_RESPONSE_TOPIC)
                .payload(errorPayload)
                .correlationId(correlationId)
                .build();
            
            publishEvent(errorEvent);
            
        } catch (Exception e) {
            logMessage("❌ Error sending error response: " + e.getMessage());
        }
    }
    
    /**
     * Registers agent capabilities with the orchestrator
     */
    private void registerCapabilities() {
        try {
            Map<String, Object> capabilities = new HashMap<>();
            capabilities.put("travel.destination.recommendations", "Provide destination recommendations based on preferences");
            capabilities.put("travel.planning.itinerary", "Create detailed trip plans and itineraries");
            capabilities.put("travel.booking.flights", "Flight search tips and booking strategies");
            capabilities.put("travel.booking.hotels", "Hotel and accommodation recommendations");
            capabilities.put("travel.tips.general", "General travel advice and tips");
            capabilities.put("travel.budget.planning", "Budget planning and cost estimation");
            
            Map<String, Object> registrationData = new HashMap<>();
            registrationData.put("agentId", agentId.toString());
            registrationData.put("agentType", AGENT_TYPE);
            registrationData.put("version", VERSION);
            registrationData.put("capabilities", capabilities);
            registrationData.put("status", "active");
            registrationData.put("specialization", "travel_planning");
            registrationData.put("timestamp", LocalDateTime.now().toString());
            
            Event registrationEvent = Event.builder()
                .topic("registry.agent.register")
                .payload(registrationData)
                .correlationId("travel-registration-" + System.currentTimeMillis())
                .build();
            
            publishEvent(registrationEvent);
            
            logMessage("📋 Registered travel planning capabilities");
            
        } catch (Exception e) {
            logMessage("⚠️ Failed to register capabilities: " + e.getMessage());
        }
    }
    
    /**
     * Initialize travel destination database
     */
    private void initializeTravelData() {
        // Popular destinations
        destinationDatabase.put("paris", new TravelDestination("Paris, France", 
            "The City of Light, famous for art, fashion, and romance", 
            "April-June, September-October",
            Arrays.asList("Eiffel Tower", "Louvre Museum", "Seine River Cruise", "Montmartre")));
            
        destinationDatabase.put("tokyo", new TravelDestination("Tokyo, Japan",
            "Modern metropolis blending traditional culture with futuristic innovation",
            "March-May, September-November", 
            Arrays.asList("Shibuya Crossing", "Senso-ji Temple", "Tsukiji Market", "Imperial Palace")));
            
        destinationDatabase.put("newyork", new TravelDestination("New York City, USA",
            "The Big Apple - iconic skyline, Broadway shows, and diverse neighborhoods",
            "April-June, September-November",
            Arrays.asList("Statue of Liberty", "Central Park", "Times Square", "Brooklyn Bridge")));
            
        destinationDatabase.put("maldives", new TravelDestination("Maldives",
            "Tropical paradise with crystal-clear waters and overwater bungalows",
            "November-April",
            Arrays.asList("Snorkeling", "Diving", "Beach relaxation", "Water sports")));
            
        destinationDatabase.put("switzerland", new TravelDestination("Swiss Alps, Switzerland",
            "Stunning mountain scenery, skiing, and charming alpine villages",
            "December-March (skiing), June-September (hiking)",
            Arrays.asList("Skiing", "Mountain hiking", "Train rides", "Lake views")));
            
        destinationDatabase.put("rome", new TravelDestination("Rome, Italy",
            "The Eternal City with incredible history, art, and cuisine",
            "April-June, September-October",
            Arrays.asList("Colosseum", "Vatican City", "Trevi Fountain", "Roman Forum")));
            
        destinationDatabase.put("kyoto", new TravelDestination("Kyoto, Japan",
            "Former imperial capital with beautiful temples and traditional culture",
            "March-May, September-November",
            Arrays.asList("Fushimi Inari Shrine", "Bamboo Grove", "Kiyomizu Temple", "Geisha districts")));
            
        destinationDatabase.put("hawaii", new TravelDestination("Hawaii, USA",
            "Tropical islands with volcanic landscapes and beautiful beaches",
            "April-June, September-November",
            Arrays.asList("Volcanoes National Park", "Snorkeling", "Luau", "Surfing")));
            
        destinationDatabase.put("santorini", new TravelDestination("Santorini, Greece",
            "Picturesque island with white-washed buildings and stunning sunsets",
            "April-June, September-October",
            Arrays.asList("Sunset viewing", "Wine tasting", "Beach relaxation", "Ancient ruins")));
            
        destinationDatabase.put("aspen", new TravelDestination("Aspen, Colorado, USA",
            "Premier ski destination with luxury amenities and mountain beauty",
            "December-March (skiing), June-September (hiking)",
            Arrays.asList("World-class skiing", "Mountain hiking", "Fine dining", "Art galleries")));
            
        destinationDatabase.put("banff", new TravelDestination("Banff, Canada",
            "Spectacular Canadian Rockies with pristine wilderness and wildlife",
            "June-September (hiking), December-March (skiing)",
            Arrays.asList("Lake Louise", "Wildlife viewing", "Hiking trails", "Hot springs")));
        
        // Travel tips
        travelTips.addAll(Arrays.asList(
            "Pack light and bring versatile clothing items",
            "Always have backup copies of important documents",
            "Research local customs and basic phrases before visiting",
            "Notify your bank of travel plans to avoid card issues",
            "Pack a small first-aid kit with basic medications",
            "Use packing cubes to stay organized and save space",
            "Download offline maps before traveling",
            "Keep your phone charged and bring a portable charger",
            "Try to learn a few words in the local language",
            "Be flexible with your itinerary and open to spontaneous adventures",
            "Always check visa requirements well in advance",
            "Consider travel insurance for expensive trips",
            "Pack an extra day's worth of essentials in your carry-on",
            "Research local transportation options before arrival",
            "Keep cash in local currency for small purchases",
            "Respect local dress codes and cultural norms",
            "Stay hydrated, especially during long flights",
            "Take photos of your luggage contents before traveling"
        ));
    }
    
    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [TravelPlannerAgent] " + message);
    }
    
    /**
     * Health check for orchestrator integration
     */
    public boolean isHealthy() {
        // Check if agent is active
        if (lifecycleState != AgentLifecycle.ACTIVE) {
            return false;
        }
        
        // Check if context is available
        if (context == null) {
            return false;
        }
        
        // Check if destination database is initialized
        if (destinationDatabase.isEmpty()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Travel destination data class
     */
    public static class TravelDestination {
        private final String name;
        private final String description;
        private final String bestTime;
        private final List<String> highlights;
        
        public TravelDestination(String name, String description, String bestTime, List<String> highlights) {
            this.name = name;
            this.description = description;
            this.bestTime = bestTime;
            this.highlights = new ArrayList<>(highlights);
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getBestTime() { return bestTime; }
        public List<String> getHighlights() { return highlights; }
    }
    
    /**
     * Travel response data class
     */
    public static class TravelResponse {
        private final String serviceType;
        private final String content;
        
        public TravelResponse(String serviceType, String content) {
            this.serviceType = serviceType;
            this.content = content;
        }
        
        public String getServiceType() { return serviceType; }
        public String getContent() { return content; }
    }
}