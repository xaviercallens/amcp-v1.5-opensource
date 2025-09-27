package io.amcp.cloudevents;

import io.amcp.core.Event;
import io.amcp.core.AgentID;
import io.amcp.messaging.EventBroker;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * CloudEvents-compliant adapter for AMCP EventBroker.
 * 
 * <p>This adapter provides bidirectional conversion between AMCP Events
 * and CloudEvents v1.0 specification, enabling full compliance with
 * the CloudEvents standard while maintaining AMCP system compatibility.</p>
 * 
 * <p><strong>Key Features:</strong></p>
 * <ul>
 *   <li>Automatic AMCP Event ↔ CloudEvent conversion</li>
 *   <li>CloudEvents v1.0 specification validation</li>
 *   <li>Content-type handling for structured and binary modes</li>
 *   <li>Extension attribute support for AMCP metadata</li>
 *   <li>Async processing with CompletableFuture</li>
 * </ul>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>
 * EventBroker underlying = new InMemoryEventBroker();
 * CloudEventsAdapter adapter = new CloudEventsAdapter(underlying);
 * 
 * // Publish CloudEvent
 * CloudEvent event = CloudEvent.builder()
 *     .type("io.amcp.weather.updated")
 *     .source("//weather-agent/paris")
 *     .id("weather-001")
 *     .data("{\"temperature\": 22.5}")
 *     .build();
 * 
 * adapter.publishCloudEvent(event);
 * </pre>
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class CloudEventsAdapter {
    
    private final EventBroker underlying;
    private final String defaultSource;
    
    /**
     * Create a CloudEvents adapter with the specified underlying EventBroker.
     * 
     * @param underlying The underlying EventBroker implementation
     */
    public CloudEventsAdapter(EventBroker underlying) {
        this(underlying, "//amcp/system");
    }
    
    /**
     * Create a CloudEvents adapter with a custom default source.
     * 
     * @param underlying The underlying EventBroker implementation
     * @param defaultSource Default source URI for CloudEvents
     */
    public CloudEventsAdapter(EventBroker underlying, String defaultSource) {
        this.underlying = underlying;
        this.defaultSource = defaultSource;
    }
    
    /**
     * Publish a CloudEvent through the underlying EventBroker.
     * 
     * @param cloudEvent The CloudEvent to publish
     * @return CompletableFuture that completes when publication is done
     */
    public CompletableFuture<Void> publishCloudEvent(CloudEvent cloudEvent) {
        try {
            cloudEvent.validate();
            Event amcpEvent = convertToAMCPEvent(cloudEvent);
            return underlying.publish(amcpEvent);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Convert an AMCP Event to a CloudEvent.
     * 
     * @param amcpEvent The AMCP Event to convert
     * @return Corresponding CloudEvent
     */
    public CloudEvent convertToCloudEvent(Event amcpEvent) {
        try {
            CloudEvent.Builder builder = CloudEvent.builder()
                .specVersion("1.0")
                .type("io.amcp.event." + amcpEvent.getTopic().replace(".", "-"))
                .source(determineSource(amcpEvent))
                .id(amcpEvent.getId())
                .time(OffsetDateTime.now())
                .dataContentType("application/json")
                .subject(amcpEvent.getTopic());
            
            // Add event data
            if (amcpEvent.getPayload() != null) {
                builder.data(amcpEvent.getPayload());
            }
            
            // Add AMCP-specific extensions
            builder.extension("amcp-topic", amcpEvent.getTopic());
            
            if (amcpEvent.getSender() != null) {
                builder.extension("amcp-sender", amcpEvent.getSender().toString());
            }
            
            if (amcpEvent.getTimestamp() != null) {
                builder.extension("amcp-timestamp", amcpEvent.getTimestamp().toString());
            }
            
            // Add metadata as extensions
            amcpEvent.getMetadata().forEach((key, value) -> {
                builder.extension("amcp-meta-" + key, value);
            });
            
            return builder.build();
            
        } catch (Exception e) {
            throw new CloudEventException("Failed to convert AMCP Event to CloudEvent", e);
        }
    }
    
    /**
     * Convert a CloudEvent to an AMCP Event.
     * 
     * @param cloudEvent The CloudEvent to convert
     * @return Corresponding AMCP Event
     */
    public Event convertToAMCPEvent(CloudEvent cloudEvent) {
        try {
            Event.Builder builder = Event.builder()
                .id(cloudEvent.getId())
                .topic(extractTopic(cloudEvent))
                .payload(cloudEvent.getData().orElse(null))
                .timestamp(cloudEvent.getTime().orElse(OffsetDateTime.now()).toLocalDateTime());
            
            // Extract sender from extensions
            cloudEvent.getExtension("amcp-sender")
                .map(Object::toString)
                .map(AgentID::named)
                .ifPresent(builder::sender);
            
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
            
        } catch (Exception e) {
            throw new CloudEventException("Failed to convert CloudEvent to AMCP Event", e);
        }
    }
    
    /**
     * Create a CloudEvent from AMCP Event data with automatic type inference.
     * 
     * @param topic The event topic
     * @param payload The event payload
     * @param sender The sender agent
     * @return A CloudEvent instance
     */
    public CloudEvent createCloudEvent(String topic, Object payload, AgentID sender) {
        return CloudEvent.builder()
            .type("io.amcp.event." + topic.replace(".", "-"))
            .source(sender != null ? 
                URI.create("//amcp/agent/" + sender.toString()) : 
                URI.create(defaultSource))
            .id(java.util.UUID.randomUUID().toString())
            .time(OffsetDateTime.now())
            .dataContentType("application/json")
            .subject(topic)
            .data(payload)
            .extension("amcp-topic", topic)
            .extension("amcp-sender", sender != null ? sender.toString() : "system")
            .build();
    }
    
    /**
     * Determine the source URI from an AMCP Event.
     */
    private URI determineSource(Event amcpEvent) {
        if (amcpEvent.getSender() != null) {
            return URI.create("//amcp/agent/" + amcpEvent.getSender().toString());
        }
        return URI.create(defaultSource);
    }
    
    /**
     * Extract the AMCP topic from a CloudEvent.
     */
    private String extractTopic(CloudEvent cloudEvent) {
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
     * Get the underlying EventBroker.
     */
    public EventBroker getUnderlying() {
        return underlying;
    }
}