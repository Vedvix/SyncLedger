package com.vedvix.syncledger.model;

/**
 * Approval workflow actions.
 * 
 * @author vedvix
 */
public enum ApprovalAction {
    
    APPROVED("Approved"),
    REJECTED("Rejected"),
    ESCALATED("Escalated"),
    RETURNED_FOR_REVIEW("Returned for Review");

    private final String displayName;

    ApprovalAction(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
