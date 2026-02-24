package com.vedvix.syncledger.payment.gateway.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Request DTO for creating a subscription in the payment gateway.
 * Gateway-agnostic - works with any payment provider.
 */
@Data
@Builder
public class CreateSubscriptionRequest {

    /**
     * Gateway customer ID (e.g., cus_xxx for Stripe)
     */
    private String gatewayCustomerId;

    /**
     * Price ID from the gateway (e.g., price_xxx for Stripe)
     */
    private String priceId;

    /**
     * Optional: number of trial days for the subscription
     */
    private Integer trialDays;

    /**
     * Optional: default payment method ID to use
     */
    private String defaultPaymentMethodId;

    /**
     * Optional: metadata to attach to the subscription
     */
    private Map<String, String> metadata;

    /**
     * Whether to cancel at period end by default
     */
    @Builder.Default
    private boolean cancelAtPeriodEnd = false;
}
