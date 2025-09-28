package io.amcp.connectors.ai;

import java.util.*;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Intelligent Prompt Management System for AMCP v1.5 ChatAgent
 * Routes user requests to appropriate agents based on intent analysis
 * and capability matching.
 */
public class PromptManager {
    
    private final AgentRegistry agentRegistry;
    private final Map<String, IntentPattern> intentPatterns;
    private final Map<String, String> agentMappings;
    
    public PromptManager(AgentRegistry agentRegistry) {
        this.agentRegistry = agentRegistry;
        this.intentPatterns = new HashMap<>();
        this.agentMappings = new HashMap<>();
        initializeIntentPatterns();
    }
    
    /**
     * Intent pattern for matching user requests
     */
    public static class IntentPattern {
        private final String intent;
        private final List<Pattern> patterns;
        private final String targetAgent;
        private final Set<String> keywords;
        private final int priority;
        
        public IntentPattern(String intent, List<String> patterns, String targetAgent, 
                           Set<String> keywords, int priority) {
            this.intent = intent;
            this.patterns = patterns.stream()
                    .map(p -> Pattern.compile(p, Pattern.CASE_INSENSITIVE))
                    .toList();
            this.targetAgent = targetAgent;
            this.keywords = keywords;
            this.priority = priority;
        }
        
        public boolean matches(String input) {
            return patterns.stream().anyMatch(p -> p.matcher(input).find()) ||
                   keywords.stream().anyMatch(k -> input.toLowerCase().contains(k.toLowerCase()));
        }
        
        // Getters
        public String getIntent() { return intent; }
        public String getTargetAgent() { return targetAgent; }
        public int getPriority() { return priority; }
    }
    
    /**
     * Request routing result
     */
    public static class RoutingResult {
        private final String intent;
        private final String targetAgent;
        private final String processedPrompt;
        private final double confidence;
        private final Map<String, Object> parameters;
        
        public RoutingResult(String intent, String targetAgent, String processedPrompt, 
                           double confidence, Map<String, Object> parameters) {
            this.intent = intent;
            this.targetAgent = targetAgent;
            this.processedPrompt = processedPrompt;
            this.confidence = confidence;
            this.parameters = parameters;
        }
        
        // Getters
        public String getIntent() { return intent; }
        public String getTargetAgent() { return targetAgent; }
        public String getProcessedPrompt() { return processedPrompt; }
        public double getConfidence() { return confidence; }
        public Map<String, Object> getParameters() { return parameters; }
    }
    
    private void initializeIntentPatterns() {
        // Weather Intent Patterns
        intentPatterns.put("weather", new IntentPattern(
            "weather",
            Arrays.asList(
                ".*weather.*in.*",
                ".*temperature.*",
                ".*forecast.*",
                ".*rain.*",
                ".*sunny.*",
                ".*climate.*"
            ),
            "WeatherAgent",
            Set.of("weather", "temperature", "forecast", "rain", "sun", "climate", "humidity", "wind"),
            100
        ));
        
        // Stock Intent Patterns  
        intentPatterns.put("stock", new IntentPattern(
            "stock",
            Arrays.asList(
                ".*stock price.*",
                ".*share price.*",
                ".*market.*",
                ".*ticker.*",
                ".*financial.*",
                ".*investment.*"
            ),
            "StockPriceAgent", 
            Set.of("stock", "share", "price", "market", "ticker", "financial", "investment", "trading"),
            100
        ));
        
        // Travel Intent Patterns
        intentPatterns.put("travel", new IntentPattern(
            "travel",
            Arrays.asList(
                ".*plan.*trip.*",
                ".*travel.*",
                ".*vacation.*",
                ".*hotel.*",
                ".*flight.*",
                ".*itinerary.*",
                ".*organize.*trip.*"
            ),
            "TravelPlannerAgent",
            Set.of("travel", "trip", "vacation", "hotel", "flight", "itinerary", "destination", "organize"),
            100
        ));
        
        // General Chat Intent (lowest priority)
        intentPatterns.put("chat", new IntentPattern(
            "chat",
            Arrays.asList(".*hello.*", ".*hi.*", ".*help.*", ".*thanks.*"),
            "ChatAgent",
            Set.of("hello", "hi", "help", "thanks", "how are you"),
            10
        ));
        
        // Agent mappings
        agentMappings.put("weather", "WeatherAgent");
        agentMappings.put("stock", "StockPriceAgent");
        agentMappings.put("travel", "TravelPlannerAgent");
        agentMappings.put("chat", "ChatAgent");
        
        logMessage("Initialized " + intentPatterns.size() + " intent patterns");
    }
    
    /**
     * Analyzes user input and determines the best agent to handle the request
     */
    public CompletableFuture<RoutingResult> routeRequest(String userInput) {
        return CompletableFuture.supplyAsync(() -> {
            if (userInput == null || userInput.trim().isEmpty()) {
                return createChatResult(userInput, 0.5);
            }
            
            String normalizedInput = userInput.trim().toLowerCase();
            logMessage("Routing request: \"" + userInput + "\"");
            
            // Find best matching intent
            IntentPattern bestMatch = null;
            double bestScore = 0.0;
            
            for (IntentPattern pattern : intentPatterns.values()) {
                if (pattern.matches(normalizedInput)) {
                    double score = calculateMatchScore(normalizedInput, pattern);
                    if (score > bestScore) {
                        bestScore = score;
                        bestMatch = pattern;
                    }
                }
            }
            
            if (bestMatch != null && bestScore > 0.3) {
                Map<String, Object> parameters = extractParameters(normalizedInput, bestMatch);
                String processedPrompt = preprocessPrompt(userInput, bestMatch);
                
                logMessage("Matched intent: " + bestMatch.getIntent() + 
                          " -> " + bestMatch.getTargetAgent() + 
                          " (confidence: " + String.format("%.2f", bestScore) + ")");
                
                return new RoutingResult(
                    bestMatch.getIntent(),
                    bestMatch.getTargetAgent(),
                    processedPrompt,
                    bestScore,
                    parameters
                );
            }
            
            // Default to chat agent
            logMessage("No specific intent detected, routing to ChatAgent");
            return createChatResult(userInput, 0.3);
        });
    }
    
    private double calculateMatchScore(String input, IntentPattern pattern) {
        double score = 0.0;
        int matches = 0;
        
        // Pattern matching score
        for (Pattern p : pattern.patterns) {
            if (p.matcher(input).find()) {
                score += 0.4;
                matches++;
            }
        }
        
        // Keyword matching score
        for (String keyword : pattern.keywords) {
            if (input.contains(keyword.toLowerCase())) {
                score += 0.3;
                matches++;
            }
        }
        
        // Priority bonus
        score += (pattern.getPriority() / 1000.0);
        
        // Multiple matches bonus
        if (matches > 1) {
            score *= 1.2;
        }
        
        return Math.min(score, 1.0);
    }
    
    private Map<String, Object> extractParameters(String input, IntentPattern pattern) {
        Map<String, Object> parameters = new HashMap<>();
        
        switch (pattern.getIntent()) {
            case "weather":
                extractWeatherParameters(input, parameters);
                break;
            case "stock":
                extractStockParameters(input, parameters);
                break;
            case "travel":
                extractTravelParameters(input, parameters);
                break;
            default:
                parameters.put("originalInput", input);
        }
        
        return parameters;
    }
    
    private void extractWeatherParameters(String input, Map<String, Object> parameters) {
        // Extract location from weather requests
        String[] locationKeywords = {"in ", "at ", "for ", "of "};
        for (String keyword : locationKeywords) {
            int index = input.toLowerCase().indexOf(keyword);
            if (index != -1) {
                String location = input.substring(index + keyword.length()).trim();
                // Clean up location (remove punctuation, extra words)
                location = location.replaceAll("[.,!?].*", "").trim();
                if (!location.isEmpty()) {
                    parameters.put("location", location);
                    break;
                }
            }
        }
        
        // Extract time frame
        if (input.contains("tomorrow")) {
            parameters.put("timeframe", "tomorrow");
        } else if (input.contains("today")) {
            parameters.put("timeframe", "today");
        } else if (input.contains("forecast")) {
            parameters.put("timeframe", "forecast");
        }
        
        parameters.put("originalInput", input);
    }
    
    private void extractStockParameters(String input, Map<String, Object> parameters) {
        // Extract company/ticker symbols
        String[] stockKeywords = {"stock price of ", "price of ", "shares of ", "ticker "};
        for (String keyword : stockKeywords) {
            int index = input.toLowerCase().indexOf(keyword);
            if (index != -1) {
                String symbol = input.substring(index + keyword.length()).trim();
                symbol = symbol.replaceAll("[.,!?].*", "").trim();
                if (!symbol.isEmpty()) {
                    parameters.put("symbol", symbol.toUpperCase());
                    break;
                }
            }
        }
        
        parameters.put("originalInput", input);
    }
    
    private void extractTravelParameters(String input, Map<String, Object> parameters) {
        // Extract destinations
        String[] travelKeywords = {"to ", "in ", "visit ", "trip to "};
        for (String keyword : travelKeywords) {
            int index = input.toLowerCase().indexOf(keyword);
            if (index != -1) {
                String destination = input.substring(index + keyword.length()).trim();
                destination = destination.replaceAll("[.,!?].*", "").trim();
                if (!destination.isEmpty()) {
                    parameters.put("destination", destination);
                    break;
                }
            }
        }
        
        // Extract travel type
        if (input.contains("business")) {
            parameters.put("travelType", "business");
        } else if (input.contains("vacation") || input.contains("holiday")) {
            parameters.put("travelType", "leisure");
        }
        
        parameters.put("originalInput", input);
    }
    
    private String preprocessPrompt(String input, IntentPattern pattern) {
        // Clean up the prompt for the target agent
        switch (pattern.getIntent()) {
            case "weather":
                return "Please provide weather information for: " + input;
            case "stock":
                return "Please provide stock price information for: " + input;
            case "travel":
                return "Please help plan travel for: " + input;
            default:
                return input;
        }
    }
    
    private RoutingResult createChatResult(String input, double confidence) {
        return new RoutingResult(
            "chat",
            "ChatAgent",
            input,
            confidence,
            Map.of("originalInput", input)
        );
    }
    
    /**
     * Get all available agents and their capabilities
     */
    public CompletableFuture<Map<String, String>> getAvailableAgents() {
        return agentRegistry.discoverAgents()
                .thenApply(agents -> {
                    Map<String, String> result = new HashMap<>();
                    agents.forEach(agent -> 
                        result.put(agent.getAgentId(), agent.getDescription()));
                    return result;
                });
    }
    
    /**
     * Find agents by capability
     */
    public CompletableFuture<List<String>> findAgentsForCapability(String capability) {
        return agentRegistry.findAgentsByCapability(capability)
                .thenApply(agents -> 
                    agents.stream()
                            .map(AgentRegistry.AgentInfo::getAgentId)
                            .toList()
                );
    }
    
    /**
     * Get examples of what each agent can do
     */
    public Map<String, List<String>> getAgentExamples() {
        Map<String, List<String>> examples = new HashMap<>();
        
        examples.put("WeatherAgent", Arrays.asList(
            "What's the weather like in Nice?",
            "Will it rain tomorrow in Paris?",
            "Show me the forecast for London",
            "What's the temperature in Tokyo?"
        ));
        
        examples.put("StockPriceAgent", Arrays.asList(
            "What's the stock price of Amadeus?",
            "Show me Apple stock price",
            "How is Tesla trading today?",
            "Get me the latest price for MSFT"
        ));
        
        examples.put("TravelPlannerAgent", Arrays.asList(
            "Help me plan a trip to Rome",
            "Organize a vacation to Thailand",
            "Plan a business trip to New York",
            "Create an itinerary for Barcelona"
        ));
        
        return examples;
    }
    
    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [PromptManager] " + message);
    }
}