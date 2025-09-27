package io.amcp.security;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

/**
 * AMCP v1.5 Authentication Context.
 * Encapsulates security credentials and context for tool invocations and API calls.
 */
public class AuthenticationContext implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String token;
    private final String tokenType;
    private final long expirationTime;
    private final Map<String, String> headers;
    private final Map<String, Object> properties;
    
    private AuthenticationContext(Builder builder) {
        this.token = builder.token;
        this.tokenType = builder.tokenType;
        this.expirationTime = builder.expirationTime;
        this.headers = new HashMap<>(builder.headers);
        this.properties = new HashMap<>(builder.properties);
    }
    
    public String getToken() {
        return token;
    }
    
    public String getTokenType() {
        return tokenType;
    }
    
    public boolean hasToken() {
        return token != null && !token.trim().isEmpty();
    }
    
    public boolean isExpired() {
        return expirationTime > 0 && System.currentTimeMillis() > expirationTime;
    }
    
    public long getExpirationTime() {
        return expirationTime;
    }
    
    public Map<String, String> getHeaders() {
        return new HashMap<>(headers);
    }
    
    public Map<String, Object> getProperties() {
        return new HashMap<>(properties);
    }
    
    public String getProperty(String key) {
        Object value = properties.get(key);
        return value != null ? value.toString() : null;
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, Class<T> type) {
        Object value = properties.get(key);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return null;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String token;
        private String tokenType = "Bearer";
        private long expirationTime = 0;
        private Map<String, String> headers = new HashMap<>();
        private Map<String, Object> properties = new HashMap<>();
        
        public Builder token(String token) {
            this.token = token;
            return this;
        }
        
        public Builder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }
        
        public Builder expirationTime(long expirationTime) {
            this.expirationTime = expirationTime;
            return this;
        }
        
        public Builder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }
        
        public Builder headers(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }
        
        public Builder property(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }
        
        public Builder properties(Map<String, Object> properties) {
            this.properties.putAll(properties);
            return this;
        }
        
        public AuthenticationContext build() {
            return new AuthenticationContext(this);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthenticationContext that = (AuthenticationContext) o;
        return expirationTime == that.expirationTime &&
               Objects.equals(token, that.token) &&
               Objects.equals(tokenType, that.tokenType) &&
               Objects.equals(headers, that.headers) &&
               Objects.equals(properties, that.properties);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(token, tokenType, expirationTime, headers, properties);
    }
    
    @Override
    public String toString() {
        return "AuthenticationContext{" +
               "tokenType='" + tokenType + '\'' +
               ", hasToken=" + hasToken() +
               ", expired=" + isExpired() +
               ", headers=" + headers.size() +
               ", properties=" + properties.size() +
               '}';
    }
}