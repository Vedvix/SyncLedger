package com.vedvix.syncledger.model;

/**
 * Subscription lifecycle statuses.
 * 
 * @author vedvix
 */
public enum SubscriptionStatus {
    TRIAL("Trial", "Organization is in free trial period"),
    ACTIVE("Active", "Subscription is active and paid"),
    PAST_DUE("Past Due", "Payment failed, in grace period"),
    CANCELLED("Cancelled", "Subscription cancelled by user"),
    EXPIRED("Expired", "Trial or subscription has expired"),
    SUSPENDED("Suspended", "Suspended by admin or due to policy violation");

    private final String displayName;
    private final String description;

    SubscriptionStatus(String displayName, String description) {
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
     * Check if the subscription allows system access.
     */
    public boolean allowsAccess() {
        return this == TRIAL || this == ACTIVE || this == PAST_DUE;
    }
}
