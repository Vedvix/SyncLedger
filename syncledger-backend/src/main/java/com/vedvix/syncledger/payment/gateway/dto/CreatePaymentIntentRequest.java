package com.vedvix.syncledger.payment.gateway.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Request DTO for creating a payment intent.
 * Gateway-agnostic - works with any payment provider.
 */
@Data
@Builder
public class CreatePaymentIntentRequest {
    
    /**
     * Amount in smallest currency unit (e.g., cents for USD)
     */
    private Long amountInCents;
    
    /**
     * Amount in decimal form (dollars).
     * Alternative to amountInCents.
     */
    private BigDecimal amount;
    
    /**
     * Three-letter ISO currency code (lowercase)
     */
    private String currency;
    
    /**
     * Gateway customer ID (e.g., cus_xxx for Stripe)
     */
    private String gatewayCustomerId;
    
    /**
     * Optional: Saved payment method ID for charging directly
     */
    private String paymentMethodId;
    
    /**
     * Whether to confirm immediately (for off-session payments)
     */
    private boolean confirmImmediately;
    
    /**
     * Whether this is an off-session payment (no customer present)
     */
    private boolean offSession;
    
    /**
     * Test mode: auto-confirm with test payment method.
     * When true, uses testPaymentMethod to immediately confirm payment.
     * IMPORTANT: Only use in non-production environments!
     */
    private boolean autoConfirmTestMode;
    
    /**
     * Test payment method to use when autoConfirmTestMode is true.
     * Default: pm_card_visa (always succeeds)
     * 
     * Options:
     * - pm_card_visa: Visa success
     * - pm_card_mastercard: Mastercard success
     * - pm_card_declined: Always declines
     * - pm_card_chargeDeclinedInsufficientFunds: Insufficient funds
     */
    private String testPaymentMethod;
    
    /**
     * Description for the payment (shown on customer's statement)
     */
    private String description;
    
    /**
     * Order ID or reference for reconciliation
     */
    private String orderId;
    
    /**
     * Additional metadata to store with the payment
     */
    private Map<String, String> metadata;
    
    /**
     * For marketplace: connected account ID to receive funds
     */
    private String connectedAccountId;
    
    /**
     * For marketplace: application fee amount in cents
     */
    private Long applicationFeeAmount;
    
    /**
     * Capture method for the payment
     */
    private CaptureMethod captureMethod;
    
    /**
     * Capture method enum
     */
    public enum CaptureMethod {
        AUTOMATIC,
        MANUAL
    }
    
    /**
     * Get amount in cents, converting from decimal if needed.
     */
    public Long getAmountInCentsResolved() {
        if (amountInCents != null) {
            return amountInCents;
        }
        if (amount != null) {
            return amount.movePointRight(2).longValue();
        }
        return null;
    }
}
