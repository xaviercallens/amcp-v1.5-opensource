package io.amcp.a2a.security;

import io.quarkus.oidc.runtime.OidcJwtCallerPrincipal;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OAuth2 token validator for A2A messages.
 * Validates JWT tokens and checks required scopes.
 */
@ApplicationScoped
public class OAuth2TokenValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(OAuth2TokenValidator.class);
    
    @Inject
    OAuth2Config oauth2Config;
    
    private final Map<String, TokenValidationResult> tokenCache = new ConcurrentHashMap<>();
    
    /**
     * Validates an OAuth2 token.
     *
     * @param token the JWT token
     * @return validation result with token claims
     */
    public TokenValidationResult validateToken(String token) {
        if (!oauth2Config.isOAuth2Enabled()) {
            return TokenValidationResult.valid(new HashMap<>());
        }
        
        // Check cache first
        TokenValidationResult cached = tokenCache.get(token);
        if (cached != null && !cached.isExpired()) {
            logger.debug("Token validation result from cache");
            return cached;
        }
        
        try {
            // Validate token format
            if (!isValidTokenFormat(token)) {
                logger.warn("Invalid token format");
                return TokenValidationResult.invalid("Invalid token format");
            }
            
            // Parse and validate JWT
            Map<String, Object> claims = parseToken(token);
            
            // Validate required claims
            if (!hasRequiredClaims(claims)) {
                logger.warn("Token missing required claims");
                return TokenValidationResult.invalid("Missing required claims");
            }
            
            // Validate scopes
            if (!hasRequiredScopes(claims)) {
                logger.warn("Token missing required scopes");
                return TokenValidationResult.invalid("Missing required scopes");
            }
            
            // Validate expiration
            if (isTokenExpired(claims)) {
                logger.warn("Token expired");
                return TokenValidationResult.invalid("Token expired");
            }
            
            // Validate audience if configured
            if (oauth2Config.getTokenAudience().isPresent()) {
                if (!hasValidAudience(claims)) {
                    logger.warn("Invalid token audience");
                    return TokenValidationResult.invalid("Invalid audience");
                }
            }
            
            TokenValidationResult result = TokenValidationResult.valid(claims);
            tokenCache.put(token, result);
            
            logger.debug("Token validated successfully");
            return result;
            
        } catch (Exception e) {
            logger.error("Token validation failed", e);
            return TokenValidationResult.invalid(e.getMessage());
        }
    }
    
    /**
     * Validates token format (Bearer token).
     */
    private boolean isValidTokenFormat(String token) {
        return token != null && !token.isEmpty() && token.contains(".");
    }
    
    /**
     * Parses JWT token and extracts claims.
     */
    private Map<String, Object> parseToken(String token) {
        Map<String, Object> claims = new HashMap<>();
        
        // Split JWT
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }
        
        // Decode payload (simplified - in production use proper JWT library)
        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            // Parse JSON payload
            claims.put("payload", payload);
            claims.put("sub", extractClaim(payload, "sub"));
            claims.put("aud", extractClaim(payload, "aud"));
            claims.put("scope", extractClaim(payload, "scope"));
            claims.put("exp", extractClaim(payload, "exp"));
            claims.put("iat", extractClaim(payload, "iat"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse token", e);
        }
        
        return claims;
    }
    
    /**
     * Extracts a claim value from JWT payload.
     */
    private String extractClaim(String payload, String claimName) {
        try {
            int startIdx = payload.indexOf("\"" + claimName + "\"");
            if (startIdx == -1) {
                return null;
            }
            
            int colonIdx = payload.indexOf(":", startIdx);
            int commaIdx = payload.indexOf(",", colonIdx);
            int braceIdx = payload.indexOf("}", colonIdx);
            
            int endIdx = commaIdx > 0 && commaIdx < braceIdx ? commaIdx : braceIdx;
            
            String value = payload.substring(colonIdx + 1, endIdx).trim();
            if (value.startsWith("\"") && value.endsWith("\"")) {
                return value.substring(1, value.length() - 1);
            }
            return value;
        } catch (Exception e) {
            logger.debug("Failed to extract claim: {}", claimName);
            return null;
        }
    }
    
    /**
     * Checks if token has required claims.
     */
    private boolean hasRequiredClaims(Map<String, Object> claims) {
        return claims.containsKey("sub") && 
               claims.containsKey("exp") && 
               claims.containsKey("iat");
    }
    
    /**
     * Checks if token has required scopes.
     */
    private boolean hasRequiredScopes(Map<String, Object> claims) {
        String tokenScopes = (String) claims.get("scope");
        if (tokenScopes == null || tokenScopes.isEmpty()) {
            return false;
        }
        
        String[] requiredScopes = oauth2Config.getRequiredScopes().split(",");
        String[] tokenScopeArray = tokenScopes.split(" ");
        Set<String> tokenScopeSet = new HashSet<>(Arrays.asList(tokenScopeArray));
        
        for (String requiredScope : requiredScopes) {
            if (!tokenScopeSet.contains(requiredScope.trim())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Checks if token is expired.
     */
    private boolean isTokenExpired(Map<String, Object> claims) {
        try {
            String expStr = (String) claims.get("exp");
            if (expStr == null) {
                return true;
            }
            
            long expirationTime = Long.parseLong(expStr) * 1000;
            return System.currentTimeMillis() > expirationTime;
        } catch (Exception e) {
            logger.debug("Failed to check token expiration", e);
            return true;
        }
    }
    
    /**
     * Checks if token has valid audience.
     */
    private boolean hasValidAudience(Map<String, Object> claims) {
        String tokenAudience = (String) claims.get("aud");
        String requiredAudience = oauth2Config.getTokenAudience().orElse("");
        
        if (tokenAudience == null || requiredAudience.isEmpty()) {
            return false;
        }
        
        return tokenAudience.contains(requiredAudience);
    }
    
    /**
     * Clears token cache.
     */
    public void clearCache() {
        tokenCache.clear();
        logger.debug("Token cache cleared");
    }
    
    /**
     * Token validation result.
     */
    public static class TokenValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final Map<String, Object> claims;
        private final long createdAt;
        
        private TokenValidationResult(boolean valid, String errorMessage, Map<String, Object> claims) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.claims = claims;
            this.createdAt = System.currentTimeMillis();
        }
        
        public static TokenValidationResult valid(Map<String, Object> claims) {
            return new TokenValidationResult(true, null, claims);
        }
        
        public static TokenValidationResult invalid(String errorMessage) {
            return new TokenValidationResult(false, errorMessage, new HashMap<>());
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public Map<String, Object> getClaims() {
            return claims;
        }
        
        public String getSubject() {
            return (String) claims.get("sub");
        }
        
        public String getScope() {
            return (String) claims.get("scope");
        }
        
        public boolean isExpired() {
            // Cache expires after 5 minutes
            return System.currentTimeMillis() - createdAt > 300000;
        }
    }
}
