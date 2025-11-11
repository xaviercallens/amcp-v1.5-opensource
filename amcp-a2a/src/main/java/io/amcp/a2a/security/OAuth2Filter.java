package io.amcp.a2a.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

/**
 * OAuth2 filter for A2A gateway endpoints.
 * Validates JWT tokens in Authorization header.
 */
@Provider
@ApplicationScoped
public class OAuth2Filter implements ContainerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(OAuth2Filter.class);
    
    @Inject
    OAuth2Config oauth2Config;
    
    @Inject
    OAuth2TokenValidator tokenValidator;
    
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Skip validation if OAuth2 is disabled
        if (!oauth2Config.isOAuth2Enabled()) {
            logger.debug("OAuth2 disabled, skipping token validation");
            return;
        }
        
        // Skip validation for public endpoints
        String path = requestContext.getUriInfo().getPath();
        if (isPublicEndpoint(path)) {
            logger.debug("Public endpoint, skipping token validation: {}", path);
            return;
        }
        
        // Extract token from Authorization header
        Optional<String> token = extractToken(requestContext);
        
        if (token.isEmpty()) {
            logger.warn("Missing authorization token");
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Missing authorization token\"}")
                    .build()
            );
            return;
        }
        
        // Validate token
        OAuth2TokenValidator.TokenValidationResult result = tokenValidator.validateToken(token.get());
        
        if (!result.isValid()) {
            logger.warn("Invalid token: {}", result.getErrorMessage());
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"" + result.getErrorMessage() + "\"}")
                    .build()
            );
            return;
        }
        
        // Set security context with token claims
        requestContext.setSecurityContext(
            new OAuth2SecurityContext(result.getSubject(), result.getScope())
        );
        
        logger.debug("Token validated for subject: {}", result.getSubject());
    }
    
    /**
     * Extracts token from Authorization header.
     */
    private Optional<String> extractToken(ContainerRequestContext requestContext) {
        String authHeader = requestContext.getHeaderString(oauth2Config.getTokenHeader());
        
        if (authHeader == null || authHeader.isEmpty()) {
            return Optional.empty();
        }
        
        String prefix = oauth2Config.getTokenPrefix();
        if (!authHeader.startsWith(prefix + " ")) {
            return Optional.empty();
        }
        
        return Optional.of(authHeader.substring(prefix.length() + 1));
    }
    
    /**
     * Checks if endpoint is public (doesn't require authentication).
     */
    private boolean isPublicEndpoint(String path) {
        return path.contains("/health") || 
               path.contains("/metrics") ||
               path.contains("/openapi") ||
               path.contains("/swagger");
    }
}
