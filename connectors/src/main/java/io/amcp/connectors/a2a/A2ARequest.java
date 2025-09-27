package io.amcp.connectors.a2a;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * A2A (Agent-to-Agent) request wrapper for Google A2A protocol integration.
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 * @since 1.5.0
 */
public class A2ARequest {
    
    private final String requestId;
    private final A2AMessage message;
    private final boolean expectResponse;
    private final long timeout;
    private final OffsetDateTime timestamp;
    
    @JsonCreator
    public A2ARequest(
            @JsonProperty("requestId") String requestId,
            @JsonProperty("message") A2AMessage message,
            @JsonProperty("expectResponse") boolean expectResponse,
            @JsonProperty("timeout") long timeout,
            @JsonProperty("timestamp") OffsetDateTime timestamp) {
        this.requestId = requestId;
        this.message = message;
        this.expectResponse = expectResponse;
        this.timeout = timeout;
        this.timestamp = timestamp != null ? timestamp : OffsetDateTime.now();
    }
    
    // Getters
    public String getRequestId() { return requestId; }
    public A2AMessage getMessage() { return message; }
    public boolean isExpectResponse() { return expectResponse; }
    public long getTimeout() { return timeout; }
    public OffsetDateTime getTimestamp() { return timestamp; }
    
    /**
     * Creates a new builder for A2ARequest.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder class for A2ARequest.
     */
    public static class Builder {
        private String requestId;
        private A2AMessage message;
        private boolean expectResponse = false;
        private long timeout = 30000; // 30 seconds default
        private OffsetDateTime timestamp;
        
        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }
        
        public Builder message(A2AMessage message) {
            this.message = message;
            return this;
        }
        
        public Builder expectResponse(boolean expectResponse) {
            this.expectResponse = expectResponse;
            return this;
        }
        
        public Builder timeout(long timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public Builder timestamp(OffsetDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public A2ARequest build() {
            return new A2ARequest(requestId, message, expectResponse, timeout, timestamp);
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        A2ARequest that = (A2ARequest) obj;
        return expectResponse == that.expectResponse &&
               timeout == that.timeout &&
               Objects.equals(requestId, that.requestId) &&
               Objects.equals(message, that.message) &&
               Objects.equals(timestamp, that.timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(requestId, message, expectResponse, timeout, timestamp);
    }
    
    @Override
    public String toString() {
        return "A2ARequest{" +
                "requestId='" + requestId + '\'' +
                ", expectResponse=" + expectResponse +
                ", timeout=" + timeout +
                ", timestamp=" + timestamp +
                '}';
    }
}