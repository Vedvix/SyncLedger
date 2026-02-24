package com.vedvix.syncledger.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for billing address information.
 * Used when adding or updating billing address for a payment method.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingAddressDTO {
    
    /**
     * Cardholder name.
     */
    @NotBlank(message = "Billing name is required")
    private String name;
    
    /**
     * Address line 1 (street address).
     */
    @NotBlank(message = "Address line 1 is required")
    private String line1;
    
    /**
     * Address line 2 (apartment, suite, etc.) - optional.
     */
    private String line2;
    
    /**
     * City name.
     */
    @NotBlank(message = "City is required")
    private String city;
    
    /**
     * State or province.
     */
    @NotBlank(message = "State is required")
    private String state;
    
    /**
     * Postal or ZIP code.
     */
    @NotBlank(message = "Postal code is required")
    @Size(min = 5, max = 10, message = "Invalid postal code length")
    private String postalCode;
    
    /**
     * Country (2-letter ISO code, e.g., "US").
     */
    @NotBlank(message = "Country is required")
    @Size(min = 2, max = 2, message = "Country must be 2-letter ISO code")
    private String country;
}
