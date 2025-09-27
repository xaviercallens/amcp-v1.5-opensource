package io.amcp.connectors.a2a;

/**
 * Exception thrown when A2A protocol operations fail.
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 * @since 1.5.0
 */
public class A2AProtocolException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    public A2AProtocolException(String message) {
        super(message);
    }
    
    public A2AProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}