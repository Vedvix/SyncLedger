package com.vedvix.syncledger.payment.gateway.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO representing a refund from the gateway.
 */
@Data
@Builder
public class GatewayRefund {
    
    /**
     * Gateway-specific refund ID (e.g., re_xxx for Stripe)
     */
    private String refundId;
    
    /**
     * Payment intent ID that was refunded
     */
    private String paymentIntentId;
    
    /**
     * Amount refunded in smallest currency unit
     */
    private Long amount;
    
    /**
     * Currency code
     */
    private String currency;
    
    /**
     * Refund status: pending, succeeded, failed, canceled
     */
    private String status;
    
    /**
     * Reason for the refund
     */
    private String reason;
    
    /**
     * Failure reason if refund failed
     */
    private String failureReason;
    
    /**
     * Balance transaction ID for reconciliation
     */
    private String balanceTransactionId;
    
    /**
     * Timestamp when created (epoch seconds)
     */
    private Long createdAt;
    
    /**
     * Whether the operation was successful
     */
    private boolean success;
    
    /**
     * Error message if operation failed
     */
    private String errorMessage;
    
    /**
     * Check if refund succeeded
     */
    public boolean isSucceeded() {
        return "succeeded".equals(status);
    }
    
    /**
     * Alias for refundId for generic access.
     * @return the refundId
     */
    public String getId() {
        return refundId;
    }
}
