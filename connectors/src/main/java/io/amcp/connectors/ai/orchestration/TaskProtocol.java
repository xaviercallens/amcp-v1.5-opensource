package io.amcp.connectors.ai.orchestration;

import io.amcp.core.Event;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Standardized Task Request/Response Protocol for AMCP v1.5 LLM Orchestration
 * 
 * This class defines the standardized message formats for agent-to-agent task delegation
 * and response handling in the AMCP orchestration system. All task communication follows
 * CloudEvents v1.0 compliance with AMCP-specific extensions.
 * 
 * Key Features:
 * - CloudEvents v1.0 compliant message structure
 * - Correlation ID tracking for request/response matching
 * - User context propagation for security and personalization
 * - Error handling and timeout management
 * - Task dependency and priority support
 * 
 * @author AMCP Team
 * @version 1.5.0
 * @since 2024-09-29
 */
public class TaskProtocol {

    // AMCP Topic constants for event routing
    public static final String TASK_REQUEST_TOPIC = "orchestrator.task.request";
    public static final String TASK_RESPONSE_TOPIC = "orchestrator.task.response";
    public static final String CAPABILITY_REGISTER_TOPIC = "orchestrator.capability.register";
    public static final String CAPABILITY_DISCOVER_TOPIC = "orchestrator.capability.discover";

    /**
     * Creates a standardized task request event for agent delegation
     * 
     * @param capability The capability being requested (e.g., "weather.get", "stock.query")
     * @param parameters Task-specific parameters
     * @param userContext User information and authorization context
     * @param correlationId Unique identifier linking this task to the original request
     * @param sourceAgent The agent making the request (typically OrchestratorAgent)
     * @param priority Task priority (0=low, 5=normal, 10=high)
     * @param timeoutMs Maximum time to wait for response in milliseconds
     * @return CloudEvents-compliant task request event
     */
    public static Event createTaskRequest(String capability, 
                                        Map<String, Object> parameters,
                                        UserContext userContext,
                                        String correlationId,
                                        String sourceAgent,
                                        int priority,
                                        long timeoutMs) {
        
        TaskRequestData taskData = new TaskRequestData(
            capability, parameters, userContext, priority, timeoutMs,
            System.currentTimeMillis()
        );
        
        return Event.builder()
            .topic(TASK_REQUEST_TOPIC)
            .payload(taskData.toMap())
            .correlationId(correlationId)
            .metadata("capability", capability)
            .metadata("priority", String.valueOf(priority))
            .metadata("timeout", String.valueOf(timeoutMs))
            .metadata("userAgent", userContext.getUserId())
            .build();
    }

    /**
     * Creates a standardized task response event for result reporting
     * 
     * @param originalRequest The original task request event
     * @param success Whether the task completed successfully
     * @param result Task result data (null if error)
     * @param error Error information (null if success)
     * @param respondingAgent The agent providing the response
     * @param executionTimeMs Time taken to execute the task
     * @return CloudEvents-compliant task response event
     */
    public static Event createTaskResponse(Event originalRequest,
                                         boolean success,
                                         Object result,
                                         TaskError error,
                                         String respondingAgent,
                                         long executionTimeMs) {
        
        String capability = (String) originalRequest.getMetadata().get("capability");
        String correlationId = originalRequest.getCorrelationId();
        
        TaskResponseData responseData = new TaskResponseData(
            capability, success, result, error, executionTimeMs,
            System.currentTimeMillis()
        );
        
        return Event.builder()
            .topic(TASK_RESPONSE_TOPIC)
            .payload(responseData.toMap())
            .correlationId(correlationId)
            .metadata("capability", capability)
            .metadata("success", String.valueOf(success))
            .metadata("executionTime", String.valueOf(executionTimeMs))
            .build();
    }

    /**
     * Creates a capability registration event for agent discovery
     * 
     * @param agentId The agent's unique identifier
     * @param agentType The type/class of the agent
     * @param capabilities List of capabilities this agent provides
     * @param description Human-readable description of the agent
     * @param endpoint Communication endpoint/topic for this agent
     * @param metadata Additional agent metadata
     * @return CloudEvents-compliant capability registration event
     */
    public static Event createCapabilityRegistration(String agentId,
                                                   String agentType,
                                                   List<String> capabilities,
                                                   String description,
                                                   String endpoint,
                                                   Map<String, Object> metadata) {
        
        CapabilityRegistration registration = new CapabilityRegistration(
            agentId, agentType, capabilities, description, endpoint, metadata
        );
        
        return Event.builder()
            .topic(CAPABILITY_REGISTER_TOPIC)
            .payload(registration.toMap())
            .correlationId(agentId + "-" + System.currentTimeMillis())
            .metadata("agentType", agentType)
            .metadata("capabilityCount", String.valueOf(capabilities.size()))
            .build();
    }

    /**
     * Creates a capability discovery request event
     * 
     * @param requestingAgent The agent requesting capability information
     * @param capabilityQuery Optional filter for specific capabilities
     * @param correlationId Unique identifier for tracking the response
     * @return CloudEvents-compliant capability discovery event
     */
    public static Event createCapabilityDiscovery(String requestingAgent,
                                                String capabilityQuery,
                                                String correlationId) {
        
        Map<String, Object> queryData = new HashMap<>();
        queryData.put("requestingAgent", requestingAgent);
        queryData.put("query", capabilityQuery);
        queryData.put("timestamp", System.currentTimeMillis());
        
        return Event.builder()
            .topic(CAPABILITY_DISCOVER_TOPIC)
            .payload(queryData)
            .correlationId(correlationId)
            .metadata("requestingAgent", requestingAgent)
            .metadata("query", capabilityQuery)
            .build();
    }

    /**
     * Extracts and validates a TaskRequestData from an event payload
     */
    public static TaskRequestData parseTaskRequest(Event event) {
        if (!TASK_REQUEST_TOPIC.equals(event.getTopic())) {
            throw new IllegalArgumentException("Event is not a task request: " + event.getTopic());
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            return TaskRequestData.fromMap(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid task request payload", e);
        }
    }

    /**
     * Extracts and validates a TaskResponseData from an event payload
     */
    public static TaskResponseData parseTaskResponse(Event event) {
        if (!TASK_RESPONSE_TOPIC.equals(event.getTopic())) {
            throw new IllegalArgumentException("Event is not a task response: " + event.getTopic());
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            return TaskResponseData.fromMap(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid task response payload", e);
        }
    }

    /**
     * User context information for security and personalization
     */
    public static class UserContext {
        @JsonProperty("userId")
        private final String userId;
        
        @JsonProperty("sessionId")
        private final String sessionId;
        
        @JsonProperty("roles")
        private final List<String> roles;
        
        @JsonProperty("permissions")
        private final List<String> permissions;
        
        @JsonProperty("metadata")
        private final Map<String, Object> metadata;

        public UserContext(String userId, String sessionId, List<String> roles, 
                          List<String> permissions, Map<String, Object> metadata) {
            this.userId = userId;
            this.sessionId = sessionId;
            this.roles = roles;
            this.permissions = permissions;
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }

        // Getters
        public String getUserId() { return userId; }
        public String getSessionId() { return sessionId; }
        public List<String> getRoles() { return roles; }
        public List<String> getPermissions() { return permissions; }
        public Map<String, Object> getMetadata() { return metadata; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", userId);
            map.put("sessionId", sessionId);
            map.put("roles", roles);
            map.put("permissions", permissions);
            map.put("metadata", metadata);
            return map;
        }

        @SuppressWarnings("unchecked")
        public static UserContext fromMap(Map<String, Object> map) {
            return new UserContext(
                (String) map.get("userId"),
                (String) map.get("sessionId"),
                (List<String>) map.get("roles"),
                (List<String>) map.get("permissions"),
                (Map<String, Object>) map.get("metadata")
            );
        }
    }

    /**
     * Task request data structure
     */
    public static class TaskRequestData {
        @JsonProperty("capability")
        private final String capability;
        
        @JsonProperty("parameters")
        private final Map<String, Object> parameters;
        
        @JsonProperty("userContext")
        private final UserContext userContext;
        
        @JsonProperty("priority")
        private final int priority;
        
        @JsonProperty("timeoutMs")
        private final long timeoutMs;
        
        @JsonProperty("timestamp")
        private final long timestamp;

        public TaskRequestData(String capability, Map<String, Object> parameters,
                              UserContext userContext, int priority, long timeoutMs, long timestamp) {
            this.capability = capability;
            this.parameters = parameters != null ? parameters : new HashMap<>();
            this.userContext = userContext;
            this.priority = priority;
            this.timeoutMs = timeoutMs;
            this.timestamp = timestamp;
        }

        // Getters
        public String getCapability() { return capability; }
        public Map<String, Object> getParameters() { return parameters; }
        public UserContext getUserContext() { return userContext; }
        public int getPriority() { return priority; }
        public long getTimeoutMs() { return timeoutMs; }
        public long getTimestamp() { return timestamp; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("capability", capability);
            map.put("parameters", parameters);
            map.put("userContext", userContext.toMap());
            map.put("priority", priority);
            map.put("timeoutMs", timeoutMs);
            map.put("timestamp", timestamp);
            return map;
        }

        @SuppressWarnings("unchecked")
        public static TaskRequestData fromMap(Map<String, Object> map) {
            return new TaskRequestData(
                (String) map.get("capability"),
                (Map<String, Object>) map.get("parameters"),
                UserContext.fromMap((Map<String, Object>) map.get("userContext")),
                ((Number) map.get("priority")).intValue(),
                ((Number) map.get("timeoutMs")).longValue(),
                ((Number) map.get("timestamp")).longValue()
            );
        }
    }

    /**
     * Task response data structure
     */
    public static class TaskResponseData {
        @JsonProperty("capability")
        private final String capability;
        
        @JsonProperty("success")
        private final boolean success;
        
        @JsonProperty("result")
        private final Object result;
        
        @JsonProperty("error")
        private final TaskError error;
        
        @JsonProperty("executionTimeMs")
        private final long executionTimeMs;
        
        @JsonProperty("timestamp")
        private final long timestamp;

        public TaskResponseData(String capability, boolean success, Object result,
                               TaskError error, long executionTimeMs, long timestamp) {
            this.capability = capability;
            this.success = success;
            this.result = result;
            this.error = error;
            this.executionTimeMs = executionTimeMs;
            this.timestamp = timestamp;
        }

        // Getters
        public String getCapability() { return capability; }
        public boolean isSuccess() { return success; }
        public Object getResult() { return result; }
        public TaskError getError() { return error; }
        public long getExecutionTimeMs() { return executionTimeMs; }
        public long getTimestamp() { return timestamp; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("capability", capability);
            map.put("success", success);
            map.put("result", result);
            map.put("error", error != null ? error.toMap() : null);
            map.put("executionTimeMs", executionTimeMs);
            map.put("timestamp", timestamp);
            return map;
        }

        @SuppressWarnings("unchecked")
        public static TaskResponseData fromMap(Map<String, Object> map) {
            Map<String, Object> errorMap = (Map<String, Object>) map.get("error");
            TaskError error = errorMap != null ? TaskError.fromMap(errorMap) : null;
            
            return new TaskResponseData(
                (String) map.get("capability"),
                (Boolean) map.get("success"),
                map.get("result"),
                error,
                ((Number) map.get("executionTimeMs")).longValue(),
                ((Number) map.get("timestamp")).longValue()
            );
        }
    }

    /**
     * Task error information
     */
    public static class TaskError {
        @JsonProperty("code")
        private final String code;
        
        @JsonProperty("message")
        private final String message;
        
        @JsonProperty("details")
        private final Map<String, Object> details;

        public TaskError(String code, String message, Map<String, Object> details) {
            this.code = code;
            this.message = message;
            this.details = details != null ? details : new HashMap<>();
        }

        // Getters
        public String getCode() { return code; }
        public String getMessage() { return message; }
        public Map<String, Object> getDetails() { return details; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("code", code);
            map.put("message", message);
            map.put("details", details);
            return map;
        }

        @SuppressWarnings("unchecked")
        public static TaskError fromMap(Map<String, Object> map) {
            return new TaskError(
                (String) map.get("code"),
                (String) map.get("message"),
                (Map<String, Object>) map.get("details")
            );
        }

        // Common error codes
        public static TaskError timeout(String message) {
            return new TaskError("TASK_TIMEOUT", message, Map.of("category", "timeout"));
        }

        public static TaskError unauthorized(String message) {
            return new TaskError("UNAUTHORIZED", message, Map.of("category", "security"));
        }

        public static TaskError agentUnavailable(String agentId) {
            return new TaskError("AGENT_UNAVAILABLE", "Agent not available: " + agentId, 
                Map.of("category", "routing", "agentId", agentId));
        }

        public static TaskError invalidParameters(String message) {
            return new TaskError("INVALID_PARAMETERS", message, Map.of("category", "validation"));
        }

        public static TaskError executionFailed(String message, Throwable cause) {
            Map<String, Object> details = new HashMap<>();
            details.put("category", "execution");
            if (cause != null) {
                details.put("cause", cause.getClass().getSimpleName());
                details.put("causeMessage", cause.getMessage());
            }
            return new TaskError("EXECUTION_FAILED", message, details);
        }
    }

    /**
     * Capability registration data structure
     */
    public static class CapabilityRegistration {
        @JsonProperty("agentId")
        private final String agentId;
        
        @JsonProperty("agentType")
        private final String agentType;
        
        @JsonProperty("capabilities")
        private final List<String> capabilities;
        
        @JsonProperty("description")
        private final String description;
        
        @JsonProperty("endpoint")
        private final String endpoint;
        
        @JsonProperty("metadata")
        private final Map<String, Object> metadata;

        public CapabilityRegistration(String agentId, String agentType, List<String> capabilities,
                                    String description, String endpoint, Map<String, Object> metadata) {
            this.agentId = agentId;
            this.agentType = agentType;
            this.capabilities = capabilities;
            this.description = description;
            this.endpoint = endpoint;
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }

        // Getters
        public String getAgentId() { return agentId; }
        public String getAgentType() { return agentType; }
        public List<String> getCapabilities() { return capabilities; }
        public String getDescription() { return description; }
        public String getEndpoint() { return endpoint; }
        public Map<String, Object> getMetadata() { return metadata; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("agentId", agentId);
            map.put("agentType", agentType);
            map.put("capabilities", capabilities);
            map.put("description", description);
            map.put("endpoint", endpoint);
            map.put("metadata", metadata);
            return map;
        }

        @SuppressWarnings("unchecked")
        public static CapabilityRegistration fromMap(Map<String, Object> map) {
            return new CapabilityRegistration(
                (String) map.get("agentId"),
                (String) map.get("agentType"),
                (List<String>) map.get("capabilities"),
                (String) map.get("description"),
                (String) map.get("endpoint"),
                (Map<String, Object>) map.get("metadata")
            );
        }
    }
}