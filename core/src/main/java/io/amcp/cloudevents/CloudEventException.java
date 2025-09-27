package io.amcp.cloudevents;

/**
 * Base exception for CloudEvents operations.
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class CloudEventException extends RuntimeException {
    
    public CloudEventException(String message) {
        super(message);
    }
    
    public CloudEventException(String message, Throwable cause) {
        super(message, cause);
    }
}