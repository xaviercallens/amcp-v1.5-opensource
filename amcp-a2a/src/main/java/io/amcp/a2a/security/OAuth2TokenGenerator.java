package io.amcp.a2a.security;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Optional;

/**
 * OAuth2 token generator for testing and development.
 * Generates JWT tokens with required claims and scopes.
 */
@ApplicationScoped
public class OAuth2TokenGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(OAuth2TokenGenerator.class);
    
    @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "https://amcp-issuer")
    String issuer;
    
    @ConfigProperty(name = "a2a.oauth2.token-expiration", defaultValue = "3600")
    long tokenExpiration;
    
    @Inject
    OAuth2Config oauth2Config;
    
    /**
     * Generates a JWT token for A2A communication.
     *
     * @param subject the subject (agent ID)
     * @param scopes the required scopes
     * @return JWT token
     */
    public String generateToken(String subject, String scopes) {
        try {
            long now = System.currentTimeMillis() / 1000;
            long expirationTime = now + tokenExpiration;
            
            String token = Jwt.issuer(issuer)
                    .subject(subject)
                    .issuedAt(now)
                    .expiresAt(expirationTime)
                    .claim("scope", scopes)
                    .claim("agent_id", subject)
                    .claim("type", "a2a")
                    .sign();
            
            logger.debug("Generated token for subject: {}", subject);
            return token;
            
        } catch (Exception e) {
            logger.error("Failed to generate token", e);
            throw new RuntimeException("Token generation failed", e);
        }
    }
    
    /**
     * Generates a token with default A2A scopes.
     *
     * @param subject the subject (agent ID)
     * @return JWT token
     */
    public String generateDefaultToken(String subject) {
        return generateToken(subject, oauth2Config.getRequiredScopes());
    }
    
    /**
     * Generates a token with custom scopes.
     *
     * @param subject the subject (agent ID)
     * @param customScopes custom scopes
     * @return JWT token
     */
    public String generateTokenWithScopes(String subject, String customScopes) {
        return generateToken(subject, customScopes);
    }
    
    /**
     * Generates a token for a specific agent with sender scope.
     *
     * @param agentId the agent ID
     * @return JWT token
     */
    public String generateSenderToken(String agentId) {
        return generateToken(agentId, "a2a:send");
    }
    
    /**
     * Generates a token for a specific agent with receiver scope.
     *
     * @param agentId the agent ID
     * @return JWT token
     */
    public String generateReceiverToken(String agentId) {
        return generateToken(agentId, "a2a:receive");
    }
    
    /**
     * Generates a token for a specific agent with both scopes.
     *
     * @param agentId the agent ID
     * @return JWT token
     */
    public String generateBidirectionalToken(String agentId) {
        return generateToken(agentId, "a2a:send a2a:receive");
    }
}
