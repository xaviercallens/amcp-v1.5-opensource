package io.amcp.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authentication context for external service integration.
 * Supports OAuth 2.0 tokens, API keys, and other credential types.
 * Java 8 compatible implementation.
 */
public class AuthenticationContext {
    
    public enum CredentialType {
        API_KEY,
        OAUTH_TOKEN,
        BEARER_TOKEN,
        BASIC_AUTH,
        CERTIFICATE,
        CUSTOM
    }
    
    private final Map<CredentialType, String> credentials = new ConcurrentHashMap<CredentialType, String>();
    private final Map<String, String> metadata = new ConcurrentHashMap<String, String>();
    private final String scope;
    private final long expirationTime;
    
    private AuthenticationContext(Builder builder) {
        this.credentials.putAll(builder.credentials);
        this.metadata.putAll(builder.metadata);
        this.scope = builder.scope;
        this.expirationTime = builder.expirationTime;
    }
    
    public String getCredential(CredentialType type) {
        return credentials.get(type);
    }
    
    public boolean hasCredential(CredentialType type) {
        return credentials.containsKey(type) && credentials.get(type) != null;
    }
    
    public String getMetadata(String key) {
        return metadata.get(key);
    }
    
    public String getScope() {
        return scope;
    }
    
    public boolean isExpired() {
        return expirationTime > 0 && System.currentTimeMillis() > expirationTime;
    }
    
    public long getExpirationTime() {
        return expirationTime;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private final Map<CredentialType, String> credentials = new ConcurrentHashMap<CredentialType, String>();
        private final Map<String, String> metadata = new ConcurrentHashMap<String, String>();
        private String scope;
        private long expirationTime = 0;
        
        public Builder credential(CredentialType type, String value) {
            this.credentials.put(type, value);
            return this;
        }
        
        public Builder apiKey(String apiKey) {
            return credential(CredentialType.API_KEY, apiKey);
        }
        
        public Builder oauthToken(String token) {
            return credential(CredentialType.OAUTH_TOKEN, token);
        }
        
        public Builder bearerToken(String token) {
            return credential(CredentialType.BEARER_TOKEN, token);
        }
        
        public Builder basicAuth(String username, String password) {
            String credentials = username + ":" + password;
            String encoded = javax.xml.bind.DatatypeConverter.printBase64Binary(credentials.getBytes());
            return credential(CredentialType.BASIC_AUTH, encoded);
        }
        
        public Builder metadata(String key, String value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public Builder scope(String scope) {
            this.scope = scope;
            return this;
        }
        
        public Builder expiresAt(long expirationTime) {
            this.expirationTime = expirationTime;
            return this;
        }
        
        public AuthenticationContext build() {
            return new AuthenticationContext(this);
        }
    }
}