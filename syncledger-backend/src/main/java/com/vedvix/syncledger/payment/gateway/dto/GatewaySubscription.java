package com.vedvix.syncledger.payment.gateway.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO representing a subscription from the payment gateway.
 * Gateway-agnostic representation of subscription data.
 */
@Data
@Builder
public class GatewaySubscription {

    /**
     * Gateway-specific subscription ID (e.g., sub_xxx for Stripe)
     */
    private String subscriptionId;

    /**
     * Gateway customer ID associated with the subscription
     */
    private String customerId;

    /**
     * Current status of the subscription.
     * Common values: active, past_due, canceled, unpaid, trialing, incomplete
     */
    private String status;

    /**
     * Price ID for the subscription
     */
    private String priceId;

    /**
     * Current period start timestamp (epoch seconds)
     */
    private Long currentPeriodStart;

    /**
     * Current period end timestamp (epoch seconds)
     */
    private Long currentPeriodEnd;

    /**
     * Whether the subscription will cancel at period end
     */
    private boolean cancelAtPeriodEnd;

    /**
     * Latest invoice ID (if any)
     */
    private String latestInvoiceId;

    /**
     * Default payment method ID
     */
    private String defaultPaymentMethodId;

    /**
     * Whether the operation was successful
     */
    @Builder.Default
    private boolean success = true;

    /**
     * Error message if the operation failed
     */
    private String errorMessage;

    /**
     * Metadata from the subscription
     */
    private Map<String, String> metadata;
}
