package com.vedvix.syncledger.model;

/**
 * Invoice processing status values.
 * 
 * @author vedvix
 */
public enum InvoiceStatus {
    
    PENDING("Pending", "Invoice awaiting review"),
    UNDER_REVIEW("Under Review", "Invoice is being reviewed"),
    APPROVED("Approved", "Invoice has been approved"),
    REJECTED("Rejected", "Invoice has been rejected"),
    SYNCED("Synced", "Invoice synced to Sage"),
    SYNC_FAILED("Sync Failed", "Failed to sync to Sage"),
    ARCHIVED("Archived", "Invoice has been archived");

    private final String displayName;
    private final String description;

    InvoiceStatus(String displayName, String description) {
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
