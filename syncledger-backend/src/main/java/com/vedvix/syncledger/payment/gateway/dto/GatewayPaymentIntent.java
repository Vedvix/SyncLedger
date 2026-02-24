package com.vedvix.syncledger.payment.gateway.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Response DTO representing a payment intent from the gateway.
 * Gateway-agnostic representation of payment intent data.
 */
@Data
@Builder
public class GatewayPaymentIntent {
    
    /**
     * Gateway-specific payment intent ID (e.g., pi_xxx for Stripe)
     */
    private String paymentIntentId;
    
    /**
     * Client secret for frontend confirmation
     */
    private String clientSecret;
    
    /**
     * Current status of the payment intent
     * Common values: requires_payment_method, requires_confirmation, 
     * requires_action, processing, succeeded, canceled
     */
    private String status;
    
    /**
     * Amount in smallest currency unit
     */
    private Long amount;
    
    /**
     * Currency code (lowercase)
     */
    private String currency;
    
    /**
     * Payment method ID used (if any)
     */
    private String paymentMethodId;
    
    /**
     * Customer ID associated with this payment
     */
    private String customerId;
    
    /**
     * Failure message if payment failed
     */
    private String failureMessage;
    
    /**
     * Failure code if payment failed
     */
    private String failureCode;
    
    /**
     * Additional metadata from gateway
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
     * Whether this payment was auto-confirmed (test mode).
     * 
     * TRUE: Payment was confirmed automatically using a test payment method.
     *       Frontend should skip Stripe Elements confirmation.
     * FALSE: Normal production flow, frontend must complete payment.
     */
    private boolean autoConfirmed;
    
    /**
     * Check if payment requires additional action (e.g., 3D Secure)
     */
    public boolean requiresAction() {
        return "requires_action".equals(status);
    }
    
    /**
     * Check if payment succeeded
     */
    public boolean isSucceeded() {
        return "succeeded".equals(status);
    }
    
    /**
     * Check if payment requires a payment method
     */
    public boolean requiresPaymentMethod() {
        return "requires_payment_method".equals(status);
    }
    
    /**
     * Alias for paymentIntentId for generic access.
     * @return the paymentIntentId
     */
    public String getId() {
        return paymentIntentId;
    }
    
    /**
     * Returns the amount refunded. 
     * Note: This is a placeholder - actual refund tracking should use Refund entities.
     * @return 0L as default
     */
    public Long getAmountRefunded() {
        return 0L;
    }
}
