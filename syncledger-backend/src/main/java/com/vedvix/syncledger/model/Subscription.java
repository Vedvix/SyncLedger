package com.vedvix.syncledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Subscription entity tracking trial, billing, and plan lifecycle per organization.
 * Each organization has exactly one subscription record.
 * 
 * @author vedvix
 */
@Entity
@Table(name = "subscriptions", indexes = {
    @Index(name = "idx_subscription_status", columnList = "status"),
    @Index(name = "idx_subscription_plan", columnList = "plan"),
    @Index(name = "idx_subscription_trial_end", columnList = "trialEndDate"),
    @Index(name = "idx_subscription_end", columnList = "subscriptionEndDate")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false, unique = true)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SubscriptionPlan plan = SubscriptionPlan.TRIAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.TRIAL;

    // Trial period
    @Column(nullable = false)
    private LocalDateTime trialStartDate;

    @Column(nullable = false)
    private LocalDateTime trialEndDate;

    // Subscription period (null during trial)
    @Column
    private LocalDateTime subscriptionStartDate;

    @Column
    private LocalDateTime subscriptionEndDate;

    // Billing
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private BillingCycle billingCycle = BillingCycle.MONTHLY;

    @Column
    @Builder.Default
    private Long priceCents = 0L;

    @Column(length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(length = 255)
    private String stripeCustomerId;

    @Column(length = 255)
    private String stripeSubscriptionId;

    // Pending upgrade info (set when Stripe Checkout created, cleared on webhook)
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private SubscriptionPlan pendingPlan;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BillingCycle pendingBillingCycle;

    // Cancellation
    @Column
    private LocalDateTime cancelledAt;

    @Column(length = 500)
    private String cancellationReason;

    // Notification tracking flags
    @Column(name = "trial_expiry_notified_3d")
    @Builder.Default
    private Boolean trialExpiryNotified3d = false;

    @Column(name = "trial_expiry_notified_1d")
    @Builder.Default
    private Boolean trialExpiryNotified1d = false;

    @Column(name = "trial_expired_notified")
    @Builder.Default
    private Boolean trialExpiredNotified = false;

    @Column(name = "subscription_expiry_notified_7d")
    @Builder.Default
    private Boolean subscriptionExpiryNotified7d = false;

    @Column(name = "subscription_expiry_notified_3d")
    @Builder.Default
    private Boolean subscriptionExpiryNotified3d = false;

    @Column(name = "subscription_expired_notified")
    @Builder.Default
    private Boolean subscriptionExpiredNotified = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;

    // ==================== Business Methods ====================

    /**
     * Check if trial has expired.
     */
    public boolean isTrialExpired() {
        return status == SubscriptionStatus.TRIAL &&
               trialEndDate != null &&
               LocalDateTime.now().isAfter(trialEndDate);
    }

    /**
     * Check if subscription has expired.
     */
    public boolean isSubscriptionExpired() {
        return subscriptionEndDate != null &&
               LocalDateTime.now().isAfter(subscriptionEndDate);
    }

    /**
     * Check if the organization currently has system access.
     */
    public boolean hasAccess() {
        if (status == SubscriptionStatus.TRIAL) {
            return !isTrialExpired();
        }
        return status.allowsAccess();
    }

    /**
     * Get remaining trial days.
     */
    public long getRemainingTrialDays() {
        if (trialEndDate == null) return 0;
        long days = java.time.Duration.between(LocalDateTime.now(), trialEndDate).toDays();
        return Math.max(0, days);
    }
}
