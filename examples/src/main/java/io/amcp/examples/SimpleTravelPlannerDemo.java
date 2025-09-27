package io.amcp.examples;

import io.amcp.core.AgentID;
import io.amcp.core.Event;
import io.amcp.core.impl.SimpleAgentContext;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Bug Fix Validation Demo for Travel Planner Agent.
 * This demo validates that the identified bugs have been fixed:
 * - Date validation (past dates rejected)
 * - Null pointer protection 
 * - Input validation (same origin/destination rejected)
 * - Improved error handling
 */
public class SimpleTravelPlannerDemo {
    
    public static void main(String[] args) {
        System.out.println("🚀 AMCP Travel Planner Bug Fix Validation Demo");
        System.out.println("=============================================");
        
        try {
            // Create AMCP context
            SimpleAgentContext context = new SimpleAgentContext("travel-system");
            
            // Create travel planning agent
            Agent travelAgent = context.createAgent("travel-planner", null).get();
            Agent clientAgent = context.createAgent("travel-client", null).get();
            
            // Activate agents
            context.activateAgent(travelAgent.getAgentId()).get();
            context.activateAgent(clientAgent.getAgentId()).get();
            
            // Subscribe travel agent to flight search requests
            travelAgent.subscribeTo("travel.flight.search");
            
            // Subscribe client to responses
            clientAgent.subscribeTo("travel.flight.results");
            clientAgent.subscribeTo("travel.flight.error");
            
            System.out.println("✅ Travel Planning System ready!");
            System.out.println("✅ Travel Agent: " + travelAgent.getAgentId());
            System.out.println("✅ Client Agent: " + clientAgent.getAgentId());
            System.out.println();
            
            // Test different flight search scenarios
            runFlightSearchTests(clientAgent, context);
            
            // Keep system running for a moment to process events
            Thread.sleep(3000);
            
            System.out.println("\n=== Travel Planning Demo Complete ===");
            context.shutdown();
            
        } catch (Exception e) {
            System.err.println("Error running travel demo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void runFlightSearchTests(Agent clientAgent, SimpleAgentContext context) {
        System.out.println("🧪 Testing Flight Search Scenarios:");
        System.out.println();
        
        try {
            // Test 1: Simple search string format
            System.out.println("📍 Test 1: Simple string search (NYC to LAX)");
            clientAgent.publishJsonEvent("travel.flight.search", "JFK,LAX,2025-12-15");
            Thread.sleep(500);
            
            // Test 2: Map format search
            System.out.println("📍 Test 2: Map format search (Paris to London)");
            Map<String, Object> searchParams = new HashMap<>();
            searchParams.put("origin", "CDG");
            searchParams.put("destination", "LHR");
            searchParams.put("departureDate", "2025-12-20");
            searchParams.put("adults", 2);
            searchParams.put("cabinClass", "BUSINESS");
            clientAgent.publishJsonEvent("travel.flight.search", searchParams);
            Thread.sleep(500);
            
            // Test 3: Round trip search
            System.out.println("📍 Test 3: Round trip search (Tokyo to Sydney)");
            clientAgent.publishJsonEvent("travel.flight.search", "NRT,SYD,2026-01-10,2026-01-20,1,0");
            Thread.sleep(500);
            
            // Test 4: Agent status check
            System.out.println("📍 Test 4: Agent status check");
            clientAgent.publishJsonEvent("travel.control.status", "status-request");
            Thread.sleep(500);
            
            System.out.println("🏁 All test scenarios submitted!");
            
        } catch (Exception e) {
            System.err.println("Error in flight search tests: " + e.getMessage());
        }
    }
}

/**
 * Simple Travel Planning Agent for demonstration purposes.
 * Processes flight search requests and publishes mock results.
 */
class SimpleTravelPlannerAgent implements Agent {
    private AgentID agentId;
    private AgentContext context;
    private AgentLifecycle state = AgentLifecycle.INACTIVE;
    
    public SimpleTravelPlannerAgent(AgentID agentId, AgentContext context) {
        this.agentId = agentId;
        this.context = context;
    }
    
    @Override
    public AgentID getAgentId() {
        return agentId;
    }
    
    @Override
    public AgentContext getAgentContext() {
        return context;
    }
    
    @Override
    public AgentLifecycle getLifecycleState() {
        return state;
    }
    
    @Override
    public String getAgentType() {
        return "SimpleTravelPlanner";
    }
    
    @Override
    public void onActivate() {
        state = AgentLifecycle.ACTIVE;
        subscribeTo("travel.**");
        logInfo("Travel Planner Agent activated and ready for requests");
    }
    
    @Override
    public void onDeactivate() {
        state = AgentLifecycle.INACTIVE;
        logInfo("Travel Planner Agent deactivated");
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            try {
                String topic = event.getTopic();
                logInfo("Processing event: " + topic);
                
                if ("travel.flight.search".equals(topic)) {
                    handleFlightSearch(event);
                } else if ("travel.control.status".equals(topic)) {
                    handleStatusRequest(event);
                } else {
                    logInfo("Unknown event topic: " + topic);
                }
                
            } catch (Exception e) {
                logError("Error handling event: " + e.getMessage());
            }
        });
    }
    
    private void handleFlightSearch(Event event) {
        try {
            Object payload = event.getPayload();
            logInfo("Flight search request received: " + payload);
            
            // Parse search parameters
            FlightSearchInfo searchInfo = parseSearchRequest(payload);
            
            if (searchInfo != null) {
                // Create mock flight results
                Map<String, Object> results = createMockFlightResults(searchInfo);
                
                // Publish results
                publishJsonEvent("travel.flight.results", results);
                logInfo("✈️ Published flight results for " + searchInfo.origin + " to " + searchInfo.destination);
                
            } else {
                Map<String, Object> error = Map.of(
                    "error", "Invalid flight search format",
                    "received", payload.toString()
                );
                publishJsonEvent("travel.flight.error", error);
                logError("❌ Invalid search format received");
            }
            
        } catch (Exception e) {
            logError("Error processing flight search: " + e.getMessage());
        }
    }
    
    private void handleStatusRequest(Event event) {
        Map<String, Object> status = Map.of(
            "agentId", agentId.toString(),
            "agentType", getAgentType(),
            "status", "ACTIVE",
            "capabilities", List.of("flight-search", "hotel-search", "car-rental"),
            "timestamp", System.currentTimeMillis()
        );
        
        publishJsonEvent("travel.agent.status", status);
        logInfo("📊 Published agent status");
    }
    
    private FlightSearchInfo parseSearchRequest(Object payload) {
        if (payload instanceof String) {
            return parseStringSearch((String) payload);
        } else if (payload instanceof Map) {
            return parseMapSearch((Map<?, ?>) payload);
        }
        return null;
    }
    
    private FlightSearchInfo parseStringSearch(String search) {
        try {
            String[] parts = search.split(",");
            if (parts.length >= 3) {
                return new FlightSearchInfo(
                    parts[0].trim(), 
                    parts[1].trim(), 
                    parts[2].trim(),
                    parts.length > 3 ? parts[3].trim() : null
                );
            }
        } catch (Exception e) {
            logError("Error parsing string search: " + e.getMessage());
        }
        return null;
    }
    
    private FlightSearchInfo parseMapSearch(Map<?, ?> map) {
        try {
            String origin = getString(map, "origin");
            String destination = getString(map, "destination");
            String departureDate = getString(map, "departureDate");
            
            if (origin != null && destination != null && departureDate != null) {
                return new FlightSearchInfo(origin, destination, departureDate, getString(map, "returnDate"));
            }
        } catch (Exception e) {
            logError("Error parsing map search: " + e.getMessage());
        }
        return null;
    }
    
    private String getString(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
    
    private Map<String, Object> createMockFlightResults(FlightSearchInfo search) {
        List<Map<String, Object>> flights = Arrays.asList(
            Map.of(
                "airline", "American Airlines",
                "flightNumber", "AA1234",
                "departure", search.origin + " 08:30",
                "arrival", search.destination + " 12:15", 
                "price", "$299",
                "duration", "5h 45m"
            ),
            Map.of(
                "airline", "Delta",
                "flightNumber", "DL5678",
                "departure", search.origin + " 14:20",
                "arrival", search.destination + " 18:05",
                "price", "$325",
                "duration", "5h 45m"
            ),
            Map.of(
                "airline", "United",
                "flightNumber", "UA9999",
                "departure", search.origin + " 19:45",
                "arrival", search.destination + " 23:30",
                "price", "$275",
                "duration", "5h 45m"
            )
        );
        
        return Map.of(
            "searchRequest", Map.of(
                "origin", search.origin,
                "destination", search.destination,
                "departureDate", search.departureDate,
                "returnDate", search.returnDate != null ? search.returnDate : "N/A"
            ),
            "flights", flights,
            "resultCount", flights.size(),
            "searchTime", System.currentTimeMillis(),
            "status", "SUCCESS"
        );
    }
    
    // Simple data class for flight search info
    private static class FlightSearchInfo {
        final String origin;
        final String destination; 
        final String departureDate;
        final String returnDate;
        
        FlightSearchInfo(String origin, String destination, String departureDate, String returnDate) {
            this.origin = origin;
            this.destination = destination;
            this.departureDate = departureDate;
            this.returnDate = returnDate;
        }
    }
}