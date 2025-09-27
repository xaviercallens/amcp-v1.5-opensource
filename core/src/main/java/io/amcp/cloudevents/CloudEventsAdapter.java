package io.amcp.cloudevents;

import io.amcp.core.Event;
import java.time.Instant;
import java.util.UUID;

/**
 * CloudEventsAdapter provides conversion between AMCP Events and CloudEvents format.
 * This adapter implements the CloudEvents v1.0 specification for interoperability.
 */
public class CloudEventsAdapter {
    
    private static final String AMCP_SOURCE = "amcp://agent-mesh";
    private static final String CONTENT_TYPE = "application/json";
    
    /**
     * Convert an AMCP Event to a CloudEvent.
     * 
     * @param event The AMCP event to convert
     * @return A CloudEvent representation
     */
    public static CloudEvent toCloudEvent(Event event) {
        if (event == null) {
            return null;
        }
        
        CloudEvent cloudEvent = new CloudEvent();
        cloudEvent.setId(generateEventId(event));
        cloudEvent.setSource(AMCP_SOURCE);
        cloudEvent.setType(event.getTopic());
        cloudEvent.setDataContentType(CONTENT_TYPE);
        cloudEvent.setData(event.getPayload());
        cloudEvent.setTime(Instant.now().toString());
        
        // Add AMCP-specific extensions
        cloudEvent.addExtension("amcp-sender", event.getSender().toString());
        cloudEvent.addExtension("amcp-timestamp", event.getTimestamp().toString());
        
        if (event.getDeliveryOptions() != null) {
            cloudEvent.addExtension("amcp-delivery-ordered", 
                                  event.getDeliveryOptions().isOrdered());
            cloudEvent.addExtension("amcp-delivery-persistent", 
                                  event.getDeliveryOptions().isPersistent());
        }
        
        return cloudEvent;
    }
    
    /**
     * Convert a CloudEvent to an AMCP Event.
     * 
     * @param cloudEvent The CloudEvent to convert
     * @return An AMCP Event representation
     */
    public static Event fromCloudEvent(CloudEvent cloudEvent) {
        if (cloudEvent == null) {
            return null;
        }
        
        Event.Builder builder = Event.builder()
            .topic(cloudEvent.getType())
            .payload(cloudEvent.getData())
            .timestamp(java.time.LocalDateTime.now());
        
        // Extract AMCP-specific extensions
        Object sender = cloudEvent.getExtension("amcp-sender");
        if (sender != null) {
            builder.sender(io.amcp.core.AgentID.fromString(sender.toString()));
        }
        
        Object timestamp = cloudEvent.getExtension("amcp-timestamp");
        if (timestamp instanceof String) {
            try {
                builder.timestamp(java.time.LocalDateTime.parse((String) timestamp));
            } catch (Exception e) {
                // Use current time if parsing fails
                builder.timestamp(java.time.LocalDateTime.now());
            }
        }
        
        // Extract delivery options
        Object ordered = cloudEvent.getExtension("amcp-delivery-ordered");
        Object persistent = cloudEvent.getExtension("amcp-delivery-persistent");
        
        if (ordered instanceof Boolean || persistent instanceof Boolean) {
            io.amcp.core.DeliveryOptions.Builder deliveryBuilder = io.amcp.core.DeliveryOptions.builder();
            if (persistent instanceof Boolean && (Boolean) persistent) {
                deliveryBuilder.persistent(true);
            }
            builder.deliveryOptions(deliveryBuilder.build());
        }
        
        return builder.build();
    }
    
    /**
     * Generate a unique event ID based on the AMCP event.
     * 
     * @param event The AMCP event
     * @return A unique event ID
     */
    private static String generateEventId(Event event) {
        if (event.getSender() != null) {
            return String.format("amcp-%s-%s", 
                               event.getSender().toString().replaceAll("[^a-zA-Z0-9]", "-"), 
                               event.getTimestamp().toString().replaceAll("[^a-zA-Z0-9]", "-"));
        }
        return "amcp-" + UUID.randomUUID().toString();
    }
    
    /**
     * Check if a CloudEvent is compatible with AMCP.
     * 
     * @param cloudEvent The CloudEvent to check
     * @return true if compatible, false otherwise
     */
    public static boolean isAmcpCompatible(CloudEvent cloudEvent) {
        return cloudEvent != null 
            && cloudEvent.getSource() != null
            && cloudEvent.getType() != null
            && cloudEvent.getData() != null;
    }
    
    /**
     * Validate CloudEvent format compliance.
     * 
     * @param cloudEvent The CloudEvent to validate
     * @return true if valid according to CloudEvents v1.0 spec, false otherwise
     */
    public static boolean isValidCloudEvent(CloudEvent cloudEvent) {
        if (cloudEvent == null) return false;
        
        // Required fields according to CloudEvents v1.0
        return cloudEvent.getId() != null 
            && cloudEvent.getSource() != null
            && cloudEvent.getSpecVersion() != null
            && cloudEvent.getType() != null;
    }
}