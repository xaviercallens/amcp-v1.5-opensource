package io.amcp.security;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Authentication context for secure API calls and MCP tool invocations.
 * 
 * <p>This class encapsulates authentication information including OAuth2 tokens,
 * API keys, and other security credentials that need to be propagated across
 * agent operations and tool calls.</p>
 * 
 * @author Xavier Callens
 * @version 1.5.0
 * @since 1.5.0
 */
public class AuthenticationContext implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String tokenType;
    private final String accessToken;
    private final String refreshToken;
    private final Instant expiresAt;
    private final Map<String, String> headers;
    private final Map<String, String> metadata;
    
    private AuthenticationContext(Builder builder) {
        this.tokenType = builder.tokenType;
        this.accessToken = builder.accessToken;
        this.refreshToken = builder.refreshToken;
        this.expiresAt = builder.expiresAt;
        this.headers = new HashMap<>(builder.headers);
        this.metadata = new HashMap<>(builder.metadata);
    }
    
    public String getTokenType() {
        return tokenType;
    }
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public Instant getExpiresAt() {
        return expiresAt;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
    
    public String getAuthorizationHeader() {
        if (tokenType != null && accessToken != null) {
            return tokenType + " " + accessToken;
        }
        return null;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String tokenType = "Bearer";
        private String accessToken;
        private String refreshToken;
        private Instant expiresAt;
        private Map<String, String> headers = new HashMap<>();
        private Map<String, String> metadata = new HashMap<>();
        
        public Builder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }
        
        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }
        
        public Builder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }
        
        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }
        
        public Builder expiresIn(long seconds) {
            this.expiresAt = Instant.now().plusSeconds(seconds);
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
        
        public Builder metadata(String key, String value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public Builder metadata(Map<String, String> metadata) {
            this.metadata.putAll(metadata);
            return this;
        }
        
        public AuthenticationContext build() {
            return new AuthenticationContext(this);
        }
    }
    
    @Override
    public String toString() {
        return "AuthenticationContext{" +
               "tokenType='" + tokenType + '\'' +
               ", accessToken='[REDACTED]'" +
               ", refreshToken='[REDACTED]'" +
               ", expiresAt=" + expiresAt +
               ", headers=" + headers.keySet() +
               ", metadata=" + metadata.keySet() +
               '}';
    }
}