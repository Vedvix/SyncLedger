package com.vedvix.syncledger.payment.gateway.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Request DTO for creating a setup intent.
 * Used for saving payment methods without charging.
 */
@Data
@Builder
public class CreateSetupIntentRequest {
    
    /**
     * Gateway customer ID
     */
    private String gatewayCustomerId;
    
    /**
     * Customer email (for notifications)
     */
    private String customerEmail;
    
    /**
     * Customer name
     */
    private String customerName;
    
    /**
     * Payment method types to allow (e.g., "card")
     */
    private String[] paymentMethodTypes;
    
    /**
     * Usage type: on_session or off_session
     */
    private String usage;
    
    /**
     * Additional metadata
     */
    private Map<String, String> metadata;
}
