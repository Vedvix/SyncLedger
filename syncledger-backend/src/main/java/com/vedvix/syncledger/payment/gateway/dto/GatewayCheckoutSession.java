package com.vedvix.syncledger.payment.gateway.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO representing a Checkout Session from the payment gateway.
 * Used for Stripe Checkout or equivalent hosted payment pages.
 */
@Data
@Builder
public class GatewayCheckoutSession {

    /**
     * Gateway-specific session ID (e.g., cs_xxx for Stripe)
     */
    private String sessionId;

    /**
     * URL to redirect the customer to for payment
     */
    private String url;

    /**
     * Session status (e.g., open, complete, expired)
     */
    private String status;

    /**
     * Whether the operation was successful
     */
    @Builder.Default
    private boolean success = true;

    /**
     * Error message if the operation failed
     */
    private String errorMessage;
}
