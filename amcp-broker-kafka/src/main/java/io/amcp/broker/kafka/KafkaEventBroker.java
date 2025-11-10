package io.amcp.broker.kafka;

import io.amcp.core.Event;
import io.amcp.core.EventBroker;
import io.cloudevents.CloudEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Kafka-based EventBroker implementation for distributed AMCP mesh.
 * 
 * Features:
 * - Distributed message routing via Kafka
 * - Topic-based pub/sub
 * - Consumer groups for scalability
 * - CloudEvents serialization
 * - Multi-instance support
 */
public class KafkaEventBroker implements EventBroker {
    private static final Logger logger = LoggerFactory.getLogger(KafkaEventBroker.class);
    
    private final String bootstrapServers;
    private final String groupId;
    private final String instanceId;
    
    private KafkaProducer<String, String> producer;
    private KafkaConsumer<String, String> consumer;
    private ExecutorService executor;
    private volatile boolean running = false;
    private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();

    private static class Subscription {
        final String topicPattern;
        final Consumer<Event> handler;

        Subscription(String topicPattern, Consumer<Event> handler) {
            this.topicPattern = topicPattern;
            this.handler = handler;
        }
    }

    /**
     * Creates a new KafkaEventBroker.
     *
     * @param bootstrapServers Kafka bootstrap servers (e.g., "localhost:9092")
     * @param groupId Consumer group ID for this instance
     * @param instanceId Unique instance identifier
     */
    public KafkaEventBroker(String bootstrapServers, String groupId, String instanceId) {
        this.bootstrapServers = bootstrapServers;
        this.groupId = groupId;
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
                String topic = event.getTopic();
                String key = instanceId + "-" + UUID.randomUUID();
                String value = serializeEvent(event);
                
                ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
                producer.send(record, (metadata, exception) -> {
                    if (exception != null) {
                        logger.error("Error publishing to topic {}: {}", topic, exception.getMessage());
                    } else {
                        logger.debug("Published to topic {} partition {}", topic, metadata.partition());
                    }
                });
                
            } catch (Exception e) {
                logger.error("Error serializing event: {}", e.getMessage(), e);
            }
        }, executor);
    }

    @Override
    public String subscribe(String topicPattern, Consumer<Event> handler) {
        String subscriptionId = UUID.randomUUID().toString();
        subscriptions.put(subscriptionId, new Subscription(topicPattern, handler));
        
        // Convert pattern to Kafka topic
        String kafkaTopic = topicPattern.replace("**", "*").replace(".", "-");
        
        try {
            consumer.subscribe(Collections.singletonList(kafkaTopic));
            logger.debug("Subscribed to topic pattern: {}", topicPattern);
        } catch (Exception e) {
            logger.error("Error subscribing to topic {}: {}", topicPattern, e.getMessage());
        }
        
        return subscriptionId;
    }

    @Override
    public void unsubscribe(String subscriptionId) {
        subscriptions.remove(subscriptionId);
        logger.debug("Removed subscription: {}", subscriptionId);
    }

    @Override
    public CompletableFuture<Void> start() {
        if (running) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                // Create producer
                Properties producerProps = new Properties();
                producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
                producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
                producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
                producerProps.put(ProducerConfig.RETRIES_CONFIG, 3);
                
                producer = new KafkaProducer<>(producerProps);
                logger.info("Kafka producer created");
                
                // Create consumer
                Properties consumerProps = new Properties();
                consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
                consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
                consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
                consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
                consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
                
                consumer = new KafkaConsumer<>(consumerProps);
                logger.info("Kafka consumer created with group: {}", groupId);
                
                // Start consumer thread
                executor = Executors.newVirtualThreadPerTaskExecutor();
                running = true;
                
                executor.submit(this::consumerLoop);
                logger.info("KafkaEventBroker started for instance: {}", instanceId);
                
            } catch (Exception e) {
                logger.error("Failed to start KafkaEventBroker: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to start Kafka broker", e);
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
                
                if (consumer != null) {
                    consumer.close();
                }
                
                if (producer != null) {
                    producer.close();
                }
                
                if (executor != null) {
                    executor.shutdown();
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                }
                
                logger.info("KafkaEventBroker stopped");
                
            } catch (Exception e) {
                logger.error("Error stopping KafkaEventBroker: {}", e.getMessage(), e);
            }
        });
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public String getBrokerType() {
        return "kafka";
    }

    /**
     * Consumer loop for processing messages.
     */
    private void consumerLoop() {
        while (running) {
            try {
                var records = consumer.poll(java.time.Duration.ofSeconds(1));
                
                records.forEach(record -> {
                    try {
                        Event event = deserializeEvent(record.value());
                        
                        // Route to matching subscribers
                        for (Subscription sub : subscriptions.values()) {
                            if (event.matchesTopic(sub.topicPattern)) {
                                try {
                                    executor.submit(() -> sub.handler.accept(event));
                                } catch (Exception e) {
                                    logger.error("Error delivering event: {}", e.getMessage());
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Error processing message: {}", e.getMessage(), e);
                    }
                });
                
            } catch (Exception e) {
                logger.error("Error in consumer loop: {}", e.getMessage());
            }
        }
    }

    /**
     * Serializes an Event to JSON string.
     */
    private String serializeEvent(Event event) {
        // Use CloudEvents JSON format
        CloudEvent ce = event.getCloudEvent();
        return ce.toString();
    }

    /**
     * Deserializes a JSON string to an Event.
     */
    private Event deserializeEvent(String json) {
        // Parse CloudEvents JSON and reconstruct Event
        // This is a simplified version - full implementation would use proper JSON parsing
        return Event.create("amcp.event", json);
    }
}
