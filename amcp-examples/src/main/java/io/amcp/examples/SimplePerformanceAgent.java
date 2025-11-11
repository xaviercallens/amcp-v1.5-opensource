package io.amcp.examples;

import io.amcp.core.AbstractMobileAgent;
import io.amcp.core.Event;
import io.amcp.core.EventBroker;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple performance testing agent for AMCP v1.6.
 * Tests basic agent lifecycle, messaging, and performance metrics.
 */
@ApplicationScoped
public class SimplePerformanceAgent extends AbstractMobileAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(SimplePerformanceAgent.class);
    
    private final AtomicLong messageCount = new AtomicLong(0);
    private final AtomicLong totalLatency = new AtomicLong(0);
    private final Map<String, Long> messageTimestamps = new HashMap<>();
    private long startTime;
    
    @Override
    public void onActivate() {
        logger.info("🚀 SimplePerformanceAgent activated: {}", getAgentId());
        startTime = System.currentTimeMillis();
    }
    
    @Override
    public void onDeactivate() {
        logger.info("🛑 SimplePerformanceAgent deactivated: {}", getAgentId());
        printPerformanceMetrics();
    }
    
    @Override
    public void onEventReceived(Event event) {
        try {
            long receivedTime = System.currentTimeMillis();
            String messageId = event.getId();
            
            // Record latency
            if (messageTimestamps.containsKey(messageId)) {
                long sentTime = messageTimestamps.get(messageId);
                long latency = receivedTime - sentTime;
                totalLatency.addAndGet(latency);
                messageCount.incrementAndGet();
                
                logger.debug("📨 Message {} received - Latency: {}ms", messageId, latency);
            }
            
            // Send response
            sendResponse(event);
            
        } catch (Exception e) {
            logger.error("Error processing event", e);
        }
    }
    
    /**
     * Sends a test message and records the timestamp.
     */
    public void sendTestMessage(String messageId, String content) {
        try {
            messageTimestamps.put(messageId, System.currentTimeMillis());
            
            Event event = new Event(
                messageId,
                getAgentId(),
                "test-receiver",
                "REQUEST",
                content
            );
            
            logger.debug("📤 Sending test message: {}", messageId);
            // In real scenario, would send via broker
            
        } catch (Exception e) {
            logger.error("Error sending test message", e);
        }
    }
    
    /**
     * Sends a response to received message.
     */
    private void sendResponse(Event event) {
        try {
            Event response = new Event(
                event.getId() + "-response",
                getAgentId(),
                event.getSender(),
                "INFORM",
                "Response to: " + event.getContent()
            );
            
            logger.debug("📤 Sending response: {}", response.getId());
            
        } catch (Exception e) {
            logger.error("Error sending response", e);
        }
    }
    
    /**
     * Prints performance metrics.
     */
    public void printPerformanceMetrics() {
        long uptime = System.currentTimeMillis() - startTime;
        long count = messageCount.get();
        
        logger.info("=== Performance Metrics ===");
        logger.info("Agent ID: {}", getAgentId());
        logger.info("Uptime: {}ms", uptime);
        logger.info("Messages Processed: {}", count);
        
        if (count > 0) {
            long avgLatency = totalLatency.get() / count;
            logger.info("Average Latency: {}ms", avgLatency);
            logger.info("Throughput: {:.2f} msg/sec", (count * 1000.0) / uptime);
        }
        
        logger.info("==========================");
    }
    
    /**
     * Gets current performance metrics.
     */
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        long uptime = System.currentTimeMillis() - startTime;
        long count = messageCount.get();
        
        metrics.put("agentId", getAgentId());
        metrics.put("uptime", uptime);
        metrics.put("messagesProcessed", count);
        
        if (count > 0) {
            metrics.put("averageLatency", totalLatency.get() / count);
            metrics.put("throughput", (count * 1000.0) / uptime);
        }
        
        return metrics;
    }
    
    /**
     * Resets metrics for new test run.
     */
    public void resetMetrics() {
        messageCount.set(0);
        totalLatency.set(0);
        messageTimestamps.clear();
        startTime = System.currentTimeMillis();
        logger.info("Metrics reset");
    }
}
