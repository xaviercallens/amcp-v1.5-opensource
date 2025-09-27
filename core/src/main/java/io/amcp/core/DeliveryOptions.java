package io.amcp.core;

import java.io.Serializable;
import java.time.Duration;

/**
 * Configuration options for event delivery in the AMCP system.
 * 
 * @since AMCP 1.0
 */
public final class DeliveryOptions implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final boolean requireAcknowledgment;
    private final int maxRetries;
    private final Duration timeout;
    private final DeliveryMode deliveryMode;
    private final int priority;
    
    public enum DeliveryMode {
        /**
         * Fire-and-forget delivery - no guarantees
         */
        FIRE_AND_FORGET,
        
        /**
         * At-least-once delivery with possible duplicates
         */
        AT_LEAST_ONCE,
        
        /**
         * Exactly-once delivery (best effort, depends on broker capability)
         */
        EXACTLY_ONCE
    }
    
    /**
     * Default delivery options: no acknowledgment required, fire-and-forget mode.
     */
    public static final DeliveryOptions DEFAULT = new DeliveryOptions();
    
    /**
     * Reliable delivery options: acknowledgment required, at-least-once delivery.
     */
    public static final DeliveryOptions RELIABLE = new DeliveryOptions.Builder()
            .requireAcknowledgment(true)
            .deliveryMode(DeliveryMode.AT_LEAST_ONCE)
            .maxRetries(3)
            .timeout(Duration.ofSeconds(30))
            .build();
    
    /**
     * High priority delivery options for control messages.
     */
    public static final DeliveryOptions HIGH_PRIORITY = new DeliveryOptions.Builder()
            .requireAcknowledgment(true)
            .deliveryMode(DeliveryMode.AT_LEAST_ONCE)
            .priority(10)
            .maxRetries(5)
            .timeout(Duration.ofSeconds(60))
            .build();
    
    /**
     * Builder for creating custom DeliveryOptions.
     */
    public static class Builder {
        private boolean requireAcknowledgment = false;
        private int maxRetries = 0;
        private Duration timeout = Duration.ofSeconds(10);
        private DeliveryMode deliveryMode = DeliveryMode.FIRE_AND_FORGET;
        private int priority = 5;
        
        public Builder requireAcknowledgment(boolean requireAck) {
            this.requireAcknowledgment = requireAck;
            return this;
        }
        
        public Builder maxRetries(int maxRetries) {
            if (maxRetries < 0) {
                throw new IllegalArgumentException("Max retries cannot be negative");
            }
            this.maxRetries = maxRetries;
            return this;
        }
        
        public Builder timeout(Duration timeout) {
            if (timeout == null || timeout.isNegative()) {
                throw new IllegalArgumentException("Timeout must be positive");
            }
            this.timeout = timeout;
            return this;
        }
        
        public Builder deliveryMode(DeliveryMode deliveryMode) {
            this.deliveryMode = deliveryMode != null ? deliveryMode : DeliveryMode.FIRE_AND_FORGET;
            return this;
        }
        
        public Builder priority(int priority) {
            if (priority < 1 || priority > 10) {
                throw new IllegalArgumentException("Priority must be between 1 and 10");
            }
            this.priority = priority;
            return this;
        }
        
        public DeliveryOptions build() {
            return new DeliveryOptions(requireAcknowledgment, maxRetries, timeout, deliveryMode, priority);
        }
    }
    
    /**
     * Default constructor - creates default delivery options.
     */
    public DeliveryOptions() {
        this(false, 0, Duration.ofSeconds(10), DeliveryMode.FIRE_AND_FORGET, 5);
    }
    
    private DeliveryOptions(boolean requireAcknowledgment, int maxRetries, Duration timeout,
                           DeliveryMode deliveryMode, int priority) {
        this.requireAcknowledgment = requireAcknowledgment;
        this.maxRetries = maxRetries;
        this.timeout = timeout;
        this.deliveryMode = deliveryMode;
        this.priority = priority;
    }
    
    public boolean isRequireAcknowledgment() {
        return requireAcknowledgment;
    }
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public Duration getTimeout() {
        return timeout;
    }
    
    public DeliveryMode getDeliveryMode() {
        return deliveryMode;
    }
    
    public int getPriority() {
        return priority;
    }
    
    /**
     * Creates a new DeliveryOptions with modified acknowledgment requirement.
     */
    public DeliveryOptions withRequireAcknowledgment(boolean requireAck) {
        return new DeliveryOptions(requireAck, maxRetries, timeout, deliveryMode, priority);
    }
    
    /**
     * Creates a new DeliveryOptions with modified max retries.
     */
    public DeliveryOptions withMaxRetries(int maxRetries) {
        return new DeliveryOptions(requireAcknowledgment, maxRetries, timeout, deliveryMode, priority);
    }
    
    /**
     * Creates a new DeliveryOptions with modified timeout.
     */
    public DeliveryOptions withTimeout(Duration timeout) {
        return new DeliveryOptions(requireAcknowledgment, maxRetries, timeout, deliveryMode, priority);
    }
    
    /**
     * Creates a new DeliveryOptions with modified delivery mode.
     */
    public DeliveryOptions withDeliveryMode(DeliveryMode deliveryMode) {
        return new DeliveryOptions(requireAcknowledgment, maxRetries, timeout, deliveryMode, priority);
    }
    
    /**
     * Creates a new DeliveryOptions with modified priority.
     */
    public DeliveryOptions withPriority(int priority) {
        return new DeliveryOptions(requireAcknowledgment, maxRetries, timeout, deliveryMode, priority);
    }
    
    @Override
    public String toString() {
        return "DeliveryOptions{" +
                "requireAcknowledgment=" + requireAcknowledgment +
                ", maxRetries=" + maxRetries +
                ", timeout=" + timeout +
                ", deliveryMode=" + deliveryMode +
                ", priority=" + priority +
                '}';
    }
}