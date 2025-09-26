package io.amcp.tools;

import java.util.Map;
import java.util.HashMap;

/**
 * AMCP v1.4 Tool Request.
 * Represents a request to execute an operation using a tool connector.
 */
public class ToolRequest {
    private final String operation;
    private final Map<String, Object> parameters;
    private final String requestId;
    private final long timestamp;
    
    public ToolRequest(String operation, Map<String, Object> parameters) {
        this(operation, parameters, generateRequestId());
    }
    
    public ToolRequest(String operation, Map<String, Object> parameters, String requestId) {
        this.operation = operation;
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
        this.requestId = requestId;
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
    
    private static String generateRequestId() {
        return "req_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
    
    @Override
    public String toString() {
        return "ToolRequest{" +
                "operation='" + operation + '\'' +
                ", parameters=" + parameters +
                ", requestId='" + requestId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}