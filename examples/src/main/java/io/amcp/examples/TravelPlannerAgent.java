package io.amcp.examples.travel;

import io.amcp.core.AbstractAgent;
import io.amcp.core.AgentID;
import io.amcp.core.Event;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Travel Planner Agent that integrates with Amadeus Flight Offers Search API.
 * Follows AMCP patterns from WeatherCollectorAgent for external API integration,
 * event handling, and agent lifecycle management.
 */
public class TravelPlannerAgent extends AbstractAgent {
    private static final long serialVersionUID = 1L;
    
    // Agent configuration
    private static final String AGENT_TYPE = "TravelPlanner";
    
    // Supported event topics
    private static final String FLIGHT_SEARCH_TOPIC = "travel.flight.search";
    private static final String FLIGHT_RESULTS_TOPIC = "travel.flight.results";
    private static final String FLIGHT_ERROR_TOPIC = "travel.flight.error";
    private static final String TRAVEL_CONTROL_TOPIC = "travel.control";
    
    // API integration
    private AmadeusApiClient amadeusClient;
    
    // Request tracking and state management
    private final AtomicLong requestCounter = new AtomicLong(0);
    private final Map<String, FlightSearchRequest> activeRequests = new ConcurrentHashMap<>();
    private final Map<String, FlightOffersSearchResult> cachedResults = new ConcurrentHashMap<>();
    
    public TravelPlannerAgent(AgentID agentId) {
        super(agentId, AGENT_TYPE);
        log("TravelPlannerAgent initialized");
    }
    
    @Override
    public void onActivate() {
        super.onActivate();
        
        try {
            // Initialize Amadeus API client
            this.amadeusClient = new AmadeusApiClient("travel-agent-" + getAgentId());
            
            // Subscribe to travel-related events
            subscribe(FLIGHT_SEARCH_TOPIC);
            subscribe(TRAVEL_CONTROL_TOPIC + ".*");
            
            log("TravelPlannerAgent activated and subscribed to topics");
            log("Amadeus API integration: " + (amadeusClient != null ? amadeusClient.getBaseUrl() : "Not available"));
            log("API Key type: " + (amadeusClient != null ? amadeusClient.getApiKeyType() : "Not configured"));
        } catch (Exception e) {
            log("Error during agent activation: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void onDeactivate() {
        log("TravelPlannerAgent deactivating...");
        
        try {
            // Clear cached data
            cachedResults.clear();
            activeRequests.clear();
            
            // Cleanup API client resources
            if (amadeusClient != null) {
                // Add cleanup if AmadeusApiClient has cleanup methods
                log("Cleaning up API client resources");
            }
            
            // Log statistics
            logStatistics();
        } catch (Exception e) {
            log("Error during agent deactivation: " + e.getMessage());
        } finally {
            super.onDeactivate();
        }
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            try {
                String topic = event.getTopic();
                long requestId = requestCounter.incrementAndGet();
                
                log("Received event #" + requestId + " on topic: " + topic);
                
                if (FLIGHT_SEARCH_TOPIC.equals(topic)) {
                    handleFlightSearchEvent(event, requestId);
                } else if (topic.startsWith(TRAVEL_CONTROL_TOPIC)) {
                    handleControlEvent(event, requestId);
                } else {
                    log("Unhandled event topic: " + topic);
                }
                
            } catch (Exception e) {
                log("Error handling event: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Handles flight search events. Supports multiple search request formats:
     * 1. FlightSearchRequest object as payload
     * 2. Map with search parameters
     * 3. String with formatted search request
     */
    private void handleFlightSearchEvent(Event event, long requestId) {
        try {
            FlightSearchRequest searchRequest = parseSearchRequest(event);
            
            if (searchRequest != null) {
                log("Processing flight search request #" + requestId + ": " + searchRequest);
                
                // Store request for tracking
                String requestKey = "req-" + requestId;
                activeRequests.put(requestKey, searchRequest);
                
                // Perform the flight search
                performFlightSearch(searchRequest, requestKey, event.getSender());
                
            } else {
                publishErrorEvent("Invalid flight search request format", event.getSender(), requestId);
            }
            
        } catch (Exception e) {
            log("Error processing flight search event #" + requestId + ": " + e.getMessage());
            publishErrorEvent("Flight search processing failed: " + e.getMessage(), event.getSender(), requestId);
        }
    }
    
    /**
     * Performs the actual flight search using Amadeus API.
     */
    private void performFlightSearch(FlightSearchRequest searchRequest, String requestKey, AgentID requestingAgent) {
        try {
            // Validate search request before making API call
            String validationError = validateSearchRequest(searchRequest);
            if (validationError != null) {
                log("Search request validation failed for " + requestKey + ": " + validationError);
                activeRequests.remove(requestKey);
                publishErrorEvent("Invalid search request: " + validationError, requestingAgent, requestKey);
                return;
            }
            
            // Check if API client is available
            if (amadeusClient == null) {
                log("API client not available for request: " + requestKey);
                activeRequests.remove(requestKey);
                publishErrorEvent("Travel planning service temporarily unavailable", requestingAgent, requestKey);
                return;
            }
            
            log("Calling Amadeus Flight Offers Search API for request: " + requestKey);
            
            // Make the API call
            FlightOffersSearchResult result = amadeusClient.searchFlightOffers(searchRequest);
            
            // Cache the result
            cachedResults.put(requestKey, result);
            
            // Remove from active requests
            activeRequests.remove(requestKey);
            
            if (result != null && result.isSuccessful()) {
                log("Flight search completed successfully: " + result.getSummary());
                publishFlightResults(result, requestingAgent, requestKey);
            } else {
                String errorMsg = result != null ? result.getError() : "Unknown API error";
                log("Flight search failed: " + errorMsg);
                publishErrorEvent("Flight search failed: " + errorMsg, requestingAgent, requestKey);
            }
            
        } catch (IOException e) {
            log("API call failed for request " + requestKey + ": " + e.getMessage());
            activeRequests.remove(requestKey);
            publishErrorEvent("API call failed: " + e.getMessage(), requestingAgent, requestKey);
            
        } catch (Exception e) {
            log("Unexpected error during flight search " + requestKey + ": " + e.getMessage());
            activeRequests.remove(requestKey);
            publishErrorEvent("Unexpected error: " + e.getMessage(), requestingAgent, requestKey);
        }
    }
    
    /**
     * Parses flight search request from event payload.
     * Supports multiple input formats for flexibility.
     */
    private FlightSearchRequest parseSearchRequest(Event event) {
        Object payload = event.getPayload();
        
        if (payload instanceof FlightSearchRequest) {
            return (FlightSearchRequest) payload;
        }
        
        if (payload instanceof Map) {
            return parseSearchRequestFromMap((Map<?, ?>) payload);
        }
        
        if (payload instanceof String) {
            return parseSearchRequestFromString((String) payload);
        }
        
        return null;
    }
    
    /**
     * Parses flight search request from Map payload.
     * Expected keys: origin, destination, departureDate, returnDate (optional), adults, children, cabinClass, etc.
     */
    @SuppressWarnings("unchecked")
    private FlightSearchRequest parseSearchRequestFromMap(Map<?, ?> map) {
        try {
            FlightSearchRequest.Builder builder = FlightSearchRequest.builder();
            
            // Required fields
            String origin = getStringValue(map, "origin");
            String destination = getStringValue(map, "destination"); 
            String departureDateStr = getStringValue(map, "departureDate");
            
            if (origin == null || destination == null || departureDateStr == null) {
                log("Missing required fields in map: origin, destination, or departureDate");
                return null;
            }
            
            builder.origin(origin.toUpperCase())
                   .destination(destination.toUpperCase())
                   .departureDate(LocalDate.parse(departureDateStr));
            
            // Optional fields
            String returnDateStr = getStringValue(map, "returnDate");
            if (returnDateStr != null) {
                builder.returnDate(LocalDate.parse(returnDateStr));
            }
            
            Integer adults = getIntegerValue(map, "adults");
            if (adults != null && adults > 0) {
                builder.adults(adults);
            }
            
            Integer children = getIntegerValue(map, "children");
            if (children != null && children >= 0) {
                builder.children(children);
            }
            
            String cabinClass = getStringValue(map, "cabinClass");
            if (cabinClass != null) {
                builder.cabinClass(cabinClass.toUpperCase());
            }
            
            Boolean nonStop = getBooleanValue(map, "nonStop");
            if (nonStop != null) {
                builder.nonStop(nonStop);
            }
            
            String currencyCode = getStringValue(map, "currencyCode");
            if (currencyCode != null) {
                builder.currencyCode(currencyCode.toUpperCase());
            }
            
            Integer maxResults = getIntegerValue(map, "maxResults");
            if (maxResults != null && maxResults > 0) {
                builder.maxResults(maxResults);
            }
            
            return builder.build();
            
        } catch (DateTimeParseException e) {
            log("Invalid date format in search request: " + e.getMessage());
            return null;
        } catch (Exception e) {
            log("Error parsing search request from map: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Parses flight search request from String payload.
     * Expected format: "ORIGIN,DESTINATION,YYYY-MM-DD[,YYYY-MM-DD][,adults][,children]"
     */
    private FlightSearchRequest parseSearchRequestFromString(String str) {
        try {
            String[] parts = str.trim().split(",");
            
            if (parts.length < 3) {
                log("Invalid string format. Expected: ORIGIN,DESTINATION,YYYY-MM-DD[,YYYY-MM-DD][,adults][,children]");
                return null;
            }
            
            FlightSearchRequest.Builder builder = FlightSearchRequest.builder()
                    .origin(parts[0].trim().toUpperCase())
                    .destination(parts[1].trim().toUpperCase())
                    .departureDate(LocalDate.parse(parts[2].trim()));
            
            // Optional return date
            if (parts.length > 3 && !parts[3].trim().isEmpty()) {
                builder.returnDate(LocalDate.parse(parts[3].trim()));
            }
            
            // Optional adults count
            if (parts.length > 4 && !parts[4].trim().isEmpty()) {
                builder.adults(Integer.parseInt(parts[4].trim()));
            }
            
            // Optional children count
            if (parts.length > 5 && !parts[5].trim().isEmpty()) {
                builder.children(Integer.parseInt(parts[5].trim()));
            }
            
            return builder.build();
            
        } catch (Exception e) {
            log("Error parsing search request from string: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Handles control events for the travel agent.
     */
    private void handleControlEvent(Event event, long requestId) {
        String topic = event.getTopic();
        Object payload = event.getPayload();
        
        if ("travel.control.status".equals(topic)) {
            publishStatusEvent(event.getSender());
        } else if ("travel.control.clear-cache".equals(topic)) {
            clearCache(event.getSender());
        } else if ("travel.control.list-requests".equals(topic)) {
            publishActiveRequests(event.getSender());
        } else {
            log("Unknown control event: " + topic);
        }
    }
    
    /**
     * Publishes flight search results to the event mesh.
     */
    private void publishFlightResults(FlightOffersSearchResult result, AgentID requestingAgent, String requestKey) {
        try {
            Event resultEvent = Event.builder()
                    .topic(FLIGHT_RESULTS_TOPIC)
                    .payload(result)
                    .sender(getAgentId())
                    .metadata("requestKey", requestKey)
                    .metadata("requestingAgent", requestingAgent.toString())
                    .metadata("offerCount", String.valueOf(result.getResultCount()))
                    .build();
            
            publishEvent(resultEvent);
            log("Published flight results for request " + requestKey + " to " + requestingAgent);
            
        } catch (Exception e) {
            log("Error publishing flight results: " + e.getMessage());
        }
    }
    
    /**
     * Publishes error events when flight search fails.
     */
    private void publishErrorEvent(String errorMessage, AgentID requestingAgent, Object requestId) {
        try {
            Map<String, Object> errorData = new ConcurrentHashMap<>();
            errorData.put("error", errorMessage);
            errorData.put("requestId", requestId.toString());
            errorData.put("timestamp", System.currentTimeMillis());
            errorData.put("agentId", getAgentId().toString());
            
            Event errorEvent = Event.builder()
                    .topic(FLIGHT_ERROR_TOPIC)
                    .payload(errorData)
                    .sender(getAgentId())
                    .metadata("requestingAgent", requestingAgent.toString())
                    .build();
            
            publishEvent(errorEvent);
            log("Published error event for request " + requestId + " to " + requestingAgent);
            
        } catch (Exception e) {
            log("Error publishing error event: " + e.getMessage());
        }
    }
    
    /**
     * Publishes status information about the travel agent.
     */
    private void publishStatusEvent(AgentID requestingAgent) {
        try {
            Map<String, Object> status = new ConcurrentHashMap<>();
            status.put("agentId", getAgentId().toString());
            status.put("agentType", getAgentType());
            status.put("lifecycleState", getLifecycleState().toString());
            status.put("totalRequests", requestCounter.get());
            status.put("activeRequests", activeRequests.size());
            status.put("cachedResults", cachedResults.size());
            status.put("apiCallCount", amadeusClient != null ? amadeusClient.getApiCallCount() : 0);
            status.put("hasValidToken", amadeusClient != null ? amadeusClient.hasValidToken() : false);
            status.put("timestamp", System.currentTimeMillis());
            
            Event statusEvent = Event.builder()
                    .topic("travel.agent.status")
                    .payload(status)
                    .sender(getAgentId())
                    .build();
            
            publishEvent(statusEvent);
            log("Published status event to " + requestingAgent);
            
        } catch (Exception e) {
            log("Error publishing status event: " + e.getMessage());
        }
    }
    
    /**
     * Clears cached results.
     */
    private void clearCache(AgentID requestingAgent) {
        int cleared = cachedResults.size();
        cachedResults.clear();
        log("Cleared " + cleared + " cached results at request of " + requestingAgent);
    }
    
    /**
     * Publishes list of active requests.
     */
    private void publishActiveRequests(AgentID requestingAgent) {
        try {
            Event requestsEvent = Event.builder()
                    .topic("travel.agent.active-requests")
                    .payload(new ConcurrentHashMap<>(activeRequests))
                    .sender(getAgentId())
                    .build();
            
            publishEvent(requestsEvent);
            log("Published active requests list to " + requestingAgent);
            
        } catch (Exception e) {
            log("Error publishing active requests: " + e.getMessage());
        }
    }
    
    /**
     * Logs agent statistics.
     */
    private void logStatistics() {
        log("=== TravelPlannerAgent Statistics ===");
        log("Total requests processed: " + requestCounter.get());
        log("Active requests: " + activeRequests.size());
        log("Cached results: " + cachedResults.size());
        if (amadeusClient != null) {
            log("API calls made: " + amadeusClient.getApiCallCount());
            log("API environment: " + (amadeusClient.isUsingProduction() ? "PRODUCTION" : "TEST"));
        }
        log("=== End Statistics ===");
    }
    
    /**
     * Validates a flight search request for business logic errors.
     */
    private String validateSearchRequest(FlightSearchRequest request) {
        if (request == null) {
            return "Search request is null";
        }
        
        // Validate airport codes (basic validation)
        if (!isValidAirportCode(request.getOrigin())) {
            return "Invalid origin airport code: " + request.getOrigin();
        }
        
        if (!isValidAirportCode(request.getDestination())) {
            return "Invalid destination airport code: " + request.getDestination();
        }
        
        // Check if origin and destination are the same
        if (request.getOrigin().equalsIgnoreCase(request.getDestination())) {
            return "Origin and destination cannot be the same";
        }
        
        // Validate departure date is not in the past
        if (request.getDepartureDate().isBefore(LocalDate.now())) {
            return "Departure date cannot be in the past";
        }
        
        // Validate departure date is not too far in the future (airlines typically limit to ~11 months)
        if (request.getDepartureDate().isAfter(LocalDate.now().plusMonths(11))) {
            return "Departure date is too far in the future (max 11 months)";
        }
        
        // Validate return date if provided
        if (request.getReturnDate() != null) {
            if (request.getReturnDate().isBefore(request.getDepartureDate())) {
                return "Return date cannot be before departure date";
            }
            if (request.getReturnDate().isAfter(LocalDate.now().plusMonths(11))) {
                return "Return date is too far in the future (max 11 months)";
            }
        }
        
        // Validate passenger counts
        if (request.getAdults() < 1 || request.getAdults() > 9) {
            return "Adults count must be between 1 and 9";
        }
        
        if (request.getChildren() < 0 || request.getChildren() > 8) {
            return "Children count must be between 0 and 8";
        }
        
        if (request.getAdults() + request.getChildren() > 9) {
            return "Total passengers (adults + children) cannot exceed 9";
        }
        
        return null; // No validation errors
    }
    
    /**
     * Basic airport code validation (3-letter IATA codes).
     */
    private boolean isValidAirportCode(String code) {
        return code != null && 
               code.length() == 3 && 
               code.matches("[A-Z]{3}") && 
               !code.equals("XXX"); // XXX is often used as placeholder
    }
    
    // Helper methods for map parsing
    
    private String getStringValue(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString().trim() : null;
    }
    
    private Integer getIntegerValue(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        
        try {
            if (value instanceof Integer) return (Integer) value;
            if (value instanceof Number) return ((Number) value).intValue();
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private Boolean getBooleanValue(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        
        if (value instanceof Boolean) return (Boolean) value;
        
        String str = value.toString().trim().toLowerCase();
        return "true".equals(str) || "1".equals(str) || "yes".equals(str);
    }
    
    /**
     * Logging method following the weather agent pattern.
     */
    private void log(String message) {
        String timestamp = DateTimeFormatter.ISO_LOCAL_TIME.format(java.time.LocalTime.now());
        System.out.println("[" + timestamp + "] [" + getAgentId() + "] " + message);
    }
    
    // Public API methods for agent management
    
    public long getRequestCount() {
        return requestCounter.get();
    }
    
    public int getActiveRequestsCount() {
        return activeRequests.size();
    }
    
    public int getCachedResultsCount() {
        return cachedResults.size();
    }
    
    public long getApiCallCount() {
        return amadeusClient != null ? amadeusClient.getApiCallCount() : 0;
    }
    
    public boolean hasValidApiToken() {
        return amadeusClient != null && amadeusClient.hasValidToken();
    }
}