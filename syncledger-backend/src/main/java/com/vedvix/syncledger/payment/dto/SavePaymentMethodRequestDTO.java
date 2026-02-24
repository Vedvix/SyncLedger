package com.vedvix.syncledger.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for confirming and saving a payment method.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavePaymentMethodRequestDTO {
    
    /**
     * The Stripe PaymentMethod ID returned after SetupIntent confirmation.
     */
    @NotBlank(message = "Payment method ID is required")
    private String paymentMethodId;
    
    /**
     * Whether to set this as the default payment method.
     */
    private boolean setAsDefault;
    
    /**
     * Optional nickname for the card (e.g., "Personal Visa", "Work Card").
     */
    private String nickname;
}
