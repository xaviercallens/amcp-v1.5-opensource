package io.amcp.cloudevents;

import io.amcp.core.Event;
import io.amcp.messaging.EventBroker;
import io.amcp.messaging.EventSubscriber;
import io.amcp.messaging.impl.InMemoryEventBroker;

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
    public CompletableFuture<Void> subscribe(String topicPattern, EventSubscriber subscriber) {
        if (strictValidation) {
            // Wrap subscriber to validate CloudEvents compliance
            EventSubscriber wrappedSubscriber = event -> {
                try {
                    CloudEvent cloudEvent = adapter.convertToCloudEvent(event);
                    cloudEvent.validate();
                    subscriber.onEvent(event);
                } catch (Exception e) {
                    // Log validation error but continue processing
                    System.err.println("CloudEvents validation warning for event " + 
                        event.getId() + ": " + e.getMessage());
                    subscriber.onEvent(event);
                }
            };
            return underlying.subscribe(topicPattern, wrappedSubscriber);
        } else {
            return underlying.subscribe(topicPattern, subscriber);
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
        
        EventSubscriber subscriber = event -> {
            try {
                CloudEvent cloudEvent = adapter.convertToCloudEvent(event);
                cloudEventConsumer.accept(cloudEvent);
            } catch (Exception e) {
                throw new CloudEventException("Failed to convert event to CloudEvent", e);
            }
        };
        
        return underlying.subscribe(topicPattern, subscriber);
    }
    
    @Override
    public CompletableFuture<Void> unsubscribe(String topicPattern, EventSubscriber subscriber) {
        return underlying.unsubscribe(topicPattern, subscriber);
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
}