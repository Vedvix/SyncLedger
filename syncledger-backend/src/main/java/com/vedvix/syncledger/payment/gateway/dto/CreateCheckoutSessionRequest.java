package com.vedvix.syncledger.payment.gateway.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Request DTO for creating a Checkout Session.
 * Gateway-agnostic - works with any provider supporting hosted checkout.
 */
@Data
@Builder
public class CreateCheckoutSessionRequest {

    /**
     * Gateway customer ID (e.g., cus_xxx for Stripe)
     */
    private String gatewayCustomerId;

    /**
     * Price ID from the gateway (e.g., price_xxx for Stripe)
     */
    private String priceId;

    /**
     * Checkout mode: "subscription", "payment", or "setup"
     */
    @Builder.Default
    private String mode = "subscription";

    /**
     * URL to redirect to after successful checkout
     */
    private String successUrl;

    /**
     * URL to redirect to if customer cancels checkout
     */
    private String cancelUrl;

    /**
     * Optional trial period days
     */
    private Integer trialDays;

    /**
     * Optional metadata to attach
     */
    private Map<String, String> metadata;
}
