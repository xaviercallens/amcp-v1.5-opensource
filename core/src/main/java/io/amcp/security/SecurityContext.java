package io.amcp.security;

import io.amcp.core.AgentID;

import java.security.Principal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced Security Context for AMCP v1.5 Enterprise Edition
 * 
 * Provides comprehensive security context management including:
 * - Multi-tenant isolation
 * - Role-based access control (RBAC)
 * - Session management
 * - Security audit trail
 * - Dynamic permissions evaluation
 * 
 * Thread-safe implementation supporting high-concurrency environments.
 */
public class SecurityContext implements Principal {
    
    private final String principalId;
    private final AgentID agentId;
    private final String tenantId;
    private final Set<String> roles;
    private final Map<String, String> attributes;
    private final Map<String, Object> permissions;
    private final String sessionId;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final Map<String, Object> securityMetadata;
    private final String authenticationMethod;
    private volatile boolean authenticated;
    private volatile Instant lastAccessTime;
    
    private SecurityContext(Builder builder) {
        this.principalId = builder.principalId;
        this.agentId = builder.agentId;
        this.tenantId = builder.tenantId;
        this.roles = Collections.unmodifiableSet(new HashSet<>(builder.roles));
        this.attributes = Collections.unmodifiableMap(new HashMap<>(builder.attributes));
        this.permissions = Collections.unmodifiableMap(new HashMap<>(builder.permissions));
        this.sessionId = builder.sessionId;
        this.createdAt = builder.createdAt;
        this.expiresAt = builder.expiresAt;
        this.securityMetadata = new ConcurrentHashMap<>(builder.securityMetadata);
        this.authenticationMethod = builder.authenticationMethod;
        this.authenticated = builder.authenticated;
        this.lastAccessTime = Instant.now();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    public String getName() {
        return principalId;
    }
    
    public String getPrincipalId() {
        return principalId;
    }
    
    public AgentID getAgentId() {
        return agentId;
    }
    
    public String getTenantId() {
        return tenantId;
    }
    
    public Set<String> getRoles() {
        return roles;
    }
    
    public Map<String, String> getAttributes() {
        return attributes;
    }
    
    public String getAttribute(String key) {
        return attributes.get(key);
    }
    
    public String getAttribute(String key, String defaultValue) {
        return attributes.getOrDefault(key, defaultValue);
    }
    
    public Map<String, Object> getPermissions() {
        return permissions;
    }
    
    public boolean hasPermission(String permission) {
        return permissions.containsKey(permission) && 
               Boolean.TRUE.equals(permissions.get(permission));
    }
    
    public boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (this.roles.contains(role)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean hasAllRoles(String... roles) {
        for (String role : roles) {
            if (!this.roles.contains(role)) {
                return false;
            }
        }
        return true;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getExpiresAt() {
        return expiresAt;
    }
    
    public Instant getLastAccessTime() {
        return lastAccessTime;
    }
    
    public void updateLastAccessTime() {
        this.lastAccessTime = Instant.now();
    }
    
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
    
    public boolean isAuthenticated() {
        return authenticated && !isExpired();
    }
    
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
    
    public String getAuthenticationMethod() {
        return authenticationMethod;
    }
    
    public Map<String, Object> getSecurityMetadata() {
        return Collections.unmodifiableMap(securityMetadata);
    }
    
    public void addSecurityMetadata(String key, Object value) {
        securityMetadata.put(key, value);
    }
    
    public Object getSecurityMetadata(String key) {
        return securityMetadata.get(key);
    }
    
    /**
     * Check if context has permission for specific resource with action
     */
    public boolean hasResourcePermission(String resource, String action) {
        String permissionKey = resource + ":" + action;
        return hasPermission(permissionKey);
    }
    
    /**
     * Check if context can access tenant-specific resource
     */
    public boolean canAccessTenantResource(String resourceTenantId) {
        return tenantId.equals(resourceTenantId) || hasRole("SUPER_ADMIN");
    }
    
    /**
     * Check if context has administrative privileges
     */
    public boolean isAdmin() {
        return hasAnyRole("ADMIN", "SUPER_ADMIN", "SYSTEM_ADMIN");
    }
    
    /**
     * Check if context has system-level privileges
     */
    public boolean isSystem() {
        return hasAnyRole("SYSTEM", "SYSTEM_ADMIN");
    }
    
    /**
     * Get effective permissions including role-based permissions
     */
    public Set<String> getEffectivePermissions() {
        Set<String> effective = new HashSet<>();
        
        // Add explicit permissions
        permissions.forEach((key, value) -> {
            if (Boolean.TRUE.equals(value)) {
                effective.add(key);
            }
        });
        
        // Add role-based permissions
        for (String role : roles) {
            effective.addAll(getRolePermissions(role));
        }
        
        return effective;
    }
    
    private Set<String> getRolePermissions(String role) {
        // This would typically be loaded from a role configuration
        Map<String, Set<String>> rolePermissions = getRolePermissionsMap();
        return rolePermissions.getOrDefault(role, Collections.emptySet());
    }
    
    private Map<String, Set<String>> getRolePermissionsMap() {
        // Default role permissions - would be configurable in production
        Map<String, Set<String>> rolePermissions = new HashMap<>();
        
        rolePermissions.put("USER", Set.of(
            "event:publish",
            "event:subscribe",
            "agent:create",
            "agent:read"
        ));
        
        rolePermissions.put("ADMIN", Set.of(
            "event:publish",
            "event:subscribe",
            "event:admin",
            "agent:create",
            "agent:read",
            "agent:update",
            "agent:delete",
            "system:monitor",
            "tenant:manage"
        ));
        
        rolePermissions.put("SUPER_ADMIN", Set.of(
            "*:*"  // All permissions
        ));
        
        rolePermissions.put("SYSTEM", Set.of(
            "system:*",
            "event:*",
            "agent:*",
            "tenant:*"
        ));
        
        return rolePermissions;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SecurityContext)) return false;
        SecurityContext that = (SecurityContext) o;
        return Objects.equals(principalId, that.principalId) &&
               Objects.equals(sessionId, that.sessionId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(principalId, sessionId);
    }
    
    @Override
    public String toString() {
        return String.format(
            "SecurityContext{principalId='%s', tenantId='%s', roles=%s, authenticated=%s, expires=%s}",
            principalId, tenantId, roles, authenticated, expiresAt
        );
    }
    
    /**
     * Create security audit entry for this context
     */
    public SecurityAuditEntry createAuditEntry(String action, String resource, boolean success) {
        return SecurityAuditEntry.builder()
            .principalId(principalId)
            .tenantId(tenantId)
            .sessionId(sessionId)
            .action(action)
            .resource(resource)
            .success(success)
            .timestamp(Instant.now())
            .metadata(Map.of(
                "roles", String.join(",", roles),
                "authMethod", authenticationMethod,
                "agentId", agentId != null ? agentId.toString() : "none"
            ))
            .build();
    }
    
    public static class Builder {
        private String principalId;
        private AgentID agentId;
        private String tenantId;
        private Set<String> roles = new HashSet<>();
        private Map<String, String> attributes = new HashMap<>();
        private Map<String, Object> permissions = new HashMap<>();
        private String sessionId = UUID.randomUUID().toString();
        private Instant createdAt = Instant.now();
        private Instant expiresAt;
        private Map<String, Object> securityMetadata = new HashMap<>();
        private String authenticationMethod = "unknown";
        private boolean authenticated = false;
        
        public Builder principalId(String principalId) {
            this.principalId = principalId;
            return this;
        }
        
        public Builder agentId(AgentID agentId) {
            this.agentId = agentId;
            return this;
        }
        
        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }
        
        public Builder roles(String... roles) {
            this.roles.addAll(Arrays.asList(roles));
            return this;
        }
        
        public Builder roles(Collection<String> roles) {
            this.roles.addAll(roles);
            return this;
        }
        
        public Builder role(String role) {
            this.roles.add(role);
            return this;
        }
        
        public Builder attribute(String key, String value) {
            this.attributes.put(key, value);
            return this;
        }
        
        public Builder attributes(Map<String, String> attributes) {
            this.attributes.putAll(attributes);
            return this;
        }
        
        public Builder permission(String permission, boolean granted) {
            this.permissions.put(permission, granted);
            return this;
        }
        
        public Builder permissions(Map<String, Object> permissions) {
            this.permissions.putAll(permissions);
            return this;
        }
        
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
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
        
        public Builder securityMetadata(String key, Object value) {
            this.securityMetadata.put(key, value);
            return this;
        }
        
        public Builder securityMetadata(Map<String, Object> metadata) {
            this.securityMetadata.putAll(metadata);
            return this;
        }
        
        public Builder authenticationMethod(String method) {
            this.authenticationMethod = method;
            return this;
        }
        
        public Builder authenticated(boolean authenticated) {
            this.authenticated = authenticated;
            return this;
        }
        
        public SecurityContext build() {
            if (principalId == null || principalId.trim().isEmpty()) {
                throw new IllegalArgumentException("Principal ID is required");
            }
            if (tenantId == null || tenantId.trim().isEmpty()) {
                throw new IllegalArgumentException("Tenant ID is required");
            }
            
            return new SecurityContext(this);
        }
    }
}