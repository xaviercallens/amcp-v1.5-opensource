package io.amcp.messaging.impl;

import io.amcp.messaging.EventBroker;

import java.util.Map;
import java.util.Properties;

/**
 * Factory for creating EventBroker instances based on configuration.
 * 
 * <p>Supports multiple broker implementations:
 * <ul>
 *   <li>memory - In-memory broker for development and testing</li>
 *   <li>kafka - Apache Kafka broker for production</li>
 *   <li>nats - NATS broker for lightweight messaging</li>
 *   <li>solace - Solace PubSub+ for enterprise messaging</li>
 * </ul>
 * </p>
 * 
 * @author AMCP Development Team
 * @version 1.4.0
 * @since 1.0.0
 */
public final class EventBrokerFactory {

    private EventBrokerFactory() {
        // Utility class
    }

    /**
     * Creates an EventBroker based on the specified type.
     * 
     * @param type the broker type (memory, kafka, nats, solace)
     * @param config configuration properties
     * @return configured EventBroker instance
     * @throws IllegalArgumentException if the broker type is unsupported
     */
    public static EventBroker create(String type, Properties config) {
        if (config == null) {
            config = new Properties();
        }

        switch (type.toLowerCase()) {
            case "memory":
            case "inmemory":
                return new InMemoryEventBroker();
                
            case "kafka":
                return createKafkaBroker(config);
                
            case "nats":
                return createNatsBroker(config);
                
            case "solace":
                return createSolaceBroker(config);
                
            default:
                throw new IllegalArgumentException("Unsupported broker type: " + type);
        }
    }

    /**
     * Creates an EventBroker based on configuration map.
     * 
     * @param type the broker type
     * @param config configuration map
     * @return configured EventBroker instance
     */
    public static EventBroker create(String type, Map<String, Object> config) {
        Properties properties = new Properties();
        if (config != null) {
            config.forEach((key, value) -> properties.put(key, value.toString()));
        }
        return create(type, properties);
    }

    /**
     * Creates an EventBroker with default configuration.
     * 
     * @param type the broker type
     * @return EventBroker with default configuration
     */
    public static EventBroker create(String type) {
        return create(type, new Properties());
    }

    private static EventBroker createKafkaBroker(Properties config) {
        // Implementation would create KafkaEventBroker
        throw new UnsupportedOperationException("Kafka broker not yet implemented");
    }

    private static EventBroker createNatsBroker(Properties config) {
        // Implementation would create NatsEventBroker
        throw new UnsupportedOperationException("NATS broker not yet implemented");
    }

    private static EventBroker createSolaceBroker(Properties config) {
        // Implementation would create SolaceEventBroker
        throw new UnsupportedOperationException("Solace broker not yet implemented");
    }

}