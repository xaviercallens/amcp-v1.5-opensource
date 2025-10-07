package io.amcp.connectors.ai.prompts;

import java.util.*;

/**
 * Centralized prompt library for LLM interactions.
 * Provides versioned, reusable prompts with best practices.
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class PromptLibrary {
    
    private static final String VERSION = "1.0.0";
    
    // Prompt templates indexed by name and version
    private final Map<String, Map<String, PromptTemplate>> prompts = new HashMap<>();
    
    public PromptLibrary() {
        initializePrompts();
    }
    
    /**
     * Gets a prompt template by name (latest version)
     */
    public PromptTemplate getPrompt(String name) {
        return getPrompt(name, null);
    }
    
    /**
     * Gets a prompt template by name and version
     */
    public PromptTemplate getPrompt(String name, String version) {
        Map<String, PromptTemplate> versions = prompts.get(name);
        if (versions == null) {
            return null;
        }
        
        if (version == null) {
            // Get latest version
            return versions.values().stream()
                .max(Comparator.comparing(p -> p.version))
                .orElse(null);
        }
        
        return versions.get(version);
    }
    
    /**
     * Renders a prompt with variables
     */
    public String renderPrompt(String name, Map<String, String> variables) {
        PromptTemplate template = getPrompt(name);
        if (template == null) {
            throw new IllegalArgumentException("Prompt not found: " + name);
        }
        
        return template.render(variables);
    }
    
    private void addPrompt(PromptTemplate template) {
        prompts.computeIfAbsent(template.name, k -> new HashMap<>())
               .put(template.version, template);
    }
    
    private void initializePrompts() {
        // Task Planning Prompt
        addPrompt(new PromptTemplate(
            "task_planning",
            "1.0.0",
            "Task planning for orchestration",
            buildTaskPlanningPrompt(),
            List.of("user_query", "available_agents"),
            Map.of("temperature", "0.3", "max_tokens", "1000")
        ));
        
        // Agent Selection Prompt
        addPrompt(new PromptTemplate(
            "agent_selection",
            "1.0.0",
            "Select appropriate agents for a task",
            buildAgentSelectionPrompt(),
            List.of("user_query", "agent_capabilities"),
            Map.of("temperature", "0.2", "max_tokens", "500")
        ));
        
        // Response Synthesis Prompt
        addPrompt(new PromptTemplate(
            "response_synthesis",
            "1.0.0",
            "Synthesize final response from agent outputs",
            buildResponseSynthesisPrompt(),
            List.of("user_query", "agent_responses"),
            Map.of("temperature", "0.5", "max_tokens", "800")
        ));
        
        // Error Recovery Prompt
        addPrompt(new PromptTemplate(
            "error_recovery",
            "1.0.0",
            "Generate fallback response for errors",
            buildErrorRecoveryPrompt(),
            List.of("user_query", "error_message"),
            Map.of("temperature", "0.4", "max_tokens", "400")
        ));
        
        // Chat Agent Prompts
        addPrompt(new PromptTemplate(
            "chat_empathetic",
            "1.0.0",
            "Empathetic chat response (2-3 sentences)",
            buildEmpatheticChatPrompt(),
            List.of("user_message", "mood"),
            Map.of("temperature", "0.7", "max_tokens", "150")
        ));
        
        addPrompt(new PromptTemplate(
            "chat_manager",
            "1.0.0",
            "Manager agent productivity advice",
            buildManagerPrompt(),
            List.of("user_query"),
            Map.of("temperature", "0.5", "max_tokens", "600")
        ));
        
        addPrompt(new PromptTemplate(
            "chat_tech",
            "1.0.0",
            "Tech agent technical advice",
            buildTechPrompt(),
            List.of("user_query"),
            Map.of("temperature", "0.4", "max_tokens", "600")
        ));
        
        addPrompt(new PromptTemplate(
            "chat_culture",
            "1.0.0",
            "Culture agent team morale advice",
            buildCulturePrompt(),
            List.of("user_query"),
            Map.of("temperature", "0.6", "max_tokens", "600")
        ));
    }
    
    private String buildTaskPlanningPrompt() {
        return """
            You are an AI orchestration planner. Your task is to break down a user query into actionable tasks.
            
            RESPOND ONLY with valid JSON. Do not include any explanatory text outside the JSON structure.
            
            User Query: {{user_query}}
            
            Available Agents: {{available_agents}}
            
            Create a task plan with the following structure:
            {
              "tasks": [
                {
                  "taskId": "unique_task_id",
                  "agentType": "agent_name",
                  "description": "what this task does",
                  "parameters": {
                    "key": "value"
                  },
                  "dependencies": ["task_id_1", "task_id_2"],
                  "priority": "high|medium|low"
                }
              ],
              "executionStrategy": "parallel|sequential|hybrid"
            }
            
            Guidelines:
            - Use clear, descriptive task IDs
            - Specify dependencies accurately
            - Prioritize tasks appropriately
            - Choose optimal execution strategy
            - Keep parameters minimal and relevant
            
            IMPORTANT: Output ONLY the JSON object, nothing else.
            """;
    }
    
    private String buildAgentSelectionPrompt() {
        return """
            You are an agent selection expert. Select the most appropriate agents for the given query.
            
            RESPOND ONLY with valid JSON.
            
            User Query: {{user_query}}
            
            Agent Capabilities:
            {{agent_capabilities}}
            
            Select agents and respond in this format:
            {
              "selectedAgents": [
                {
                  "agentType": "agent_name",
                  "confidence": 0.95,
                  "reasoning": "why this agent is suitable"
                }
              ],
              "executionOrder": ["agent1", "agent2"]
            }
            
            IMPORTANT: Output ONLY the JSON object.
            """;
    }
    
    private String buildResponseSynthesisPrompt() {
        return """
            You are a response synthesizer. Combine agent responses into a coherent final answer.
            
            User Query: {{user_query}}
            
            Agent Responses:
            {{agent_responses}}
            
            Create a natural, comprehensive response that:
            - Directly answers the user's question
            - Integrates information from all agents
            - Maintains a conversational tone
            - Is concise but complete
            - Highlights key insights
            
            Synthesized Response:
            """;
    }
    
    private String buildErrorRecoveryPrompt() {
        return """
            An error occurred while processing the user's request. Generate a helpful fallback response.
            
            User Query: {{user_query}}
            Error: {{error_message}}
            
            Provide a response that:
            - Acknowledges the issue professionally
            - Offers alternative suggestions if possible
            - Maintains a helpful tone
            - Is brief (2-3 sentences)
            
            Fallback Response:
            """;
    }
    
    private String buildEmpatheticChatPrompt() {
        return """
            You are an empathetic chat assistant. Respond with warmth and understanding.
            
            User Message: {{user_message}}
            Detected Mood: {{mood}}
            
            Guidelines:
            - Keep response to 2-3 sentences maximum
            - Be empathetic and supportive
            - Do NOT include famous quotes
            - Do NOT give lengthy advice
            - Focus on emotional support
            - Use a warm, conversational tone
            
            Response:
            """;
    }
    
    private String buildManagerPrompt() {
        return """
            You are a professional project manager focused on productivity and efficiency.
            
            User Query: {{user_query}}
            
            Provide practical management advice that:
            - Is actionable and specific
            - Focuses on productivity and organization
            - Uses frameworks (SMART goals, Eisenhower Matrix, etc.)
            - Is structured with clear sections
            - Includes pro tips
            
            Response:
            """;
    }
    
    private String buildTechPrompt() {
        return """
            You are a technical expert focused on tools, automation, and engineering excellence.
            
            User Query: {{user_query}}
            
            Provide technical advice that:
            - Is practical and implementation-focused
            - Includes specific tools and technologies
            - Follows best practices
            - Is structured with clear sections
            - Includes code examples if relevant
            
            Response:
            """;
    }
    
    private String buildCulturePrompt() {
        return """
            You are an empathetic culture advocate focused on team well-being and morale.
            
            User Query: {{user_query}}
            
            Provide culture-focused advice that:
            - Emphasizes empathy and support
            - Focuses on team building and morale
            - Includes practical initiatives
            - Is warm and encouraging
            - Is structured with clear sections
            
            Response:
            """;
    }
    
    /**
     * Prompt template with metadata
     */
    public static class PromptTemplate {
        public final String name;
        public final String version;
        public final String description;
        public final String template;
        public final List<String> requiredVariables;
        public final Map<String, String> modelConfig;
        
        public PromptTemplate(String name, String version, String description,
                            String template, List<String> requiredVariables,
                            Map<String, String> modelConfig) {
            this.name = name;
            this.version = version;
            this.description = description;
            this.template = template;
            this.requiredVariables = new ArrayList<>(requiredVariables);
            this.modelConfig = new HashMap<>(modelConfig);
        }
        
        /**
         * Renders the template with provided variables
         */
        public String render(Map<String, String> variables) {
            String rendered = template;
            
            // Check required variables
            for (String required : requiredVariables) {
                if (!variables.containsKey(required)) {
                    throw new IllegalArgumentException("Missing required variable: " + required);
                }
            }
            
            // Replace variables
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                rendered = rendered.replace(placeholder, entry.getValue());
            }
            
            return rendered;
        }
        
        /**
         * Gets model configuration value
         */
        public String getModelConfig(String key) {
            return modelConfig.get(key);
        }
        
        /**
         * Gets temperature setting
         */
        public double getTemperature() {
            String temp = modelConfig.get("temperature");
            return temp != null ? Double.parseDouble(temp) : 0.7;
        }
        
        /**
         * Gets max tokens setting
         */
        public int getMaxTokens() {
            String tokens = modelConfig.get("max_tokens");
            return tokens != null ? Integer.parseInt(tokens) : 500;
        }
    }
}
