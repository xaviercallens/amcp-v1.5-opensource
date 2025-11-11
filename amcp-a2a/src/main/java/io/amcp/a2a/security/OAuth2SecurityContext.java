package io.amcp.a2a.security;

import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;

/**
 * OAuth2 security context for A2A requests.
 * Provides access to authenticated user and scopes.
 */
public class OAuth2SecurityContext implements SecurityContext {
    
    private final String subject;
    private final String scopes;
    private final OAuth2Principal principal;
    
    public OAuth2SecurityContext(String subject, String scopes) {
        this.subject = subject;
        this.scopes = scopes;
        this.principal = new OAuth2Principal(subject, scopes);
    }
    
    @Override
    public Principal getUserPrincipal() {
        return principal;
    }
    
    @Override
    public boolean isUserInRole(String role) {
        if (scopes == null || scopes.isEmpty()) {
            return false;
        }
        
        String[] scopeArray = scopes.split(" ");
        for (String scope : scopeArray) {
            if (scope.equals(role)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean isSecure() {
        return true;
    }
    
    @Override
    public String getAuthenticationScheme() {
        return "OAuth2";
    }
    
    /**
     * OAuth2 principal representing authenticated user.
     */
    public static class OAuth2Principal implements Principal {
        
        private final String subject;
        private final String scopes;
        
        public OAuth2Principal(String subject, String scopes) {
            this.subject = subject;
            this.scopes = scopes;
        }
        
        @Override
        public String getName() {
            return subject;
        }
        
        public String getScopes() {
            return scopes;
        }
        
        public boolean hasScope(String scope) {
            if (scopes == null || scopes.isEmpty()) {
                return false;
            }
            
            String[] scopeArray = scopes.split(" ");
            for (String s : scopeArray) {
                if (s.equals(scope)) {
                    return true;
                }
            }
            return false;
        }
    }
}
