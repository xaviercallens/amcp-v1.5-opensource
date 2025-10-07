package io.amcp.connectors.ai.validation;

import java.util.*;

/**
 * CloudEvents 1.0 specification schema validator.
 * Validates CloudEvents structure and required fields before dispatch.
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class CloudEventsSchemaValidator {
    
    // CloudEvents 1.0 required attributes
    private static final Set<String> REQUIRED_ATTRIBUTES = Set.of(
        "id", "source", "specversion", "type"
    );
    
    // CloudEvents 1.0 optional attributes
    private static final Set<String> OPTIONAL_ATTRIBUTES = Set.of(
        "datacontenttype", "dataschema", "subject", "time"
    );
    
    // Valid specversion values
    private static final Set<String> VALID_SPECVERSIONS = Set.of("1.0");
    
    // Agent-specific schema definitions
    private final Map<String, AgentSchema> agentSchemas = new HashMap<>();
    
    public CloudEventsSchemaValidator() {
        initializeAgentSchemas();
    }
    
    /**
     * Validates a CloudEvent structure
     */
    public ValidationResult validateCloudEvent(Map<String, Object> event) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (event == null) {
            errors.add("Event cannot be null");
            return new ValidationResult(false, errors, warnings);
        }
        
        // Validate required attributes
        for (String required : REQUIRED_ATTRIBUTES) {
            if (!event.containsKey(required) || event.get(required) == null) {
                errors.add("Missing required attribute: " + required);
            }
        }
        
        // Validate specversion
        Object specversion = event.get("specversion");
        if (specversion != null && !VALID_SPECVERSIONS.contains(specversion.toString())) {
            errors.add("Invalid specversion: " + specversion + ". Must be one of: " + VALID_SPECVERSIONS);
        }
        
        // Validate source format (should be URI)
        Object source = event.get("source");
        if (source != null && !isValidUri(source.toString())) {
            warnings.add("Source should be a valid URI: " + source);
        }
        
        // Validate type format (reverse-DNS notation recommended)
        Object type = event.get("type");
        if (type != null && !isValidEventType(type.toString())) {
            warnings.add("Event type should use reverse-DNS notation: " + type);
        }
        
        // Validate datacontenttype if present
        Object datacontenttype = event.get("datacontenttype");
        if (datacontenttype != null && !isValidContentType(datacontenttype.toString())) {
            warnings.add("Invalid datacontenttype: " + datacontenttype);
        }
        
        // Validate data payload if present
        if (event.containsKey("data")) {
            Object data = event.get("data");
            if (data == null) {
                warnings.add("Data attribute is present but null");
            }
        }
        
        boolean valid = errors.isEmpty();
        return new ValidationResult(valid, errors, warnings);
    }
    
    /**
     * Validates event data payload against agent-specific schema
     */
    public ValidationResult validateAgentPayload(String agentType, Map<String, Object> payload) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        AgentSchema schema = agentSchemas.get(agentType);
        if (schema == null) {
            warnings.add("No schema defined for agent type: " + agentType);
            return new ValidationResult(true, errors, warnings);
        }
        
        // Validate required fields
        for (String required : schema.requiredFields) {
            if (!payload.containsKey(required) || payload.get(required) == null) {
                errors.add("Missing required field for " + agentType + ": " + required);
            }
        }
        
        // Validate field types
        for (Map.Entry<String, Class<?>> entry : schema.fieldTypes.entrySet()) {
            String field = entry.getKey();
            Class<?> expectedType = entry.getValue();
            
            if (payload.containsKey(field)) {
                Object value = payload.get(field);
                if (value != null && !expectedType.isInstance(value)) {
                    errors.add("Invalid type for field " + field + ": expected " + 
                              expectedType.getSimpleName() + ", got " + 
                              value.getClass().getSimpleName());
                }
            }
        }
        
        boolean valid = errors.isEmpty();
        return new ValidationResult(valid, errors, warnings);
    }
    
    /**
     * Validates complete CloudEvent with agent-specific payload
     */
    public ValidationResult validateComplete(Map<String, Object> event, String agentType) {
        // Validate CloudEvent structure
        ValidationResult eventResult = validateCloudEvent(event);
        
        // Validate payload if present
        Object data = event.get("data");
        if (data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) data;
            ValidationResult payloadResult = validateAgentPayload(agentType, payload);
            
            // Combine results
            List<String> allErrors = new ArrayList<>(eventResult.errors);
            allErrors.addAll(payloadResult.errors);
            
            List<String> allWarnings = new ArrayList<>(eventResult.warnings);
            allWarnings.addAll(payloadResult.warnings);
            
            return new ValidationResult(allErrors.isEmpty(), allErrors, allWarnings);
        }
        
        return eventResult;
    }
    
    /**
     * Enforces strict schema validation (rejects on warnings too)
     */
    public ValidationResult validateStrict(Map<String, Object> event, String agentType) {
        ValidationResult result = validateComplete(event, agentType);
        
        if (!result.warnings.isEmpty()) {
            List<String> allErrors = new ArrayList<>(result.errors);
            allErrors.addAll(result.warnings);
            return new ValidationResult(false, allErrors, new ArrayList<>());
        }
        
        return result;
    }
    
    private boolean isValidUri(String uri) {
        // Simple URI validation
        return uri != null && (uri.startsWith("http://") || 
                               uri.startsWith("https://") || 
                               uri.startsWith("urn:") ||
                               uri.contains("://"));
    }
    
    private boolean isValidEventType(String type) {
        // Check for reverse-DNS notation (e.g., io.amcp.orchestration.task.request)
        return type != null && type.matches("^[a-z][a-z0-9]*\\.[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$");
    }
    
    private boolean isValidContentType(String contentType) {
        // Basic content type validation
        return contentType != null && contentType.matches("^[a-z]+/[a-z0-9+.-]+$");
    }
    
    private void initializeAgentSchemas() {
        // WeatherAgent schema
        AgentSchema weatherSchema = new AgentSchema("WeatherAgent");
        weatherSchema.addRequiredField("location", String.class);
        weatherSchema.addOptionalField("query", String.class);
        weatherSchema.addOptionalField("parameters", Map.class);
        agentSchemas.put("WeatherAgent", weatherSchema);
        
        // TravelPlannerAgent schema
        AgentSchema travelSchema = new AgentSchema("TravelPlannerAgent");
        travelSchema.addRequiredField("query", String.class);
        travelSchema.addOptionalField("taskType", String.class);
        travelSchema.addOptionalField("parameters", Map.class);
        agentSchemas.put("TravelPlannerAgent", travelSchema);
        
        // ChatAgent schema
        AgentSchema chatSchema = new AgentSchema("ChatAgent");
        chatSchema.addRequiredField("query", String.class);
        chatSchema.addOptionalField("conversationId", String.class);
        chatSchema.addOptionalField("priorMessages", List.class);
        agentSchemas.put("ChatAgent", chatSchema);
        agentSchemas.put("ManagerAgent", chatSchema);
        agentSchemas.put("TechAgent", chatSchema);
        agentSchemas.put("CultureAgent", chatSchema);
        
        // QuoteAgent schema
        AgentSchema quoteSchema = new AgentSchema("QuoteAgent");
        quoteSchema.addRequiredField("query", String.class);
        quoteSchema.addOptionalField("category", String.class);
        agentSchemas.put("QuoteAgent", quoteSchema);
    }
    
    /**
     * Agent-specific schema definition
     */
    private static class AgentSchema {
        public final String agentType;
        public final Set<String> requiredFields = new HashSet<>();
        public final Map<String, Class<?>> fieldTypes = new HashMap<>();
        
        public AgentSchema(String agentType) {
            this.agentType = agentType;
        }
        
        public void addRequiredField(String field, Class<?> type) {
            requiredFields.add(field);
            fieldTypes.put(field, type);
        }
        
        public void addOptionalField(String field, Class<?> type) {
            fieldTypes.put(field, type);
        }
    }
    
    /**
     * Validation result with errors and warnings
     */
    public static class ValidationResult {
        public final boolean valid;
        public final List<String> errors;
        public final List<String> warnings;
        
        public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = new ArrayList<>(errors);
            this.warnings = new ArrayList<>(warnings);
        }
        
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
        
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
        
        public String getErrorMessage() {
            if (errors.isEmpty()) {
                return null;
            }
            return String.join("; ", errors);
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ValidationResult[valid=").append(valid);
            if (!errors.isEmpty()) {
                sb.append(", errors=").append(errors);
            }
            if (!warnings.isEmpty()) {
                sb.append(", warnings=").append(warnings);
            }
            sb.append("]");
            return sb.toString();
        }
    }
}
