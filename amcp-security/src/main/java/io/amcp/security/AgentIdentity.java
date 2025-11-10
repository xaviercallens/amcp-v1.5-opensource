package io.amcp.security;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents an agent's identity and security context.
 * 
 * Contains:
 * - Agent ID and name
 * - Security roles (for RBAC)
 * - Permissions
 * - Authentication token reference
 * 
 * Used for authorization decisions and audit logging.
 * Spec Reference: Quarkus AMCP Extension.md §2.5 (Lines 163-164)
 */
public class AgentIdentity {
    
    private final String agentId;
    private final String agentName;
    private final String agentType;
    private final Set<String> roles;
    private final Set<String> permissions;
    private final String tokenId;
    private final String issuer;
    private final long issuedAt;
    private final long expiresAt;

    private AgentIdentity(Builder builder) {
        this.agentId = builder.agentId;
        this.agentName = builder.agentName;
        this.agentType = builder.agentType;
        this.roles = Collections.unmodifiableSet(new HashSet<>(builder.roles));
        this.permissions = Collections.unmodifiableSet(new HashSet<>(builder.permissions));
        this.tokenId = builder.tokenId;
        this.issuer = builder.issuer;
        this.issuedAt = builder.issuedAt;
        this.expiresAt = builder.expiresAt;
    }

    // Getters

    public String getAgentId() {
        return agentId;
    }

    public String getAgentName() {
        return agentName;
    }

    public String getAgentType() {
        return agentType;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public String getTokenId() {
        return tokenId;
    }

    public String getIssuer() {
        return issuer;
    }

    public long getIssuedAt() {
        return issuedAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    // Authorization checks

    public boolean hasRole(String role) {
        return roles.contains(role);
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

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    public boolean isExpired() {
        return expiresAt > 0 && System.currentTimeMillis() > expiresAt;
    }

    @Override
    public String toString() {
        return "AgentIdentity{" +
                "agentId='" + agentId + '\'' +
                ", agentName='" + agentName + '\'' +
                ", roles=" + roles +
                ", expired=" + isExpired() +
                '}';
    }

    // Builder

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String agentId;
        private String agentName;
        private String agentType;
        private Set<String> roles = new HashSet<>();
        private Set<String> permissions = new HashSet<>();
        private String tokenId;
        private String issuer;
        private long issuedAt;
        private long expiresAt;

        public Builder agentId(String agentId) {
            this.agentId = agentId;
            return this;
        }

        public Builder agentName(String agentName) {
            this.agentName = agentName;
            return this;
        }

        public Builder agentType(String agentType) {
            this.agentType = agentType;
            return this;
        }

        public Builder addRole(String role) {
            this.roles.add(role);
            return this;
        }

        public Builder roles(Set<String> roles) {
            this.roles.addAll(roles);
            return this;
        }

        public Builder addPermission(String permission) {
            this.permissions.add(permission);
            return this;
        }

        public Builder permissions(Set<String> permissions) {
            this.permissions.addAll(permissions);
            return this;
        }

        public Builder tokenId(String tokenId) {
            this.tokenId = tokenId;
            return this;
        }

        public Builder issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public Builder issuedAt(long issuedAt) {
            this.issuedAt = issuedAt;
            return this;
        }

        public Builder expiresAt(long expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public AgentIdentity build() {
            if (agentId == null) {
                throw new IllegalStateException("agentId is required");
            }
            return new AgentIdentity(this);
        }
    }
}
