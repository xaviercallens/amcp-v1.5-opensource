package io.amcp.core;

import java.io.Serializable;

/**
 * Delivery options and guarantees for event messaging.
 * 
 * <p>DeliveryOptions specify how events should be delivered through the messaging
 * system, including reliability guarantees, ordering requirements, and timeout
 * settings. This enables different quality-of-service levels based on application
 * requirements.</p>
 * 
 * @author AMCP Development Team
 * @version 1.4.0
 * @since 1.0.0
 */
public final class DeliveryOptions implements Serializable {

    private static final long serialVersionUID = 1L;

    private final DeliveryMode mode;
    private final boolean ordered;
    private final long timeoutMillis;
    private final int maxRetries;
    private final boolean persistent;

    private DeliveryOptions(Builder builder) {
        this.mode = builder.mode != null ? builder.mode : DeliveryMode.AT_LEAST_ONCE;
        this.ordered = builder.ordered;
        this.timeoutMillis = builder.timeoutMillis > 0 ? builder.timeoutMillis : 30000; // 30 seconds default
        this.maxRetries = builder.maxRetries >= 0 ? builder.maxRetries : 3;
        this.persistent = builder.persistent;
    }

    /**
     * Gets the delivery mode.
     * 
     * @return the delivery mode
     */
    public DeliveryMode getMode() {
        return mode;
    }

    /**
     * Checks if ordered delivery is required.
     * 
     * @return true if events must be delivered in order
     */
    public boolean isOrdered() {
        return ordered;
    }

    /**
     * Gets the delivery timeout in milliseconds.
     * 
     * @return timeout in milliseconds
     */
    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    /**
     * Gets the maximum number of delivery retries.
     * 
     * @return maximum retry count
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Checks if persistent delivery is enabled.
     * 
     * @return true if events should be persisted
     */
    public boolean isPersistent() {
        return persistent;
    }

    /**
     * Creates default delivery options.
     * 
     * @return default delivery options
     */
    public static DeliveryOptions defaultOptions() {
        return builder().build();
    }

    /**
     * Creates fire-and-forget delivery options.
     * 
     * @return fire-and-forget options
     */
    public static DeliveryOptions fireAndForget() {
        return builder()
            .mode(DeliveryMode.FIRE_AND_FORGET)
            .maxRetries(0)
            .build();
    }

    /**
     * Creates reliable delivery options.
     * 
     * @return reliable delivery options
     */
    public static DeliveryOptions reliable() {
        return builder()
            .mode(DeliveryMode.EXACTLY_ONCE)
            .persistent(true)
            .maxRetries(5)
            .build();
    }

    /**
     * Creates ordered delivery options.
     * 
     * @return ordered delivery options
     */
    public static DeliveryOptions ordered() {
        return builder()
            .ordered(true)
            .build();
    }

    /**
     * Creates a new builder.
     * 
     * @return new DeliveryOptions builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Event delivery modes defining reliability guarantees.
     */
    public enum DeliveryMode {
        /**
         * Fire and forget - no delivery guarantees.
         */
        FIRE_AND_FORGET,

        /**
         * At most once delivery - may lose messages but no duplicates.
         */
        AT_MOST_ONCE,

        /**
         * At least once delivery - no message loss but may have duplicates.
         */
        AT_LEAST_ONCE,

        /**
         * Exactly once delivery - no loss and no duplicates.
         */
        EXACTLY_ONCE
    }

    /**
     * Builder for DeliveryOptions.
     */
    public static final class Builder {
        private DeliveryMode mode;
        private boolean ordered = false;
        private long timeoutMillis = 30000;
        private int maxRetries = 3;
        private boolean persistent = false;

        private Builder() {}

        public Builder mode(DeliveryMode mode) {
            this.mode = mode;
            return this;
        }

        public Builder ordered(boolean ordered) {
            this.ordered = ordered;
            return this;
        }

        public Builder timeoutMillis(long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder persistent(boolean persistent) {
            this.persistent = persistent;
            return this;
        }

        public DeliveryOptions build() {
            return new DeliveryOptions(this);
        }
    }

}