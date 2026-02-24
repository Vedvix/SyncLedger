package com.vedvix.syncledger.payment.gateway.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Response DTO representing a customer in the payment gateway.
 * Gateway-agnostic representation of customer data.
 */
@Data
@Builder
public class GatewayCustomer {
    
    /**
     * Gateway-specific customer ID (e.g., cus_xxx for Stripe)
     */
    private String gatewayCustomerId;
    
    /**
     * Customer email
     */
    private String email;
    
    /**
     * Customer name
     */
    private String name;
    
    /**
     * Customer phone
     */
    private String phone;
    
    /**
     * Default payment method ID (if set)
     */
    private String defaultPaymentMethodId;
    
    /**
     * Additional metadata from gateway
     */
    private Map<String, String> metadata;
    
    /**
     * Timestamp when customer was created (epoch seconds)
     */
    private Long createdAt;
    
    /**
     * Whether the customer was deleted
     */
    @Builder.Default
    private boolean deleted = false;
    
    // ============================================
    // ALIAS METHODS
    // ============================================
    
    /**
     * Alias for gatewayCustomerId
     */
    public String getId() {
        return gatewayCustomerId;
    }
    
    /**
     * Alias for deleted
     */
    public boolean isDeleted() {
        return deleted;
    }
}
