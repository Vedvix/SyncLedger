package com.vedvix.syncledger.model;

/**
 * User roles with their permissions.
 * Role hierarchy: SUPER_ADMIN > ADMIN > APPROVER > VIEWER
 * 
 * @author vedvix
 */
public enum UserRole {
    
    SUPER_ADMIN("Super Admin", "Platform-level access - manages all organizations and users"),
    ADMIN("Organization Admin", "Full access within their organization including user management"),
    APPROVER("Approver", "Can approve/reject invoices and edit data within their organization"),
    VIEWER("Viewer", "Read-only access to view invoices and dashboard within their organization");

    private final String displayName;
    private final String description;

    UserRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this role has higher or equal privilege than another role.
     */
    public boolean hasPrivilege(UserRole requiredRole) {
        return this.ordinal() <= requiredRole.ordinal();
    }

    /**
     * Check if this role is platform-level (Super Admin).
     */
    public boolean isPlatformLevel() {
        return this == SUPER_ADMIN;
    }
}
