package io.amcp.connectors.ai.enhanced;

import io.amcp.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Enhanced Agent Processing Framework for AMCP v1.5 Enterprise Edition
 * 
 * Provides advanced agent-side capabilities including:
 * - Structured JSON payload parsing and validation
 * - Standardized response formatting with CloudEvents compliance
 * - Multi-turn interaction support with conversation state
 * - Enhanced error handling and recovery mechanisms
 * - Parameter extraction and validation from natural language
 * - Response metadata and performance tracking
 * 
 * This abstract base class should be extended by all enhanced agents
 * to provide consistent processing capabilities across the agent mesh.
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 * @since 1.5.0
 */
public abstract class EnhancedAgentBase implements Agent {
    
    // Core agent properties
    private final AgentID agentId;
    private AgentContext context;
    private AgentLifecycle lifecycleState = AgentLifecycle.INACTIVE;
    private final Set<String> subscriptions = new CopyOnWriteArraySet<>();
    
    // JSON processing
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Response templates and formatting
    private static final String RESPONSE_TEMPLATE_SUCCESS = "✅ %s";
    private static final String RESPONSE_TEMPLATE_ERROR = "❌ %s";
    private static final String RESPONSE_TEMPLATE_INFO = "ℹ️ %s";
    private static final String RESPONSE_TEMPLATE_WARNING = "⚠️ %s";
    
    // Conversation state management
    private final Map<String, ConversationContext> activeConversations = new ConcurrentHashMap<>();
    private final Map<String, ResponseMetadata> responseMetrics = new ConcurrentHashMap<>();
    
    // Agent capabilities
    private final Set<String> supportedCapabilities = new HashSet<>();
    private final Map<String, ParameterSchema> parameterSchemas = new HashMap<>();
    
    protected EnhancedAgentBase(AgentID agentId) {
        this.agentId = agentId;
        initializeCapabilities();
        initializeParameterSchemas();
    }
    
    // Agent interface implementations
    
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
        return lifecycleState;
    }
    
    @Override
    public void onActivate() {
        lifecycleState = AgentLifecycle.ACTIVE;
        logMessage("Enhanced agent activated with capabilities: " + supportedCapabilities);
    }
    
    @Override
    public void onDeactivate() {
        lifecycleState = AgentLifecycle.INACTIVE;
        subscriptions.clear();
        activeConversations.clear();
        logMessage("Enhanced agent deactivated");
    }
    
    @Override
    public void onDestroy() {
        lifecycleState = AgentLifecycle.DESTROYED;
        subscriptions.clear();
        activeConversations.clear();
        responseMetrics.clear();
        logMessage("Enhanced agent destroyed");
    }
    
    @Override
    public void onBeforeMigration(String destinationContext) {
        logMessage("Preparing for migration to: " + destinationContext);
    }
    
    @Override
    public void onAfterMigration(String sourceContext) {
        logMessage("Completed migration from: " + sourceContext);
    }
    
    @Override
    public CompletableFuture<Void> subscribe(String topicPattern) {
        subscriptions.add(topicPattern);
        if (context != null) {
            return context.subscribe(agentId, topicPattern);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> unsubscribe(String topicPattern) {
        subscriptions.remove(topicPattern);
        if (context != null) {
            return context.unsubscribe(agentId, topicPattern);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> publishEvent(Event event) {
        if (context != null) {
            return context.publishEvent(event);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Abstract method to initialize agent-specific capabilities
     */
    protected abstract void initializeCapabilities();
    
    /**
     * Abstract method to initialize parameter schemas for validation
     */
    protected abstract void initializeParameterSchemas();
    
    /**
     * Enhanced event handling with structured processing
     */
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            String correlationId = event.getCorrelationId();
            long startTime = System.currentTimeMillis();
            
            try {
                logMessage("Processing event: " + event.getTopic() + " [" + correlationId + "]");
                
                // Parse and validate incoming event
                EnhancedEventPayload payload = parseEventPayload(event);
                
                if (payload.isValid()) {
                    // Update conversation context
                    updateConversationContext(correlationId, payload);
                    
                    // Route to specific handler
                    handleEnhancedEvent(event, payload);
                    
                    // Track response metrics
                    recordResponseMetrics(correlationId, startTime, true, null);
                    
                } else {
                    // Send validation error response
                    sendValidationErrorResponse(event, payload.getValidationErrors());
                    recordResponseMetrics(correlationId, startTime, false, "Validation failed");
                }
                
            } catch (Exception e) {
                logMessage("Error handling event " + event.getTopic() + ": " + e.getMessage());
                sendErrorResponse(event, "Internal processing error: " + e.getMessage());
                recordResponseMetrics(correlationId, startTime, false, e.getMessage());
            }
        });
    }
    
    /**
     * Abstract method for handling validated enhanced events
     */
    protected abstract void handleEnhancedEvent(Event event, EnhancedEventPayload payload);
    
    /**
     * Parse and validate event payload with enhanced structure
     */
    protected EnhancedEventPayload parseEventPayload(Event event) {
        try {
            Object rawPayload = event.getPayload();
            
            if (rawPayload instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> payloadMap = (Map<String, Object>) rawPayload;
                return parseMapPayload(payloadMap, event.getTopic());
            } else if (rawPayload instanceof String) {
                return parseStringPayload((String) rawPayload, event.getTopic());
            } else {
                return new EnhancedEventPayload(false, Collections.singletonList("Unsupported payload type"));
            }
            
        } catch (Exception e) {
            return new EnhancedEventPayload(false, Collections.singletonList("Payload parsing error: " + e.getMessage()));
        }
    }
    
    /**
     * Parse map-based payload (structured format)
     */
    private EnhancedEventPayload parseMapPayload(Map<String, Object> payloadMap, String topic) {
        List<String> errors = new ArrayList<>();
        
        // Extract standard fields
        String query = (String) payloadMap.get("query");
        String capability = (String) payloadMap.get("capability");
        Object parametersObj = payloadMap.get("parameters");
        String responseFormat = (String) payloadMap.getOrDefault("responseFormat", "text");
        String conversationId = (String) payloadMap.get("conversationId");
        
        // Validate required fields
        if (query == null || query.trim().isEmpty()) {
            errors.add("Query is required");
        }
        
        // Parse and validate parameters
        Map<String, Object> parameters = new HashMap<>();
        if (parametersObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> paramMap = (Map<String, Object>) parametersObj;
            parameters.putAll(paramMap);
            
            // Validate against schema if capability is specified
            if (capability != null && parameterSchemas.containsKey(capability)) {
                List<String> schemaErrors = validateParameters(parameters, parameterSchemas.get(capability));
                errors.addAll(schemaErrors);
            }
        } else if (parametersObj != null) {
            // Try to extract parameters from query text
            parameters = extractParametersFromQuery(query);
        }
        
        // Check if agent supports the requested capability
        if (capability != null && !supportedCapabilities.contains(capability)) {
            errors.add("Unsupported capability: " + capability + ". Supported: " + supportedCapabilities);
        }
        
        boolean isValid = errors.isEmpty();
        EnhancedEventPayload payload = new EnhancedEventPayload(isValid, errors);
        
        if (isValid) {
            payload.setQuery(query);
            payload.setCapability(capability);
            payload.setParameters(parameters);
            payload.setResponseFormat(responseFormat);
            payload.setConversationId(conversationId);
            payload.setPayloadType(PayloadType.STRUCTURED);
        }
        
        return payload;
    }
    
    /**
     * Parse string-based payload (natural language format)
     */
    private EnhancedEventPayload parseStringPayload(String payload, String topic) {
        List<String> errors = new ArrayList<>();
        
        if (payload == null || payload.trim().isEmpty()) {
            errors.add("Empty payload");
            return new EnhancedEventPayload(false, errors);
        }
        
        // Extract information from natural language
        String query = payload.trim();
        String capability = inferCapabilityFromTopic(topic);
        Map<String, Object> parameters = extractParametersFromQuery(query);
        
        EnhancedEventPayload enhancedPayload = new EnhancedEventPayload(true, new ArrayList<>());
        enhancedPayload.setQuery(query);
        enhancedPayload.setCapability(capability);
        enhancedPayload.setParameters(parameters);
        enhancedPayload.setResponseFormat("text");
        enhancedPayload.setPayloadType(PayloadType.NATURAL_LANGUAGE);
        
        return enhancedPayload;
    }
    
    /**
     * Extract parameters from natural language query using patterns
     */
    protected abstract Map<String, Object> extractParametersFromQuery(String query);
    
    /**
     * Infer capability from event topic
     */
    private String inferCapabilityFromTopic(String topic) {
        // Map common topic patterns to capabilities
        if (topic.contains("weather")) return "weather.get";
        if (topic.contains("stock")) return "stock.price";
        if (topic.contains("travel")) return "travel.plan";
        if (topic.contains("chat")) return "chat.response";
        
        return "general.query";
    }
    
    /**
     * Validate parameters against schema
     */
    private List<String> validateParameters(Map<String, Object> parameters, ParameterSchema schema) {
        List<String> errors = new ArrayList<>();
        
        // Check required parameters
        for (String required : schema.getRequiredFields()) {
            if (!parameters.containsKey(required) || parameters.get(required) == null) {
                errors.add("Required parameter missing: " + required);
            }
        }
        
        // Validate parameter types and formats
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String paramName = entry.getKey();
            Object value = entry.getValue();
            
            if (schema.getFieldTypes().containsKey(paramName)) {
                String expectedType = schema.getFieldTypes().get(paramName);
                if (!isValidType(value, expectedType)) {
                    errors.add("Invalid type for parameter " + paramName + ": expected " + expectedType);
                }
            }
        }
        
        return errors;
    }
    
    /**
     * Check if value matches expected type
     */
    private boolean isValidType(Object value, String expectedType) {
        if (value == null) return false;
        
        switch (expectedType.toLowerCase()) {
            case "string":
                return value instanceof String;
            case "number":
                return value instanceof Number;
            case "boolean":
                return value instanceof Boolean;
            case "array":
                return value instanceof List || value instanceof Object[];
            case "object":
                return value instanceof Map;
            default:
                return true; // Unknown type, allow it
        }
    }
    
    /**
     * Update conversation context for multi-turn interactions
     */
    private void updateConversationContext(String correlationId, EnhancedEventPayload payload) {
        String conversationId = payload.getConversationId() != null 
            ? payload.getConversationId() 
            : correlationId;
        
        ConversationContext context = activeConversations.computeIfAbsent(conversationId,
            k -> new ConversationContext(conversationId));
        
        context.addInteraction(payload.getQuery(), LocalDateTime.now());
        
        // Clean up old conversations (keep last 100)
        if (activeConversations.size() > 100) {
            String oldestKey = activeConversations.entrySet().stream()
                .min(Map.Entry.comparingByValue((a, b) -> a.getLastInteraction().compareTo(b.getLastInteraction())))
                .map(Map.Entry::getKey)
                .orElse(null);
            
            if (oldestKey != null) {
                activeConversations.remove(oldestKey);
            }
        }
    }
    
    /**
     * Send enhanced response with CloudEvents compliance
     */
    protected void sendEnhancedResponse(Event originalEvent, String response, 
                                      ResponseType type, Map<String, Object> additionalData) {
        try {
            Map<String, Object> responsePayload = createResponsePayload(response, type, additionalData);
            
            Event responseEvent = Event.builder()
                .topic(generateResponseTopic(originalEvent.getTopic()))
                .payload(responsePayload)
                .correlationId(originalEvent.getCorrelationId())
                .sender(getAgentId())
                .metadata("ce-specversion", "1.0")
                .metadata("ce-type", "io.amcp.agent.response")
                .metadata("ce-source", "amcp://agents/" + getAgentId().getId())
                .metadata("ce-time", LocalDateTime.now().toString())
                .metadata("ce-datacontenttype", "application/json")
                .build();
            
            publishEvent(responseEvent);
            
            logMessage("Sent enhanced response [" + type + "] for correlation: " + 
                      originalEvent.getCorrelationId());
            
        } catch (Exception e) {
            logMessage("Error sending enhanced response: " + e.getMessage());
        }
    }
    
    /**
     * Create standardized response payload
     */
    private Map<String, Object> createResponsePayload(String response, ResponseType type, 
                                                     Map<String, Object> additionalData) {
        Map<String, Object> payload = new HashMap<>();
        
        // Standard response fields
        payload.put("response", formatResponse(response, type));
        payload.put("agentId", getAgentId().getId());
        payload.put("timestamp", LocalDateTime.now().toString());
        payload.put("responseType", type.toString());
        payload.put("status", type == ResponseType.ERROR ? "error" : "success");
        
        // Add additional data if provided
        if (additionalData != null) {
            payload.put("data", additionalData);
        }
        
        // Add performance metrics
        payload.put("metadata", createResponseMetadata());
        
        return payload;
    }
    
    /**
     * Format response with appropriate emoji and styling
     */
    private String formatResponse(String response, ResponseType type) {
        switch (type) {
            case SUCCESS:
                return String.format(RESPONSE_TEMPLATE_SUCCESS, response);
            case ERROR:
                return String.format(RESPONSE_TEMPLATE_ERROR, response);
            case INFO:
                return String.format(RESPONSE_TEMPLATE_INFO, response);
            case WARNING:
                return String.format(RESPONSE_TEMPLATE_WARNING, response);
            default:
                return response;
        }
    }
    
    /**
     * Generate response topic based on original topic
     */
    private String generateResponseTopic(String originalTopic) {
        if (originalTopic.contains(".request")) {
            return originalTopic.replace(".request", ".response");
        }
        
        // Default response topic pattern
        return "agent.response." + getAgentId().getId().toLowerCase();
    }
    
    /**
     * Create response metadata for observability
     */
    private Map<String, Object> createResponseMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("agentVersion", "1.5.0");
        metadata.put("processingTime", System.currentTimeMillis());
        metadata.put("capabilities", new ArrayList<>(supportedCapabilities));
        
        return metadata;
    }
    
    /**
     * Send validation error response
     */
    private void sendValidationErrorResponse(Event originalEvent, List<String> errors) {
        String errorMessage = "Validation failed: " + String.join(", ", errors);
        sendEnhancedResponse(originalEvent, errorMessage, ResponseType.ERROR, 
                           Map.of("validationErrors", errors));
    }
    
    /**
     * Send general error response
     */
    protected void sendErrorResponse(Event originalEvent, String errorMessage) {
        sendEnhancedResponse(originalEvent, errorMessage, ResponseType.ERROR, null);
    }
    
    /**
     * Record response metrics for monitoring
     */
    private void recordResponseMetrics(String correlationId, long startTime, 
                                     boolean success, String errorMessage) {
        long duration = System.currentTimeMillis() - startTime;
        
        ResponseMetadata metrics = new ResponseMetadata(
            correlationId, startTime, duration, success, errorMessage);
        
        responseMetrics.put(correlationId, metrics);
        
        // Clean up old metrics (keep last 1000)
        if (responseMetrics.size() > 1000) {
            String oldestKey = responseMetrics.entrySet().stream()
                .min(Map.Entry.comparingByValue((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp())))
                .map(Map.Entry::getKey)
                .orElse(null);
            
            if (oldestKey != null) {
                responseMetrics.remove(oldestKey);
            }
        }
    }
    
    /**
     * Get conversation context for multi-turn interactions
     */
    protected ConversationContext getConversationContext(String conversationId) {
        return activeConversations.get(conversationId);
    }
    
    /**
     * Get agent performance metrics
     */
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        long totalRequests = responseMetrics.size();
        long successfulRequests = responseMetrics.values().stream()
            .mapToLong(m -> m.isSuccess() ? 1 : 0)
            .sum();
        
        double averageResponseTime = responseMetrics.values().stream()
            .mapToLong(ResponseMetadata::getDuration)
            .average()
            .orElse(0.0);
        
        metrics.put("totalRequests", totalRequests);
        metrics.put("successfulRequests", successfulRequests);
        metrics.put("successRate", totalRequests > 0 ? (double) successfulRequests / totalRequests : 0.0);
        metrics.put("averageResponseTime", averageResponseTime);
        metrics.put("activeConversations", activeConversations.size());
        
        return metrics;
    }
    
    /**
     * Add supported capability
     */
    protected void addCapability(String capability) {
        supportedCapabilities.add(capability);
    }
    
    /**
     * Add parameter schema for validation
     */
    protected void addParameterSchema(String capability, ParameterSchema schema) {
        parameterSchemas.put(capability, schema);
    }
    
    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [" + getClass().getSimpleName() + "] " + message);
    }
    
    // Supporting classes and enums
    
    public enum ResponseType {
        SUCCESS, ERROR, INFO, WARNING
    }
    
    public enum PayloadType {
        STRUCTURED, NATURAL_LANGUAGE
    }
    
    public static class EnhancedEventPayload {
        private boolean valid;
        private List<String> validationErrors;
        private String query;
        private String capability;
        private Map<String, Object> parameters;
        private String responseFormat;
        private String conversationId;
        private PayloadType payloadType;
        
        public EnhancedEventPayload(boolean valid, List<String> validationErrors) {
            this.valid = valid;
            this.validationErrors = new ArrayList<>(validationErrors);
            this.parameters = new HashMap<>();
        }
        
        // Getters and setters
        public boolean isValid() { return valid; }
        public List<String> getValidationErrors() { return new ArrayList<>(validationErrors); }
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public String getCapability() { return capability; }
        public void setCapability(String capability) { this.capability = capability; }
        public Map<String, Object> getParameters() { return new HashMap<>(parameters); }
        public void setParameters(Map<String, Object> parameters) { this.parameters = new HashMap<>(parameters); }
        public String getResponseFormat() { return responseFormat; }
        public void setResponseFormat(String responseFormat) { this.responseFormat = responseFormat; }
        public String getConversationId() { return conversationId; }
        public void setConversationId(String conversationId) { this.conversationId = conversationId; }
        public PayloadType getPayloadType() { return payloadType; }
        public void setPayloadType(PayloadType payloadType) { this.payloadType = payloadType; }
    }
    
    public static class ConversationContext {
        private final String conversationId;
        private final List<ConversationInteraction> interactions;
        private LocalDateTime lastInteraction;
        
        public ConversationContext(String conversationId) {
            this.conversationId = conversationId;
            this.interactions = new ArrayList<>();
            this.lastInteraction = LocalDateTime.now();
        }
        
        public void addInteraction(String query, LocalDateTime timestamp) {
            interactions.add(new ConversationInteraction(query, timestamp));
            this.lastInteraction = timestamp;
        }
        
        public String getConversationId() { return conversationId; }
        public List<ConversationInteraction> getInteractions() { return new ArrayList<>(interactions); }
        public LocalDateTime getLastInteraction() { return lastInteraction; }
        
        public List<String> getRecentQueries(int limit) {
            return interactions.stream()
                .map(ConversationInteraction::getQuery)
                .skip(Math.max(0, interactions.size() - limit))
                .collect(java.util.stream.Collectors.toList());
        }
    }
    
    public static class ConversationInteraction {
        private final String query;
        private final LocalDateTime timestamp;
        
        public ConversationInteraction(String query, LocalDateTime timestamp) {
            this.query = query;
            this.timestamp = timestamp;
        }
        
        public String getQuery() { return query; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    public static class ParameterSchema {
        private final Set<String> requiredFields;
        private final Map<String, String> fieldTypes;
        private final Map<String, String> fieldDescriptions;
        
        public ParameterSchema() {
            this.requiredFields = new HashSet<>();
            this.fieldTypes = new HashMap<>();
            this.fieldDescriptions = new HashMap<>();
        }
        
        public ParameterSchema addRequired(String field, String type, String description) {
            requiredFields.add(field);
            fieldTypes.put(field, type);
            fieldDescriptions.put(field, description);
            return this;
        }
        
        public ParameterSchema addOptional(String field, String type, String description) {
            fieldTypes.put(field, type);
            fieldDescriptions.put(field, description);
            return this;
        }
        
        public Set<String> getRequiredFields() { return new HashSet<>(requiredFields); }
        public Map<String, String> getFieldTypes() { return new HashMap<>(fieldTypes); }
        public Map<String, String> getFieldDescriptions() { return new HashMap<>(fieldDescriptions); }
    }
    
    public static class ResponseMetadata {
        private final String correlationId;
        private final long timestamp;
        private final long duration;
        private final boolean success;
        private final String errorMessage;
        
        public ResponseMetadata(String correlationId, long timestamp, long duration, 
                              boolean success, String errorMessage) {
            this.correlationId = correlationId;
            this.timestamp = timestamp;
            this.duration = duration;
            this.success = success;
            this.errorMessage = errorMessage;
        }
        
        public String getCorrelationId() { return correlationId; }
        public long getTimestamp() { return timestamp; }
        public long getDuration() { return duration; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }
}