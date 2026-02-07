package com.vedvix.syncledger.model;

/**
 * Organization status enum.
 * 
 * @author vedvix
 */
public enum OrganizationStatus {
    ACTIVE("Active", "Organization is active and can use the system"),
    INACTIVE("Inactive", "Organization is temporarily disabled"),
    SUSPENDED("Suspended", "Organization is suspended due to billing/policy issues"),
    ONBOARDING("Onboarding", "Organization is being set up");

    private final String displayName;
    private final String description;

    OrganizationStatus(String displayName, String description) {
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
