package io.amcp.cli;

/**
 * Represents the result of a CLI command execution
 */
public class CommandResult {
    
    public enum Type {
        SUCCESS,
        ERROR,
        WARNING,
        INFO
    }
    
    private final Type type;
    private final String message;
    private final Object data;
    
    private CommandResult(Type type, String message, Object data) {
        this.type = type;
        this.message = message;
        this.data = data;
    }
    
    public static CommandResult success(String message) {
        return new CommandResult(Type.SUCCESS, message, null);
    }
    
    public static CommandResult success(String message, Object data) {
        return new CommandResult(Type.SUCCESS, message, data);
    }
    
    public static CommandResult error(String message) {
        return new CommandResult(Type.ERROR, message, null);
    }
    
    public static CommandResult error(String message, Object data) {
        return new CommandResult(Type.ERROR, message, data);
    }
    
    public static CommandResult warning(String message) {
        return new CommandResult(Type.WARNING, message, null);
    }
    
    public static CommandResult warning(String message, Object data) {
        return new CommandResult(Type.WARNING, message, data);
    }
    
    public static CommandResult info(String message) {
        return new CommandResult(Type.INFO, message, null);
    }
    
    public static CommandResult info(String message, Object data) {
        return new CommandResult(Type.INFO, message, data);
    }
    
    public Type getType() {
        return type;
    }
    
    public String getMessage() {
        return message;
    }
    
    public Object getData() {
        return data;
    }
    
    public boolean isSuccess() {
        return type == Type.SUCCESS;
    }
    
    public boolean isError() {
        return type == Type.ERROR;
    }
    
    public boolean isWarning() {
        return type == Type.WARNING;
    }
    
    public boolean isInfo() {
        return type == Type.INFO;
    }
}