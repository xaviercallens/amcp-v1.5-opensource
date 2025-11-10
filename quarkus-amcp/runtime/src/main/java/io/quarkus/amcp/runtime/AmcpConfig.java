package io.quarkus.amcp.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

/**
 * Configuration for the AMCP extension.
 */
@ConfigMapping(prefix = "quarkus.amcp")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface AmcpConfig {

    /**
     * The broker type to use (memory, kafka, nats).
     */
    @WithDefault("memory")
    String brokerType();

    /**
     * Kafka configuration.
     */
    KafkaConfig kafka();

    /**
     * NATS configuration.
     */
    NatsConfig nats();

    /**
     * Whether to automatically activate agents on startup.
     */
    @WithDefault("true")
    boolean autoActivate();

    interface KafkaConfig {
        /**
         * Kafka bootstrap servers.
         */
        @WithDefault("localhost:9092")
        String bootstrapServers();

        /**
         * Kafka consumer group ID.
         */
        Optional<String> groupId();

        /**
         * Additional Kafka properties.
         */
        Optional<String> properties();
    }

    interface NatsConfig {
        /**
         * NATS server URLs.
         */
        @WithDefault("nats://localhost:4222")
        String servers();

        /**
         * NATS connection name.
         */
        Optional<String> connectionName();
    }
}
