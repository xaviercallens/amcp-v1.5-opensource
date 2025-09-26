package io.amcp.security;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.HashMap;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicLong;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * AMCP v1.4 OAuth 2.0 Authentication Manager.
 * Provides OAuth 2.0 authentication capabilities for AMCP agents and services.
 */
public class OAuth2Manager {
    
    private static final String TOKEN_TYPE = "Bearer";
    private static final int DEFAULT_ACCESS_TOKEN_LIFETIME = 3600; // 1 hour
    private static final int DEFAULT_REFRESH_TOKEN_LIFETIME = 86400; // 24 hours
    
    private final Map<String, OAuth2Client> registeredClients = new ConcurrentHashMap<>();
    private final Map<String, AccessToken> activeTokens = new ConcurrentHashMap<>();
    private final Map<String, RefreshToken> refreshTokens = new ConcurrentHashMap<>();
    private final Map<String, AuthorizationCode> authCodes = new ConcurrentHashMap<>();
    
    private final AtomicLong tokenCounter = new AtomicLong(0);
    private final SecureRandom secureRandom = new SecureRandom();
    
    private volatile boolean initialized = false;
    
    /**
     * Initialize the OAuth 2.0 manager
     */
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            // Register default clients
            registerDefaultClients();
            this.initialized = true;
        });
    }
    
    /**
     * Register OAuth 2.0 client
     */
    public CompletableFuture<OAuth2Client> registerClient(String clientId, String clientSecret,
                                                         String[] redirectUris, String[] grantTypes,
                                                         String[] scopes) {
        return CompletableFuture.supplyAsync(() -> {
            if (registeredClients.containsKey(clientId)) {
                throw new IllegalArgumentException("Client already registered: " + clientId);
            }
            
            OAuth2Client client = new OAuth2Client(
                clientId,
                clientSecret,
                redirectUris,
                grantTypes,
                scopes
            );
            
            registeredClients.put(clientId, client);
            return client;
        });
    }
    
    /**
     * Authorization Code Grant Flow - Step 1: Generate authorization code
     */
    public CompletableFuture<AuthorizationResponse> authorize(String clientId, String redirectUri,
                                                             String[] scopes, String state,
                                                             String userId) {
        return CompletableFuture.supplyAsync(() -> {
            if (!initialized) {
                throw new IllegalStateException("OAuth2Manager not initialized");
            }
            
            OAuth2Client client = registeredClients.get(clientId);
            if (client == null) {
                return AuthorizationResponse.error("invalid_client", "Unknown client ID");
            }
            
            if (!client.isValidRedirectUri(redirectUri)) {
                return AuthorizationResponse.error("invalid_request", "Invalid redirect URI");
            }
            
            // Generate authorization code
            String code = generateAuthorizationCode();
            
            AuthorizationCode authCode = new AuthorizationCode(
                code,
                clientId,
                userId,
                scopes,
                redirectUri,
                System.currentTimeMillis() + 600000 // 10 minutes expiry
            );
            
            authCodes.put(code, authCode);
            
            return AuthorizationResponse.success(code, state);
        });
    }
    
    /**
     * Authorization Code Grant Flow - Step 2: Exchange code for tokens
     */
    public CompletableFuture<TokenResponse> exchangeCodeForTokens(String clientId, String clientSecret,
                                                                 String code, String redirectUri) {
        return CompletableFuture.supplyAsync(() -> {
            if (!initialized) {
                throw new IllegalStateException("OAuth2Manager not initialized");
            }
            
            // Validate client credentials
            OAuth2Client client = registeredClients.get(clientId);
            if (client == null || !client.validateSecret(clientSecret)) {
                return TokenResponse.error("invalid_client", "Invalid client credentials");
            }
            
            // Validate authorization code
            AuthorizationCode authCode = authCodes.get(code);
            if (authCode == null || authCode.isExpired() || !authCode.getClientId().equals(clientId)) {
                return TokenResponse.error("invalid_grant", "Invalid or expired authorization code");
            }
            
            if (!authCode.getRedirectUri().equals(redirectUri)) {
                return TokenResponse.error("invalid_grant", "Redirect URI mismatch");
            }
            
            // Generate tokens
            String accessToken = generateAccessToken();
            String refreshToken = generateRefreshToken();
            
            long expiresIn = DEFAULT_ACCESS_TOKEN_LIFETIME;
            long expiresAt = System.currentTimeMillis() + (expiresIn * 1000);
            
            AccessToken token = new AccessToken(
                accessToken,
                clientId,
                authCode.getUserId(),
                authCode.getScopes(),
                expiresAt
            );
            
            RefreshToken refresh = new RefreshToken(
                refreshToken,
                clientId,
                authCode.getUserId(),
                authCode.getScopes(),
                System.currentTimeMillis() + (DEFAULT_REFRESH_TOKEN_LIFETIME * 1000)
            );
            
            activeTokens.put(accessToken, token);
            refreshTokens.put(refreshToken, refresh);
            authCodes.remove(code); // One-time use
            
            return TokenResponse.success(accessToken, TOKEN_TYPE, expiresIn, refreshToken, authCode.getScopes());
        });
    }
    
    /**
     * Client Credentials Grant Flow
     */
    public CompletableFuture<TokenResponse> clientCredentialsGrant(String clientId, String clientSecret,
                                                                  String[] scopes) {
        return CompletableFuture.supplyAsync(() -> {
            if (!initialized) {
                throw new IllegalStateException("OAuth2Manager not initialized");
            }
            
            // Validate client credentials
            OAuth2Client client = registeredClients.get(clientId);
            if (client == null || !client.validateSecret(clientSecret)) {
                return TokenResponse.error("invalid_client", "Invalid client credentials");
            }
            
            // Validate requested scopes
            if (!client.hasAllScopes(scopes)) {
                return TokenResponse.error("invalid_scope", "Requested scope exceeds client privileges");
            }
            
            // Generate access token (no refresh token for client credentials)
            String accessToken = generateAccessToken();
            long expiresIn = DEFAULT_ACCESS_TOKEN_LIFETIME;
            long expiresAt = System.currentTimeMillis() + (expiresIn * 1000);
            
            AccessToken token = new AccessToken(
                accessToken,
                clientId,
                null, // No user for client credentials
                scopes,
                expiresAt
            );
            
            activeTokens.put(accessToken, token);
            
            return TokenResponse.success(accessToken, TOKEN_TYPE, expiresIn, null, scopes);
        });
    }
    
    /**
     * Refresh Token Grant Flow
     */
    public CompletableFuture<TokenResponse> refreshAccessToken(String clientId, String clientSecret,
                                                              String refreshToken) {
        return CompletableFuture.supplyAsync(() -> {
            if (!initialized) {
                throw new IllegalStateException("OAuth2Manager not initialized");
            }
            
            // Validate client credentials
            OAuth2Client client = registeredClients.get(clientId);
            if (client == null || !client.validateSecret(clientSecret)) {
                return TokenResponse.error("invalid_client", "Invalid client credentials");
            }
            
            // Validate refresh token
            RefreshToken refresh = refreshTokens.get(refreshToken);
            if (refresh == null || refresh.isExpired() || !refresh.getClientId().equals(clientId)) {
                return TokenResponse.error("invalid_grant", "Invalid or expired refresh token");
            }
            
            // Generate new access token
            String newAccessToken = generateAccessToken();
            long expiresIn = DEFAULT_ACCESS_TOKEN_LIFETIME;
            long expiresAt = System.currentTimeMillis() + (expiresIn * 1000);
            
            AccessToken token = new AccessToken(
                newAccessToken,
                clientId,
                refresh.getUserId(),
                refresh.getScopes(),
                expiresAt
            );
            
            activeTokens.put(newAccessToken, token);
            
            return TokenResponse.success(newAccessToken, TOKEN_TYPE, expiresIn, refreshToken, refresh.getScopes());
        });
    }
    
    /**
     * Validate access token
     */
    public CompletableFuture<TokenValidationResult> validateToken(String accessToken) {
        return CompletableFuture.supplyAsync(() -> {
            if (!initialized) {
                return TokenValidationResult.invalid("OAuth2Manager not initialized");
            }
            
            AccessToken token = activeTokens.get(accessToken);
            if (token == null) {
                return TokenValidationResult.invalid("Token not found");
            }
            
            if (token.isExpired()) {
                activeTokens.remove(accessToken);
                return TokenValidationResult.invalid("Token expired");
            }
            
            return TokenValidationResult.valid(token);
        });
    }
    
    /**
     * Revoke token
     */
    public CompletableFuture<Boolean> revokeToken(String token, String tokenTypeHint) {
        return CompletableFuture.supplyAsync(() -> {
            if (!initialized) return false;
            
            boolean revoked = false;
            
            // Try to revoke as access token
            if (activeTokens.remove(token) != null) {
                revoked = true;
            }
            
            // Try to revoke as refresh token
            if (refreshTokens.remove(token) != null) {
                revoked = true;
            }
            
            return revoked;
        });
    }
    
    /**
     * Get token introspection information
     */
    public CompletableFuture<TokenIntrospectionResult> introspectToken(String token) {
        return CompletableFuture.supplyAsync(() -> {
            if (!initialized) {
                return TokenIntrospectionResult.inactive();
            }
            
            AccessToken accessToken = activeTokens.get(token);
            if (accessToken != null && !accessToken.isExpired()) {
                return TokenIntrospectionResult.active(accessToken);
            }
            
            RefreshToken refreshToken = refreshTokens.get(token);
            if (refreshToken != null && !refreshToken.isExpired()) {
                return TokenIntrospectionResult.active(refreshToken);
            }
            
            return TokenIntrospectionResult.inactive();
        });
    }
    
    /**
     * Shutdown OAuth2 manager
     */
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            this.initialized = false;
            activeTokens.clear();
            refreshTokens.clear();
            authCodes.clear();
            registeredClients.clear();
        });
    }
    
    private void registerDefaultClients() {
        // Register default AMCP system client
        OAuth2Client systemClient = new OAuth2Client(
            "amcp-system",
            "system-secret-" + System.currentTimeMillis(),
            new String[]{"urn:ietf:wg:oauth:2.0:oob"},
            new String[]{"client_credentials", "authorization_code", "refresh_token"},
            new String[]{"agent:migrate", "agent:control", "system:admin"}
        );
        registeredClients.put(systemClient.getClientId(), systemClient);
        
        // Register default agent client
        OAuth2Client agentClient = new OAuth2Client(
            "amcp-agent",
            "agent-secret-" + System.currentTimeMillis(),
            new String[]{"urn:ietf:wg:oauth:2.0:oob"},
            new String[]{"client_credentials"},
            new String[]{"agent:migrate", "agent:communicate"}
        );
        registeredClients.put(agentClient.getClientId(), agentClient);
    }
    
    private String generateAuthorizationCode() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    private String generateAccessToken() {
        long tokenId = tokenCounter.incrementAndGet();
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return "amcp_at_" + tokenId + "_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    private String generateRefreshToken() {
        long tokenId = tokenCounter.incrementAndGet();
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return "amcp_rt_" + tokenId + "_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    // Inner classes for OAuth2 data structures
    
    public static class OAuth2Client {
        private final String clientId;
        private final String clientSecret;
        private final String[] redirectUris;
        private final String[] grantTypes;
        private final String[] scopes;
        
        public OAuth2Client(String clientId, String clientSecret, String[] redirectUris,
                           String[] grantTypes, String[] scopes) {
            this.clientId = clientId;
            this.clientSecret = clientSecret;
            this.redirectUris = redirectUris != null ? redirectUris : new String[0];
            this.grantTypes = grantTypes != null ? grantTypes : new String[0];
            this.scopes = scopes != null ? scopes : new String[0];
        }
        
        public String getClientId() { return clientId; }
        public String getClientSecret() { return clientSecret; }
        public String[] getRedirectUris() { return redirectUris.clone(); }
        public String[] getGrantTypes() { return grantTypes.clone(); }
        public String[] getScopes() { return scopes.clone(); }
        
        public boolean validateSecret(String secret) {
            return clientSecret != null && clientSecret.equals(secret);
        }
        
        public boolean isValidRedirectUri(String redirectUri) {
            if (redirectUris.length == 0) return true; // Allow any for demo
            for (String uri : redirectUris) {
                if (uri.equals(redirectUri)) return true;
            }
            return false;
        }
        
        public boolean hasAllScopes(String[] requestedScopes) {
            if (requestedScopes == null || requestedScopes.length == 0) return true;
            for (String requestedScope : requestedScopes) {
                boolean found = false;
                for (String allowedScope : scopes) {
                    if (allowedScope.equals(requestedScope)) {
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
            return true;
        }
    }
    
    public static class AccessToken {
        private final String token;
        private final String clientId;
        private final String userId;
        private final String[] scopes;
        private final long expiresAt;
        
        public AccessToken(String token, String clientId, String userId, String[] scopes, long expiresAt) {
            this.token = token;
            this.clientId = clientId;
            this.userId = userId;
            this.scopes = scopes != null ? scopes : new String[0];
            this.expiresAt = expiresAt;
        }
        
        public String getToken() { return token; }
        public String getClientId() { return clientId; }
        public String getUserId() { return userId; }
        public String[] getScopes() { return scopes.clone(); }
        public long getExpiresAt() { return expiresAt; }
        public boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
    }
    
    public static class RefreshToken {
        private final String token;
        private final String clientId;
        private final String userId;
        private final String[] scopes;
        private final long expiresAt;
        
        public RefreshToken(String token, String clientId, String userId, String[] scopes, long expiresAt) {
            this.token = token;
            this.clientId = clientId;
            this.userId = userId;
            this.scopes = scopes != null ? scopes : new String[0];
            this.expiresAt = expiresAt;
        }
        
        public String getToken() { return token; }
        public String getClientId() { return clientId; }
        public String getUserId() { return userId; }
        public String[] getScopes() { return scopes.clone(); }
        public long getExpiresAt() { return expiresAt; }
        public boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
    }
    
    public static class AuthorizationCode {
        private final String code;
        private final String clientId;
        private final String userId;
        private final String[] scopes;
        private final String redirectUri;
        private final long expiresAt;
        
        public AuthorizationCode(String code, String clientId, String userId, String[] scopes,
                               String redirectUri, long expiresAt) {
            this.code = code;
            this.clientId = clientId;
            this.userId = userId;
            this.scopes = scopes != null ? scopes : new String[0];
            this.redirectUri = redirectUri;
            this.expiresAt = expiresAt;
        }
        
        public String getCode() { return code; }
        public String getClientId() { return clientId; }
        public String getUserId() { return userId; }
        public String[] getScopes() { return scopes.clone(); }
        public String getRedirectUri() { return redirectUri; }
        public long getExpiresAt() { return expiresAt; }
        public boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
    }
    
    public static class AuthorizationResponse {
        private final boolean success;
        private final String code;
        private final String state;
        private final String error;
        private final String errorDescription;
        
        private AuthorizationResponse(boolean success, String code, String state, String error, String errorDescription) {
            this.success = success;
            this.code = code;
            this.state = state;
            this.error = error;
            this.errorDescription = errorDescription;
        }
        
        public static AuthorizationResponse success(String code, String state) {
            return new AuthorizationResponse(true, code, state, null, null);
        }
        
        public static AuthorizationResponse error(String error, String errorDescription) {
            return new AuthorizationResponse(false, null, null, error, errorDescription);
        }
        
        public boolean isSuccess() { return success; }
        public String getCode() { return code; }
        public String getState() { return state; }
        public String getError() { return error; }
        public String getErrorDescription() { return errorDescription; }
    }
    
    public static class TokenResponse {
        private final boolean success;
        private final String accessToken;
        private final String tokenType;
        private final long expiresIn;
        private final String refreshToken;
        private final String[] scopes;
        private final String error;
        private final String errorDescription;
        
        private TokenResponse(boolean success, String accessToken, String tokenType, long expiresIn,
                            String refreshToken, String[] scopes, String error, String errorDescription) {
            this.success = success;
            this.accessToken = accessToken;
            this.tokenType = tokenType;
            this.expiresIn = expiresIn;
            this.refreshToken = refreshToken;
            this.scopes = scopes;
            this.error = error;
            this.errorDescription = errorDescription;
        }
        
        public static TokenResponse success(String accessToken, String tokenType, long expiresIn,
                                          String refreshToken, String[] scopes) {
            return new TokenResponse(true, accessToken, tokenType, expiresIn, refreshToken, scopes, null, null);
        }
        
        public static TokenResponse error(String error, String errorDescription) {
            return new TokenResponse(false, null, null, 0, null, null, error, errorDescription);
        }
        
        public boolean isSuccess() { return success; }
        public String getAccessToken() { return accessToken; }
        public String getTokenType() { return tokenType; }
        public long getExpiresIn() { return expiresIn; }
        public String getRefreshToken() { return refreshToken; }
        public String[] getScopes() { return scopes != null ? scopes.clone() : new String[0]; }
        public String getError() { return error; }
        public String getErrorDescription() { return errorDescription; }
    }
    
    public static class TokenValidationResult {
        private final boolean valid;
        private final AccessToken token;
        private final String error;
        
        private TokenValidationResult(boolean valid, AccessToken token, String error) {
            this.valid = valid;
            this.token = token;
            this.error = error;
        }
        
        public static TokenValidationResult valid(AccessToken token) {
            return new TokenValidationResult(true, token, null);
        }
        
        public static TokenValidationResult invalid(String error) {
            return new TokenValidationResult(false, null, error);
        }
        
        public boolean isValid() { return valid; }
        public AccessToken getToken() { return token; }
        public String getError() { return error; }
    }
    
    public static class TokenIntrospectionResult {
        private final boolean active;
        private final String clientId;
        private final String userId;
        private final String[] scopes;
        private final long expiresAt;
        
        private TokenIntrospectionResult(boolean active, String clientId, String userId, String[] scopes, long expiresAt) {
            this.active = active;
            this.clientId = clientId;
            this.userId = userId;
            this.scopes = scopes;
            this.expiresAt = expiresAt;
        }
        
        public static TokenIntrospectionResult active(AccessToken token) {
            return new TokenIntrospectionResult(true, token.getClientId(), token.getUserId(), 
                                              token.getScopes(), token.getExpiresAt());
        }
        
        public static TokenIntrospectionResult active(RefreshToken token) {
            return new TokenIntrospectionResult(true, token.getClientId(), token.getUserId(), 
                                              token.getScopes(), token.getExpiresAt());
        }
        
        public static TokenIntrospectionResult inactive() {
            return new TokenIntrospectionResult(false, null, null, null, 0);
        }
        
        public boolean isActive() { return active; }
        public String getClientId() { return clientId; }
        public String getUserId() { return userId; }
        public String[] getScopes() { return scopes != null ? scopes.clone() : new String[0]; }
        public long getExpiresAt() { return expiresAt; }
    }
}