package io.amcp.connectors.ai.fallback;

import io.amcp.connectors.ai.cache.LLMResponseCache;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Fallback system for LLM timeouts using rule-based responses derived from cached LLM calls.
 * 
 * Features:
 * - Pattern-based response matching from cached LLM responses
 * - Keyword extraction and similarity scoring
 * - Template-based response generation
 * - Automatic rule learning from successful LLM interactions
 * - Configurable fallback strategies
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class LLMFallbackSystem {
    
    private static final String DEFAULT_RULES_DIR = ".amcp/fallback-rules";
    private static final int DEFAULT_MIN_CONFIDENCE = 70; // Minimum confidence percentage
    private static final int DEFAULT_MAX_FALLBACK_RESPONSES = 100;
    
    private final Path rulesDirectory;
    private final LLMResponseCache responseCache;
    private final Map<String, FallbackRule> rules;
    private final int minConfidence;
    private final int maxFallbackResponses;
    
    // Statistics
    private long fallbackAttempts = 0;
    private long successfulFallbacks = 0;
    private long ruleLearningEvents = 0;
    
    public LLMFallbackSystem(LLMResponseCache responseCache) {
        this(responseCache, DEFAULT_RULES_DIR, DEFAULT_MIN_CONFIDENCE, DEFAULT_MAX_FALLBACK_RESPONSES);
    }
    
    public LLMFallbackSystem(LLMResponseCache responseCache, String rulesDir, 
                            int minConfidence, int maxFallbackResponses) {
        this.responseCache = responseCache;
        this.rulesDirectory = Paths.get(System.getProperty("user.home"), rulesDir);
        this.minConfidence = minConfidence;
        this.maxFallbackResponses = maxFallbackResponses;
        this.rules = new ConcurrentHashMap<>();
        
        initializeRulesDirectory();
        loadExistingRules();
        learnFromCachedResponses();
    }
    
    /**
     * Fallback rule containing pattern matching and response templates
     */
    public static class FallbackRule {
        private final String id;
        private final List<String> keywords;
        private final List<Pattern> patterns;
        private final List<String> responseTemplates;
        private final String category;
        private final int confidence;
        private final long createdTime;
        private int usageCount;
        
        public FallbackRule(String id, List<String> keywords, List<String> patterns,
                           List<String> responseTemplates, String category, int confidence) {
            this.id = id;
            this.keywords = new ArrayList<>(keywords);
            this.patterns = patterns.stream()
                .map(p -> Pattern.compile(p, Pattern.CASE_INSENSITIVE))
                .collect(Collectors.toList());
            this.responseTemplates = new ArrayList<>(responseTemplates);
            this.category = category;
            this.confidence = confidence;
            this.createdTime = System.currentTimeMillis();
            this.usageCount = 0;
        }
        
        public String getId() { return id; }
        public List<String> getKeywords() { return keywords; }
        public List<Pattern> getPatterns() { return patterns; }
        public List<String> getResponseTemplates() { return responseTemplates; }
        public String getCategory() { return category; }
        public int getConfidence() { return confidence; }
        public long getCreatedTime() { return createdTime; }
        public int getUsageCount() { return usageCount; }
        
        public void incrementUsage() { usageCount++; }
        
        /**
         * Calculate match score for a given prompt
         */
        public int calculateMatchScore(String prompt) {
            String lowerPrompt = prompt.toLowerCase();
            int score = 0;
            
            // Keyword matching (40% weight)
            int keywordMatches = 0;
            for (String keyword : keywords) {
                if (lowerPrompt.contains(keyword.toLowerCase())) {
                    keywordMatches++;
                }
            }
            score += (int) ((keywordMatches * 40.0) / keywords.size());
            
            // Pattern matching (60% weight)
            int patternMatches = 0;
            for (Pattern pattern : patterns) {
                if (pattern.matcher(prompt).find()) {
                    patternMatches++;
                }
            }
            if (!patterns.isEmpty()) {
                score += (int) ((patternMatches * 60.0) / patterns.size());
            }
            
            return Math.min(score, 100);
        }
        
        /**
         * Generate response using templates
         */
        public String generateResponse(String prompt) {
            if (responseTemplates.isEmpty()) {
                return "I apologize, but I'm currently experiencing technical difficulties. Please try again later.";
            }
            
            // Select template based on prompt characteristics
            String template = selectBestTemplate(prompt);
            return personalizeResponse(template, prompt);
        }
        
        private String selectBestTemplate(String prompt) {
            // Simple selection - could be enhanced with more sophisticated logic
            return responseTemplates.get(usageCount % responseTemplates.size());
        }
        
        private String personalizeResponse(String template, String prompt) {
            // Basic template variable replacement
            return template
                .replace("{prompt}", prompt)
                .replace("{category}", category)
                .replace("{timestamp}", new Date().toString());
        }
    }
    
    /**
     * Attempt fallback response for a given prompt
     */
    public Optional<String> attemptFallback(String prompt, String model, Map<String, Object> parameters) {
        fallbackAttempts++;
        
        logMessage("Attempting fallback for prompt: " + truncate(prompt, 50));
        
        // Find best matching rule
        FallbackRule bestRule = findBestMatchingRule(prompt);
        
        if (bestRule != null && bestRule.getConfidence() >= minConfidence) {
            String response = bestRule.generateResponse(prompt);
            bestRule.incrementUsage();
            successfulFallbacks++;
            
            logMessage(String.format("Fallback successful using rule '%s' (confidence: %d%%)", 
                bestRule.getId(), bestRule.getConfidence()));
            
            return Optional.of(response);
        }
        
        // Try semantic similarity with cached responses
        Optional<String> semanticFallback = attemptSemanticFallback(prompt, model, parameters);
        if (semanticFallback.isPresent()) {
            successfulFallbacks++;
            logMessage("Fallback successful using semantic similarity");
            return semanticFallback;
        }
        
        logMessage("No suitable fallback found");
        return Optional.empty();
    }
    
    /**
     * Find best matching rule for a prompt
     */
    private FallbackRule findBestMatchingRule(String prompt) {
        FallbackRule bestRule = null;
        int bestScore = 0;
        
        for (FallbackRule rule : rules.values()) {
            int score = rule.calculateMatchScore(prompt);
            if (score > bestScore && score >= minConfidence) {
                bestScore = score;
                bestRule = rule;
            }
        }
        
        return bestRule;
    }
    
    /**
     * Attempt semantic fallback using cached responses
     */
    private Optional<String> attemptSemanticFallback(String prompt, String model, Map<String, Object> parameters) {
        // This is a simplified semantic matching - could be enhanced with embeddings
        List<String> promptKeywords = extractKeywords(prompt);
        
        // Check if we have enough cached responses to attempt semantic matching
        if (responseCache != null && rules.size() < maxFallbackResponses) {
            // Future enhancement: Use responseCache to find semantically similar cached responses
            // For now, we rely on the rule-based system
        }
        
        if (!promptKeywords.isEmpty()) {
            String genericResponse = generateGenericResponse(prompt, promptKeywords);
            return Optional.of(genericResponse);
        }
        
        return Optional.empty();
    }
    
    /**
     * Learn new rules from successful LLM interactions
     */
    public void learnFromResponse(String prompt, String response, String model, Map<String, Object> parameters) {
        ruleLearningEvents++;
        
        // Extract patterns and keywords from successful interactions
        List<String> keywords = extractKeywords(prompt);
        List<String> patterns = extractPatterns(prompt);
        String category = categorizePrompt(prompt);
        
        if (keywords.size() >= 2) { // Minimum threshold for rule creation
            String ruleId = generateRuleId(keywords, category);
            
            // Check if rule already exists
            if (!rules.containsKey(ruleId)) {
                FallbackRule newRule = new FallbackRule(
                    ruleId,
                    keywords,
                    patterns,
                    Arrays.asList(response),
                    category,
                    calculateInitialConfidence(keywords, patterns)
                );
                
                rules.put(ruleId, newRule);
                persistRule(newRule);
                
                logMessage("Learned new fallback rule: " + ruleId);
            } else {
                // Update existing rule with new response template
                FallbackRule existingRule = rules.get(ruleId);
                if (!existingRule.getResponseTemplates().contains(response)) {
                    existingRule.getResponseTemplates().add(response);
                    persistRule(existingRule);
                }
            }
        }
    }
    
    /**
     * Extract keywords from text
     */
    private List<String> extractKeywords(String text) {
        // Simple keyword extraction - could be enhanced with NLP libraries
        String[] words = text.toLowerCase()
            .replaceAll("[^a-zA-Z0-9\\s]", "")
            .split("\\s+");
        
        Set<String> stopWords = Set.of("the", "a", "an", "and", "or", "but", "in", "on", "at", 
            "to", "for", "of", "with", "by", "is", "are", "was", "were", "be", "been", "have", 
            "has", "had", "do", "does", "did", "will", "would", "could", "should", "may", "might");
        
        return Arrays.stream(words)
            .filter(word -> word.length() > 2)
            .filter(word -> !stopWords.contains(word))
            .distinct()
            .limit(10) // Limit to top 10 keywords
            .collect(Collectors.toList());
    }
    
    /**
     * Extract patterns from text
     */
    private List<String> extractPatterns(String text) {
        List<String> patterns = new ArrayList<>();
        
        // Question patterns
        if (text.trim().endsWith("?")) {
            patterns.add(".*\\?$");
        }
        
        // Command patterns
        if (text.toLowerCase().startsWith("please") || text.toLowerCase().startsWith("can you")) {
            patterns.add("^(please|can you).*");
        }
        
        // Code-related patterns
        if (text.contains("function") || text.contains("class") || text.contains("method")) {
            patterns.add(".*(function|class|method).*");
        }
        
        return patterns;
    }
    
    /**
     * Categorize prompt type
     */
    private String categorizePrompt(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        
        if (lowerPrompt.contains("code") || lowerPrompt.contains("function") || lowerPrompt.contains("programming")) {
            return "coding";
        } else if (lowerPrompt.contains("explain") || lowerPrompt.contains("what is") || lowerPrompt.contains("how does")) {
            return "explanation";
        } else if (lowerPrompt.contains("help") || lowerPrompt.contains("assist") || lowerPrompt.contains("support")) {
            return "assistance";
        } else if (lowerPrompt.endsWith("?")) {
            return "question";
        } else {
            return "general";
        }
    }
    
    /**
     * Generate rule ID from keywords and category
     */
    private String generateRuleId(List<String> keywords, String category) {
        String keywordHash = String.valueOf(String.join("-", keywords).hashCode());
        return category + "-" + keywordHash.replace("-", "");
    }
    
    /**
     * Calculate initial confidence for a new rule
     */
    private int calculateInitialConfidence(List<String> keywords, List<String> patterns) {
        int confidence = 50; // Base confidence
        
        // More keywords = higher confidence
        confidence += Math.min(keywords.size() * 5, 30);
        
        // More patterns = higher confidence
        confidence += Math.min(patterns.size() * 10, 20);
        
        return Math.min(confidence, 95); // Cap at 95%
    }
    
    /**
     * Generate generic helpful response
     */
    private String generateGenericResponse(String prompt, List<String> keywords) {
        String keywordContext = String.join(", ", keywords.subList(0, Math.min(3, keywords.size())));
        
        return String.format(
            "I understand you're asking about %s. While I'm currently experiencing some technical difficulties " +
            "with my main processing system, I can see this relates to %s. Please try rephrasing your question " +
            "or try again in a few moments when my systems are fully operational.",
            keywordContext, keywordContext
        );
    }
    
    /**
     * Initialize rules directory
     */
    private void initializeRulesDirectory() {
        try {
            Files.createDirectories(rulesDirectory);
        } catch (IOException e) {
            System.err.println("Failed to create rules directory: " + e.getMessage());
        }
    }
    
    /**
     * Load existing rules from disk
     */
    private void loadExistingRules() {
        try {
            if (Files.exists(rulesDirectory)) {
                Files.list(rulesDirectory)
                    .filter(path -> path.toString().endsWith(".rule"))
                    .forEach(this::loadRule);
            }
        } catch (IOException e) {
            System.err.println("Failed to load existing rules: " + e.getMessage());
        }
        
        logMessage("Loaded " + rules.size() + " fallback rules");
    }
    
    /**
     * Load a single rule from disk
     */
    private void loadRule(Path rulePath) {
        try (BufferedReader reader = Files.newBufferedReader(rulePath)) {
            Properties props = new Properties();
            props.load(reader);
            
            String id = props.getProperty("id");
            List<String> keywords = Arrays.asList(props.getProperty("keywords", "").split(","));
            List<String> patterns = Arrays.asList(props.getProperty("patterns", "").split(","));
            List<String> responses = Arrays.asList(props.getProperty("responses", "").split("|||"));
            String category = props.getProperty("category", "general");
            int confidence = Integer.parseInt(props.getProperty("confidence", "50"));
            
            FallbackRule rule = new FallbackRule(id, keywords, patterns, responses, category, confidence);
            rules.put(id, rule);
            
        } catch (Exception e) {
            System.err.println("Failed to load rule from " + rulePath + ": " + e.getMessage());
        }
    }
    
    /**
     * Persist rule to disk
     */
    private void persistRule(FallbackRule rule) {
        Path rulePath = rulesDirectory.resolve(rule.getId() + ".rule");
        
        try (BufferedWriter writer = Files.newBufferedWriter(rulePath)) {
            Properties props = new Properties();
            props.setProperty("id", rule.getId());
            props.setProperty("keywords", String.join(",", rule.getKeywords()));
            props.setProperty("patterns", rule.getPatterns().stream()
                .map(Pattern::pattern)
                .collect(Collectors.joining(",")));
            props.setProperty("responses", String.join("|||", rule.getResponseTemplates()));
            props.setProperty("category", rule.getCategory());
            props.setProperty("confidence", String.valueOf(rule.getConfidence()));
            props.setProperty("created", String.valueOf(rule.getCreatedTime()));
            props.setProperty("usage", String.valueOf(rule.getUsageCount()));
            
            props.store(writer, "Fallback rule for " + rule.getCategory());
            
        } catch (IOException e) {
            System.err.println("Failed to persist rule " + rule.getId() + ": " + e.getMessage());
        }
    }
    
    /**
     * Learn from existing cached responses
     */
    private void learnFromCachedResponses() {
        // This would require access to cache internals
        // For now, create some basic rules
        createBasicFallbackRules();
    }
    
    /**
     * Create basic fallback rules for common scenarios including agent-specific rules
     */
    private void createBasicFallbackRules() {
        // Weather Agent rule
        FallbackRule weatherRule = new FallbackRule(
            "weather-agent",
            Arrays.asList("weather", "temperature", "rain", "snow", "forecast", "sunny", "cloudy", "storm", "humidity", "wind"),
            Arrays.asList(".*(weather|temperature|rain|snow|forecast|sunny|cloudy|storm|humidity|wind).*"),
            Arrays.asList(
                "I'd be happy to help you check the weather information. While my live weather data service is temporarily unavailable, " +
                "I recommend checking a reliable weather service like Weather.com, AccuWeather, or your local weather app for current conditions and forecasts.",
                "For accurate weather information, please check trusted weather sources like the National Weather Service, BBC Weather, or Weather Underground. " +
                "These services provide real-time weather data, forecasts, and weather alerts for your location.",
                "Weather conditions can change rapidly, so I recommend checking current weather data from reliable meteorological services. " +
                "Most weather apps and websites provide hourly and daily forecasts along with weather warnings."
            ),
            "weather",
            90
        );
        
        // Stock/Financial Agent rule
        FallbackRule stockRule = new FallbackRule(
            "stock-agent",
            Arrays.asList("stock", "price", "market", "trading", "investment", "portfolio", "shares", "dividend", "earnings", "financial", "exchange", "ticker", "cryptocurrency", "crypto", "bitcoin", "invest"),
            Arrays.asList(".*(stock|price|market|trading|investment|portfolio|shares|dividend|earnings|financial|exchange|ticker|cryptocurrency|crypto|bitcoin|invest).*"),
            Arrays.asList(
                "I understand you're looking for financial market information. While my real-time market data feed is temporarily unavailable, " +
                "I recommend checking reliable financial sources like Yahoo Finance, Bloomberg, MarketWatch, or your brokerage platform for current stock prices and market data.",
                "For investment and stock market information, please consult trusted financial platforms like Reuters, CNBC, or major financial institutions. " +
                "Remember that all investments carry risk and it's important to do thorough research or consult with a qualified financial advisor.",
                "Financial markets change rapidly, so real-time data is essential. Check reputable financial news sources and trading platforms for current market information, " +
                "stock prices, and investment analysis. Always consider your risk tolerance and investment goals."
            ),
            "financial",
            90
        );
        
        // Travel Agent rule
        FallbackRule travelRule = new FallbackRule(
            "travel-agent",
            Arrays.asList("travel", "trip", "flight", "hotel", "booking", "vacation", "destination", "itinerary", "visit", "tourism", "accommodation", "airline"),
            Arrays.asList(".*(travel|trip|flight|hotel|booking|vacation|destination|itinerary|visit|tourism|accommodation|airline).*"),
            Arrays.asList(
                "I'd love to help you with your travel planning! While my travel booking and real-time information services are temporarily unavailable, " +
                "I recommend checking major travel platforms like Expedia, Booking.com, TripAdvisor, or consulting with a travel agent for current prices, availability, and detailed planning.",
                "For travel planning and bookings, please check reliable travel websites like Kayak, Priceline, or airline websites directly. " +
                "These platforms provide real-time flight information, hotel availability, and travel deals. Don't forget to check visa requirements and travel advisories.",
                "Travel planning requires current information about prices, availability, and local conditions. I recommend using established travel booking sites, " +
                "reading recent travel reviews, and checking official tourism websites for your destination. Consider travel insurance for your trip."
            ),
            "travel",
            90
        );
        
        // Coding assistance rule
        FallbackRule codingRule = new FallbackRule(
            "coding-assistance",
            Arrays.asList("code", "function", "programming", "java", "method", "class", "algorithm", "debug", "syntax", "development"),
            Arrays.asList(".*(code|function|programming|java|method|class|algorithm|debug|syntax|development).*"),
            Arrays.asList(
                "I can help with coding questions. While my main system is temporarily unavailable, " +
                "I recommend checking the official documentation or trying a simpler approach to your coding problem.",
                "For coding assistance, please ensure your question is specific about the programming language " +
                "and the exact issue you're facing. I'll be back to full functionality shortly."
            ),
            "coding",
            85
        );
        
        // General help rule
        FallbackRule helpRule = new FallbackRule(
            "general-help",
            Arrays.asList("help", "assist", "support", "question", "how"),
            Arrays.asList(".*(help|assist|support|how).*", ".*\\?$"),
            Arrays.asList(
                "I'm here to help! While I'm experiencing some technical difficulties, " +
                "please try rephrasing your question or be more specific about what you need assistance with.",
                "I understand you need help. My main processing system is temporarily unavailable, " +
                "but I should be back to full functionality shortly. Please try again in a few moments."
            ),
            "assistance",
            80
        );
        
        // Explanation rule
        FallbackRule explanationRule = new FallbackRule(
            "explanation-request",
            Arrays.asList("explain", "what", "why", "how", "describe"),
            Arrays.asList(".*(explain|what is|why|how does|describe).*"),
            Arrays.asList(
                "I'd be happy to explain that topic. While my main knowledge system is temporarily unavailable, " +
                "I recommend consulting reliable documentation or educational resources for detailed explanations.",
                "For explanations, I typically provide comprehensive answers. Currently experiencing technical issues, " +
                "but I'll be back to provide detailed explanations shortly."
            ),
            "explanation",
            75
        );
        
        rules.put(weatherRule.getId(), weatherRule);
        rules.put(stockRule.getId(), stockRule);
        rules.put(travelRule.getId(), travelRule);
        rules.put(codingRule.getId(), codingRule);
        rules.put(helpRule.getId(), helpRule);
        rules.put(explanationRule.getId(), explanationRule);
        
        // Persist basic rules
        rules.values().forEach(this::persistRule);
        
        logMessage("Created " + rules.size() + " basic fallback rules");
    }
    
    /**
     * Get fallback system statistics
     */
    public FallbackStats getStats() {
        double successRate = fallbackAttempts > 0 ? 
            (double) successfulFallbacks / fallbackAttempts : 0.0;
        
        return new FallbackStats(
            rules.size(),
            fallbackAttempts,
            successfulFallbacks,
            ruleLearningEvents,
            successRate
        );
    }
    
    /**
     * Cleanup old or unused rules
     */
    public void cleanupRules() {
        long cutoffTime = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000); // 30 days
        
        rules.entrySet().removeIf(entry -> {
            FallbackRule rule = entry.getValue();
            return rule.getUsageCount() == 0 && rule.getCreatedTime() < cutoffTime;
        });
        
        logMessage("Cleaned up unused fallback rules");
    }
    
    /**
     * Truncate string for logging
     */
    private String truncate(String str, int maxLength) {
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }
    
    /**
     * Log message
     */
    private void logMessage(String message) {
        String timestamp = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [LLMFallbackSystem] " + message);
    }
    
    /**
     * Fallback system statistics
     */
    public static class FallbackStats {
        private final int totalRules;
        private final long fallbackAttempts;
        private final long successfulFallbacks;
        private final long ruleLearningEvents;
        private final double successRate;
        
        public FallbackStats(int totalRules, long fallbackAttempts, long successfulFallbacks,
                            long ruleLearningEvents, double successRate) {
            this.totalRules = totalRules;
            this.fallbackAttempts = fallbackAttempts;
            this.successfulFallbacks = successfulFallbacks;
            this.ruleLearningEvents = ruleLearningEvents;
            this.successRate = successRate;
        }
        
        public int getTotalRules() { return totalRules; }
        public long getFallbackAttempts() { return fallbackAttempts; }
        public long getSuccessfulFallbacks() { return successfulFallbacks; }
        public long getRuleLearningEvents() { return ruleLearningEvents; }
        public double getSuccessRate() { return successRate; }
        
        @Override
        public String toString() {
            return String.format(
                "FallbackStats{rules=%d, attempts=%d, successful=%d, learned=%d, successRate=%.2f%%}",
                totalRules, fallbackAttempts, successfulFallbacks, ruleLearningEvents, successRate * 100
            );
        }
    }
}
