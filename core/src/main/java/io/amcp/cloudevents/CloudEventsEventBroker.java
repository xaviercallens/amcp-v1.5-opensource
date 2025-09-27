package io.amcp.cloudevents;

import io.amcp.core.Event;
import io.amcp.messaging.EventBroker;
import io.amcp.messaging.impl.InMemoryEventBroker;
import io.amcp.mobility.BrokerMetrics;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * CloudEvents-compliant EventBroker implementation for AMCP Enterprise Edition.
 * 
 * <p>This EventBroker provides full CloudEvents v1.0 specification compliance
 * while maintaining backward compatibility with existing AMCP Event system.
 * All events are automatically validated and can be serialized/deserialized
 * as CloudEvents.</p>
 * 
 * <p><strong>Enterprise Features:</strong></p>
 * <ul>
 *   <li>CloudEvents v1.0 specification compliance</li>
 *   <li>Automatic event validation and conversion</li>
 *   <li>JSON serialization with proper content-types</li>
 *   <li>Extension attribute support</li>
 *   <li>Backward compatibility with AMCP Events</li>
 * </ul>
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class CloudEventsEventBroker implements EventBroker {
    
    private final EventBroker underlying;
    private final CloudEventsAdapter adapter;
    private final boolean strictValidation;
    
    /**
     * Create a CloudEvents EventBroker with default configuration.
     */
    public CloudEventsEventBroker() {
        this(new InMemoryEventBroker(), true);
    }
    
    /**
     * Create a CloudEvents EventBroker wrapping an existing EventBroker.
     * 
     * @param underlying The underlying EventBroker implementation
     * @param strictValidation Whether to enforce strict CloudEvents validation
     */
    public CloudEventsEventBroker(EventBroker underlying, boolean strictValidation) {
        this.underlying = underlying;
        this.adapter = new CloudEventsAdapter(underlying);
        this.strictValidation = strictValidation;
    }
    
    @Override
    public CompletableFuture<Void> start() {
        return underlying.start();
    }
    
    @Override
    public CompletableFuture<Void> stop() {
        return underlying.stop();
    }
    
    @Override
    public boolean isRunning() {
        return underlying.isRunning();
    }
    
    @Override
    public CompletableFuture<Void> publish(Event event) {
        if (strictValidation) {
            // Convert to CloudEvent to validate compliance
            try {
                CloudEvent cloudEvent = adapter.convertToCloudEvent(event);
                cloudEvent.validate();
            } catch (Exception e) {
                return CompletableFuture.failedFuture(
                    new CloudEventValidationException("Event failed CloudEvents validation", e));
            }
        }
        return underlying.publish(event);
    }
    
    /**
     * Publish a CloudEvent directly.
     * 
     * @param cloudEvent The CloudEvent to publish
     * @return CompletableFuture that completes when publication is done
     */
    public CompletableFuture<Void> publishCloudEvent(CloudEvent cloudEvent) {
        return adapter.publishCloudEvent(cloudEvent);
    }
    
    @Override
    public CompletableFuture<Void> subscribe(EventSubscriber subscriber, String topicPattern) {
        if (strictValidation) {
            // Wrap subscriber to validate CloudEvents compliance
            EventSubscriber wrappedSubscriber = new CloudEventsValidatingSubscriber(subscriber, adapter);
            return underlying.subscribe(wrappedSubscriber, topicPattern);
        } else {
            return underlying.subscribe(subscriber, topicPattern);
        }
    }
    
    /**
     * Subscribe to CloudEvents with automatic conversion.
     * 
     * @param topicPattern Topic pattern for subscription
     * @param cloudEventConsumer Consumer for CloudEvents
     * @return CompletableFuture that completes when subscription is established
     */
    public CompletableFuture<Void> subscribeToCloudEvents(
            String topicPattern, 
            Consumer<CloudEvent> cloudEventConsumer) {
        
        EventSubscriber subscriber = new CloudEventConsumerAdapter(cloudEventConsumer, adapter);
        return underlying.subscribe(subscriber, topicPattern);
    }
    
    @Override
    public CompletableFuture<Void> unsubscribe(EventSubscriber subscriber, String topicPattern) {
        return underlying.unsubscribe(subscriber, topicPattern);
    }
    
    @Override
    public BrokerMetrics getMetrics() {
        return underlying.getMetrics();
    }
    
    /**
     * Get the CloudEvents adapter for manual conversions.
     */
    public CloudEventsAdapter getAdapter() {
        return adapter;
    }
    
    /**
     * Get the underlying EventBroker.
     */
    public EventBroker getUnderlying() {
        return underlying;
    }
    
    /**
     * Check if strict validation is enabled.
     */
    public boolean isStrictValidation() {
        return strictValidation;
    }
    
    /**
     * EventSubscriber wrapper that validates CloudEvents compliance.
     */
    private static class CloudEventsValidatingSubscriber implements EventSubscriber {
        private final EventSubscriber delegate;
        private final CloudEventsAdapter adapter;
        
        public CloudEventsValidatingSubscriber(EventSubscriber delegate, CloudEventsAdapter adapter) {
            this.delegate = delegate;
            this.adapter = adapter;
        }
        
        @Override
        public CompletableFuture<Void> handleEvent(Event event) {
            try {
                CloudEvent cloudEvent = adapter.convertToCloudEvent(event);
                cloudEvent.validate();
                return delegate.handleEvent(event);
            } catch (Exception e) {
                // Log validation error but continue processing
                System.err.println("CloudEvents validation warning for event " + 
                    event.getId() + ": " + e.getMessage());
                return delegate.handleEvent(event);
            }
        }
        
        @Override
        public String getSubscriberId() {
            return "cloudevents-" + delegate.getSubscriberId();
        }
    }
    
    /**
     * Adapter to convert CloudEvent consumer into EventSubscriber.
     */
    private static class CloudEventConsumerAdapter implements EventSubscriber {
        private final Consumer<CloudEvent> cloudEventConsumer;
        private final CloudEventsAdapter adapter;
        private final String subscriberId;
        
        public CloudEventConsumerAdapter(Consumer<CloudEvent> cloudEventConsumer, CloudEventsAdapter adapter) {
            this.cloudEventConsumer = cloudEventConsumer;
            this.adapter = adapter;
            this.subscriberId = "cloudevents-consumer-" + System.nanoTime();
        }
        
        @Override
        public CompletableFuture<Void> handleEvent(Event event) {
            return CompletableFuture.runAsync(() -> {
                try {
                    CloudEvent cloudEvent = adapter.convertToCloudEvent(event);
                    cloudEventConsumer.accept(cloudEvent);
                } catch (Exception e) {
                    throw new CloudEventException("Failed to convert event to CloudEvent", e);
                }
            });
        }
        
        @Override
        public String getSubscriberId() {
            return subscriberId;
        }
    }
}