package com.vedvix.syncledger.payment.gateway.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO representing a transfer to a connected account.
 */
@Data
@Builder
public class GatewayTransfer {
    
    /**
     * Gateway-specific transfer ID (e.g., tr_xxx for Stripe)
     */
    private String transferId;
    
    /**
     * Destination account ID
     */
    private String destinationAccountId;
    
    /**
     * Amount transferred in smallest currency unit
     */
    private Long amount;
    
    /**
     * Currency code
     */
    private String currency;
    
    /**
     * Transfer status
     */
    private String status;
    
    /**
     * Description
     */
    private String description;
    
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
    
    // ============================================
    // ALIAS METHODS
    // ============================================
    
    /**
     * Alias for transferId
     */
    public String getId() {
        return transferId;
    }
    
    /**
     * Factory method for success
     */
    public static GatewayTransfer success(String transferId, String balanceTransactionId) {
        return GatewayTransfer.builder()
                .transferId(transferId)
                .balanceTransactionId(balanceTransactionId)
                .success(true)
                .build();
    }
    
    /**
     * Factory method for failure
     */
    public static GatewayTransfer failure(String errorMessage) {
        return GatewayTransfer.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
