package io.quarkus.amcp.runtime;

import io.amcp.broker.kafka.KafkaEventBroker;
import io.amcp.core.AbstractMobileAgent;
import io.amcp.core.AgentContext;
import io.amcp.core.EventBroker;
import io.amcp.core.InMemoryEventBroker;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * Recorder for initializing AMCP at runtime.
 * This is called during Quarkus startup to bootstrap the agent mesh.
 */
@Recorder
public class AmcpRecorder {
    private static final Logger logger = LoggerFactory.getLogger(AmcpRecorder.class);

    /**
     * Initializes the agent mesh with the specified configuration and agent classes.
     *
     * @param config       the AMCP configuration
     * @param agentClasses the list of agent class names to instantiate
     */
    public void initAgentMesh(AmcpConfig config, List<String> agentClasses) {
        System.out.println("=== AMCP INIT: Starting with " + agentClasses.size() + " agent(s)");
        logger.info("Initializing AMCP Agent Mesh with {} agent(s)", agentClasses.size());
        
        try {
            // Create event broker based on configuration
            EventBroker broker = createBroker(config);
            
            // Create agent context
            AgentContext context = AgentContext.boot(broker);
            
            // Start the broker
            context.start().join();
            
            // Register and activate agents
            for (String className : agentClasses) {
                try {
                    System.out.println("=== AMCP INIT: Loading agent class: " + className);
                    logger.info("Loading agent class: {}", className);
                    Class<?> agentClass = Class.forName(className, true, 
                            Thread.currentThread().getContextClassLoader());
                    
                    // Try to get from Arc container first (for CDI beans)
                    Object agentInstance = null;
                    var instance = Arc.container().instance(agentClass);
                    if (instance.isAvailable()) {
                        agentInstance = instance.get();
                        System.out.println("=== AMCP INIT: Got agent from Arc container");
                    } else {
                        // Not a CDI bean, instantiate directly
                        System.out.println("=== AMCP INIT: Not in Arc (not available), instantiating directly");
                        agentInstance = agentClass.getDeclaredConstructor().newInstance();
                    }
                    
                    // Check if it's an AbstractMobileAgent using class hierarchy
                    if (AbstractMobileAgent.class.isAssignableFrom(agentInstance.getClass())) {
                        AbstractMobileAgent agent = (AbstractMobileAgent) agentInstance;
                        String agentId = context.registerAgent(agent);
                        System.out.println("=== AMCP INIT: Registered agent: " + agentId);
                        
                        if (config.autoActivate()) {
                            context.activateAgent(agentId);
                            System.out.println("=== AMCP INIT: Activated agent: " + agentId);
                            logger.info("Activated agent: {} ({})", agentId, className);
                        }
                    } else {
                        System.out.println("=== AMCP INIT: ERROR - Not assignable from AbstractMobileAgent: " + className);
                        System.out.println("===   Agent class: " + agentInstance.getClass());
                        System.out.println("===   AbstractMobileAgent class: " + AbstractMobileAgent.class);
                        logger.warn("Class {} is not an AbstractMobileAgent", className);
                    }
                } catch (Exception e) {
                    System.out.println("=== AMCP INIT: ERROR instantiating " + className + ": " + e.getMessage());
                    e.printStackTrace();
                    logger.error("Failed to instantiate agent {}: {}", className, e.getMessage(), e);
                }
            }
            System.out.println("=== AMCP INIT: Finished loading agents. Total registered: " + context.getAgents().size());
            
            // Set the context on the CDI producer
            AmcpContextProducer producer = Arc.container()
                    .instance(AmcpContextProducer.class).get();
            producer.setContext(context);
            
            // Register shutdown hook
            registerShutdownHook(context);
            
            logger.info("AMCP Agent Mesh initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize AMCP Agent Mesh: {}", e.getMessage(), e);
            throw new RuntimeException("AMCP initialization failed", e);
        }
    }

    /**
     * Creates an event broker based on the configuration.
     *
     * @param config the AMCP configuration
     * @return the event broker
     */
    private EventBroker createBroker(AmcpConfig config) {
        String brokerType = config.brokerType().toLowerCase();
        
        switch (brokerType) {
            case "memory":
                logger.info("Creating in-memory event broker");
                return new InMemoryEventBroker();
                
            case "kafka":
                logger.info("Creating Kafka event broker with servers: {}", 
                        config.kafka().bootstrapServers());
                String kafkaInstanceId = System.getenv().getOrDefault("AMCP_INSTANCE_ID", 
                        "amcp-" + UUID.randomUUID().toString().substring(0, 8));
                return new KafkaEventBroker(
                        config.kafka().bootstrapServers(),
                        config.kafka().groupId().orElse("amcp-consumer-group"),
                        kafkaInstanceId
                );
                
            case "nats":
                logger.info("Creating NATS event broker with servers: {}", 
                        config.nats().servers());
                try {
                    Class<?> natsBrokerClass = Class.forName("io.amcp.broker.nats.NatsEventBroker");
                    String natsInstanceId = System.getenv().getOrDefault("AMCP_INSTANCE_ID", 
                            "amcp-" + UUID.randomUUID().toString().substring(0, 8));
                    return (EventBroker) natsBrokerClass
                            .getConstructor(String.class, String.class, String.class)
                            .newInstance(
                                    config.nats().servers(),
                                    config.nats().connectionName(),
                                    natsInstanceId
                            );
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(
                            "NATS broker requested but amcp-broker-nats is not on classpath. " +
                            "Add dependency: <dependency><groupId>io.amcp</groupId>" +
                            "<artifactId>amcp-broker-nats</artifactId></dependency>", e);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create NATS broker", e);
                }
                
            default:
                throw new IllegalArgumentException("Unknown broker type: " + brokerType);
        }
    }

    /**
     * Creates a shutdown handler for the agent context.
     *
     * @param context the agent context
     */
    private void registerShutdownHook(AgentContext context) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (context != null && context.isStarted()) {
                    logger.info("Shutting down AMCP Agent Mesh");
                    context.stop().join();
                    logger.info("AMCP Agent Mesh shutdown complete");
                }
            } catch (Exception e) {
                logger.error("Error during AMCP shutdown: {}", e.getMessage(), e);
            }
        }));
    }
}
