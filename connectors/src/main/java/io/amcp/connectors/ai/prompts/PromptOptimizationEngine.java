package io.amcp.connectors.ai.prompts;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prompt Optimization Engine for AMCP v1.5 Enhanced Orchestrator
 * 
 * Provides model-agnostic prompt engineering with:
 * - Few-shot learning examples for consistent JSON output
 * - Model-specific prompt templates and formatting
 * - Structured prompt generation with validation
 * - Dynamic prompt adaptation based on context
 * - Performance tracking and optimization metrics
 * - Template versioning and A/B testing support
 * 
 * This engine ensures consistent, high-quality interactions with various LLMs
 * while maintaining structured JSON outputs for reliable orchestration.
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 * @since 1.5.0
 */
public class PromptOptimizationEngine {
    
    // Model-specific configurations
    private static final Map<String, ModelConfiguration> MODEL_CONFIGS = new HashMap<>();
    
    // Few-shot examples for different prompt types
    private static final Map<PromptType, List<FewShotExample>> FEW_SHOT_EXAMPLES = new HashMap<>();
    
    // Prompt templates
    private static final Map<PromptType, PromptTemplate> PROMPT_TEMPLATES = new HashMap<>();
    
    // Performance tracking
    private final Map<String, PromptPerformanceMetrics> performanceMetrics = new ConcurrentHashMap<>();
    
    static {
        initializeModelConfigurations();
        initializeFewShotExamples();
        initializePromptTemplates();
    }
    
    public PromptOptimizationEngine() {
        logMessage("Prompt Optimization Engine initialized with " + 
                  MODEL_CONFIGS.size() + " model configurations");
    }
    
    /**
     * Generate optimized prompt for task planning
     */
    public String generateTaskPlanningPrompt(String userQuery, String context, 
                                           Set<String> availableAgents, String modelType) {
        ModelConfiguration config = MODEL_CONFIGS.getOrDefault(modelType, MODEL_CONFIGS.get("default"));
        PromptTemplate template = PROMPT_TEMPLATES.get(PromptType.TASK_PLANNING);
        
        StringBuilder prompt = new StringBuilder();
        
        // Add model-specific prefix
        if (config.getSystemPrefix() != null) {
            prompt.append(config.getSystemPrefix()).append("\n\n");
        }
        
        // Add few-shot examples
        prompt.append(generateFewShotSection(PromptType.TASK_PLANNING, config));
        
        // Add main instruction
        prompt.append(template.getInstructions()).append("\n\n");
        
        // Add context information
        prompt.append("AVAILABLE AGENTS:\n");
        for (String agent : availableAgents) {
            prompt.append("- ").append(agent).append("\n");
        }
        prompt.append("\n");
        
        if (context != null && !context.trim().isEmpty()) {
            prompt.append("CONTEXT:\n").append(context).append("\n\n");
        }
        
        // Add user query
        prompt.append("USER QUERY:\n").append(userQuery).append("\n\n");
        
        // Add JSON schema enforcement
        prompt.append(generateJsonSchemaEnforcement(PromptType.TASK_PLANNING, config));
        
        // Add model-specific suffix
        if (config.getSystemSuffix() != null) {
            prompt.append("\n").append(config.getSystemSuffix());
        }
        
        return prompt.toString();
    }
    
    /**
     * Generate optimized prompt for agent capability detection
     */
    public String generateCapabilityDetectionPrompt(String userQuery, Map<String, Set<String>> agentCapabilities, 
                                                   String modelType) {
        ModelConfiguration config = MODEL_CONFIGS.getOrDefault(modelType, MODEL_CONFIGS.get("default"));
        PromptTemplate template = PROMPT_TEMPLATES.get(PromptType.CAPABILITY_DETECTION);
        
        StringBuilder prompt = new StringBuilder();
        
        // Add model-specific prefix
        if (config.getSystemPrefix() != null) {
            prompt.append(config.getSystemPrefix()).append("\n\n");
        }
        
        // Add few-shot examples
        prompt.append(generateFewShotSection(PromptType.CAPABILITY_DETECTION, config));
        
        // Add main instruction
        prompt.append(template.getInstructions()).append("\n\n");
        
        // Add capability mapping
        prompt.append("AGENT CAPABILITIES:\n");
        for (Map.Entry<String, Set<String>> entry : agentCapabilities.entrySet()) {
            prompt.append("- ").append(entry.getKey()).append(": ");
            prompt.append(String.join(", ", entry.getValue())).append("\n");
        }
        prompt.append("\n");
        
        // Add user query
        prompt.append("USER QUERY:\n").append(userQuery).append("\n\n");
        
        // Add JSON schema enforcement
        prompt.append(generateJsonSchemaEnforcement(PromptType.CAPABILITY_DETECTION, config));
        
        return prompt.toString();
    }
    
    /**
     * Generate optimized prompt for response synthesis
     */
    public String generateResponseSynthesisPrompt(String originalQuery, List<String> agentResponses, 
                                                String modelType) {
        ModelConfiguration config = MODEL_CONFIGS.getOrDefault(modelType, MODEL_CONFIGS.get("default"));
        PromptTemplate template = PROMPT_TEMPLATES.get(PromptType.RESPONSE_SYNTHESIS);
        
        StringBuilder prompt = new StringBuilder();
        
        // Add model-specific prefix
        if (config.getSystemPrefix() != null) {
            prompt.append(config.getSystemPrefix()).append("\n\n");
        }
        
        // Add few-shot examples
        prompt.append(generateFewShotSection(PromptType.RESPONSE_SYNTHESIS, config));
        
        // Add main instruction
        prompt.append(template.getInstructions()).append("\n\n");
        
        // Add original query
        prompt.append("ORIGINAL USER QUERY:\n").append(originalQuery).append("\n\n");
        
        // Add agent responses
        prompt.append("AGENT RESPONSES:\n");
        for (int i = 0; i < agentResponses.size(); i++) {
            prompt.append("Response ").append(i + 1).append(":\n");
            prompt.append(agentResponses.get(i)).append("\n\n");
        }
        
        // Add synthesis guidance
        prompt.append("SYNTHESIS REQUIREMENTS:\n");
        prompt.append("- Combine information from all responses coherently\n");
        prompt.append("- Maintain accuracy and avoid hallucination\n");
        prompt.append("- Use natural, helpful language\n");
        prompt.append("- Highlight any contradictions or uncertainties\n\n");
        
        // Add final instruction
        prompt.append("Please provide a comprehensive, synthesized response:\n");
        
        return prompt.toString();
    }
    
    /**
     * Generate optimized prompt for parameter extraction
     */
    public String generateParameterExtractionPrompt(String userQuery, Set<String> expectedParameters, 
                                                   String modelType) {
        ModelConfiguration config = MODEL_CONFIGS.getOrDefault(modelType, MODEL_CONFIGS.get("default"));
        PromptTemplate template = PROMPT_TEMPLATES.get(PromptType.PARAMETER_EXTRACTION);
        
        StringBuilder prompt = new StringBuilder();
        
        // Add model-specific prefix
        if (config.getSystemPrefix() != null) {
            prompt.append(config.getSystemPrefix()).append("\n\n");
        }
        
        // Add few-shot examples
        prompt.append(generateFewShotSection(PromptType.PARAMETER_EXTRACTION, config));
        
        // Add main instruction
        prompt.append(template.getInstructions()).append("\n\n");
        
        // Add expected parameters
        prompt.append("EXPECTED PARAMETERS:\n");
        for (String param : expectedParameters) {
            prompt.append("- ").append(param).append("\n");
        }
        prompt.append("\n");
        
        // Add user query
        prompt.append("USER QUERY:\n").append(userQuery).append("\n\n");
        
        // Add JSON schema enforcement
        prompt.append(generateJsonSchemaEnforcement(PromptType.PARAMETER_EXTRACTION, config));
        
        return prompt.toString();
    }
    
    /**
     * Generate few-shot examples section
     */
    private String generateFewShotSection(PromptType promptType, ModelConfiguration config) {
        List<FewShotExample> examples = FEW_SHOT_EXAMPLES.get(promptType);
        if (examples == null || examples.isEmpty()) {
            return "";
        }
        
        StringBuilder section = new StringBuilder();
        section.append("EXAMPLES:\n\n");
        
        int maxExamples = Math.min(examples.size(), config.getMaxFewShotExamples());
        for (int i = 0; i < maxExamples; i++) {
            FewShotExample example = examples.get(i);
            section.append("Example ").append(i + 1).append(":\n");
            section.append("Input: ").append(example.getInput()).append("\n");
            section.append("Output: ").append(example.getOutput()).append("\n\n");
        }
        
        return section.toString();
    }
    
    /**
     * Generate JSON schema enforcement section
     */
    private String generateJsonSchemaEnforcement(PromptType promptType, ModelConfiguration config) {
        StringBuilder enforcement = new StringBuilder();
        
        enforcement.append("CRITICAL JSON REQUIREMENTS:\n");
        enforcement.append("- Respond with VALID JSON ONLY\n");
        enforcement.append("- No markdown formatting, no code blocks\n");
        enforcement.append("- No explanations outside the JSON\n");
        enforcement.append("- All string values must be in double quotes\n");
        enforcement.append("- No trailing commas\n");
        enforcement.append("- Ensure proper escaping of special characters\n\n");
        
        // Add schema-specific requirements
        switch (promptType) {
            case TASK_PLANNING:
                enforcement.append("REQUIRED JSON STRUCTURE:\n");
                enforcement.append("{\n");
                enforcement.append("  \"tasks\": [\n");
                enforcement.append("    {\n");
                enforcement.append("      \"id\": \"task_1\",\n");
                enforcement.append("      \"capability\": \"weather.get\",\n");
                enforcement.append("      \"agent\": \"WeatherAgent\",\n");
                enforcement.append("      \"parameters\": {\"location\": \"Paris\"},\n");
                enforcement.append("      \"priority\": 1,\n");
                enforcement.append("      \"dependencies\": []\n");
                enforcement.append("    }\n");
                enforcement.append("  ],\n");
                enforcement.append("  \"confidence\": 0.95\n");
                enforcement.append("}\n\n");
                break;
                
            case CAPABILITY_DETECTION:
                enforcement.append("REQUIRED JSON STRUCTURE:\n");
                enforcement.append("{\n");
                enforcement.append("  \"intent\": \"weather_query\",\n");
                enforcement.append("  \"capability\": \"weather.get\",\n");
                enforcement.append("  \"targetAgent\": \"WeatherAgent\",\n");
                enforcement.append("  \"confidence\": 0.9,\n");
                enforcement.append("  \"parameters\": {\"location\": \"extracted_location\"}\n");
                enforcement.append("}\n\n");
                break;
                
            case PARAMETER_EXTRACTION:
                enforcement.append("REQUIRED JSON STRUCTURE:\n");
                enforcement.append("{\n");
                enforcement.append("  \"parameters\": {\n");
                enforcement.append("    \"location\": \"Paris\",\n");
                enforcement.append("    \"date\": \"2024-01-15\",\n");
                enforcement.append("    \"units\": \"celsius\"\n");
                enforcement.append("  },\n");
                enforcement.append("  \"confidence\": 0.85\n");
                enforcement.append("}\n\n");
                break;
                
            case RESPONSE_SYNTHESIS:
                enforcement.append("RESPONSE REQUIREMENTS:\n");
                enforcement.append("- Provide natural language response (not JSON)\n");
                enforcement.append("- Synthesize information from all agent responses\n");
                enforcement.append("- Maintain accuracy and avoid hallucination\n");
                enforcement.append("- Use helpful, conversational tone\n\n");
                break;
        }
        
        // Add model-specific enforcement
        if (config.getStrictJsonMode()) {
            enforcement.append("STRICT MODE: Your response must be parseable JSON. ");
            enforcement.append("Any non-JSON content will be rejected.\n\n");
        }
        
        enforcement.append("JSON RESPONSE:");
        
        return enforcement.toString();
    }
    
    /**
     * Validate generated prompt quality
     */
    public PromptQualityScore validatePrompt(String prompt, PromptType type) {
        List<String> issues = new ArrayList<>();
        int score = 100;
        
        // Check length
        if (prompt.length() < 100) {
            issues.add("Prompt too short, may lack sufficient context");
            score -= 20;
        } else if (prompt.length() > 8000) {
            issues.add("Prompt too long, may exceed token limits");
            score -= 15;
        }
        
        // Check for few-shot examples
        if (!prompt.contains("Example")) {
            issues.add("Missing few-shot examples");
            score -= 25;
        }
        
        // Check for JSON enforcement
        if (!prompt.contains("JSON")) {
            issues.add("Missing JSON format enforcement");
            score -= 30;
        }
        
        // Check for clear instructions
        if (!prompt.contains("REQUIRED") && !prompt.contains("CRITICAL")) {
            issues.add("Instructions may not be clear enough");
            score -= 10;
        }
        
        // Type-specific validations
        switch (type) {
            case TASK_PLANNING:
                if (!prompt.contains("AVAILABLE AGENTS")) {
                    issues.add("Missing agent information for task planning");
                    score -= 20;
                }
                break;
            case CAPABILITY_DETECTION:
                if (!prompt.contains("CAPABILITIES")) {
                    issues.add("Missing capability information");
                    score -= 20;
                }
                break;
            case PARAMETER_EXTRACTION:
                if (!prompt.contains("EXPECTED PARAMETERS")) {
                    issues.add("Missing parameter specification");
                    score -= 15;
                }
                break;
            case RESPONSE_SYNTHESIS:
                if (!prompt.contains("AGENT RESPONSES")) {
                    issues.add("Missing agent responses for synthesis");
                    score -= 25;
                }
                break;
        }
        
        return new PromptQualityScore(Math.max(0, score), issues);
    }
    
    /**
     * Record prompt performance metrics
     */
    public void recordPromptPerformance(String promptId, PromptType type, 
                                      boolean success, long responseTime, String modelType) {
        String key = type + "_" + modelType;
        PromptPerformanceMetrics metrics = performanceMetrics.computeIfAbsent(key,
            k -> new PromptPerformanceMetrics(type, modelType));
        
        metrics.addMeasurement(success, responseTime);
    }
    
    /**
     * Get performance metrics for optimization
     */
    public Map<String, PromptPerformanceMetrics> getPerformanceMetrics() {
        return new HashMap<>(performanceMetrics);
    }
    
    // Static initialization methods
    
    private static void initializeModelConfigurations() {
        // OpenAI GPT models
        MODEL_CONFIGS.put("gpt-4", new ModelConfiguration()
            .setSystemPrefix("You are a precise AI assistant that responds only with valid JSON.")
            .setMaxFewShotExamples(3)
            .setStrictJsonMode(true)
            .setMaxTokens(4000));
        
        MODEL_CONFIGS.put("gpt-3.5-turbo", new ModelConfiguration()
            .setSystemPrefix("You are an AI assistant. Respond with valid JSON only.")
            .setMaxFewShotExamples(2)
            .setStrictJsonMode(true)
            .setMaxTokens(3000));
        
        // Anthropic Claude
        MODEL_CONFIGS.put("claude-3", new ModelConfiguration()
            .setSystemPrefix("I need you to respond with only valid JSON. No explanations.")
            .setMaxFewShotExamples(3)
            .setStrictJsonMode(true)
            .setMaxTokens(4000));
        
        // Open source models
        MODEL_CONFIGS.put("llama-3", new ModelConfiguration()
            .setSystemPrefix("JSON response only:")
            .setSystemSuffix("Remember: JSON format only, no additional text.")
            .setMaxFewShotExamples(2)
            .setStrictJsonMode(false)
            .setMaxTokens(2000));
        
        MODEL_CONFIGS.put("tinyllama", new ModelConfiguration()
            .setSystemPrefix("JSON only:")
            .setSystemSuffix("Must be valid JSON.")
            .setMaxFewShotExamples(1)
            .setStrictJsonMode(false)
            .setMaxTokens(1000));
        
        // Default configuration
        MODEL_CONFIGS.put("default", new ModelConfiguration()
            .setSystemPrefix("Please respond with valid JSON only.")
            .setMaxFewShotExamples(2)
            .setStrictJsonMode(true)
            .setMaxTokens(3000));
    }
    
    private static void initializeFewShotExamples() {
        // Task Planning examples
        List<FewShotExample> taskPlanningExamples = new ArrayList<>();
        taskPlanningExamples.add(new FewShotExample(
            "What's the weather like in Paris?",
            "{\"tasks\":[{\"id\":\"task_1\",\"capability\":\"weather.get\",\"agent\":\"WeatherAgent\",\"parameters\":{\"location\":\"Paris\"},\"priority\":1,\"dependencies\":[]}],\"confidence\":0.95}"
        ));
        taskPlanningExamples.add(new FewShotExample(
            "Book a flight to London and check the weather there",
            "{\"tasks\":[{\"id\":\"task_1\",\"capability\":\"travel.flight.search\",\"agent\":\"TravelAgent\",\"parameters\":{\"destination\":\"London\"},\"priority\":1,\"dependencies\":[]},{\"id\":\"task_2\",\"capability\":\"weather.get\",\"agent\":\"WeatherAgent\",\"parameters\":{\"location\":\"London\"},\"priority\":2,\"dependencies\":[\"task_1\"]}],\"confidence\":0.9}"
        ));
        FEW_SHOT_EXAMPLES.put(PromptType.TASK_PLANNING, taskPlanningExamples);
        
        // Capability Detection examples
        List<FewShotExample> capabilityExamples = new ArrayList<>();
        capabilityExamples.add(new FewShotExample(
            "How hot is it in Tokyo?",
            "{\"intent\":\"weather_query\",\"capability\":\"weather.get\",\"targetAgent\":\"WeatherAgent\",\"confidence\":0.9,\"parameters\":{\"location\":\"Tokyo\"}}"
        ));
        capabilityExamples.add(new FewShotExample(
            "What's the stock price of Apple?",
            "{\"intent\":\"stock_query\",\"capability\":\"stock.price\",\"targetAgent\":\"StockAgent\",\"confidence\":0.95,\"parameters\":{\"symbol\":\"AAPL\"}}"
        ));
        FEW_SHOT_EXAMPLES.put(PromptType.CAPABILITY_DETECTION, capabilityExamples);
        
        // Parameter Extraction examples
        List<FewShotExample> parameterExamples = new ArrayList<>();
        parameterExamples.add(new FewShotExample(
            "Show me the weather in Berlin tomorrow in Fahrenheit",
            "{\"parameters\":{\"location\":\"Berlin\",\"date\":\"tomorrow\",\"units\":\"fahrenheit\"},\"confidence\":0.9}"
        ));
        parameterExamples.add(new FewShotExample(
            "I need flight prices from New York to San Francisco next week",
            "{\"parameters\":{\"origin\":\"New York\",\"destination\":\"San Francisco\",\"date\":\"next week\",\"type\":\"flight\"},\"confidence\":0.85}"
        ));
        FEW_SHOT_EXAMPLES.put(PromptType.PARAMETER_EXTRACTION, parameterExamples);
        
        // Response Synthesis examples
        List<FewShotExample> synthesisExamples = new ArrayList<>();
        FEW_SHOT_EXAMPLES.put(PromptType.RESPONSE_SYNTHESIS, synthesisExamples);
    }
    
    private static void initializePromptTemplates() {
        PROMPT_TEMPLATES.put(PromptType.TASK_PLANNING, new PromptTemplate(
            "TASK PLANNING INSTRUCTIONS:\n" +
            "Analyze the user query and create a structured task plan. Break down complex requests into individual tasks that can be executed by specific agents. Consider dependencies between tasks and assign appropriate priorities.\n" +
            "Each task must specify: capability, target agent, parameters, priority, and dependencies."
        ));
        
        PROMPT_TEMPLATES.put(PromptType.CAPABILITY_DETECTION, new PromptTemplate(
            "CAPABILITY DETECTION INSTRUCTIONS:\n" +
            "Analyze the user query to determine the primary intent and identify which agent capability best matches the request. Consider the available agents and their capabilities to make the best routing decision.\n" +
            "Provide: intent classification, target capability, recommended agent, confidence score, and extracted parameters."
        ));
        
        PROMPT_TEMPLATES.put(PromptType.PARAMETER_EXTRACTION, new PromptTemplate(
            "PARAMETER EXTRACTION INSTRUCTIONS:\n" +
            "Extract relevant parameters from the user query that would be needed to fulfill the request. Focus on identifying specific values like locations, dates, quantities, preferences, etc.\n" +
            "Return: structured parameter object with extracted values and confidence score."
        ));
        
        PROMPT_TEMPLATES.put(PromptType.RESPONSE_SYNTHESIS, new PromptTemplate(
            "RESPONSE SYNTHESIS INSTRUCTIONS:\n" +
            "Combine the provided agent responses into a coherent, comprehensive answer to the original user query. Ensure accuracy, avoid hallucination, and highlight any contradictions or uncertainties.\n" +
            "Create a natural, helpful response that addresses the user's needs."
        ));
    }
    
    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [PromptOptimizationEngine] " + message);
    }
    
    /**
     * Record model performance metrics
     */
    public void recordModelPerformance(String modelType, long responseTime, double successRate) {
        logMessage("Recording performance for model " + modelType + 
                  " - Response time: " + responseTime + "ms, Success rate: " + (successRate * 100) + "%");
        // In a real implementation, this would store the metrics
        // For now, we just log the operation
    }
    
    // Supporting classes and enums
    
    public enum PromptType {
        TASK_PLANNING,
        CAPABILITY_DETECTION,
        PARAMETER_EXTRACTION,
        RESPONSE_SYNTHESIS
    }
    
    public static class ModelConfiguration {
        private String systemPrefix;
        private String systemSuffix;
        private int maxFewShotExamples = 2;
        private boolean strictJsonMode = true;
        private int maxTokens = 3000;
        
        public String getSystemPrefix() { return systemPrefix; }
        public ModelConfiguration setSystemPrefix(String systemPrefix) { this.systemPrefix = systemPrefix; return this; }
        public String getSystemSuffix() { return systemSuffix; }
        public ModelConfiguration setSystemSuffix(String systemSuffix) { this.systemSuffix = systemSuffix; return this; }
        public int getMaxFewShotExamples() { return maxFewShotExamples; }
        public ModelConfiguration setMaxFewShotExamples(int maxFewShotExamples) { this.maxFewShotExamples = maxFewShotExamples; return this; }
        public boolean getStrictJsonMode() { return strictJsonMode; }
        public ModelConfiguration setStrictJsonMode(boolean strictJsonMode) { this.strictJsonMode = strictJsonMode; return this; }
        public int getMaxTokens() { return maxTokens; }
        public ModelConfiguration setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; return this; }
    }
    
    public static class FewShotExample {
        private final String input;
        private final String output;
        
        public FewShotExample(String input, String output) {
            this.input = input;
            this.output = output;
        }
        
        public String getInput() { return input; }
        public String getOutput() { return output; }
    }
    
    public static class PromptTemplate {
        private final String instructions;
        
        public PromptTemplate(String instructions) {
            this.instructions = instructions;
        }
        
        public String getInstructions() { return instructions; }
    }
    
    public static class PromptQualityScore {
        private final int score;
        private final List<String> issues;
        
        public PromptQualityScore(int score, List<String> issues) {
            this.score = score;
            this.issues = new ArrayList<>(issues);
        }
        
        public int getScore() { return score; }
        public List<String> getIssues() { return new ArrayList<>(issues); }
        public boolean isHighQuality() { return score >= 80; }
    }
    
    public static class PromptPerformanceMetrics {
        private final PromptType type;
        private final String modelType;
        private int totalAttempts = 0;
        private int successfulAttempts = 0;
        private long totalResponseTime = 0;
        private long bestResponseTime = Long.MAX_VALUE;
        private long worstResponseTime = 0;
        
        public PromptPerformanceMetrics(PromptType type, String modelType) {
            this.type = type;
            this.modelType = modelType;
        }
        
        public void addMeasurement(boolean success, long responseTime) {
            totalAttempts++;
            if (success) {
                successfulAttempts++;
            }
            
            totalResponseTime += responseTime;
            bestResponseTime = Math.min(bestResponseTime, responseTime);
            worstResponseTime = Math.max(worstResponseTime, responseTime);
        }
        
        public PromptType getType() { return type; }
        public String getModelType() { return modelType; }
        public int getTotalAttempts() { return totalAttempts; }
        public int getSuccessfulAttempts() { return successfulAttempts; }
        public double getSuccessRate() { return totalAttempts > 0 ? (double) successfulAttempts / totalAttempts : 0.0; }
        public double getAverageResponseTime() { return totalAttempts > 0 ? (double) totalResponseTime / totalAttempts : 0.0; }
        public long getBestResponseTime() { return bestResponseTime == Long.MAX_VALUE ? 0 : bestResponseTime; }
        public long getWorstResponseTime() { return worstResponseTime; }
    }
}