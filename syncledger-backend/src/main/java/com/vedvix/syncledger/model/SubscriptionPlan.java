package com.vedvix.syncledger.model;

/**
 * Subscription plan tiers.
 * 
 * @author vedvix
 */
public enum SubscriptionPlan {
    TRIAL("Trial", "15-day free trial with full access", 0),
    STARTER("Starter", "For small teams — up to 1,000 invoices/month", 34900),
    PROFESSIONAL("Professional", "For growing teams — up to 5,000 invoices/month", 64900),
    BUSINESS("Business", "For mid-size companies — up to 10,000 invoices/month", 79900),
    ENTERPRISE("Enterprise", "Unlimited usage with dedicated support & SLA", 149900);

    private final String displayName;
    private final String description;
    private final long priceInCents; // monthly price in USD cents

    SubscriptionPlan(String displayName, String description, long priceInCents) {
        this.displayName = displayName;
        this.description = description;
        this.priceInCents = priceInCents;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public long getPriceInCents() {
        return priceInCents;
    }

    public boolean isPaid() {
        return this != TRIAL;
    }
}
