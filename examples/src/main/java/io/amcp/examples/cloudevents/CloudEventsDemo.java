package io.amcp.examples.cloudevents;

import io.amcp.cloudevents.CloudEvent;
import io.amcp.cloudevents.CloudEventsAdapter;
import io.amcp.cloudevents.CloudEventsEventBroker;
import io.amcp.core.Event;
import io.amcp.core.AgentID;
import io.amcp.messaging.impl.InMemoryEventBroker;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * CloudEvents 1.0 Compliance Demonstration for AMCP v1.5 Enterprise Edition.
 * 
 * <p>This demo showcases the full CloudEvents v1.0 specification integration
 * with the AMCP messaging system, demonstrating:</p>
 * <ul>
 *   <li>CloudEvent creation and validation</li>
 *   <li>AMCP Event ↔ CloudEvent bidirectional conversion</li>
 *   <li>JSON serialization and deserialization</li>
 *   <li>CloudEvents-compliant EventBroker usage</li>
 *   <li>Extension attribute handling</li>
 * </ul>
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class CloudEventsDemo {
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║           AMCP v1.5 CloudEvents 1.0 Compliance Demo           ║");
        System.out.println("║          CloudEvents 1.0 Compliant Event Demonstration         ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");
        
        CloudEventsDemo demo = new CloudEventsDemo();
        demo.runDemonstration();
    }
    
    public void runDemonstration() {
        try {
            // 1. CloudEvent Creation Demo
            demonstrateCloudEventCreation();
            
            // 2. AMCP ↔ CloudEvent Conversion Demo
            demonstrateEventConversion();
            
            // 3. JSON Serialization Demo
            demonstrateJsonSerialization();
            
            // 4. CloudEvents EventBroker Demo
            demonstrateCloudEventsEventBroker();
            
            // 5. Extension Attributes Demo
            demonstrateExtensionAttributes();
            
            System.out.println("✅ CloudEvents 1.0 Compliance demonstration completed successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void demonstrateCloudEventCreation() {
        System.out.println("🔹 1. CloudEvent Creation and Validation");
        System.out.println("═".repeat(50));
        
        // Create a CloudEvent with all required attributes
        CloudEvent weatherEvent = CloudEvent.builder()
            .specVersion("1.0")
            .type("io.amcp.weather.updated")
            .source(URI.create("//amcp/weather-agent/paris"))
            .id("weather-paris-001")
            .time(OffsetDateTime.now())
            .dataContentType("application/json")
            .subject("weather/paris/current")
            .data("{\"temperature\": 22.5, \"condition\": \"sunny\", \"humidity\": 65}")
            .build();
        
        System.out.println("✓ Created CloudEvent: " + weatherEvent.toString());
        System.out.println("  - Spec Version: " + weatherEvent.getSpecVersion());
        System.out.println("  - Type: " + weatherEvent.getType());
        System.out.println("  - Source: " + weatherEvent.getSource());
        System.out.println("  - ID: " + weatherEvent.getId());
        System.out.println("  - Subject: " + weatherEvent.getSubject().orElse("N/A"));
        System.out.println();
    }
    
    private void demonstrateEventConversion() {
        System.out.println("🔹 2. AMCP Event ↔ CloudEvent Conversion");
        System.out.println("═".repeat(50));
        
        // Create an AMCP Event
        Event amcpEvent = Event.builder()
            .topic("mobility.agent.migrated")
            .payload("Agent successfully migrated to edge-device-01")
            .sender(AgentID.named("mobility-manager"))
            .metadata("destination", "edge-device-01")
            .metadata("migration-time", "150ms")
            .build();
        
        System.out.println("📤 Original AMCP Event:");
        System.out.println("  - Topic: " + amcpEvent.getTopic());
        System.out.println("  - Sender: " + amcpEvent.getSender());
        System.out.println("  - Payload: " + amcpEvent.getPayload());
        System.out.println("  - Metadata: " + amcpEvent.getMetadata());
        
        // Convert to CloudEvent
        CloudEventsAdapter adapter = new CloudEventsAdapter(new InMemoryEventBroker());
        CloudEvent converted = adapter.convertToCloudEvent(amcpEvent);
        
        System.out.println("\n🔄 Converted to CloudEvent:");
        System.out.println("  - Type: " + converted.getType());
        System.out.println("  - Source: " + converted.getSource());
        System.out.println("  - Subject: " + converted.getSubject().orElse("N/A"));
        System.out.println("  - Extensions: " + converted.getExtensions().size() + " attributes");
        
        // Convert back to AMCP Event
        Event convertedBack = adapter.convertToAMCPEvent(converted);
        
        System.out.println("\n🔄 Converted back to AMCP Event:");
        System.out.println("  - Topic: " + convertedBack.getTopic());
        System.out.println("  - Payload: " + convertedBack.getPayload());
        System.out.println("  - Metadata preserved: " + convertedBack.getMetadata().containsKey("destination"));
        System.out.println();
    }
    
    private void demonstrateJsonSerialization() {
        System.out.println("🔹 3. JSON Serialization and Deserialization");
        System.out.println("═".repeat(50));
        
        // Create a CloudEvent with complex data
        CloudEvent originalEvent = CloudEvent.builder()
            .type("io.amcp.agent.state.changed")
            .source("//amcp/context/production")
            .id("state-change-001")
            .time(OffsetDateTime.now())
            .dataContentType("application/json")
            .subject("agent/lifecycle/state")
            .data("{\"agentId\": \"worker-001\", \"oldState\": \"IDLE\", \"newState\": \"ACTIVE\"}")
            .extension("amcp-context", "production")
            .extension("amcp-severity", "INFO")
            .build();
        
        System.out.println("📋 Original CloudEvent: " + originalEvent.toString());
        
        // Serialize to JSON
        String json = originalEvent.toJson();
        System.out.println("\n📤 Serialized JSON:");
        System.out.println(json);
        
        // Deserialize from JSON
        CloudEvent deserializedEvent = CloudEvent.fromJson(json);
        System.out.println("\n📥 Deserialized CloudEvent: " + deserializedEvent.toString());
        
        // Verify equality
        boolean isEqual = originalEvent.equals(deserializedEvent);
        System.out.println("✓ Serialization roundtrip successful: " + isEqual);
        System.out.println();
    }
    
    private void demonstrateCloudEventsEventBroker() throws Exception {
        System.out.println("🔹 4. CloudEvents-Compliant EventBroker");
        System.out.println("═".repeat(50));
        
        // Create CloudEvents EventBroker
        CloudEventsEventBroker broker = new CloudEventsEventBroker();
        broker.start().get();
        
        System.out.println("✓ CloudEvents EventBroker started");
        
        // Subscribe to CloudEvents
        CompletableFuture<CloudEvent> receivedEvent = new CompletableFuture<>();
        broker.subscribeToCloudEvents("system.**", cloudEvent -> {
            System.out.println("📨 Received CloudEvent: " + cloudEvent.getType());
            receivedEvent.complete(cloudEvent);
        });
        
        // Publish a CloudEvent
        CloudEvent systemEvent = CloudEvent.builder()
            .type("io.amcp.system.startup.completed")
            .source("//amcp/system/core")
            .id("startup-001")
            .time(OffsetDateTime.now())
            .subject("system.startup")
            .data("System initialization completed successfully")
            .extension("amcp-version", "1.5.0")
            .build();
        
        broker.publishCloudEvent(systemEvent);
        System.out.println("📤 Published system startup CloudEvent");
        
        // Wait for event reception
        CloudEvent received = receivedEvent.get();
        System.out.println("✓ Event received and processed: " + received.getId());
        
        broker.stop().get();
        System.out.println("✓ CloudEvents EventBroker stopped");
        System.out.println();
    }
    
    private void demonstrateExtensionAttributes() {
        System.out.println("🔹 5. Extension Attributes and AMCP Integration");
        System.out.println("═".repeat(50));
        
        // Create CloudEvent with AMCP-specific extensions
        CloudEvent eventWithExtensions = CloudEvent.builder()
            .type("io.amcp.alert.severe.weather")
            .source("//amcp/weather-service/europe")
            .id("alert-storm-001")
            .time(OffsetDateTime.now())
            .dataContentType("application/json")
            .subject("alert/weather/storm")
            .data("{\"location\": \"Paris\", \"severity\": \"HIGH\", \"type\": \"thunderstorm\"}")
            .extension("amcp-priority", "HIGH")
            .extension("amcp-correlation-id", "weather-correlation-001")
            .extension("amcp-retry-count", 0)
            .extension("custom-alert-zone", "EU-WEST-1")
            .extension("notification-channels", "sms,email,push")
            .build();
        
        System.out.println("📋 CloudEvent with Extensions:");
        System.out.println("  - Basic attributes: " + eventWithExtensions.toString());
        System.out.println("  - Extension count: " + eventWithExtensions.getExtensions().size());
        
        System.out.println("\n🔧 Extension Attributes:");
        eventWithExtensions.getExtensions().forEach((key, value) -> 
            System.out.println("  - " + key + ": " + value));
        
        // Demonstrate extension retrieval
        eventWithExtensions.getExtension("amcp-priority")
            .ifPresent(priority -> System.out.println("\n⚡ Alert Priority: " + priority));
        
        eventWithExtensions.getExtension("custom-alert-zone")
            .ifPresent(zone -> System.out.println("🌍 Alert Zone: " + zone));
        
        System.out.println("\n✓ Extension attributes handled successfully");
        System.out.println();
    }
}