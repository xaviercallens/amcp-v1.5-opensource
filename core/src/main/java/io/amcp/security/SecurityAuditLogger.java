package io.amcp.security;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.time.format.DateTimeFormatter;

/**
 * Security Audit Logger for AMCP v1.5 Enterprise Edition
 * 
 * Asynchronous, high-performance audit logging system for security events.
 * Features:
 * - Non-blocking audit logging
 * - Structured log format (JSON/text)
 * - Configurable log levels
 * - Automatic log rotation
 * - Compliance reporting
 */
public class SecurityAuditLogger {
    
    private static final Logger logger = Logger.getLogger(SecurityAuditLogger.class.getName());
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_INSTANT;
    
    private final BlockingQueue<SecurityAuditEntry> auditQueue;
    private final ExecutorService loggerExecutor;
    private final boolean structuredLogging;
    private volatile boolean running = true;
    
    public SecurityAuditLogger() {
        this(true, 10000); // Default: structured logging, 10k queue capacity
    }
    
    public SecurityAuditLogger(boolean structuredLogging, int queueCapacity) {
        this.structuredLogging = structuredLogging;
        this.auditQueue = new LinkedBlockingQueue<>(queueCapacity);
        this.loggerExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "security-audit-logger");
            t.setDaemon(true);
            return t;
        });
        
        startAuditProcessor();
    }
    
    /**
     * Log security audit entry (non-blocking)
     */
    public void log(SecurityAuditEntry entry) {
        if (!running) {
            return;
        }
        
        if (!auditQueue.offer(entry)) {
            // Queue is full, log to standard logger as fallback
            logger.warning("Audit queue full, dropping entry: " + entry.toString());
        }
    }
    
    /**
     * Log security audit entry with blocking behavior
     */
    public boolean logBlocking(SecurityAuditEntry entry, long timeout, TimeUnit unit) {
        if (!running) {
            return false;
        }
        
        try {
            return auditQueue.offer(entry, timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * Get current audit queue size
     */
    public int getQueueSize() {
        return auditQueue.size();
    }
    
    /**
     * Check if audit logger is healthy
     */
    public boolean isHealthy() {
        return running && !loggerExecutor.isShutdown() && 
               auditQueue.size() < auditQueue.remainingCapacity() / 2;
    }
    
    /**
     * Shutdown audit logger
     */
    public void shutdown() {
        running = false;
        
        // Process remaining entries
        try {
            loggerExecutor.shutdown();
            if (!loggerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warning("Audit logger didn't shutdown gracefully, forcing shutdown");
                loggerExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            loggerExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Log any remaining entries
        int remaining = auditQueue.size();
        if (remaining > 0) {
            logger.info("Processing " + remaining + " remaining audit entries");
            SecurityAuditEntry entry;
            while ((entry = auditQueue.poll()) != null) {
                writeAuditEntry(entry);
            }
        }
        
        logger.info("Security audit logger shutdown complete");
    }
    
    private void startAuditProcessor() {
        loggerExecutor.submit(() -> {
            while (running || !auditQueue.isEmpty()) {
                try {
                    SecurityAuditEntry entry = auditQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (entry != null) {
                        writeAuditEntry(entry);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.severe("Error processing audit entry: " + e.getMessage());
                }
            }
        });
    }
    
    private void writeAuditEntry(SecurityAuditEntry entry) {
        try {
            String logMessage;
            if (structuredLogging) {
                logMessage = entry.toJson();
            } else {
                logMessage = entry.toLogEntry();
            }
            
            // Route to appropriate log level based on severity
            switch (entry.getSeverity()) {
                case CRITICAL:
                case HIGH:
                    logger.severe(logMessage);
                    break;
                case MEDIUM:
                    logger.warning(logMessage);
                    break;
                case LOW:
                default:
                    logger.info(logMessage);
                    break;
            }
            
            // For production, would also write to:
            // - Security Information and Event Management (SIEM) systems
            // - Audit databases
            // - Compliance reporting systems
            // - Log aggregation services (ELK, Splunk, etc.)
            
        } catch (Exception e) {
            logger.severe("Failed to write audit entry: " + e.getMessage());
        }
    }
}