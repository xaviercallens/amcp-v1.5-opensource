package io.amcp.tools;

import io.amcp.security.AuthenticationContext;
import java.util.Map;
import java.util.HashMap;

/**
 * AMCP v1.5 Enhanced Tool Request.
 * Represents a request to execute an operation using a tool connector with authentication support.
 */
public class ToolRequest {
    private final String operation;
    private final Map<String, Object> parameters;
    private final String requestId;
    private final long timestamp;
    private final AuthenticationContext authContext;
    
    public ToolRequest(String operation, Map<String, Object> parameters) {
        this(operation, parameters, generateRequestId(), null);
    }
    
    public ToolRequest(String operation, Map<String, Object> parameters, String requestId) {
        this(operation, parameters, requestId, null);
    }
    
    public ToolRequest(String operation, Map<String, Object> parameters, String requestId, 
                      AuthenticationContext authContext) {
        this.operation = operation;
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
        this.requestId = requestId;
        this.authContext = authContext;
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getOperation() {
        return operation;
    }
    
    public Map<String, Object> getParameters() {
        return new HashMap<>(parameters);
    }
    
    public <T> T getParameter(String key, Class<T> type) {
        Object value = parameters.get(key);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return type.cast(value);
        }
        return null;
    }
    
    public String getStringParameter(String key) {
        return getParameter(key, String.class);
    }
    
    public Integer getIntegerParameter(String key) {
        return getParameter(key, Integer.class);
    }
    
    public Boolean getBooleanParameter(String key) {
        return getParameter(key, Boolean.class);
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public AuthenticationContext getAuthContext() {
        return authContext;
    }
    
    public boolean hasAuthentication() {
        return authContext != null;
    }
    
    private static String generateRequestId() {
        return "req-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
    }
    
    @Override
    public String toString() {
        return "ToolRequest{" +
                "operation='" + operation + '\'' +
                ", parameters=" + parameters.size() + " params" +
                ", requestId='" + requestId + '\'' +
                ", authenticated=" + hasAuthentication() +
                ", timestamp=" + timestamp +
                '}';
    }
}