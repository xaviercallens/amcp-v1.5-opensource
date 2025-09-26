package io.amcp.tools;

import java.util.Map;
import java.util.HashMap;

/**
 * AMCP v1.4 Tool Response.
 * Represents the response from a tool connector execution.
 */
public class ToolResponse {
    private final boolean success;
    private final Object data;
    private final String errorMessage;
    private final String requestId;
    private final long executionTimeMs;
    private final Map<String, Object> metadata;
    
    private ToolResponse(boolean success, Object data, String errorMessage, String requestId, 
                        long executionTimeMs, Map<String, Object> metadata) {
        this.success = success;
        this.data = data;
        this.errorMessage = errorMessage;
        this.requestId = requestId;
        this.executionTimeMs = executionTimeMs;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }
    
    public static ToolResponse success(Object data, String requestId, long executionTimeMs) {
        return new ToolResponse(true, data, null, requestId, executionTimeMs, null);
    }
    
    public static ToolResponse success(Object data, String requestId, long executionTimeMs, 
                                     Map<String, Object> metadata) {
        return new ToolResponse(true, data, null, requestId, executionTimeMs, metadata);
    }
    
    public static ToolResponse error(String errorMessage, String requestId, long executionTimeMs) {
        return new ToolResponse(false, null, errorMessage, requestId, executionTimeMs, null);
    }
    
    public static ToolResponse error(String errorMessage, String requestId, long executionTimeMs,
                                   Map<String, Object> metadata) {
        return new ToolResponse(false, null, errorMessage, requestId, executionTimeMs, metadata);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public Object getData() {
        return data;
    }
    
    public <T> T getData(Class<T> type) {
        if (data != null && type.isAssignableFrom(data.getClass())) {
            return type.cast(data);
        }
        return null;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    @Override
    public String toString() {
        return "ToolResponse{" +
                "success=" + success +
                ", data=" + data +
                ", errorMessage='" + errorMessage + '\'' +
                ", requestId='" + requestId + '\'' +
                ", executionTimeMs=" + executionTimeMs +
                ", metadata=" + metadata +
                '}';
    }
}