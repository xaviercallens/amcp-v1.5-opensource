package io.amcp.a2a.security;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.runtime.OidcConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * OAuth2 configuration for A2A gateway.
 * Supports multiple OAuth2 providers and token validation.
 */
@ApplicationScoped
public class OAuth2Config {
    
    private static final Logger logger = LoggerFactory.getLogger(OAuth2Config.class);
    
    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    Optional<String> authServerUrl;
    
    @ConfigProperty(name = "quarkus.oidc.client-id")
    Optional<String> clientId;
    
    @ConfigProperty(name = "quarkus.oidc.client-secret")
    Optional<String> clientSecret;
    
    @ConfigProperty(name = "quarkus.oidc.token.audience")
    Optional<String> tokenAudience;
    
    @ConfigProperty(name = "a2a.oauth2.enabled", defaultValue = "false")
    boolean oauth2Enabled;
    
    @ConfigProperty(name = "a2a.oauth2.token-validation-cache-ttl", defaultValue = "300")
    long tokenValidationCacheTtl;
    
    @ConfigProperty(name = "a2a.oauth2.required-scopes", defaultValue = "a2a:send,a2a:receive")
    String requiredScopes;
    
    @ConfigProperty(name = "a2a.oauth2.token-header", defaultValue = "Authorization")
    String tokenHeader;
    
    @ConfigProperty(name = "a2a.oauth2.token-prefix", defaultValue = "Bearer")
    String tokenPrefix;
    
    public boolean isOAuth2Enabled() {
        return oauth2Enabled;
    }
    
    public Optional<String> getAuthServerUrl() {
        return authServerUrl;
    }
    
    public Optional<String> getClientId() {
        return clientId;
    }
    
    public Optional<String> getClientSecret() {
        return clientSecret;
    }
    
    public Optional<String> getTokenAudience() {
        return tokenAudience;
    }
    
    public long getTokenValidationCacheTtl() {
        return tokenValidationCacheTtl;
    }
    
    public String getRequiredScopes() {
        return requiredScopes;
    }
    
    public String getTokenHeader() {
        return tokenHeader;
    }
    
    public String getTokenPrefix() {
        return tokenPrefix;
    }
    
    public void validateConfiguration() {
        if (oauth2Enabled) {
            if (authServerUrl.isEmpty()) {
                logger.warn("OAuth2 enabled but auth-server-url not configured");
            }
            if (clientId.isEmpty()) {
                logger.warn("OAuth2 enabled but client-id not configured");
            }
            logger.info("OAuth2 configured for A2A gateway");
            logger.info("Auth Server: {}", authServerUrl.orElse("not configured"));
            logger.info("Required Scopes: {}", requiredScopes);
        }
    }
}
