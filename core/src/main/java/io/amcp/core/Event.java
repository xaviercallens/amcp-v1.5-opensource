package io.amcp.core;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an event within the AMCP v1.5 Enterprise Edition event-driven architecture.
 * 
 * <p>Events are the primary communication mechanism between agents in the AMCP framework.
 * They follow the CloudEvents v1.0 specification for interoperability and include rich metadata
 * for tracing, correlation, and routing purposes.</p>
 * 
 * <p>Key features:
 * <ul>
 *   <li>CloudEvents v1.0 specification compliance</li>
 *   <li>Hierarchical topic routing (travel.*, travel.**)</li>
 *   <li>Correlation IDs for distributed tracing</li>
 *   <li>Delivery guarantees and options</li>
 *   <li>Rich metadata support</li>
 *   <li>Enterprise-grade validation and serialization</li>
 * </ul>
 * </p>
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 * @since 1.0.0
 */
public final class Event implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String topic;
    private final Object payload;
    private final AgentID sender;
    private final LocalDateTime timestamp;
    private final String correlationId;
    private final DeliveryOptions deliveryOptions;
    private final Map<String, Object> metadata;

    private Event(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.topic = Objects.requireNonNull(builder.topic, "Event topic cannot be null");
        this.payload = builder.payload;
        this.sender = builder.sender;
        this.timestamp = builder.timestamp != null ? builder.timestamp : LocalDateTime.now();
        this.correlationId = builder.correlationId;
        this.deliveryOptions = builder.deliveryOptions != null ? builder.deliveryOptions : DeliveryOptions.defaultOptions();
        this.metadata = new HashMap<>(builder.metadata);
    }

    /**
     * Gets the unique event identifier.
     * 
     * @return the event ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the event topic for routing and filtering.
     * 
     * @return the event topic
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Gets the event payload.
     * 
     * @return the event payload
     */
    public Object getPayload() {
        return payload;
    }

    /**
     * Gets the payload cast to a specific type.
     * 
     * @param <T> the expected payload type
     * @param clazz the payload class
     * @return the typed payload
     * @throws ClassCastException if the payload is not of the expected type
     */
    @SuppressWarnings("unchecked")
    public <T> T getPayload(Class<T> clazz) {
        if (payload == null) {
            return null;
        }
        if (!clazz.isInstance(payload)) {
            throw new ClassCastException("Payload is not of type " + clazz.getName());
        }
        return (T) payload;
    }

    /**
     * Gets the agent that sent this event.
     * 
     * @return the sender agent ID, or null if sent by system
     */
    public AgentID getSender() {
        return sender;
    }

    /**
     * Gets the event timestamp.
     * 
     * @return when the event was created
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the correlation ID for distributed tracing.
     * 
     * @return the correlation ID, or null if not set
     */
    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * Gets the delivery options for this event.
     * 
     * @return the delivery options
     */
    public DeliveryOptions getDeliveryOptions() {
        return deliveryOptions;
    }

    /**
     * Gets all event metadata.
     * 
     * @return immutable view of event metadata
     */
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    /**
     * Gets metadata value by key.
     * 
     * @param key the metadata key
     * @return the metadata value, or null if not found
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * Gets metadata value by key and type.
     * 
     * @param <T> the expected value type
     * @param key the metadata key
     * @param clazz the value class
     * @return the typed metadata value, or null if not found
     * @throws ClassCastException if the value is not of the expected type
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, Class<T> clazz) {
        Object value = metadata.get(key);
        if (value == null) {
            return null;
        }
        if (!clazz.isInstance(value)) {
            throw new ClassCastException("Metadata value is not of type " + clazz.getName());
        }
        return (T) value;
    }

    /**
     * Checks if this event matches the given topic pattern.
     * 
     * <p>Supports hierarchical pattern matching:
     * <ul>
     *   <li>"travel.*" matches "travel.request" but not "travel.request.new"</li>
     *   <li>"travel.**" matches "travel.request.new" and all nested topics</li>
     *   <li>"travel.request" matches exactly "travel.request"</li>
     * </ul>
     * </p>
     * 
     * @param pattern the topic pattern to match against
     * @return true if this event matches the pattern
     */
    public boolean matchesTopic(String pattern) {
        if (pattern == null || topic == null) {
            return false;
        }

        if (pattern.equals(topic)) {
            return true;
        }

        if (pattern.endsWith(".**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return topic.startsWith(prefix + ".");
        }

        if (pattern.endsWith(".*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return topic.startsWith(prefix + ".") && 
                   topic.indexOf('.', prefix.length() + 1) == -1;
        }

        return false;
    }
    
    /**
     * Converts this AMCP Event to a CloudEvents v1.0 compliant format.
     * 
     * <p>This method provides seamless integration with CloudEvents specification
     * by mapping AMCP Event attributes to CloudEvents context attributes.</p>
     * 
     * @param sourceUri The CloudEvents source URI for this event
     * @return CloudEvents representation of this event
     * @throws RuntimeException if CloudEvents conversion fails
     */
    public io.amcp.cloudevents.CloudEvent toCloudEvent(java.net.URI sourceUri) {
        try {
            io.amcp.cloudevents.CloudEvent.Builder builder = io.amcp.cloudevents.CloudEvent.builder()
                .specVersion("1.0")
                .type("io.amcp.event." + topic.replace(".", "-"))
                .source(sourceUri)
                .id(id)
                .time(java.time.OffsetDateTime.of(timestamp, java.time.ZoneOffset.UTC))
                .dataContentType("application/json")
                .subject(topic);
            
            // Add event data
            if (payload != null) {
                builder.data(payload);
            }
            
            // Add AMCP-specific extensions
            builder.extension("amcp-topic", topic);
            
            if (sender != null) {
                builder.extension("amcp-sender", sender.toString());
            }
            
            if (correlationId != null) {
                builder.extension("amcp-correlation-id", correlationId);
            }
            
            // Add metadata as extensions
            metadata.forEach((key, value) -> {
                builder.extension("amcp-meta-" + key, value.toString());
            });
            
            return builder.build();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert AMCP Event to CloudEvent", e);
        }
    }
    
    /**
     * Validates this event for CloudEvents v1.0 compliance.
     * 
     * @return true if the event is CloudEvents compliant
     */
    public boolean isCloudEventsCompliant() {
        try {
            // Check required CloudEvents attributes through AMCP Event mapping
            if (id == null || id.trim().isEmpty()) return false;
            if (topic == null || topic.trim().isEmpty()) return false;
            if (timestamp == null) return false;
            
            // Validate extension attribute names (no 'ce-' prefix allowed)
            for (String metaKey : metadata.keySet()) {
                if (metaKey.startsWith("ce-")) {
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Creates a new builder for constructing events.
     * 
     * @return new Event builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Creates an AMCP Event from a CloudEvents v1.0 instance.
     * 
     * <p>This method provides seamless integration with CloudEvents by
     * extracting AMCP-specific information from CloudEvents extensions
     * and context attributes.</p>
     * 
     * @param cloudEvent The CloudEvent to convert
     * @return AMCP Event representation
     */
    public static Event fromCloudEvent(io.amcp.cloudevents.CloudEvent cloudEvent) {
        Builder builder = builder()
            .id(cloudEvent.getId())
            .topic(extractTopicFromCloudEvent(cloudEvent))
            .payload(cloudEvent.getData().orElse(null))
            .timestamp(cloudEvent.getTime()
                .map(offsetDateTime -> offsetDateTime.toLocalDateTime())
                .orElse(java.time.LocalDateTime.now()));
        
        // Extract sender from extensions
        cloudEvent.getExtension("amcp-sender")
            .map(Object::toString)
            .map(AgentID::named)
            .ifPresent(builder::sender);
        
        // Extract correlation ID
        cloudEvent.getExtension("amcp-correlation-id")
            .map(Object::toString)
            .ifPresent(builder::correlationId);
        
        // Extract metadata from extensions
        cloudEvent.getExtensions().forEach((key, value) -> {
            if (key.startsWith("amcp-meta-")) {
                String metaKey = key.substring("amcp-meta-".length());
                builder.metadata(metaKey, value.toString());
            } else if (!key.startsWith("amcp-")) {
                // Non-AMCP extensions become metadata
                builder.metadata("cloudevent-" + key, value.toString());
            }
        });
        
        // Add CloudEvent metadata
        builder.metadata("cloudevent-type", cloudEvent.getType());
        builder.metadata("cloudevent-source", cloudEvent.getSource().toString());
        cloudEvent.getSubject().ifPresent(subject -> 
            builder.metadata("cloudevent-subject", subject));
        cloudEvent.getDataContentType().ifPresent(contentType -> 
            builder.metadata("cloudevent-datacontenttype", contentType));
        
        return builder.build();
    }
    
    /**
     * Extract AMCP topic from CloudEvent.
     */
    private static String extractTopicFromCloudEvent(io.amcp.cloudevents.CloudEvent cloudEvent) {
        // First try to get the original AMCP topic from extensions
        return cloudEvent.getExtension("amcp-topic")
            .map(Object::toString)
            .orElse(
                // Fallback: use subject or derive from type
                cloudEvent.getSubject().orElse(
                    cloudEvent.getType().startsWith("io.amcp.event.") ?
                        cloudEvent.getType().substring("io.amcp.event.".length()).replace("-", ".") :
                        cloudEvent.getType()
                )
            );
    }

    /**
     * Creates a new builder initialized with this event's values.
     * 
     * @return new Event builder with this event's values
     */
    public Builder toBuilder() {
        return new Builder()
            .id(id)
            .topic(topic)
            .payload(payload)
            .sender(sender)
            .timestamp(timestamp)
            .correlationId(correlationId)
            .deliveryOptions(deliveryOptions)
            .metadata(metadata);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Event other = (Event) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Event{" +
                "id='" + id + '\'' +
                ", topic='" + topic + '\'' +
                ", sender=" + sender +
                ", timestamp=" + timestamp +
                ", correlationId='" + correlationId + '\'' +
                '}';
    }

    /**
     * Builder class for constructing Event instances.
     */
    public static final class Builder {
        private String id;
        private String topic;
        private Object payload;
        private AgentID sender;
        private LocalDateTime timestamp;
        private String correlationId;
        private DeliveryOptions deliveryOptions;
        private Map<String, Object> metadata = new HashMap<>();

        private Builder() {}

        public Builder id(String id) {
            this.id = id;
            return this;
        }

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

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder deliveryOptions(DeliveryOptions deliveryOptions) {
            this.deliveryOptions = deliveryOptions;
            return this;
        }

        public Builder metadata(String key, Object value) {
            if (key != null) {
                this.metadata.put(key, value);
            }
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            if (metadata != null) {
                this.metadata.putAll(metadata);
            }
            return this;
        }

        public Event build() {
            return new Event(this);
        }
    }

}