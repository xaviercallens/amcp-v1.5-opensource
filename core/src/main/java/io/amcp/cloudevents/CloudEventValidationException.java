package io.amcp.cloudevents;

/**
 * Exception thrown when CloudEvent validation fails.
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class CloudEventValidationException extends CloudEventException {
    
    public CloudEventValidationException(String message) {
        super(message);
    }
    
    public CloudEventValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}