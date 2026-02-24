package com.vedvix.syncledger.payment.gateway.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Request DTO for creating a refund.
 */
@Data
@Builder
public class CreateRefundRequest {
    
    /**
     * Gateway payment intent ID to refund
     */
    private String paymentIntentId;
    
    /**
     * Amount to refund in smallest currency unit.
     * If null, full refund is assumed.
     */
    private Long amountInCents;
    
    /**
     * Alias for amountInCents for backward compatibility.
     */
    private Long amount;
    
    /**
     * Reason for the refund
     */
    private String reason;
    
    /**
     * Reason code: duplicate, fraudulent, requested_by_customer
     */
    private String reasonCode;
    
    /**
     * Additional metadata
     */
    private Map<String, String> metadata;
    
    /**
     * Get amount in cents, preferring amountInCents over amount.
     */
    public Long getAmountInCentsResolved() {
        return amountInCents != null ? amountInCents : amount;
    }
}
