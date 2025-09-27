package io.amcp.connectors.a2a;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * A2A (Agent-to-Agent) message format for Google A2A protocol integration.
 * 
 * <p>This class represents a message in the Google A2A protocol format,
 * providing interoperability between AMCP agents and A2A-based systems.</p>
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 * @since 1.5.0
 */
public class A2AMessage {
    
    private final String requestId;
    private final String correlationId;
    private final String messageType;
    private final String senderId;
    private final String targetService;
    private final Object payload;
    private final OffsetDateTime timestamp;
    private final Map<String, Object> metadata;
    
    @JsonCreator
    public A2AMessage(
            @JsonProperty("requestId") String requestId,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("messageType") String messageType,
            @JsonProperty("senderId") String senderId,
            @JsonProperty("targetService") String targetService,
            @JsonProperty("payload") Object payload,
            @JsonProperty("timestamp") OffsetDateTime timestamp,
            @JsonProperty("metadata") Map<String, Object> metadata) {
        this.requestId = requestId;
        this.correlationId = correlationId;
        this.messageType = messageType;
        this.senderId = senderId;
        this.targetService = targetService;
        this.payload = payload;
        this.timestamp = timestamp;
        this.metadata = metadata;
    }
    
    // Getters
    public String getRequestId() { return requestId; }
    public String getCorrelationId() { return correlationId; }
    public String getMessageType() { return messageType; }
    public String getSenderId() { return senderId; }
    public String getTargetService() { return targetService; }
    public Object getPayload() { return payload; }
    public OffsetDateTime getTimestamp() { return timestamp; }
    public Map<String, Object> getMetadata() { return metadata; }
    
    /**
     * Creates a new builder for A2AMessage.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder class for A2AMessage.
     */
    public static class Builder {
        private String requestId;
        private String correlationId;
        private String messageType;
        private String senderId;
        private String targetService;
        private Object payload;
        private OffsetDateTime timestamp;
        private Map<String, Object> metadata;
        
        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }
        
        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }
        
        public Builder messageType(String messageType) {
            this.messageType = messageType;
            return this;
        }
        
        public Builder senderId(String senderId) {
            this.senderId = senderId;
            return this;
        }
        
        public Builder targetService(String targetService) {
            this.targetService = targetService;
            return this;
        }
        
        public Builder payload(Object payload) {
            this.payload = payload;
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
        
        public A2AMessage build() {
            return new A2AMessage(requestId, correlationId, messageType, 
                senderId, targetService, payload, timestamp, metadata);
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        A2AMessage that = (A2AMessage) obj;
        return Objects.equals(requestId, that.requestId) &&
               Objects.equals(correlationId, that.correlationId) &&
               Objects.equals(messageType, that.messageType) &&
               Objects.equals(senderId, that.senderId) &&
               Objects.equals(targetService, that.targetService) &&
               Objects.equals(payload, that.payload) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(metadata, that.metadata);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(requestId, correlationId, messageType, senderId, 
            targetService, payload, timestamp, metadata);
    }
    
    @Override
    public String toString() {
        return "A2AMessage{" +
                "requestId='" + requestId + '\'' +
                ", correlationId='" + correlationId + '\'' +
                ", messageType='" + messageType + '\'' +
                ", senderId='" + senderId + '\'' +
                ", targetService='" + targetService + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}