package io.amcp.security;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Validates JWT tokens and extracts agent identity.
 * 
 * Integrates with MicroProfile JWT to:
 * - Validate token signatures
 * - Check expiration
 * - Extract claims
 * - Create AgentIdentity
 * 
 * Spec Reference: Quarkus AMCP Extension.md §2.5 (Lines 163-165)
 * "signed JWT representing its identity and roles"
 */
public class JWTValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(JWTValidator.class);
    
    private static final String CLAIM_AGENT_ID = "agent_id";
    private static final String CLAIM_AGENT_NAME = "agent_name";
    private static final String CLAIM_AGENT_TYPE = "agent_type";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_PERMISSIONS = "permissions";

    /**
     * Validates a JWT token and creates an AgentIdentity.
     * 
     * @param jwt The JWT token to validate
     * @return AgentIdentity created from token claims
     * @throws SecurityException if token is invalid or expired
     */
    public AgentIdentity validateAndExtract(JsonWebToken jwt) throws SecurityException {
        try {
            // Check if token is present
            if (jwt == null) {
                throw new SecurityException("JWT token is null");
            }
            
            // Check expiration
            if (isExpired(jwt)) {
                throw new SecurityException("JWT token has expired");
            }
            
            // Extract agent identity from claims
            return extractIdentity(jwt);
            
        } catch (Exception e) {
            logger.error("JWT validation failed: {}", e.getMessage());
            throw new SecurityException("Invalid JWT token", e);
        }
    }

    /**
     * Extracts AgentIdentity from JWT claims.
     */
    private AgentIdentity extractIdentity(JsonWebToken jwt) {
        AgentIdentity.Builder builder = AgentIdentity.builder();
        
        // Required claims
        String agentId = jwt.getClaim(CLAIM_AGENT_ID);
        if (agentId == null) {
            agentId = jwt.getSubject(); // Fallback to 'sub' claim
        }
        builder.agentId(agentId);
        
        // Optional claims
        String agentName = jwt.getClaim(CLAIM_AGENT_NAME);
        if (agentName != null) {
            builder.agentName(agentName);
        }
        
        String agentType = jwt.getClaim(CLAIM_AGENT_TYPE);
        if (agentType != null) {
            builder.agentType(agentType);
        }
        
        // Token metadata
        builder.tokenId(jwt.getTokenID())
               .issuer(jwt.getIssuer())
               .issuedAt(jwt.getIssuedAtTime())
               .expiresAt(jwt.getExpirationTime());
        
        // Roles (from standard 'groups' claim or custom 'roles' claim)
        Set<String> roles = extractRoles(jwt);
        builder.roles(roles);
        
        // Permissions (custom claim)
        Set<String> permissions = extractPermissions(jwt);
        builder.permissions(permissions);
        
        AgentIdentity identity = builder.build();
        logger.debug("Extracted identity for agent: {}", identity.getAgentId());
        
        return identity;
    }

    /**
     * Extracts roles from JWT.
     * Checks both 'groups' (MicroProfile standard) and 'roles' (custom) claims.
     */
    private Set<String> extractRoles(JsonWebToken jwt) {
        Set<String> roles = new HashSet<>();
        
        // Standard MicroProfile 'groups' claim
        Set<String> groups = jwt.getGroups();
        if (groups != null) {
            roles.addAll(groups);
        }
        
        // Custom 'roles' claim
        Object rolesClaim = jwt.getClaim(CLAIM_ROLES);
        if (rolesClaim instanceof Iterable) {
            ((Iterable<?>) rolesClaim).forEach(role -> roles.add(role.toString()));
        }
        
        return roles;
    }

    /**
     * Extracts permissions from JWT custom claim.
     */
    private Set<String> extractPermissions(JsonWebToken jwt) {
        Set<String> permissions = new HashSet<>();
        
        Object permClaim = jwt.getClaim(CLAIM_PERMISSIONS);
        if (permClaim instanceof Iterable) {
            ((Iterable<?>) permClaim).forEach(perm -> permissions.add(perm.toString()));
        }
        
        return permissions;
    }

    /**
     * Checks if JWT token is expired.
     */
    private boolean isExpired(JsonWebToken jwt) {
        long expirationTime = jwt.getExpirationTime();
        if (expirationTime == 0) {
            return false; // No expiration set
        }
        return System.currentTimeMillis() / 1000 > expirationTime;
    }

    /**
     * Validates specific role requirement.
     */
    public void requireRole(AgentIdentity identity, String role) throws SecurityException {
        if (!identity.hasRole(role)) {
            throw new SecurityException("Required role not found: " + role);
        }
    }

    /**
     * Validates permission requirement.
     */
    public void requirePermission(AgentIdentity identity, String permission) throws SecurityException {
        if (!identity.hasPermission(permission)) {
            throw new SecurityException("Required permission not found: " + permission);
        }
    }
}
