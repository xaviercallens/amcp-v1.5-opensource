package io.amcp.connectors.a2a;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * A2A (Agent-to-Agent) response for Google A2A protocol integration.
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 * @since 1.5.0
 */
public class A2AResponse {
    
    private final String requestId;
    private final String status;
    private final String message;
    private final Object data;
    private final OffsetDateTime timestamp;
    private final Map<String, Object> metadata;
    
    @JsonCreator
    public A2AResponse(
            @JsonProperty("requestId") String requestId,
            @JsonProperty("status") String status,
            @JsonProperty("message") String message,
            @JsonProperty("data") Object data,
            @JsonProperty("timestamp") OffsetDateTime timestamp,
            @JsonProperty("metadata") Map<String, Object> metadata) {
        this.requestId = requestId;
        this.status = status;
        this.message = message;
        this.data = data;
        this.timestamp = timestamp != null ? timestamp : OffsetDateTime.now();
        this.metadata = metadata;
    }
    
    // Getters
    public String getRequestId() { return requestId; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
    public OffsetDateTime getTimestamp() { return timestamp; }
    public Map<String, Object> getMetadata() { return metadata; }
    
    /**
     * Creates a new builder for A2AResponse.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder class for A2AResponse.
     */
    public static class Builder {
        private String requestId;
        private String status;
        private String message;
        private Object data;
        private OffsetDateTime timestamp;
        private Map<String, Object> metadata;
        
        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }
        
        public Builder status(String status) {
            this.status = status;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder data(Object data) {
            this.data = data;
            return this;
        }
        
        public Builder timestamp(OffsetDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public A2AResponse build() {
            return new A2AResponse(requestId, status, message, data, timestamp, metadata);
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        A2AResponse that = (A2AResponse) obj;
        return Objects.equals(requestId, that.requestId) &&
               Objects.equals(status, that.status) &&
               Objects.equals(message, that.message) &&
               Objects.equals(data, that.data) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(metadata, that.metadata);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(requestId, status, message, data, timestamp, metadata);
    }
    
    @Override
    public String toString() {
        return "A2AResponse{" +
                "requestId='" + requestId + '\'' +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}