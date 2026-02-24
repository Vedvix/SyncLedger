package com.vedvix.syncledger.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO for payment method details.
 * Contains all non-sensitive information about a saved payment method.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodResponseDTO {
    
    /**
     * Unique identifier for the payment method.
     */
    private UUID uuid;
    
    /**
     * Card brand (visa, mastercard, amex, discover, etc.).
     */
    private String cardBrand;
    
    /**
     * Last four digits of the card number.
     */
    private String cardLastFour;
    
    /**
     * Card expiration month (1-12).
     */
    private Integer expMonth;
    
    /**
     * Card expiration year (4-digit).
     */
    private Integer expYear;
    
    /**
     * Whether this is the default payment method.
     */
    private boolean isDefault;
    
    /**
     * Display name for the card (e.g., "Visa •••• 4242").
     */
    private String displayName;
    
    /**
     * Expiration date display (e.g., "12/27").
     */
    private String expirationDisplay;
    
    /**
     * Whether the card is expired.
     */
    private boolean isExpired;
    
    // Billing Address fields
    
    /**
     * Cardholder name on billing address.
     */
    private String billingName;
    
    /**
     * Billing address line 1.
     */
    private String billingAddressLine1;
    
    /**
     * Billing address line 2 (optional).
     */
    private String billingAddressLine2;
    
    /**
     * Billing city.
     */
    private String billingCity;
    
    /**
     * Billing state/province.
     */
    private String billingState;
    
    /**
     * Billing postal/zip code.
     */
    private String billingPostalCode;
    
    /**
     * Billing country (2-letter ISO code).
     */
    private String billingCountry;
    
    // Address Verification Status
    
    /**
     * Result of address line 1 verification (pass, fail, unavailable, unchecked).
     */
    private String addressVerificationStatus;
    
    /**
     * Result of postal code verification (pass, fail, unavailable, unchecked).
     */
    private String postalCodeVerificationStatus;
    
    /**
     * Result of CVC verification (pass, fail, unavailable, unchecked).
     */
    private String cvcVerificationStatus;
    
    /**
     * Timestamp when the payment method was added.
     */
    private String createdAt;
}
