package com.vedvix.syncledger.payment.gateway.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Request DTO for creating a transfer to a connected account.
 */
@Data
@Builder
public class CreateTransferRequest {
    
    /**
     * Destination connected account ID
     */
    private String destinationAccountId;
    
    /**
     * Amount in smallest currency unit
     */
    private Long amountInCents;
    
    /**
     * Currency code
     */
    private String currency;
    
    /**
     * Description for the transfer
     */
    private String description;
    
    /**
     * Idempotency key to prevent duplicates
     */
    private String idempotencyKey;
    
    /**
     * Additional metadata
     */
    private Map<String, String> metadata;
}
