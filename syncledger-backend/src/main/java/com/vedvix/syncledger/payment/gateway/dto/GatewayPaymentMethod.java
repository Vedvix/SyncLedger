package com.vedvix.syncledger.payment.gateway.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO representing a payment method from the gateway.
 * Contains non-sensitive card details suitable for display.
 */
@Data
@Builder
public class GatewayPaymentMethod {
    
    /**
     * Gateway-specific payment method ID (e.g., pm_xxx for Stripe)
     */
    private String paymentMethodId;
    
    /**
     * Type of payment method: "card", "bank_account", "paypal", etc.
     */
    private String type;
    
    /**
     * Card brand: "visa", "mastercard", "amex", etc.
     */
    private String brand;
    
    /**
     * Last 4 digits of the card
     */
    private String last4;
    
    /**
     * Card expiration month (1-12)
     */
    private Integer expMonth;
    
    /**
     * Card expiration year (4-digit)
     */
    private Integer expYear;
    
    /**
     * Card fingerprint for duplicate detection
     */
    private String fingerprint;
    
    /**
     * Card funding type: "credit", "debit", "prepaid", etc.
     */
    private String funding;
    
    /**
     * Cardholder name
     */
    private String cardholderName;
    
    /**
     * Billing address details
     */
    private GatewayBillingAddress billingAddress;
    
    /**
     * Address line 1 verification result
     */
    private String addressLine1Check;
    
    /**
     * Postal code verification result
     */
    private String postalCodeCheck;
    
    /**
     * CVC verification result
     */
    private String cvcCheck;
    
    /**
     * Country of card issuer (2-letter ISO code)
     */
    private String country;
    
    /**
     * Customer ID this payment method is attached to
     */
    private String customerId;
    
    /**
     * Timestamp when created (epoch seconds)
     */
    private Long createdAt;
    
    /**
     * Whether this is the default payment method
     */
    @Builder.Default
    private boolean isDefault = false;
    
    /**
     * Whether the operation was successful
     */
    private boolean success;
    
    /**
     * Error message if operation failed
     */
    private String errorMessage;
    
    // ============================================
    // ALIAS METHODS FOR CONTROLLER COMPATIBILITY
    // ============================================
    
    /**
     * Alias for paymentMethodId - controller uses getId()
     */
    public String getId() {
        return paymentMethodId;
    }
    
    /**
     * Alias for brand - controller uses getCardBrand()
     */
    public String getCardBrand() {
        return brand;
    }
    
    /**
     * Alias for last4 - controller uses getCardLast4()
     */
    public String getCardLast4() {
        return last4;
    }
    
    /**
     * Alias for expMonth - controller uses getExpiryMonth()
     */
    public Integer getExpiryMonth() {
        return expMonth;
    }
    
    /**
     * Alias for expYear - controller uses getExpiryYear()
     */
    public Integer getExpiryYear() {
        return expYear;
    }
    
    /**
     * Check if card is expired
     */
    public boolean isExpired() {
        if (expMonth == null || expYear == null) return false;
        java.time.YearMonth expiry = java.time.YearMonth.of(expYear, expMonth);
        return java.time.YearMonth.now().isAfter(expiry);
    }
    
    /**
     * Get display name for the card (e.g., "Visa •••• 4242")
     */
    public String getDisplayName() {
        String brandDisplay = brand != null ? 
                brand.substring(0, 1).toUpperCase() + brand.substring(1).toLowerCase() : "Card";
        return brandDisplay + " •••• " + last4;
    }
}
