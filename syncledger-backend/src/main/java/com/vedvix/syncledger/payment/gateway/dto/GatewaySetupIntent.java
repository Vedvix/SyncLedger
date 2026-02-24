package com.vedvix.syncledger.payment.gateway.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Response DTO representing a setup intent from the gateway.
 * Used for saving payment methods without charging.
 */
@Data
@Builder
public class GatewaySetupIntent {
    
    /**
     * Gateway-specific setup intent ID (e.g., seti_xxx for Stripe)
     */
    private String setupIntentId;
    
    /**
     * Client secret for frontend confirmation
     */
    private String clientSecret;
    
    /**
     * Current status of the setup intent
     * Common values: requires_payment_method, requires_confirmation,
     * requires_action, processing, succeeded, canceled
     */
    private String status;
    
    /**
     * Customer ID associated with this setup intent
     */
    private String customerId;
    
    /**
     * Payment method ID that was created/attached (after success)
     */
    private String paymentMethodId;
    
    /**
     * Additional metadata
     */
    private Map<String, String> metadata;
    
    /**
     * Whether the operation was successful
     */
    private boolean success;
    
    /**
     * Error message if operation failed
     */
    private String errorMessage;
    
    /**
     * Check if setup succeeded
     */
    public boolean isSucceeded() {
        return "succeeded".equals(status);
    }
    
    /**
     * Alias for setupIntentId for generic access.
     * @return the setupIntentId
     */
    public String getId() {
        return setupIntentId;
    }
}
