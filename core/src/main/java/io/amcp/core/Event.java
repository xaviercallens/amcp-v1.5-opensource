package io.amcp.core;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an event in the AMCP system following CloudEvents 1.0 specification.
 * Events are the primary means of communication between agents.
 * 
 * AMCP v1.5 enhances events with:
 * - CloudEvents 1.0 compliance for standardized event format
 * - Extended metadata support with type-safe accessors
 * - Standardized error handling and validation
 * - Multi-language serialization compatibility
 * - Tracing and correlation support for distributed debugging
 * - Reply pattern support for request-response workflows
 * 
 * CloudEvents Compliance:
 * - messageId maps to CloudEvents 'id'
 * - topic maps to CloudEvents 'type'
 * - timestamp maps to CloudEvents 'time'
 * - sender maps to CloudEvents 'source'
 * - payload maps to CloudEvents 'data'
 * - Additional AMCP-specific extensions in metadata
 * 
 * @since AMCP 1.0
 * @version 1.5.0 - Enhanced with CloudEvents compliance and v1.5 features
 */
public final class Event implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String messageId;
    private final String topic;
    private final Object payload;
    private final Instant timestamp;
    private final AgentID sender;
    private final String traceId;
    private final Map<String, Object> metadata;
    private final DeliveryOptions deliveryOptions;
    
    // CloudEvents 1.0 spec version
    private final String specVersion = "1.0";
    
    // AMCP-specific extensions for enhanced functionality
    private final String dataContentType;
    private final String dataSchema;
    
    /**
     * Builder pattern for creating Events with optional parameters.
     */
    public static class Builder {
        private String messageId;
        private String topic;
        private Object payload;
        private Instant timestamp;
        private AgentID sender;
        private String traceId;
        private Map<String, Object> metadata = new HashMap<>();
        private DeliveryOptions deliveryOptions = new DeliveryOptions();
        private String dataContentType = "application/json";
        private String dataSchema;
        
        public Builder topic(String topic) {
            this.topic = topic;
            return this;
        }
        
        public Builder payload(Object payload) {
            this.payload = payload;
            return this;
        }
        
        public Builder sender(AgentID sender) {
            this.sender = sender;
            return this;
        }
        
        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }
        
        public Builder metadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            if (metadata != null) {
                this.metadata.putAll(metadata);
            }
            return this;
        }
        
        public Builder deliveryOptions(DeliveryOptions options) {
            this.deliveryOptions = options != null ? options : new DeliveryOptions();
            return this;
        }
        
        public Builder requireAcknowledgment(boolean requireAck) {
            this.deliveryOptions = this.deliveryOptions.withRequireAcknowledgment(requireAck);
            return this;
        }
        
        /**
         * Sets the data content type (CloudEvents compliance).
         */
        public Builder dataContentType(String contentType) {
            this.dataContentType = contentType;
            return this;
        }
        
        /**
         * Sets the data schema URI (CloudEvents compliance).
         */
        public Builder dataSchema(String schemaUri) {
            this.dataSchema = schemaUri;
            return this;
        }
        
        /**
         * Convenience method for JSON content type.
         */
        public Builder asJson() {
            return dataContentType("application/json");
        }
        
        /**
         * Convenience method for XML content type.
         */
        public Builder asXml() {
            return dataContentType("application/xml");
        }
        
        /**
         * Convenience method for plain text content type.
         */
        public Builder asText() {
            return dataContentType("text/plain");
        }
        
        public Event build() {
            if (topic == null || topic.trim().isEmpty()) {
                throw new IllegalArgumentException("Topic cannot be null or empty");
            }
            
            return new Event(
                messageId != null ? messageId : UUID.randomUUID().toString(),
                topic.trim(),
                payload,
                timestamp != null ? timestamp : Instant.now(),
                sender,
                traceId,
                new HashMap<>(metadata),
                deliveryOptions,
                dataContentType,
                dataSchema
            );
        }
    }
    
    /**
     * Static factory method to create a builder.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Convenient factory method for simple events.
     */
    public static Event create(String topic, Object payload) {
        return builder()
                .topic(topic)
                .payload(payload)
                .build();
    }
    
    /**
     * Convenient factory method for events with sender.
     */
    public static Event create(String topic, Object payload, AgentID sender) {
        return builder()
                .topic(topic)
                .payload(payload)
                .sender(sender)
                .build();
    }
    
    private Event(String messageId, String topic, Object payload, Instant timestamp,
                  AgentID sender, String traceId, Map<String, Object> metadata,
                  DeliveryOptions deliveryOptions, String dataContentType, String dataSchema) {
        this.messageId = messageId;
        this.topic = topic;
        this.payload = payload;
        this.timestamp = timestamp;
        this.sender = sender;
        this.traceId = traceId;
        this.metadata = metadata;
        this.deliveryOptions = deliveryOptions;
        this.dataContentType = dataContentType != null ? dataContentType : "application/json";
        this.dataSchema = dataSchema;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public String getTopic() {
        return topic;
    }
    
    public Object getPayload() {
        return payload;
    }
    
    /**
     * Type-safe payload accessor with casting.
     */
    @SuppressWarnings("unchecked")
    public <T> T getPayload(Class<T> type) {
        if (payload == null) {
            return null;
        }
        
        if (type.isAssignableFrom(payload.getClass())) {
            return (T) payload;
        }
        
        throw new ClassCastException("Payload is not of type " + type.getName() + 
                                     ", actual type: " + payload.getClass().getName());
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public AgentID getSender() {
        return sender;
    }
    
    public String getTraceId() {
        return traceId;
    }
    
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (value == null) {
            return null;
        }
        
        if (type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        
        throw new ClassCastException("Metadata value for key '" + key + 
                                     "' is not of type " + type.getName());
    }
    
    public DeliveryOptions getDeliveryOptions() {
        return deliveryOptions;
    }
    
    // CloudEvents 1.0 compliance accessors
    
    /**
     * CloudEvents spec version (always "1.0" for AMCP v1.5).
     */
    public String getSpecVersion() {
        return specVersion;
    }
    
    /**
     * CloudEvents data content type.
     */
    public String getDataContentType() {
        return dataContentType;
    }
    
    /**
     * CloudEvents data schema URI.
     */
    public String getDataSchema() {
        return dataSchema;
    }
    
    /**
     * CloudEvents ID (maps to messageId).
     */
    public String getId() {
        return messageId;
    }
    
    /**
     * CloudEvents type (maps to topic).
     */
    public String getType() {
        return topic;
    }
    
    /**
     * CloudEvents source (maps to sender).
     */
    public String getSource() {
        return sender != null ? sender.toString() : null;
    }
    
    /**
     * CloudEvents time (maps to timestamp).
     */
    public Instant getTime() {
        return timestamp;
    }
    
    /**
     * CloudEvents data (maps to payload).
     */
    public Object getData() {
        return payload;
    }
    
    /**
     * Checks if this event conforms to CloudEvents 1.0 specification.
     */
    public boolean isCloudEventsCompliant() {
        return messageId != null && !messageId.trim().isEmpty() &&
               topic != null && !topic.trim().isEmpty() &&
               specVersion != null && specVersion.equals("1.0");
    }
    
    /**
     * Validates CloudEvents required fields and throws exception if invalid.
     */
    public void validateCloudEventsCompliance() throws IllegalStateException {
        if (messageId == null || messageId.trim().isEmpty()) {
            throw new IllegalStateException("CloudEvents compliance: 'id' (messageId) is required");
        }
        if (topic == null || topic.trim().isEmpty()) {
            throw new IllegalStateException("CloudEvents compliance: 'type' (topic) is required");
        }
        if (!specVersion.equals("1.0")) {
            throw new IllegalStateException("CloudEvents compliance: specversion must be '1.0'");
        }
    }
    
    /**
     * Creates a reply event to this event, setting appropriate correlation metadata.
     */
    public Builder createReply(String replyTopic) {
        return builder()
                .topic(replyTopic)
                .traceId(this.traceId)
                .metadata("correlationId", this.messageId)
                .metadata("replyTo", this.topic);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Event event = (Event) obj;
        return Objects.equals(messageId, event.messageId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(messageId);
    }
    
    @Override
    public String toString() {
        return "Event{" +
                "messageId='" + messageId + '\'' +
                ", topic='" + topic + '\'' +
                ", timestamp=" + timestamp +
                ", sender=" + sender +
                ", traceId='" + traceId + '\'' +
                ", payloadType=" + (payload != null ? payload.getClass().getSimpleName() : "null") +
                ", dataContentType='" + dataContentType + '\'' +
                ", specVersion='" + specVersion + '\'' +
                ", cloudEventsCompliant=" + isCloudEventsCompliant() +
                '}';
    }
    
    /**
     * Creates a CloudEvents-formatted representation for external systems.
     */
    public Map<String, Object> toCloudEventsMap() {
        Map<String, Object> cloudEvent = new HashMap<>();
        cloudEvent.put("specversion", specVersion);
        cloudEvent.put("id", messageId);
        cloudEvent.put("type", topic);
        cloudEvent.put("time", timestamp.toString());
        
        if (sender != null) {
            cloudEvent.put("source", sender.toString());
        }
        
        if (payload != null) {
            cloudEvent.put("data", payload);
        }
        
        if (dataContentType != null) {
            cloudEvent.put("datacontenttype", dataContentType);
        }
        
        if (dataSchema != null) {
            cloudEvent.put("dataschema", dataSchema);
        }
        
        // Add AMCP-specific extensions with "amcp" prefix
        if (traceId != null) {
            cloudEvent.put("amcptraceid", traceId);
        }
        
        if (!metadata.isEmpty()) {
            cloudEvent.put("amcpmetadata", new HashMap<>(metadata));
        }
        
        if (deliveryOptions != null) {
            cloudEvent.put("amcpdeliveryoptions", deliveryOptions);
        }
        
        return cloudEvent;
    }
}