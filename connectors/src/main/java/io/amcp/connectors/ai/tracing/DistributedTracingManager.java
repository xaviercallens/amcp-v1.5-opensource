package io.amcp.connectors.ai.tracing;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Distributed tracing manager for tracking requests across the orchestration lifecycle.
 * Implements CloudEvents extension attributes for trace propagation.
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class DistributedTracingManager {
    
    // Active traces indexed by traceId
    private final Map<String, TraceContext> activeTraces = new ConcurrentHashMap<>();
    
    // Trace spans for detailed tracking
    private final Map<String, List<TraceSpan>> traceSpans = new ConcurrentHashMap<>();
    
    /**
     * Creates a new trace context for an orchestration request
     */
    public TraceContext createTrace(String orchestrationId, String userQuery) {
        String traceId = generateTraceId();
        
        TraceContext context = new TraceContext(
            traceId,
            orchestrationId,
            userQuery,
            System.currentTimeMillis()
        );
        
        activeTraces.put(traceId, context);
        traceSpans.put(traceId, new ArrayList<>());
        
        // Create root span
        TraceSpan rootSpan = new TraceSpan(
            traceId,
            generateSpanId(),
            null, // no parent
            "orchestration.start",
            System.currentTimeMillis()
        );
        
        traceSpans.get(traceId).add(rootSpan);
        
        return context;
    }
    
    /**
     * Adds a span to an existing trace
     */
    public TraceSpan addSpan(String traceId, String parentSpanId, String operation) {
        if (!activeTraces.containsKey(traceId)) {
            throw new IllegalArgumentException("Trace not found: " + traceId);
        }
        
        TraceSpan span = new TraceSpan(
            traceId,
            generateSpanId(),
            parentSpanId,
            operation,
            System.currentTimeMillis()
        );
        
        traceSpans.get(traceId).add(span);
        
        return span;
    }
    
    /**
     * Completes a span with optional metadata
     */
    public void completeSpan(String traceId, String spanId, Map<String, Object> metadata) {
        List<TraceSpan> spans = traceSpans.get(traceId);
        if (spans == null) {
            return;
        }
        
        for (TraceSpan span : spans) {
            if (span.spanId.equals(spanId)) {
                span.complete(metadata);
                break;
            }
        }
    }
    
    /**
     * Completes a trace
     */
    public void completeTrace(String traceId, boolean success, String result) {
        TraceContext context = activeTraces.get(traceId);
        if (context != null) {
            context.complete(success, result);
        }
    }
    
    /**
     * Gets trace context by ID
     */
    public TraceContext getTrace(String traceId) {
        return activeTraces.get(traceId);
    }
    
    /**
     * Gets all spans for a trace
     */
    public List<TraceSpan> getSpans(String traceId) {
        return traceSpans.getOrDefault(traceId, new ArrayList<>());
    }
    
    /**
     * Injects trace context into CloudEvents extension attributes
     */
    public Map<String, Object> injectTraceContext(String traceId, String spanId) {
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("amcptraceid", traceId);
        extensions.put("amcpspanid", spanId);
        extensions.put("amcptracetimestamp", System.currentTimeMillis());
        return extensions;
    }
    
    /**
     * Extracts trace context from CloudEvents extension attributes
     */
    public TraceInfo extractTraceContext(Map<String, Object> extensions) {
        if (extensions == null) {
            return null;
        }
        
        String traceId = (String) extensions.get("amcptraceid");
        String spanId = (String) extensions.get("amcpspanid");
        Long timestamp = (Long) extensions.get("amcptracetimestamp");
        
        if (traceId != null) {
            return new TraceInfo(traceId, spanId, timestamp);
        }
        
        return null;
    }
    
    /**
     * Generates trace summary for reporting
     */
    public TraceSummary getTraceSummary(String traceId) {
        TraceContext context = activeTraces.get(traceId);
        List<TraceSpan> spans = traceSpans.get(traceId);
        
        if (context == null || spans == null) {
            return null;
        }
        
        long totalDuration = context.endTime > 0 ? 
            context.endTime - context.startTime : 
            System.currentTimeMillis() - context.startTime;
        
        return new TraceSummary(
            traceId,
            context.orchestrationId,
            context.userQuery,
            spans.size(),
            totalDuration,
            context.success,
            context.completed
        );
    }
    
    /**
     * Cleans up old completed traces
     */
    public void cleanupOldTraces(long maxAgeMs) {
        long now = System.currentTimeMillis();
        List<String> toRemove = new ArrayList<>();
        
        for (Map.Entry<String, TraceContext> entry : activeTraces.entrySet()) {
            TraceContext context = entry.getValue();
            if (context.completed && (now - context.endTime) > maxAgeMs) {
                toRemove.add(entry.getKey());
            }
        }
        
        for (String traceId : toRemove) {
            activeTraces.remove(traceId);
            traceSpans.remove(traceId);
        }
    }
    
    private String generateTraceId() {
        return "trace-" + UUID.randomUUID().toString();
    }
    
    private String generateSpanId() {
        return "span-" + UUID.randomUUID().toString().substring(0, 16);
    }
    
    /**
     * Trace context representing an entire orchestration
     */
    public static class TraceContext {
        public final String traceId;
        public final String orchestrationId;
        public final String userQuery;
        public final long startTime;
        public long endTime;
        public boolean completed;
        public boolean success;
        public String result;
        
        public TraceContext(String traceId, String orchestrationId, String userQuery, long startTime) {
            this.traceId = traceId;
            this.orchestrationId = orchestrationId;
            this.userQuery = userQuery;
            this.startTime = startTime;
            this.completed = false;
        }
        
        public void complete(boolean success, String result) {
            this.endTime = System.currentTimeMillis();
            this.completed = true;
            this.success = success;
            this.result = result;
        }
        
        public long getDuration() {
            return endTime > 0 ? endTime - startTime : System.currentTimeMillis() - startTime;
        }
    }
    
    /**
     * Trace span representing a single operation
     */
    public static class TraceSpan {
        public final String traceId;
        public final String spanId;
        public final String parentSpanId;
        public final String operation;
        public final long startTime;
        public long endTime;
        public boolean completed;
        public Map<String, Object> metadata;
        
        public TraceSpan(String traceId, String spanId, String parentSpanId, 
                        String operation, long startTime) {
            this.traceId = traceId;
            this.spanId = spanId;
            this.parentSpanId = parentSpanId;
            this.operation = operation;
            this.startTime = startTime;
            this.completed = false;
            this.metadata = new HashMap<>();
        }
        
        public void complete(Map<String, Object> metadata) {
            this.endTime = System.currentTimeMillis();
            this.completed = true;
            if (metadata != null) {
                this.metadata.putAll(metadata);
            }
        }
        
        public long getDuration() {
            return endTime > 0 ? endTime - startTime : System.currentTimeMillis() - startTime;
        }
    }
    
    /**
     * Extracted trace information from CloudEvents
     */
    public static class TraceInfo {
        public final String traceId;
        public final String spanId;
        public final Long timestamp;
        
        public TraceInfo(String traceId, String spanId, Long timestamp) {
            this.traceId = traceId;
            this.spanId = spanId;
            this.timestamp = timestamp;
        }
    }
    
    /**
     * Summary of a trace for reporting
     */
    public static class TraceSummary {
        public final String traceId;
        public final String orchestrationId;
        public final String userQuery;
        public final int spanCount;
        public final long totalDuration;
        public final boolean success;
        public final boolean completed;
        
        public TraceSummary(String traceId, String orchestrationId, String userQuery,
                           int spanCount, long totalDuration, boolean success, boolean completed) {
            this.traceId = traceId;
            this.orchestrationId = orchestrationId;
            this.userQuery = userQuery;
            this.spanCount = spanCount;
            this.totalDuration = totalDuration;
            this.success = success;
            this.completed = completed;
        }
        
        @Override
        public String toString() {
            return String.format("Trace[%s] orchestration=%s spans=%d duration=%dms success=%b",
                traceId, orchestrationId, spanCount, totalDuration, success);
        }
    }
}
