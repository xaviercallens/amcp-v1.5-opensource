package io.amcp.examples.travel;

import io.amcp.core.Agent;
import io.amcp.core.AgentID;
import io.amcp.core.AgentContext;
import io.amcp.core.AgentLifecycle;
import io.amcp.core.Event;
import io.amcp.core.DeliveryOptions;
import io.amcp.tools.ToolManager;
import io.amcp.tools.ToolResponse;
import io.amcp.mobility.MobilityState;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * AMCP v1.4 Travel Planner Agent.
 * Advanced example agent demonstrating comprehensive travel planning capabilities
 * with integration of weather data, search functionality, and agent mobility.
 */
public class TravelPlannerAgent implements Agent {
    
    private AgentID agentId;
    private AgentContext context;
    private AgentLifecycle state = AgentLifecycle.INACTIVE;
    
    private final Map<String, TravelPlan> activePlans = new ConcurrentHashMap<>();
    private final Map<String, TravelRequest> pendingRequests = new ConcurrentHashMap<>();
    private final AtomicLong planCounter = new AtomicLong(0);
    private final ReentrantReadWriteLock stateLock = new ReentrantReadWriteLock();
    
    private ScheduledExecutorService scheduler;
    private ToolManager toolManager;
    private volatile boolean initialized = false;
    
    public TravelPlannerAgent() {
        this.agentId = AgentID.named("travel-planner-" + System.currentTimeMillis());
    }
    
    public TravelPlannerAgent(AgentID agentId) {
        this.agentId = agentId;
    }
    
    @Override
    public AgentID getAgentId() {
        return agentId;
    }
    
    @Override
    public AgentContext getContext() {
        return context;
    }
    
    public void setContext(AgentContext context) {
        this.context = context;
    }
    
    @Override
    public AgentLifecycle getLifecycleState() {
        return state;
    }
    
    @Override
    public void onActivate() {
        state = AgentLifecycle.ACTIVE;
        
        // Initialize scheduler for periodic tasks
        scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "TravelPlanner-" + agentId.toString());
            t.setDaemon(true);
            return t;
        });
        
        // Initialize tool manager
        toolManager = ToolManager.getInstance();
        
        // Subscribe to travel-related events
        if (context != null) {
            context.subscribe(agentId, "travel.request.*");
            context.subscribe(agentId, "travel.update.*");
            context.subscribe(agentId, "weather.alert.*");
            context.subscribe(agentId, "system.mobility.*");
        }
        
        // Start periodic weather monitoring for active plans
        scheduler.scheduleWithFixedDelay(this::monitorWeatherAlerts, 5, 15, TimeUnit.MINUTES);
        
        // Start periodic plan optimization
        scheduler.scheduleWithFixedDelay(this::optimizeTravelPlans, 1, 30, TimeUnit.MINUTES);
        
        this.initialized = true;
        
        logMessage("Travel Planner Agent activated: " + agentId);
    }
    
    @Override
    public void onDeactivate() {
        state = AgentLifecycle.INACTIVE;
        this.initialized = false;
        
        // Shutdown scheduler
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
        
        logMessage("Travel Planner Agent deactivated: " + agentId);
    }
    
    @Override
    public void onDestroy() {
        // No-op for demo
    }
    
    @Override
    public void onBeforeMigration(String destinationContext) {
        // No-op for demo
    }
    
    @Override
    public void onAfterMigration(String sourceContext) {
        // No-op for demo
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        if (!initialized || state != AgentLifecycle.ACTIVE) {
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.runAsync(() -> {
            try {
                String topic = event.getTopic();
                
                if (topic.startsWith("travel.request.")) {
                    handleTravelRequest(event);
                } else if (topic.startsWith("travel.update.")) {
                    handleTravelUpdate(event);
                } else if (topic.startsWith("weather.alert.")) {
                    handleWeatherAlert(event);
                } else if (topic.startsWith("system.mobility.")) {
                    handleMobilityEvent(event);
                }
                
            } catch (Exception e) {
                logMessage("Error handling event: " + e.getMessage());
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> publishEvent(Event event) {
        if (context != null) {
            return context.publishEvent(event);
        }
        return CompletableFuture.completedFuture(null);
    }
    
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
    
    public MobilityState captureMobilityState() {
        stateLock.readLock().lock();
        try {
            Map<String, Object> state = new HashMap<>();
            state.put("agentId", agentId.toString());
            state.put("activePlansCount", activePlans.size());
            state.put("pendingRequestsCount", pendingRequests.size());
            state.put("planCounter", planCounter.get());
            state.put("initialized", initialized);
            
            // Capture serializable plan data
            Map<String, Map<String, Object>> planData = new HashMap<>();
            for (Map.Entry<String, TravelPlan> entry : activePlans.entrySet()) {
                planData.put(entry.getKey(), entry.getValue().toSerializableMap());
            }
            state.put("activePlans", planData);
            
            return MobilityState.STATIONARY;
        } finally {
            stateLock.readLock().unlock();
        }
    }
    
    public CompletableFuture<Void> restoreMobilityState(MobilityState mobilityState) {
        return CompletableFuture.runAsync(() -> {
            stateLock.writeLock().lock();
            try {
                // For this example, just log the state restoration
                // In a real implementation, this would deserialize and restore agent state
                logMessage("Mobility state restored: " + mobilityState);
                
            } finally {
                stateLock.writeLock().unlock();
            }
        });
    }
    
    private void handleTravelRequest(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> requestData = (Map<String, Object>) event.getPayload();
            
            String requestId = (String) requestData.get("requestId");
            String origin = (String) requestData.get("origin");
            String destination = (String) requestData.get("destination");
            String startDate = (String) requestData.get("startDate");
            String endDate = (String) requestData.get("endDate");
            String userId = (String) requestData.get("userId");
            
            if (requestId == null || origin == null || destination == null) {
                logMessage("Invalid travel request - missing required fields");
                return;
            }
            
            TravelRequest request = new TravelRequest(
                requestId, userId, origin, destination, startDate, endDate, requestData
            );
            
            pendingRequests.put(requestId, request);
            
            // Process travel request asynchronously
            processTravelRequestAsync(request);
            
        } catch (Exception e) {
            logMessage("Error processing travel request: " + e.getMessage());
        }
    }
    
    private void processTravelRequestAsync(TravelRequest request) {
        CompletableFuture.runAsync(() -> {
            try {
                logMessage("Processing travel request: " + request.getRequestId());
                
                // Step 1: Get weather information for both origin and destination
                CompletableFuture<ToolResponse> originWeatherFuture = 
                    toolManager.getCurrentWeather(request.getOrigin());
                CompletableFuture<ToolResponse> destWeatherFuture = 
                    toolManager.getCurrentWeather(request.getDestination());
                
                // Step 2: Get weather forecast for destination
                CompletableFuture<ToolResponse> forecastFuture = 
                    toolManager.getWeatherForecast(request.getDestination(), 5);
                
                // Step 3: Search for destination information
                CompletableFuture<ToolResponse> searchFuture = 
                    toolManager.searchWeb("travel guide " + request.getDestination());
                
                // Wait for all external data
                CompletableFuture.allOf(originWeatherFuture, destWeatherFuture, 
                                      forecastFuture, searchFuture).get();
                
                // Collect results
                WeatherData originWeather = extractWeatherData(originWeatherFuture.get());
                WeatherData destWeather = extractWeatherData(destWeatherFuture.get());
                WeatherData forecast = extractWeatherData(forecastFuture.get());
                SearchResults searchResults = extractSearchResults(searchFuture.get());
                
                // Generate travel plan
                TravelPlan travelPlan = generateTravelPlan(
                    request, originWeather, destWeather, forecast, searchResults
                );
                
                // Store the plan
                activePlans.put(travelPlan.getPlanId(), travelPlan);
                pendingRequests.remove(request.getRequestId());
                
                // Publish travel plan created event
                publishTravelPlanEvent(travelPlan, "created");
                
                logMessage("Travel plan created: " + travelPlan.getPlanId());
                
            } catch (Exception e) {
                logMessage("Failed to process travel request " + request.getRequestId() + ": " + e.getMessage());
                
                // Publish error event
                publishTravelErrorEvent(request.getRequestId(), e.getMessage());
            }
        });
    }
    
    private TravelPlan generateTravelPlan(TravelRequest request, WeatherData originWeather,
                                        WeatherData destWeather, WeatherData forecast,
                                        SearchResults searchResults) {
        String planId = "plan_" + planCounter.incrementAndGet() + "_" + System.currentTimeMillis();
        
        TravelPlan plan = new TravelPlan(
            planId,
            request.getRequestId(),
            request.getUserId(),
            request.getOrigin(),
            request.getDestination(),
            request.getStartDate(),
            request.getEndDate()
        );
        
        // Add weather insights
        if (destWeather != null) {
            plan.addRecommendation("Current weather at " + request.getDestination() + 
                                 ": " + destWeather.getDescription() + 
                                 ", " + destWeather.getTemperature() + "°C");
        }
        
        if (forecast != null) {
            plan.addRecommendation("Weather forecast suggests: " + 
                                 generateWeatherAdvice(forecast));
        }
        
        // Add travel recommendations based on search results
        if (searchResults != null && searchResults.hasResults()) {
            plan.addRecommendation("Travel tips: " + searchResults.getSummary());
        }
        
        // Generate packing recommendations based on weather
        List<String> packingList = generatePackingRecommendations(destWeather, forecast);
        plan.setPackingRecommendations(packingList);
        
        // Add weather alerts if any severe conditions
        if (forecast != null && forecast.hasSevereWeather()) {
            plan.addAlert("Weather Alert: Severe weather conditions expected at destination. " +
                         "Consider adjusting travel dates.");
        }
        
        plan.setStatus("ACTIVE");
        plan.setCreatedTime(System.currentTimeMillis());
        
        return plan;
    }
    
    private void handleTravelUpdate(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> updateData = (Map<String, Object>) event.getPayload();
            
            String planId = (String) updateData.get("planId");
            String updateType = (String) updateData.get("updateType");
            
            TravelPlan plan = activePlans.get(planId);
            if (plan != null) {
                handlePlanUpdate(plan, updateType, updateData);
            }
            
        } catch (Exception e) {
            logMessage("Error handling travel update: " + e.getMessage());
        }
    }
    
    private void handlePlanUpdate(TravelPlan plan, String updateType, Map<String, Object> updateData) {
        switch (updateType) {
            case "modify_dates":
                String newStartDate = (String) updateData.get("newStartDate");
                String newEndDate = (String) updateData.get("newEndDate");
                if (newStartDate != null) plan.setStartDate(newStartDate);
                if (newEndDate != null) plan.setEndDate(newEndDate);
                
                // Re-evaluate weather for new dates
                refreshWeatherData(plan);
                break;
                
            case "add_note":
                String note = (String) updateData.get("note");
                if (note != null) {
                    plan.addNote(note);
                }
                break;
                
            case "cancel":
                plan.setStatus("CANCELLED");
                publishTravelPlanEvent(plan, "cancelled");
                break;
        }
        
        plan.setLastUpdated(System.currentTimeMillis());
        publishTravelPlanEvent(plan, "updated");
    }
    
    private void handleWeatherAlert(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> alertData = (Map<String, Object>) event.getPayload();
            
            String location = (String) alertData.get("location");
            String severity = (String) alertData.get("severity");
            String description = (String) alertData.get("description");
            
            // Check if any active plans are affected
            for (TravelPlan plan : activePlans.values()) {
                if ("ACTIVE".equals(plan.getStatus()) && 
                    (location.equals(plan.getDestination()) || location.equals(plan.getOrigin()))) {
                    
                    plan.addAlert("Weather Alert for " + location + ": " + description + 
                                 " (Severity: " + severity + ")");
                    
                    publishTravelPlanEvent(plan, "weather_alert");
                }
            }
            
        } catch (Exception e) {
            logMessage("Error handling weather alert: " + e.getMessage());
        }
    }
    
    private void handleMobilityEvent(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> mobilityData = (Map<String, Object>) event.getPayload();
            
            String eventType = (String) mobilityData.get("eventType");
            
            if ("migration_requested".equals(eventType)) {
                // Prepare for potential migration
                logMessage("Preparing for potential agent migration");
                
                // Save current state to ensure no data loss
                captureAndPersistState();
                
            } else if ("migration_completed".equals(eventType)) {
                // Post-migration activities
                logMessage("Agent migration completed, resuming operations");
                
                // Re-establish tool connections if needed
                if (!toolManager.isInitialized()) {
                    toolManager.initialize();
                }
            }
            
        } catch (Exception e) {
            logMessage("Error handling mobility event: " + e.getMessage());
        }
    }
    
    private void monitorWeatherAlerts() {
        if (!initialized || activePlans.isEmpty()) {
            return;
        }
        
        try {
            // Check weather for all active plan destinations
            for (TravelPlan plan : activePlans.values()) {
                if (!"ACTIVE".equals(plan.getStatus())) {
                    continue;
                }
                
                CompletableFuture.runAsync(() -> {
                    try {
                        ToolResponse weatherResponse = toolManager.getCurrentWeather(plan.getDestination()).get();
                        
                        if (weatherResponse.isSuccess()) {
                            WeatherData currentWeather = extractWeatherData(weatherResponse);
                            
                            if (currentWeather != null && currentWeather.hasSevereWeather()) {
                                plan.addAlert("Current Weather Alert: Severe conditions detected at " +
                                             plan.getDestination() + " - " + currentWeather.getDescription());
                                
                                publishTravelPlanEvent(plan, "weather_update");
                            }
                        }
                        
                    } catch (Exception e) {
                        logMessage("Failed to check weather for plan " + plan.getPlanId() + ": " + e.getMessage());
                    }
                });
            }
            
        } catch (Exception e) {
            logMessage("Error in weather monitoring: " + e.getMessage());
        }
    }
    
    private void optimizeTravelPlans() {
        if (!initialized || activePlans.isEmpty()) {
            return;
        }
        
        try {
            // Optimize active travel plans
            for (TravelPlan plan : activePlans.values()) {
                if ("ACTIVE".equals(plan.getStatus())) {
                    optimizeSinglePlan(plan);
                }
            }
            
        } catch (Exception e) {
            logMessage("Error in plan optimization: " + e.getMessage());
        }
    }
    
    private void optimizeSinglePlan(TravelPlan plan) {
        CompletableFuture.runAsync(() -> {
            try {
                // Get updated weather forecast
                ToolResponse forecastResponse = toolManager.getWeatherForecast(plan.getDestination(), 7).get();
                
                if (forecastResponse.isSuccess()) {
                    WeatherData forecast = extractWeatherData(forecastResponse);
                    
                    if (forecast != null) {
                        // Update packing recommendations based on new forecast
                        List<String> updatedPacking = generatePackingRecommendations(null, forecast);
                        plan.setPackingRecommendations(updatedPacking);
                        
                        // Check for optimal travel days
                        String advice = generateOptimalTravelAdvice(forecast);
                        if (advice != null) {
                            plan.addRecommendation("Optimization update: " + advice);
                        }
                        
                        publishTravelPlanEvent(plan, "optimized");
                    }
                }
                
            } catch (Exception e) {
                logMessage("Failed to optimize plan " + plan.getPlanId() + ": " + e.getMessage());
            }
        });
    }
    
    private void refreshWeatherData(TravelPlan plan) {
        CompletableFuture.runAsync(() -> {
            try {
                CompletableFuture<ToolResponse> currentFuture = 
                    toolManager.getCurrentWeather(plan.getDestination());
                CompletableFuture<ToolResponse> forecastFuture = 
                    toolManager.getWeatherForecast(plan.getDestination(), 5);
                
                CompletableFuture.allOf(currentFuture, forecastFuture).get();
                
                WeatherData current = extractWeatherData(currentFuture.get());
                WeatherData forecast = extractWeatherData(forecastFuture.get());
                
                plan.clearRecommendations();
                
                if (current != null) {
                    plan.addRecommendation("Updated weather: " + current.getDescription() + 
                                         ", " + current.getTemperature() + "°C");
                }
                
                if (forecast != null) {
                    plan.addRecommendation("Updated forecast: " + generateWeatherAdvice(forecast));
                    
                    List<String> updatedPacking = generatePackingRecommendations(current, forecast);
                    plan.setPackingRecommendations(updatedPacking);
                }
                
                publishTravelPlanEvent(plan, "weather_updated");
                
            } catch (Exception e) {
                logMessage("Failed to refresh weather data for plan " + plan.getPlanId() + ": " + e.getMessage());
            }
        });
    }
    
    private WeatherData extractWeatherData(ToolResponse response) {
        if (!response.isSuccess()) {
            return null;
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.getData();
            
            if (data != null) {
                return new WeatherData(data);
            }
        } catch (Exception e) {
            // Ignore extraction errors
        }
        
        return null;
    }
    
    private SearchResults extractSearchResults(ToolResponse response) {
        if (!response.isSuccess()) {
            return null;
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.getData();
            
            if (data != null) {
                return new SearchResults(data);
            }
        } catch (Exception e) {
            // Ignore extraction errors
        }
        
        return null;
    }
    
    private String generateWeatherAdvice(WeatherData weather) {
        if (weather == null) return "No weather data available";
        
        StringBuilder advice = new StringBuilder();
        
        double temp = weather.getTemperature();
        if (temp < 0) {
            advice.append("Very cold temperatures expected - pack warm clothing. ");
        } else if (temp < 10) {
            advice.append("Cold weather - bring layers and warm jacket. ");
        } else if (temp > 30) {
            advice.append("Hot weather - pack light, breathable clothing and sun protection. ");
        } else {
            advice.append("Moderate temperatures - comfortable weather conditions. ");
        }
        
        if (weather.getHumidity() > 80) {
            advice.append("High humidity expected. ");
        }
        
        if (weather.hasSevereWeather()) {
            advice.append("Severe weather conditions possible - monitor updates closely. ");
        }
        
        return advice.toString().trim();
    }
    
    private String generateOptimalTravelAdvice(WeatherData forecast) {
        if (forecast == null) return null;
        
        // Simple optimization logic
        if (forecast.hasSevereWeather()) {
            return "Consider postponing travel due to severe weather conditions in forecast.";
        }
        
        double temp = forecast.getTemperature();
        if (temp > 15 && temp < 25 && forecast.getHumidity() < 70) {
            return "Excellent travel conditions expected - ideal time to visit.";
        }
        
        return null;
    }
    
    private List<String> generatePackingRecommendations(WeatherData current, WeatherData forecast) {
        List<String> recommendations = new ArrayList<>();
        
        // Base recommendations
        recommendations.add("Travel documents and identification");
        recommendations.add("Comfortable walking shoes");
        recommendations.add("Phone charger and adapters");
        
        // Weather-based recommendations
        WeatherData weather = forecast != null ? forecast : current;
        if (weather != null) {
            double temp = weather.getTemperature();
            
            if (temp < 5) {
                recommendations.add("Heavy winter coat");
                recommendations.add("Warm hat and gloves");
                recommendations.add("Thermal undergarments");
            } else if (temp < 15) {
                recommendations.add("Warm jacket or sweater");
                recommendations.add("Long pants");
                recommendations.add("Light gloves");
            } else if (temp > 25) {
                recommendations.add("Light, breathable clothing");
                recommendations.add("Sunscreen (SPF 30+)");
                recommendations.add("Hat for sun protection");
                recommendations.add("Swimwear (if applicable)");
            }
            
            if (weather.getHumidity() > 70) {
                recommendations.add("Quick-dry clothing");
                recommendations.add("Umbrella or rain jacket");
            }
            
            if (weather.hasSevereWeather()) {
                recommendations.add("Emergency supplies");
                recommendations.add("Waterproof bags");
            }
        }
        
        return recommendations;
    }
    
    private void publishTravelPlanEvent(TravelPlan plan, String eventType) {
        if (context == null) return;
        
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("planId", plan.getPlanId());
        eventData.put("userId", plan.getUserId());
        eventData.put("origin", plan.getOrigin());
        eventData.put("destination", plan.getDestination());
        eventData.put("status", plan.getStatus());
        eventData.put("eventType", eventType);
        eventData.put("timestamp", System.currentTimeMillis());
        
        Event event = Event.builder()
            .topic("travel.plan." + eventType)
            .payload(eventData)
            .sender(agentId)
            .deliveryOptions(DeliveryOptions.reliable())
            .build();
        
        context.publishEvent(event);
    }
    
    private void publishTravelErrorEvent(String requestId, String error) {
        if (context == null) return;
        
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("requestId", requestId);
        errorData.put("error", error);
        errorData.put("timestamp", System.currentTimeMillis());
        
        Event event = Event.builder()
            .topic("travel.error")
            .payload(errorData)
            .sender(agentId)
            .deliveryOptions(DeliveryOptions.reliable())
            .build();
        
        context.publishEvent(event);
    }
    
    private void captureAndPersistState() {
        try {
            MobilityState state = captureMobilityState();
            // In a real implementation, you would persist this to durable storage
            logMessage("Agent state captured for migration safety");
        } catch (Exception e) {
            logMessage("Failed to capture agent state: " + e.getMessage());
        }
    }
    
    private void logMessage(String message) {
        String timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
        );
        System.out.println("[" + timestamp + "] [" + agentId + "] " + message);
    }
    
    // Getter methods for monitoring
    public int getActivePlansCount() {
        return activePlans.size();
    }
    
    public int getPendingRequestsCount() {
        return pendingRequests.size();
    }
    
    public TravelPlan getTravelPlan(String planId) {
        return activePlans.get(planId);
    }
    
    public List<TravelPlan> getActivePlans() {
        return new ArrayList<>(activePlans.values());
    }
    
    // Inner classes
    
    private static class TravelRequest {
        private final String requestId;
        private final String userId;
        private final String origin;
        private final String destination;
        private final String startDate;
        private final String endDate;
        private final Map<String, Object> additionalData;
        
        public TravelRequest(String requestId, String userId, String origin, String destination,
                           String startDate, String endDate, Map<String, Object> additionalData) {
            this.requestId = requestId;
            this.userId = userId;
            this.origin = origin;
            this.destination = destination;
            this.startDate = startDate;
            this.endDate = endDate;
            this.additionalData = additionalData != null ? new HashMap<>(additionalData) : new HashMap<>();
        }
        
        public String getRequestId() { return requestId; }
        public String getUserId() { return userId; }
        public String getOrigin() { return origin; }
        public String getDestination() { return destination; }
        public String getStartDate() { return startDate; }
        public String getEndDate() { return endDate; }
        public Map<String, Object> getAdditionalData() { return new HashMap<>(additionalData); }
    }
    
    public static class TravelPlan {
        private final String planId;
        private final String requestId;
        private final String userId;
        private final String origin;
        private String destination;
        private String startDate;
        private String endDate;
        private String status;
        private long createdTime;
        private long lastUpdated;
        
        private final List<String> recommendations = new ArrayList<>();
        private final List<String> alerts = new ArrayList<>();
        private final List<String> notes = new ArrayList<>();
        private List<String> packingRecommendations = new ArrayList<>();
        
        public TravelPlan(String planId, String requestId, String userId, String origin,
                         String destination, String startDate, String endDate) {
            this.planId = planId;
            this.requestId = requestId;
            this.userId = userId;
            this.origin = origin;
            this.destination = destination;
            this.startDate = startDate;
            this.endDate = endDate;
            this.status = "DRAFT";
            this.createdTime = System.currentTimeMillis();
            this.lastUpdated = System.currentTimeMillis();
        }
        
        public void addRecommendation(String recommendation) {
            synchronized (recommendations) {
                recommendations.add(recommendation);
            }
        }
        
        public void addAlert(String alert) {
            synchronized (alerts) {
                alerts.add(alert);
            }
        }
        
        public void addNote(String note) {
            synchronized (notes) {
                notes.add(note);
            }
        }
        
        public void clearRecommendations() {
            synchronized (recommendations) {
                recommendations.clear();
            }
        }
        
        public Map<String, Object> toSerializableMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("planId", planId);
            map.put("requestId", requestId);
            map.put("userId", userId);
            map.put("origin", origin);
            map.put("destination", destination);
            map.put("startDate", startDate);
            map.put("endDate", endDate);
            map.put("status", status);
            map.put("createdTime", createdTime);
            map.put("lastUpdated", lastUpdated);
            map.put("recommendations", new ArrayList<>(recommendations));
            map.put("alerts", new ArrayList<>(alerts));
            map.put("notes", new ArrayList<>(notes));
            map.put("packingRecommendations", new ArrayList<>(packingRecommendations));
            return map;
        }
        
        @SuppressWarnings("unchecked")
        public static TravelPlan fromSerializableMap(Map<String, Object> map) {
            TravelPlan plan = new TravelPlan(
                (String) map.get("planId"),
                (String) map.get("requestId"),
                (String) map.get("userId"),
                (String) map.get("origin"),
                (String) map.get("destination"),
                (String) map.get("startDate"),
                (String) map.get("endDate")
            );
            
            plan.status = (String) map.get("status");
            plan.createdTime = (Long) map.get("createdTime");
            plan.lastUpdated = (Long) map.get("lastUpdated");
            
            List<String> recs = (List<String>) map.get("recommendations");
            if (recs != null) plan.recommendations.addAll(recs);
            
            List<String> alertsList = (List<String>) map.get("alerts");
            if (alertsList != null) plan.alerts.addAll(alertsList);
            
            List<String> notesList = (List<String>) map.get("notes");
            if (notesList != null) plan.notes.addAll(notesList);
            
            List<String> packing = (List<String>) map.get("packingRecommendations");
            if (packing != null) plan.packingRecommendations = new ArrayList<>(packing);
            
            return plan;
        }
        
        // Getters and setters
        public String getPlanId() { return planId; }
        public String getRequestId() { return requestId; }
        public String getUserId() { return userId; }
        public String getOrigin() { return origin; }
        public String getDestination() { return destination; }
        public String getStartDate() { return startDate; }
        public String getEndDate() { return endDate; }
        public String getStatus() { return status; }
        public long getCreatedTime() { return createdTime; }
        public long getLastUpdated() { return lastUpdated; }
        
        public void setDestination(String destination) { this.destination = destination; }
        public void setStartDate(String startDate) { this.startDate = startDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }
        public void setStatus(String status) { this.status = status; }
        public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
        public void setCreatedTime(long createdTime) { this.createdTime = createdTime; }
        
        public List<String> getRecommendations() { return new ArrayList<>(recommendations); }
        public List<String> getAlerts() { return new ArrayList<>(alerts); }
        public List<String> getNotes() { return new ArrayList<>(notes); }
        public List<String> getPackingRecommendations() { return new ArrayList<>(packingRecommendations); }
        public void setPackingRecommendations(List<String> recommendations) { 
            this.packingRecommendations = recommendations != null ? new ArrayList<>(recommendations) : new ArrayList<>(); 
        }
    }
    
    private static class WeatherData {
        private final Map<String, Object> data;
        
        public WeatherData(Map<String, Object> data) {
            this.data = new HashMap<>(data);
        }
        
        public String getDescription() {
            Object desc = data.get("description");
            return desc != null ? desc.toString() : "Unknown";
        }
        
        public double getTemperature() {
            Object temp = data.get("temperature");
            if (temp instanceof Number) {
                return ((Number) temp).doubleValue();
            }
            return 20.0; // Default temperature
        }
        
        public double getHumidity() {
            Object humidity = data.get("humidity");
            if (humidity instanceof Number) {
                return ((Number) humidity).doubleValue();
            }
            return 50.0; // Default humidity
        }
        
        public boolean hasSevereWeather() {
            String desc = getDescription().toLowerCase();
            return desc.contains("storm") || desc.contains("severe") || 
                   desc.contains("tornado") || desc.contains("hurricane") ||
                   desc.contains("blizzard") || desc.contains("flood");
        }
    }
    
    private static class SearchResults {
        private final Map<String, Object> data;
        
        public SearchResults(Map<String, Object> data) {
            this.data = new HashMap<>(data);
        }
        
        public boolean hasResults() {
            Object query = data.get("query");
            return query != null && !query.toString().trim().isEmpty();
        }
        
        public String getSummary() {
            Object summary = data.get("abstract");
            if (summary != null) {
                return summary.toString();
            }
            
            Object answer = data.get("answer");
            if (answer != null) {
                return answer.toString();
            }
            
            return "Search completed - check detailed results for travel information";
        }
    }
}