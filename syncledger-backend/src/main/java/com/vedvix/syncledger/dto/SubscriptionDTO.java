package com.vedvix.syncledger.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO for Subscription data transfer.
 * 
 * @author vedvix
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDTO {

    private Long id;
    private Long organizationId;
    private String organizationName;
    private String plan;
    private String planDisplayName;
    private String status;
    private String statusDisplayName;

    // Trial info
    private LocalDateTime trialStartDate;
    private LocalDateTime trialEndDate;
    private Long remainingTrialDays;

    // Subscription period
    private LocalDateTime subscriptionStartDate;
    private LocalDateTime subscriptionEndDate;

    // Billing
    private String billingCycle;
    private Long priceCents;
    private String currency;

    // Stripe
    private String stripeCustomerId;
    private String stripeSubscriptionId;

    // Cancellation
    private LocalDateTime cancelledAt;
    private String cancellationReason;

    // Access
    private Boolean hasAccess;

    // Checkout (for upgrade flow)
    private String checkoutUrl;
    private String checkoutSessionId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
