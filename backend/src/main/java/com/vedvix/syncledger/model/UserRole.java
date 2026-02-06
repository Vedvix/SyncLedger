package com.vedvix.syncledger.model;

/**
 * User roles with their permissions.
 * 
 * @author vedvix
 */
public enum UserRole {
    
    ADMIN("Administrator", "Full access to all features including user management"),
    APPROVER("Approver", "Can approve/reject invoices and edit data"),
    VIEWER("Viewer", "Read-only access to view invoices and dashboard");

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
}
