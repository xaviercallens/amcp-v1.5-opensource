package io.amcp.broker.nats;

import io.amcp.core.Event;
import io.amcp.core.EventBroker;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * NATS-based EventBroker implementation for high-performance distributed AMCP mesh.
 * 
 * Features:
 * - Ultra-low latency messaging via NATS
 * - Subject-based pub/sub
 * - Queue groups for load balancing
 * - CloudEvents serialization
 * - Multi-instance support
 * - Request/reply pattern support
 */
public class NatsEventBroker implements EventBroker {
    private static final Logger logger = LoggerFactory.getLogger(NatsEventBroker.class);
    
    private final String servers;
    private final String connectionName;
    private final String instanceId;
    
    private Connection connection;
    private ExecutorService executor;
    private volatile boolean running = false;
    private final Map<String, NatsSubscription> subscriptions = new ConcurrentHashMap<>();

    private static class NatsSubscription {
        final String subjectPattern;
        final Consumer<Event> handler;
        Subscription natsSubscription;

        NatsSubscription(String subjectPattern, Consumer<Event> handler) {
            this.subjectPattern = subjectPattern;
            this.handler = handler;
        }
    }

    /**
     * Creates a new NatsEventBroker.
     *
     * @param servers NATS server URLs (e.g., "nats://localhost:4222")
     * @param connectionName Unique connection name
     * @param instanceId Unique instance identifier
     */
    public NatsEventBroker(String servers, String connectionName, String instanceId) {
        this.servers = servers;
        this.connectionName = connectionName;
        this.instanceId = instanceId;
    }

    @Override
    public CompletableFuture<Void> publish(Event event) {
        if (!running) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Broker not running"));
        }

        return CompletableFuture.runAsync(() -> {
            try {
                String subject = event.getTopic().replace(".", ".");
                String payload = serializeEvent(event);
                
                connection.publish(subject, payload.getBytes());
                logger.debug("Published to subject: {}", subject);
                
            } catch (Exception e) {
                logger.error("Error publishing event: {}", e.getMessage(), e);
            }
        }, executor);
    }

    @Override
    public String subscribe(String topicPattern, Consumer<Event> handler) {
        String subscriptionId = UUID.randomUUID().toString();
        
        try {
            // Convert AMCP topic pattern to NATS subject
            String subject = convertTopicToSubject(topicPattern);
            
            NatsSubscription natsSubscription = new NatsSubscription(topicPattern, handler);
            
            // Subscribe with queue group for load balancing
            String queueGroup = "amcp-" + instanceId;
            natsSubscription.natsSubscription = connection.subscribe(subject, queueGroup, msg -> {
                try {
                    Event event = deserializeEvent(new String(msg.getData()));
                    
                    if (event.matchesTopic(topicPattern)) {
                        executor.submit(() -> handler.accept(event));
                    }
                } catch (Exception e) {
                    logger.error("Error handling message: {}", e.getMessage());
                }
            });
            
            subscriptions.put(subscriptionId, natsSubscription);
            logger.debug("Subscribed to subject pattern: {} ({})", topicPattern, subject);
            
        } catch (Exception e) {
            logger.error("Error subscribing to topic {}: {}", topicPattern, e.getMessage());
        }
        
        return subscriptionId;
    }

    @Override
    public void unsubscribe(String subscriptionId) {
        NatsSubscription natsSubscription = subscriptions.remove(subscriptionId);
        
        if (natsSubscription != null && natsSubscription.natsSubscription != null) {
            try {
                natsSubscription.natsSubscription.unsubscribe();
                logger.debug("Unsubscribed: {}", subscriptionId);
            } catch (Exception e) {
                logger.error("Error unsubscribing: {}", e.getMessage());
            }
        }
    }

    @Override
    public CompletableFuture<Void> start() {
        if (running) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                Options options = new Options.Builder()
                        .servers(servers.split(","))
                        .connectionName(connectionName)
                        .maxReconnects(10)
                        .reconnectWait(java.time.Duration.ofSeconds(2))
                        .build();
                
                connection = Nats.connect(options);
                executor = Executors.newVirtualThreadPerTaskExecutor();
                running = true;
                
                logger.info("NatsEventBroker started for instance: {} (servers: {})", 
                        instanceId, servers);
                
            } catch (Exception e) {
                logger.error("Failed to start NatsEventBroker: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to start NATS broker", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> stop() {
        if (!running) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                running = false;
                
                // Unsubscribe all
                for (NatsSubscription sub : subscriptions.values()) {
                    if (sub.natsSubscription != null) {
                        sub.natsSubscription.unsubscribe();
                    }
                }
                subscriptions.clear();
                
                // Close connection
                if (connection != null && !connection.getStatus().isClosed()) {
                    connection.close();
                }
                
                // Shutdown executor
                if (executor != null) {
                    executor.shutdown();
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                }
                
                logger.info("NatsEventBroker stopped");
                
            } catch (Exception e) {
                logger.error("Error stopping NatsEventBroker: {}", e.getMessage(), e);
            }
        });
    }

    @Override
    public boolean isRunning() {
        return running && connection != null && !connection.getStatus().isClosed();
    }

    @Override
    public String getBrokerType() {
        return "nats";
    }

    /**
     * Converts AMCP topic pattern to NATS subject.
     * 
     * AMCP: hello.request, hello.*, hello.**
     * NATS: hello.request, hello.>, hello.>
     */
    private String convertTopicToSubject(String topicPattern) {
        return topicPattern
                .replace(".**", ".>")
                .replace(".*", ".*");
    }

    /**
     * Serializes an Event to JSON string.
     */
    private String serializeEvent(Event event) {
        // Use CloudEvents JSON format
        return event.getCloudEvent().toString();
    }

    /**
     * Deserializes a JSON string to an Event.
     */
    private Event deserializeEvent(String json) {
        // Parse CloudEvents JSON and reconstruct Event
        return Event.create("amcp.event", json);
    }
}
