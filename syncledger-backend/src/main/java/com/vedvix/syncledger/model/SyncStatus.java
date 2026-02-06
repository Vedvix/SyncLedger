package com.vedvix.syncledger.model;

/**
 * Sage synchronization status values.
 * 
 * @author vedvix
 */
public enum SyncStatus {
    
    PENDING("Pending", "Awaiting synchronization"),
    IN_PROGRESS("In Progress", "Currently syncing"),
    SUCCESS("Success", "Successfully synced"),
    FAILED("Failed", "Sync failed"),
    RETRYING("Retrying", "Retrying after failure");

    private final String displayName;
    private final String description;

    SyncStatus(String displayName, String description) {
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
