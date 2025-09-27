package io.amcp.messaging.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Circuit breaker implementation for Enhanced Kafka EventBroker resilience.
 * 
 * <p>Provides fault tolerance by temporarily blocking operations when failure
 * rate exceeds threshold, allowing the system to recover.</p>
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class CircuitBreaker {
    
    public enum State {
        CLOSED,    // Normal operation
        OPEN,      // Blocking calls due to failures
        HALF_OPEN  // Testing if service has recovered
    }
    
    private static final int DEFAULT_FAILURE_THRESHOLD = 5;
    private static final long DEFAULT_TIMEOUT_MS = 30000; // 30 seconds
    private static final int DEFAULT_SUCCESS_THRESHOLD = 3;
    
    private final int failureThreshold;
    private final long timeoutMs;
    private final int successThreshold;
    
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    
    public CircuitBreaker() {
        this(DEFAULT_FAILURE_THRESHOLD, DEFAULT_TIMEOUT_MS, DEFAULT_SUCCESS_THRESHOLD);
    }
    
    public CircuitBreaker(int failureThreshold, long timeoutMs, int successThreshold) {
        this.failureThreshold = failureThreshold;
        this.timeoutMs = timeoutMs;
        this.successThreshold = successThreshold;
    }
    
    public <T> CompletableFuture<T> execute(Supplier<CompletableFuture<T>> operation) {
        if (!canExecute()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Circuit breaker is OPEN"));
        }
        
        CompletableFuture<T> result = operation.get();
        
        result.whenComplete((value, throwable) -> {
            if (throwable != null) {
                onFailure();
            } else {
                onSuccess();
            }
        });
        
        return result;
    }
    
    public boolean canExecute() {
        State currentState = state.get();
        
        switch (currentState) {
            case CLOSED:
                return true;
                
            case OPEN:
                if (System.currentTimeMillis() - lastFailureTime.get() >= timeoutMs) {
                    // Transition to HALF_OPEN to test if service has recovered
                    if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                        successCount.set(0);
                        return true;
                    }
                }
                return false;
                
            case HALF_OPEN:
                return true;
                
            default:
                return false;
        }
    }
    
    public boolean isOpen() {
        return state.get() == State.OPEN;
    }
    
    public boolean isClosed() {
        return state.get() == State.CLOSED;
    }
    
    public boolean isHalfOpen() {
        return state.get() == State.HALF_OPEN;
    }
    
    private void onSuccess() {
        State currentState = state.get();
        
        if (currentState == State.HALF_OPEN) {
            int currentSuccessCount = successCount.incrementAndGet();
            if (currentSuccessCount >= successThreshold) {
                // Service has recovered, transition to CLOSED
                if (state.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
                    reset();
                }
            }
        } else if (currentState == State.CLOSED) {
            // Reset failure count on success in CLOSED state
            failureCount.set(0);
        }
    }
    
    private void onFailure() {
        lastFailureTime.set(System.currentTimeMillis());
        
        State currentState = state.get();
        
        if (currentState == State.HALF_OPEN) {
            // Failure during recovery test - go back to OPEN
            state.compareAndSet(State.HALF_OPEN, State.OPEN);
        } else if (currentState == State.CLOSED) {
            int currentFailureCount = failureCount.incrementAndGet();
            if (currentFailureCount >= failureThreshold) {
                // Too many failures - open the circuit
                state.compareAndSet(State.CLOSED, State.OPEN);
            }
        }
    }
    
    private void reset() {
        failureCount.set(0);
        successCount.set(0);
        lastFailureTime.set(0);
    }
    
    public State getState() {
        return state.get();
    }
    
    public int getFailureCount() {
        return failureCount.get();
    }
    
    public int getSuccessCount() {
        return successCount.get();
    }
    
    public long getLastFailureTime() {
        return lastFailureTime.get();
    }
    
    public int getFailureThreshold() {
        return failureThreshold;
    }
    
    public long getTimeoutMs() {
        return timeoutMs;
    }
    
    public int getSuccessThreshold() {
        return successThreshold;
    }
    
    /**
     * Manually open the circuit breaker.
     */
    public void open() {
        state.set(State.OPEN);
        lastFailureTime.set(System.currentTimeMillis());
    }
    
    /**
     * Manually close the circuit breaker.
     */
    public void close() {
        state.set(State.CLOSED);
        reset();
    }
    
    /**
     * Force transition to half-open state for testing.
     */
    public void halfOpen() {
        state.set(State.HALF_OPEN);
        successCount.set(0);
    }
    
    @Override
    public String toString() {
        return String.format(
            "CircuitBreaker{state=%s, failures=%d/%d, successes=%d/%d, lastFailure=%d}",
            state.get(),
            failureCount.get(),
            failureThreshold,
            successCount.get(),
            successThreshold,
            lastFailureTime.get()
        );
    }
}