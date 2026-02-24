package com.vedvix.syncledger.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for SetupIntent creation.
 * Contains the client secret needed by the frontend to confirm the card setup.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetupIntentResponseDTO {
    
    /**
     * Indicates if the SetupIntent was created successfully.
     */
    private boolean success;
    
    /**
     * The Stripe SetupIntent ID (e.g., seti_xxxxx).
     */
    private String setupIntentId;
    
    /**
     * The client secret for frontend confirmation.
     * Used with stripe.confirmCardSetup() in Stripe.js.
     */
    private String clientSecret;
    
    /**
     * The Stripe Customer ID associated with this SetupIntent.
     */
    private String customerId;
    
    /**
     * Error message if the operation failed.
     */
    private String errorMessage;
}
